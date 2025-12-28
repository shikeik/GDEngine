package com.goldsprite.solofight.android;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
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
	private final float HEIGHT_RATIO_LANDSCAPE = 0.45f; // 横屏键盘高度占比
	private final float HEIGHT_RATIO_PORTRAIT = 0.35f;  // 竖屏键盘高度占比

	// --- 全功能终端键盘布局 (5 Rows) ---
	private final String[][] terminalLayout = {
		// Row 1: Numbers & Symbols
		{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Del"},
		// Row 2: Top Letters & Brackets
		{"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"},
		// Row 3: Mid Letters & Punctuation
		{"Esc", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter"},
		// Row 4: Bot Letters & Shift/Up
		{"Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "↑"},
		// Row 5: Mods & Arrows & Control
		{"Ctrl", "Alt", "Sym", "Space", "←", "↓", "→", "Hide"}
	};

	private final Map<String, Integer> keyMap = new HashMap<>();

	// UI Components
	private RelativeLayout rootLayout;
	private LinearLayout keyboardContainer;
	private Button floatingToggleBtn;

	private boolean isKeyboardVisible = false;
	private int screenWidth, screenHeight;

	public static AndroidGdxLauncher getCtx() { return ctx; }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		ScreenUtils.hideBlackBar(this);
		UncaughtExceptionActivity.setUncaughtExceptionHandler(this, AndroidGdxLauncher.class);

		// 1. 初始化视口监听 (处理转屏)
		setupViewportListener();

		// 2. 初始化键位映射
		initKeyMap();

		// 3. 构建 UI 结构
		// Root (Relative)
		//  |- GameContainer (Frame)
		//  |- KeyboardContainer (Linear, Bottom)
		//  |- FloatingBtn (Button, Top-Left)

		rootLayout = new RelativeLayout(this);
		rootLayout.setBackgroundColor(Color.BLACK);

		// Game View
		FrameLayout gameContainer = new FrameLayout(this);
		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useImmersiveMode = true;
		View gameView = initializeForView(GdxLauncherProvider.launcherGame(), cfg);
		gameContainer.addView(gameView);

		rootLayout.addView(gameContainer, new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		setContentView(rootLayout);

		// 4. 延迟初始化覆盖层 UI (确保某些 Context 准备好)
		new Handler(getMainLooper()).post(this::initOverlayUI);

		// Hook 屏幕方向切换
		ScreenManager.orientationChanger = (orientation) -> runOnUiThread(() -> {
			setRequestedOrientation(orientation == ScreenManager.Orientation.LANDSCAPE ?
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		});

		// Hook 退出
		ScreenManager.exitGame.add(() -> moveTaskToBack(true));

		// Hook 外部显隐控制 (虽然现在主要靠悬浮按钮)
		PlatformImpl.showSoftInputKeyBoard = (show) -> runOnUiThread(() -> setKeyboardVisibility(show));
	}

	private void initOverlayUI() {
		// --- A. 键盘容器 ---
		keyboardContainer = new LinearLayout(this);
		keyboardContainer.setOrientation(LinearLayout.VERTICAL);
		keyboardContainer.setBackgroundColor(0xCC000000); // 半透明黑底
		keyboardContainer.setVisibility(View.GONE); // 默认隐藏

		RelativeLayout.LayoutParams kbParams = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0); // 高度动态计算
		kbParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rootLayout.addView(keyboardContainer, kbParams);

		// --- B. 悬浮开关按钮 ---
		floatingToggleBtn = new Button(this);
		floatingToggleBtn.setText("⌨");
		floatingToggleBtn.setTextColor(Color.CYAN);
		floatingToggleBtn.setTextSize(20);
		floatingToggleBtn.setBackgroundColor(0x44000000); // 很淡的背景
		floatingToggleBtn.setPadding(0,0,0,0);

		// 圆角背景样式
		GradientDrawable shape = new GradientDrawable();
		shape.setCornerRadius(50);
		shape.setColor(0x66000000);
		shape.setStroke(2, 0xFF00EAFF);
		floatingToggleBtn.setBackground(shape);

		RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(100, 100);
		btnParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		btnParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		btnParams.setMargins(20, 20, 0, 0);

		floatingToggleBtn.setLayoutParams(btnParams);
		floatingToggleBtn.setOnClickListener(v -> setKeyboardVisibility(true));

		rootLayout.addView(floatingToggleBtn);

		// 初始构建一次键盘
		refreshKeyboardLayout();
	}

	private void setKeyboardVisibility(boolean visible) {
		isKeyboardVisible = visible;
		keyboardContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
		floatingToggleBtn.setVisibility(visible ? View.GONE : View.VISIBLE); // 互斥显示
	}

	private void refreshKeyboardLayout() {
		if (keyboardContainer == null || screenWidth == 0) return;

		keyboardContainer.removeAllViews();

		// 1. 计算高度
		boolean isLandscape = screenWidth > screenHeight;
		float ratio = isLandscape ? HEIGHT_RATIO_LANDSCAPE : HEIGHT_RATIO_PORTRAIT;
		int totalHeight = (int) (screenHeight * ratio);

		// 更新容器高度
		ViewGroup.LayoutParams params = keyboardContainer.getLayoutParams();
		params.height = totalHeight;
		keyboardContainer.setLayoutParams(params);

		// 2. 生成行
		int rowCount = terminalLayout.length;
		for (String[] rowKeys : terminalLayout) {
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			// 每行平分总高度
			LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
			row.setLayoutParams(rowParams);

			// 3. 生成键
			for (String key : rowKeys) {
				Button keyBtn = createKeyButton(key);
				// Space 键更宽
				float weight = key.equals("Space") ? 3.0f : 1.0f;

				LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
					0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
				btnParams.setMargins(2, 2, 2, 2);
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
		btn.setTextSize(10); // 小字号适配多键
		btn.setPadding(0,0,0,0);
		btn.setGravity(Gravity.CENTER);
		btn.setBackgroundColor(0xFF333333); // 深灰按钮
		// 按下变色
		btn.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setBackgroundColor(0xFF00EAFF); // Neon Blue
				handleKeyPress(text, true);
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				v.setBackgroundColor(0xFF333333);
				handleKeyPress(text, false);
			}
			return true;
		});
		return btn;
	}

	private void handleKeyPress(String key, boolean down) {
		// 特殊功能键
		if (down && key.equals("Hide")) {
			setKeyboardVisibility(false);
			return;
		}

		// 映射到 LibGDX Input
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
		// A-Z
		for (char c = 'A'; c <= 'Z'; c++) keyMap.put(String.valueOf(c), KeyEvent.keyCodeFromString(String.valueOf(c)));
		// 0-9
		for (char c = '0'; c <= '9'; c++) keyMap.put(String.valueOf(c), KeyEvent.keyCodeFromString(String.valueOf(c)));

		// Symbols
		keyMap.put("-", KeyEvent.KEYCODE_MINUS);
		keyMap.put("=", KeyEvent.KEYCODE_EQUALS);
		keyMap.put("[", KeyEvent.KEYCODE_LEFT_BRACKET);
		keyMap.put("]", KeyEvent.KEYCODE_RIGHT_BRACKET);
		keyMap.put(";", KeyEvent.KEYCODE_SEMICOLON);
		keyMap.put("'", KeyEvent.KEYCODE_APOSTROPHE);
		keyMap.put(",", KeyEvent.KEYCODE_COMMA);
		keyMap.put(".", KeyEvent.KEYCODE_PERIOD);
		keyMap.put("/", KeyEvent.KEYCODE_SLASH);

		// Function
		keyMap.put("Del", KeyEvent.KEYCODE_DEL);
		keyMap.put("Tab", KeyEvent.KEYCODE_TAB);
		keyMap.put("Esc", KeyEvent.KEYCODE_ESCAPE);
		keyMap.put("Enter", KeyEvent.KEYCODE_ENTER);
		keyMap.put("Space", KeyEvent.KEYCODE_SPACE);
		keyMap.put("Shift", KeyEvent.KEYCODE_SHIFT_LEFT);
		keyMap.put("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT);
		keyMap.put("Alt", KeyEvent.KEYCODE_ALT_LEFT);
		keyMap.put("Sym", KeyEvent.KEYCODE_SYM);

		// Arrows
		keyMap.put("↑", KeyEvent.KEYCODE_DPAD_UP);
		keyMap.put("↓", KeyEvent.KEYCODE_DPAD_DOWN);
		keyMap.put("←", KeyEvent.KEYCODE_DPAD_LEFT);
		keyMap.put("→", KeyEvent.KEYCODE_DPAD_RIGHT);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 拦截返回键，防止直接退出，改为隐藏键盘或后台
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isKeyboardVisible) {
				setKeyboardVisibility(false);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
