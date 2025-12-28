package com.goldsprite.solofight.android;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.goldsprite.gameframeworks.PlatformImpl;
import com.goldsprite.gameframeworks.screens.ScreenManager;

import java.util.HashMap;
import java.util.Map;

public class AndroidGdxLauncher extends AndroidApplication {
	private static AndroidGdxLauncher ctx;

	// --- 配置参数 ---
	private final float HEIGHT_RATIO_LANDSCAPE = 0.45f;
	private final float HEIGHT_RATIO_PORTRAIT = 0.35f;
	private final int padding = -16; // [回归] 负内边距，防止文字被切

	// --- 全功能终端键盘布局 (5 Rows) ---
	private final String[][] terminalLayout = {
		{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Del"},
		{"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"},
		{"Esc", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter"},
		{"Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "↑"},
		{"Ctrl", "Alt", "Sym", "Space", "←", "↓", "→", "Hide"}
	};

	private final Map<String, Integer> keyMap = new HashMap<>();

	private RelativeLayout rootLayout;
	private LinearLayout keyboardContainer;
	private Button floatingToggleBtn;
	private View gameView; // [新增] 持有游戏视图引用

	private boolean isKeyboardVisible = false;
	private int screenWidth, screenHeight;

	public static AndroidGdxLauncher getCtx() { return ctx; }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		ScreenUtils.hideBlackBar(this);
		UncaughtExceptionActivity.setUncaughtExceptionHandler(this, AndroidGdxLauncher.class);

		setupViewportListener();
		initKeyMap();

		// Root
		rootLayout = new RelativeLayout(this);
//		rootLayout.setBackgroundColor(Color.BLACK);

		// Game View
		FrameLayout gameContainer = new FrameLayout(this);
		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useImmersiveMode = true;
		gameView = initializeForView(GdxLauncherProvider.launcherGame(), cfg);
		gameContainer.addView(gameView);

		rootLayout.addView(gameContainer, new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		setContentView(rootLayout);

		// UI Overlay
		new Handler(getMainLooper()).post(this::initOverlayUI);

		ScreenManager.orientationChanger = (orientation) -> runOnUiThread(() -> {
			setRequestedOrientation(orientation == ScreenManager.Orientation.LANDSCAPE ?
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		});

		ScreenManager.exitGame.add(() -> moveTaskToBack(true));
		PlatformImpl.showSoftInputKeyBoard = (show) -> runOnUiThread(() -> setKeyboardVisibility(show));

		// [修复] 确保游戏视图一启动就获得焦点，以便接收按键
		gameView.setFocusable(true);
		gameView.setFocusableInTouchMode(true);
		gameView.requestFocus();
	}

	private void initOverlayUI() {
		// Keyboard Container
		keyboardContainer = new LinearLayout(this);
		keyboardContainer.setOrientation(LinearLayout.VERTICAL);
		// [回归] 不设背景色，或者设完全透明，避免之前的半透明黑底如果不喜欢
//		keyboardContainer.setBackgroundColor(0x88000000);
		keyboardContainer.setVisibility(View.GONE);

		RelativeLayout.LayoutParams kbParams = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0);
		kbParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rootLayout.addView(keyboardContainer, kbParams);

		// Floating Button
		floatingToggleBtn = new Button(this);
		floatingToggleBtn.setText("⌨");
		floatingToggleBtn.setTextColor(Color.CYAN);
		floatingToggleBtn.setTextSize(20);
		floatingToggleBtn.setPadding(0,0,0,0);

		GradientDrawable shape = new GradientDrawable();
		shape.setCornerRadius(50);
		shape.setColor(0x44000000);
		shape.setStroke(2, 0xFF00EAFF);
		floatingToggleBtn.setBackground(shape);

		RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(100, 100);
		btnParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		btnParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		btnParams.setMargins(20, 20, 0, 0);

		floatingToggleBtn.setLayoutParams(btnParams);
		floatingToggleBtn.setOnClickListener(v -> setKeyboardVisibility(true));

		rootLayout.addView(floatingToggleBtn);

		refreshKeyboardLayout();
	}

	private void setKeyboardVisibility(boolean visible) {
		isKeyboardVisible = visible;
		keyboardContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
		floatingToggleBtn.setVisibility(visible ? View.GONE : View.VISIBLE);

		// [核心修复] 当键盘关闭时，强制把焦点还给游戏，否则返回键会被Android层拦截
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
				// [回归] margin 设为 0，让 drawable 自己处理间隔(如果有)
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
		// [回归] 字号 13
		btn.setTextSize(13);
		btn.setGravity(Gravity.CENTER);

		// [回归] 负 Padding
		int p = (int) (padding * getResources().getDisplayMetrics().density);
		btn.setPadding(p, p, p, p);

		// [回归] 使用 XML 资源作为背景 (自带按下效果)
		btn.setBackgroundResource(R.drawable.virtual_key_background);

		btn.setOnTouchListener((v, event) -> {
			// XML drawable 通常处理了按下变色，所以这里只负责发事件
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setPressed(true); // 触发 XML selector
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
		// [修正] 如果键盘开着，Back键只关键盘
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isKeyboardVisible) {
				setKeyboardVisibility(false);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
