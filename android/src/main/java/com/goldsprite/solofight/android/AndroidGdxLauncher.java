package com.goldsprite.solofight.android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.goldsprite.GdxLauncher;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.android.AndroidScriptCompiler;
import com.goldsprite.gdengine.screens.ScreenManager;

import java.util.HashMap;
import java.util.Map;

public class AndroidGdxLauncher extends AndroidApplication {
	private static AndroidGdxLauncher ctx;

	// [新增] 权限工具
	private PermissionUtils permissionUtils;

	// ... (保留原有的 Layout 变量: rootFrame, overlayLayer 等) ...
	// 略去 Layout 变量定义，保持原样即可
	private final float HEIGHT_RATIO_LANDSCAPE = 0.45f;
	private final float HEIGHT_RATIO_PORTRAIT = 0.35f;
	private final int padding = -16;
	private final String[][] terminalLayout = {
		{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Del"},
		{"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"},
		{"Esc", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter"},
		{"Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "↑"},
		{"Ctrl", "Alt", "Sym", "Space", "←", "↓", "→", "Hide"}
	};
	private final Map<String, Integer> keyMap = new HashMap<>();
	private FrameLayout rootFrame;
	private RelativeLayout overlayLayer;
	private LinearLayout keyboardContainer;
	private Button floatingToggleBtn;
	private View gameView;
	private boolean isKeyboardVisible = false;
	private int screenWidth, screenHeight;

	public static AndroidGdxLauncher getCtx() { return ctx; }

	// [新增] 持有 Launcher 引用以便后期注入
	private GdxLauncher gdxLauncher;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		ScreenUtils.hideBlackBar(this);
		UncaughtExceptionActivity.setUncaughtExceptionHandler(this, AndroidGdxLauncher.class);

		PlatformImpl.AndroidExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		PlatformImpl.defaultOrientaion = isPortrait ? ScreenManager.Orientation.Portrait : ScreenManager.Orientation.Landscape;

		setupViewportListener();
		initKeyMap();

		// ---------------------------------------------------------
		// 1. 立即初始化 GDX (传入 null compiler)
		// 这样 AndroidApplication 内部的 input/graphics 就会被创建
		// onResume 就不会崩了
		// ---------------------------------------------------------
		gdxLauncher = new GdxLauncher(null);

		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useImmersiveMode = true;
		cfg.numSamples = 2;

		gameView = initializeForView(gdxLauncher, cfg);

		// 2. 设置布局
		rootFrame = new FrameLayout(this);
		rootFrame.addView(gameView, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		setContentView(rootFrame);

		// 3. UI 初始化
		new Handler(getMainLooper()).post(this::initOverlayUI);

		// 4. 设置监听器 (这些可以现在就设，虽然游戏还没逻辑)
		setupGdxListeners();

		// ---------------------------------------------------------
		// 5. 权限检查与“热启动”
		// ---------------------------------------------------------
		String[] permissions = {
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE
		};

		permissionUtils = new PermissionUtils(this, permissions, 100, new Runnable() {
			@Override
			public void run() {
				// [关键] 权限拿到了！创建编译器并注入给游戏
				injectCompilerAndStart();
			}
		});

		permissionUtils.requestAllPermission();
	}

	private void setupGdxListeners() {
		ScreenManager.exitGame.add(() -> moveTaskToBack(true));
		PlatformImpl.showSoftInputKeyBoard = (show) -> runOnUiThread(() -> setKeyboardVisibility(show));

		// 注意：orientationChanger 可能会在 GdxLauncher 初始化前被调用吗？
		// 应该不会，因为 ScreenManager 还没初始化。但为了安全，保持原样即可。
		ScreenManager.orientationChanger = (orientation) -> runOnUiThread(() -> {
			setRequestedOrientation(orientation == ScreenManager.Orientation.Landscape ?
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		});

		gameView.setFocusable(true);
		gameView.setFocusableInTouchMode(true);
		gameView.requestFocus();
	}

	private void injectCompilerAndStart() {
		// 此时有权限了，可以安全地进行文件 IO (mkdirs)
		AndroidScriptCompiler androidCompiler = new AndroidScriptCompiler(this);

		// 通知游戏：编译器好了，开始加载资源吧！
		if (gdxLauncher != null) {
			gdxLauncher.onAndroidReady(androidCompiler);
		}
	}

	// [新增] 启动引擎逻辑
	private void startEngine() {
		// 1. 创建根容器
		rootFrame = new FrameLayout(this);

		// 2. 配置
		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useImmersiveMode = true;
		cfg.numSamples = 2;

		// 3. 启动
		AndroidScriptCompiler androidCompiler = new AndroidScriptCompiler(this);
		ApplicationListener main = new GdxLauncher(androidCompiler);

		gameView = initializeForView(main, cfg);
		rootFrame.addView(gameView, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		setContentView(rootFrame);

		// 4. UI 层
		new Handler(getMainLooper()).post(this::initOverlayUI);

		ScreenManager.orientationChanger = (orientation) -> runOnUiThread(() -> {
			setRequestedOrientation(orientation == ScreenManager.Orientation.Landscape ?
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		});

		ScreenManager.exitGame.add(() -> moveTaskToBack(true));
		PlatformImpl.showSoftInputKeyBoard = (show) -> runOnUiThread(() -> setKeyboardVisibility(show));

		gameView.setFocusable(true);
		gameView.setFocusableInTouchMode(true);
		gameView.requestFocus();
	}

	// [新增] 代理回调到 PermissionUtils
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (permissionUtils != null) {
			permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (permissionUtils != null) {
			permissionUtils.onActivityResult(requestCode, resultCode, data);
		}
	}

	// ... (原有的 initOverlayUI, initKeyMap 等方法保持不变) ...
	// 为了节省篇幅，这里略去后面的 UI 初始化代码，请确保保留它们

	private void initOverlayUI() {
		// ... (保持原样)
		overlayLayer = new RelativeLayout(this);
		overlayLayer.setBackgroundColor(Color.TRANSPARENT);
		// 让它可以点击穿透（点击空白处传给游戏），但子 View (按钮) 依然可以点击
		// RelativeLayout 默认就是不拦截空白点击的，只要不设 ClickListener

		rootFrame.addView(overlayLayer, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		// --- A. 键盘容器 ---
		keyboardContainer = new LinearLayout(this);
		keyboardContainer.setOrientation(LinearLayout.VERTICAL);
		// [回归] 不设背景色或透明
//		keyboardContainer.setBackgroundColor(0x88000000);
		keyboardContainer.setVisibility(View.GONE);

		RelativeLayout.LayoutParams kbParams = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0);
		kbParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		overlayLayer.addView(keyboardContainer, kbParams);

		// --- B. 悬浮开关按钮 ---
		floatingToggleBtn = new Button(this);
		floatingToggleBtn.setText("⌨");
		floatingToggleBtn.setTextColor(Color.CYAN);
		floatingToggleBtn.setTextSize(20);
		floatingToggleBtn.setPadding(20, 20, 20, 20); // 给点内边距

		GradientDrawable shape = new GradientDrawable();
		shape.setCornerRadius(100);
		shape.setColor(0x44000000);
		shape.setStroke(2, 0xFF00EAFF);
		floatingToggleBtn.setBackground(shape);

		// [v19.5 修复] 使用 WRAP_CONTENT，这样文字长了会自动变大，不会被切
		int btnSize = 120;
		FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);

		// 初始位置不要用 Margin，直接设为 0，靠 setX/Y 移动
		btnParams.topMargin = 0;
		btnParams.leftMargin = 0;

		floatingToggleBtn.setLayoutParams(btnParams);

		// 拖拽逻辑
		floatingToggleBtn.setOnTouchListener(new View.OnTouchListener() {
			float dX, dY;
			float downRawX, downRawY;
			boolean isDrag = false;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						dX = v.getX() - event.getRawX();
						dY = v.getY() - event.getRawY();
						downRawX = event.getRawX();
						downRawY = event.getRawY();
						isDrag = false;
						v.animate().alpha(1.0f).scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
						return true;

					case MotionEvent.ACTION_MOVE:
						if (Math.abs(event.getRawX() - downRawX) > 10 || Math.abs(event.getRawY() - downRawY) > 10) {
							isDrag = true;
						}

						if (isDrag) {
							// 这里的 setX/Y 是相对于 overlayLayer 的，也就是全屏
							float newX = event.getRawX() + dX;
							float newY = event.getRawY() + dY;

							// 边界限制
							newX = Math.max(0, Math.min(screenWidth - v.getWidth(), newX));
							newY = Math.max(0, Math.min(screenHeight - v.getHeight(), newY));

							v.setX(newX);
							v.setY(newY);
						}
						return true;

					case MotionEvent.ACTION_UP:
						v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();

						if (!isDrag) {
							setKeyboardVisibility(true);
							dockFloatingButton(v);
						} else {
							dockFloatingButton(v);
						}
						return true;
				}
				return false;
			}
		});

		floatingToggleBtn.setAlpha(0.3f);
		overlayLayer.addView(floatingToggleBtn);

		// 初始位置设置 (延时一帧确保宽高已计算)
		floatingToggleBtn.post(() -> {
			floatingToggleBtn.setY(screenHeight / 2f - floatingToggleBtn.getHeight() / 2f);
			dockFloatingButton(floatingToggleBtn);
		});

		refreshKeyboardLayout();
	}

	private void dockFloatingButton(View v) {
		float centerX = v.getX() + v.getWidth() / 2f;
		float screenMid = screenWidth / 2f;
		float targetX;

		// 隐藏 50%
		float hideRatio = 0.5f;
		float hiddenOffset = v.getWidth() * hideRatio;

		if (centerX < screenMid) {
			targetX = -hiddenOffset;
		} else {
			targetX = screenWidth - v.getWidth() + hiddenOffset;
		}

		v.animate().x(targetX).alpha(0.3f).setDuration(300).start();
	}

	private void setKeyboardVisibility(boolean visible) {
		isKeyboardVisible = visible;
		keyboardContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
		floatingToggleBtn.setVisibility(visible ? View.GONE : View.VISIBLE);

		if (!visible) {
			gameView.requestFocus();
		}
	}

	private void refreshKeyboardLayout() {
		if (keyboardContainer == null || screenWidth == 0) return;

		keyboardContainer.removeAllViews();

		boolean isLandscape = screenWidth > screenHeight;
		float ratio = isLandscape ? HEIGHT_RATIO_LANDSCAPE : HEIGHT_RATIO_PORTRAIT;
		int totalHeight = (int) (screenHeight * ratio);

		ViewGroup.LayoutParams params = keyboardContainer.getLayoutParams();
		params.height = totalHeight;
		keyboardContainer.setLayoutParams(params);

		for (String[] rowKeys : terminalLayout) {
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
			row.setLayoutParams(rowParams);

			for (String key : rowKeys) {
				Button keyBtn = createKeyButton(key);
				float weight = key.equals("Space") ? 3.0f : 1.0f;

				LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
					0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
				btnParams.setMargins(0, 0, 0, 0);
				keyBtn.setLayoutParams(btnParams);

				row.addView(keyBtn);
			}
			keyboardContainer.addView(row);
		}
	}

	private Button createKeyButton(String text) {
		Button btn = new Button(this);
		btn.setText(text);
		btn.setTextColor(Color.WHITE);
		btn.setTextSize(13);
		btn.setGravity(Gravity.CENTER);
		int p = (int) (padding * getResources().getDisplayMetrics().density);
		btn.setPadding(p, p, p, p);
		btn.setBackgroundResource(R.drawable.virtual_key_background);

		btn.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setPressed(true);
				handleKeyPress(text, true);
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				v.setPressed(false);
				handleKeyPress(text, false);
			}
			return true;
		});
		return btn;
	}

	private void handleKeyPress(String key, boolean down) {
		if (down && key.equals("Hide")) {
			setKeyboardVisibility(false);
			return;
		}

		Integer code = keyMap.get(key);
		if (code != null) {
			long time = System.currentTimeMillis();
			int action = down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
			dispatchKeyEvent(new KeyEvent(time, time, action, code, 0));
		}
	}

	private void setupViewportListener() {
		getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
			Rect r = new Rect();
			getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
			int w = r.width();
			int h = r.height();
			if (w != screenWidth || h != screenHeight) {
				screenWidth = w;
				screenHeight = h;
				runOnUiThread(this::refreshKeyboardLayout);
			}
		});
	}

	private void initKeyMap() {
		for (char c = 'A'; c <= 'Z'; c++) keyMap.put(String.valueOf(c), KeyEvent.keyCodeFromString(String.valueOf(c)));
		for (char c = '0'; c <= '9'; c++) keyMap.put(String.valueOf(c), KeyEvent.keyCodeFromString(String.valueOf(c)));

		keyMap.put("-", KeyEvent.KEYCODE_MINUS);
		keyMap.put("=", KeyEvent.KEYCODE_EQUALS);
		keyMap.put("[", KeyEvent.KEYCODE_LEFT_BRACKET);
		keyMap.put("]", KeyEvent.KEYCODE_RIGHT_BRACKET);
		keyMap.put(";", KeyEvent.KEYCODE_SEMICOLON);
		keyMap.put("'", KeyEvent.KEYCODE_APOSTROPHE);
		keyMap.put(",", KeyEvent.KEYCODE_COMMA);
		keyMap.put(".", KeyEvent.KEYCODE_PERIOD);
		keyMap.put("/", KeyEvent.KEYCODE_SLASH);

		keyMap.put("Del", KeyEvent.KEYCODE_DEL);
		keyMap.put("Tab", KeyEvent.KEYCODE_TAB);
		keyMap.put("Esc", KeyEvent.KEYCODE_ESCAPE);
		keyMap.put("Enter", KeyEvent.KEYCODE_ENTER);
		keyMap.put("Space", KeyEvent.KEYCODE_SPACE);
		keyMap.put("Shift", KeyEvent.KEYCODE_SHIFT_LEFT);
		keyMap.put("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT);
		keyMap.put("Alt", KeyEvent.KEYCODE_ALT_LEFT);
		keyMap.put("Sym", KeyEvent.KEYCODE_SYM);

		keyMap.put("↑", KeyEvent.KEYCODE_DPAD_UP);
		keyMap.put("↓", KeyEvent.KEYCODE_DPAD_DOWN);
		keyMap.put("←", KeyEvent.KEYCODE_DPAD_LEFT);
		keyMap.put("→", KeyEvent.KEYCODE_DPAD_RIGHT);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isKeyboardVisible) {
				setKeyboardVisibility(false);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
