package com.goldsprite.solofight.android;

import android.content.pm.ActivityInfo;
import android.graphics.Rect;
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
import com.goldsprite.solofight.rhino.AndroidTestRhino;
import com.goldsprite.gameframeworks.PlatformImpl;
import com.goldsprite.gameframeworks.screens.ScreenManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidGdxLauncher extends AndroidApplication {
	public static final boolean FullScreenKeyboard = true;//虚拟键盘是否全面屏
	private static AndroidGdxLauncher ctx;
	private final boolean defaultVisible = true;//是否启动时显示
	private final float keyboardWidthPercent = 1f;//虚拟键盘屏宽百分比
	private final float keyboardHeightPercent = 0.55f;//虚拟键盘屏高百分比
	private final int keyMargin = 0; // 键排列间隔 dp
	private final int padding = -16; // padding 文字内边距为负解决文字看不全问题
	// 两套布局
	private final boolean isFullKeyboard = false;//全键盘与WASD操作键盘切换
	// 完整键盘布局
	private final String[][] fullKeyboardLayout = {
//		// y=0 最上面一行
//		{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "CG"},
		{"K1", "K2", "K3", "K4", "K5", "K6", "K7", "K8", "K9", "K0", "Backspace X2", "", "", "", "", "", "CG"},
		// y=1
		{"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "", "", "", "", "N7", "N8", "N9"},
		// y=2
		{"A", "S", "D", "F", "G", "H", "J", "K", "L", "Enter X2", "", "", "", "", "N4", "N5", "N6"},
		// y=3
		{"Z", "X", "C", "V", "B", "N", "M", "", "", "", "", "", "↑", "", "N1", "N2", "N3"},
		// y=4 最下面一行
		{"Ctrl", "Alt", "SPACE X6", "", "", "", "", "", "", "", "", "←", "↓", "→", "N0", ".", "Enter"}
	};
	// 简化游戏布局
	private final String[][] simpleGameLayout = {
		// y=0 最上面一行 - 功能键区
		{"", "", "", "", "", "", "", "", "", "", "", "", "", "CG"},
		// y=1 空行
		{"", "", "", "", "", "", "", "", "", "", "", "", "", ""},
		// y=2 W键行
		{"", "W", "", "", "", "", "", "", "", "", "", "", "←", "→"},
		// y=3 A S D + 方向键行
		{"A", "", "D", "", "", "", "", "", "", "", "", "", "J", "K"},
		// y=4 J K 行（最下面一行）
		{"", "S", "", "", "", "", "", "", "", "", "", "", "", ""}
	};
	// 专属游戏布局 竖屏
	private final String[][] simpleGameLayoutVerti = {
		{"", "", "", "", "", "", ""},
		{"", "", "", "", "", "", ""},
		{"", "", "", "", "", "", ""},
		{"", "", "", "", "", "", "CG"},
		{"", "", "", "", "", "", "", ""},
		{"", "", "", "", "", "", ""},
		// y=2 W键行
		{"", "W", "", "",  "", "", ""},
		// y=3 A S D + 方向键行
		{"A", "", "D", "", "", "J", "K"},
		// y=4 J K 行（最下面一行）
		{"", "S", "", "",  "", "", ""},
		{"", "", "", "", "", "", ""},
	};
	private final String[][] emptyKeyboardLayout = {
		// y=0
		{"", "", "", "", "", "", "", "", "", "", "", "", "", "CG"},
		// y=1
		{"", "", "", "", "", "", "", "", "", "", "", "", "", ""},
		// y=2
		{"", "", "", "", "", "", "", "", "", "", "", "", "", ""},
		// y=3
		{"", "", "", "", "", "", "", "", "", "", "", "", "", ""},
		// y=4
		{"", "", "", "", "", "", "", "", "", "", "", "", "", ""},
	};
	private final List<String[][]> keyboards = Arrays.asList(fullKeyboardLayout, simpleGameLayout, simpleGameLayoutVerti, emptyKeyboardLayout);
	// 字符到KeyCode映射表
	private final Map<String, Integer> keyMap = new HashMap<>();
	private View gameView;
	private LinearLayout keyboardContainer;
	private int keyboardWidth, keyboardHeight;
	// 键盘布局参数
	private boolean initialized = false;
	private float screenWidth, screenHeight;
	private int curKeyboardIndex = 3;

	public static AndroidGdxLauncher getCtx() {
		return ctx;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		initLaunchOptions();
		UncaughtExceptionActivity.setUncaughtExceptionHandler(this, AndroidGdxLauncher.class);

		// 实现横屏/竖屏切换接口
		ScreenManager.orientationChanger = (orientation) -> {
			runOnUiThread(() -> {
				if (orientation == ScreenManager.Orientation.LANDSCAPE) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			});
		};
		//设置退出事件监听器
		ScreenManager.exitGame.add(() -> {
//			finish();
//			android.os.Process.killProcess(android.os.Process.myPid());
			moveTaskToBack(true);
		});
		setGlobalViewportListener();

		// 使用自定义布局
		setContentView(R.layout.activity_virtual_controller);

		// 获取游戏容器并初始化LibGDX视图
		FrameLayout gameContainer = findViewById(R.id.game_container);

		//隐藏虚拟键盘并绑定输入系统切换显隐事件
		RelativeLayout virtualController = findViewById(R.id.virtual_controller);
		int visible = defaultVisible ? View.VISIBLE : View.GONE;
		virtualController.setVisibility(visible);
		PlatformImpl.showSoftInputKeyBoard = (isShow) -> runOnUiThread(() -> {
			virtualController.setVisibility(isShow ? View.VISIBLE : View.GONE);
		});

		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useImmersiveMode = true;

		// 初始化游戏视图
		gameView = initializeForView(GdxLauncherProvider.launcherGame(), cfg);
		gameContainer.addView(gameView);

		// 初始化键盘映射表
		initKeyMap();

		// 创建虚拟键盘
		new Handler(getMainLooper()).post(this::createVirtualKeyboard);

		//new AndroidTestRhino(this);
	}

	private void setGlobalViewportListener() {
		// 添加全局布局监听 - 最接近 LibGDX resize 的方式
		getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
			new ViewTreeObserver.OnGlobalLayoutListener() {
				private int lastWidth = 0;
				private int lastHeight = 0;

				@Override
				public void onGlobalLayout() {
					// 获取当前窗口尺寸
					Rect visibleArea = new Rect();
					getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleArea);
					int currentWidth = visibleArea.width();
					int currentHeight = visibleArea.height();

					// 只有当尺寸真正变化时才刷新
					if (currentWidth != lastWidth || currentHeight != lastHeight) {
						lastWidth = currentWidth;
						lastHeight = currentHeight;

						// 这里相当于 LibGDX 的 resize()
						onViewportResize(currentWidth, currentHeight);
					}
				}
			});
	}

	private void initKeyMap() {
		// 字母键
		keyMap.put("A", KeyEvent.KEYCODE_A);
		keyMap.put("B", KeyEvent.KEYCODE_B);
		keyMap.put("C", KeyEvent.KEYCODE_C);
		keyMap.put("D", KeyEvent.KEYCODE_D);
		keyMap.put("E", KeyEvent.KEYCODE_E);
		keyMap.put("F", KeyEvent.KEYCODE_F);
		keyMap.put("G", KeyEvent.KEYCODE_G);
		keyMap.put("H", KeyEvent.KEYCODE_H);
		keyMap.put("I", KeyEvent.KEYCODE_I);
		keyMap.put("J", KeyEvent.KEYCODE_J);
		keyMap.put("K", KeyEvent.KEYCODE_K);
		keyMap.put("L", KeyEvent.KEYCODE_L);
		keyMap.put("M", KeyEvent.KEYCODE_M);
		keyMap.put("N", KeyEvent.KEYCODE_N);
		keyMap.put("O", KeyEvent.KEYCODE_O);
		keyMap.put("P", KeyEvent.KEYCODE_P);
		keyMap.put("Q", KeyEvent.KEYCODE_Q);
		keyMap.put("R", KeyEvent.KEYCODE_R);
		keyMap.put("S", KeyEvent.KEYCODE_S);
		keyMap.put("T", KeyEvent.KEYCODE_T);
		keyMap.put("U", KeyEvent.KEYCODE_U);
		keyMap.put("V", KeyEvent.KEYCODE_V);
		keyMap.put("W", KeyEvent.KEYCODE_W);
		keyMap.put("X", KeyEvent.KEYCODE_X);
		keyMap.put("Y", KeyEvent.KEYCODE_Y);
		keyMap.put("Z", KeyEvent.KEYCODE_Z);

		// 数字键
		keyMap.put("K0", KeyEvent.KEYCODE_0);
		keyMap.put("K1", KeyEvent.KEYCODE_1);
		keyMap.put("K2", KeyEvent.KEYCODE_2);
		keyMap.put("K3", KeyEvent.KEYCODE_3);
		keyMap.put("K4", KeyEvent.KEYCODE_4);
		keyMap.put("K5", KeyEvent.KEYCODE_5);
		keyMap.put("K6", KeyEvent.KEYCODE_6);
		keyMap.put("K7", KeyEvent.KEYCODE_7);
		keyMap.put("K8", KeyEvent.KEYCODE_8);
		keyMap.put("K9", KeyEvent.KEYCODE_9);
		//小键盘数字键
		keyMap.put("N0", KeyEvent.KEYCODE_NUMPAD_0);
		keyMap.put("N1", KeyEvent.KEYCODE_NUMPAD_1);
		keyMap.put("N2", KeyEvent.KEYCODE_NUMPAD_2);
		keyMap.put("N3", KeyEvent.KEYCODE_NUMPAD_3);
		keyMap.put("N4", KeyEvent.KEYCODE_NUMPAD_4);
		keyMap.put("N5", KeyEvent.KEYCODE_NUMPAD_5);
		keyMap.put("N6", KeyEvent.KEYCODE_NUMPAD_6);
		keyMap.put("N7", KeyEvent.KEYCODE_NUMPAD_7);
		keyMap.put("N8", KeyEvent.KEYCODE_NUMPAD_8);
		keyMap.put("N9", KeyEvent.KEYCODE_NUMPAD_9);

		// 方向键
		keyMap.put("←", KeyEvent.KEYCODE_DPAD_LEFT);
		keyMap.put("→", KeyEvent.KEYCODE_DPAD_RIGHT);
		keyMap.put("↑", KeyEvent.KEYCODE_DPAD_UP);
		keyMap.put("↓", KeyEvent.KEYCODE_DPAD_DOWN);

		// 符号键
		keyMap.put(".", KeyEvent.KEYCODE_PERIOD);
		keyMap.put(",", KeyEvent.KEYCODE_COMMA);
		keyMap.put(";", KeyEvent.KEYCODE_SEMICOLON);
		keyMap.put("'", KeyEvent.KEYCODE_APOSTROPHE);
		keyMap.put("/", KeyEvent.KEYCODE_SLASH);
		keyMap.put("[", KeyEvent.KEYCODE_LEFT_BRACKET);
		keyMap.put("]", KeyEvent.KEYCODE_RIGHT_BRACKET);

		// 功能键
		keyMap.put("SPACE", KeyEvent.KEYCODE_SPACE);
		keyMap.put("Backspace", KeyEvent.KEYCODE_DEL);
		keyMap.put("Enter", KeyEvent.KEYCODE_ENTER);
		keyMap.put("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT);
		keyMap.put("Alt", KeyEvent.KEYCODE_ALT_LEFT);
		keyMap.put("Shift", KeyEvent.KEYCODE_SHIFT_LEFT);
		keyMap.put("CG", -1); // 特殊功能键，用于切换布局
	}

	private void createVirtualKeyboard() {
		if (initialized) return;
		RelativeLayout virtualController = findViewById(R.id.virtual_controller);

		// 创建键盘主容器
		keyboardContainer = new LinearLayout(this);
		keyboardContainer.setOrientation(LinearLayout.VERTICAL);

		// 设置键盘尺寸
		DisplayMetrics metrics = new DisplayMetrics();
		if (FullScreenKeyboard) getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
		else getWindowManager().getDefaultDisplay().getMetrics(metrics);
		keyboardWidth = (int) (metrics.widthPixels * keyboardWidthPercent);
		keyboardHeight = (int) (metrics.heightPixels * keyboardHeightPercent);

		RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(
			keyboardWidth, keyboardHeight);
		containerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		containerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		keyboardContainer.setLayoutParams(containerParams);

		// 初始创建完整键盘
		createKeyboardLayout(getCurKeyboard());

		virtualController.addView(keyboardContainer);

		initialized = true;
	}

	private void createKeyboardLayout(String[][] layout) {
		keyboardContainer.removeAllViews();

		// 将dp转换为px
		DisplayMetrics metrics = new DisplayMetrics();
		if (FullScreenKeyboard) getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
		else getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float width = screenWidth;
		float height = screenHeight;
		final int marginPx = (int) (keyMargin * metrics.density);

		// 计算每个格子的基础宽度（包含margin）
		int totalWidth = (int) (width * keyboardWidthPercent);
		int columns = layout[0].length; // 假设每行列数相同
		int baseCellWidth = totalWidth / columns; // 每个格子的基础宽度

		// 基础按键宽度（不包含margin）
		int baseKeyWidth = baseCellWidth - 2 * marginPx;

		// 遍历每一行创建键盘
		for (int y = 0; y < layout.length; y++) {
			String[] row = layout[y];
			LinearLayout rowLayout = new LinearLayout(this);
			rowLayout.setOrientation(LinearLayout.HORIZONTAL);
			rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

			// 遍历该行的每个按键
			for (int x = 0; x < row.length; x++) {
				String keyText = row[x];

				if (keyText.contains(" X")) {
					// 扩展键 - 解析扩展格数
					String[] parts = keyText.split(" ");
					String displayText = parts[0];
					int expandCount = Integer.parseInt(parts[1].replace("X", "")); // 扩展格数

					// 计算扩展键的总宽度（包含margin）
					int expandTotalWidth = baseCellWidth * expandCount;
					int expandKeyWidth = expandTotalWidth - 2 * marginPx;

					// 创建扩展按键
					Button expandButton = createKeyButton(displayText, keyMap.get(displayText));
					LinearLayout.LayoutParams expandParams = new LinearLayout.LayoutParams(
						expandKeyWidth, ViewGroup.LayoutParams.MATCH_PARENT);
					expandParams.setMargins(marginPx, marginPx, marginPx, marginPx);
					expandButton.setLayoutParams(expandParams);
					rowLayout.addView(expandButton);

					// 跳过已被扩展占用的空字符位置
					x += expandCount - 1;
				} else if (keyText.isEmpty()) {
					// 空字符 - 创建透明占位（固定宽度）
					View spaceView = new View(this);
					LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
						baseKeyWidth, ViewGroup.LayoutParams.MATCH_PARENT);
					spaceParams.setMargins(marginPx, marginPx, marginPx, marginPx);
					spaceView.setLayoutParams(spaceParams);
					rowLayout.addView(spaceView);
				} else {
					// 普通按键（固定宽度）
					Button button = createKeyButton(keyText, keyMap.get(keyText));
					LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
						baseKeyWidth, ViewGroup.LayoutParams.MATCH_PARENT);
					buttonParams.setMargins(marginPx, marginPx, marginPx, marginPx);
					button.setLayoutParams(buttonParams);
					rowLayout.addView(button);
				}
			}

			keyboardContainer.addView(rowLayout);
		}
	}

	private Button createKeyButton(String text, final Integer keyCode) {
		Button button = new Button(this);
		button.setText(text);
		button.setTextColor(0xFFFFFFFF);
		button.setTextSize(13);
		button.setGravity(Gravity.CENTER);

		// 添加padding确保文字显示完整
		int paddingPx = (int) (padding * getResources().getDisplayMetrics().density);
		button.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

		// 设置背景样式
		button.setBackgroundResource(R.drawable.virtual_key_background);

		if (keyCode != null) {
			if (keyCode == -1) {
				// CG键特殊处理 - 切换布局
				button.setOnClickListener(v -> switchKeyboardLayout());
			} else {
				// 普通按键 - 绑定触摸事件
				button.setOnTouchListener((v, event) -> {
					v.performClick();
					long currentTime = System.currentTimeMillis();
					KeyEvent keyEvent = null;

					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						v.setPressed(true);
						keyEvent = new KeyEvent(currentTime, currentTime,
							KeyEvent.ACTION_DOWN, keyCode, 0);
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						v.setPressed(false);
						keyEvent = new KeyEvent(currentTime, currentTime,
							KeyEvent.ACTION_UP, keyCode, 0);
					}

					if (keyEvent != null) {
						return dispatchKeyEvent(keyEvent);
					}
					return true;
				});
			}
		} else {
			// 没有映射的键设为不可用
			button.setEnabled(false);
			button.setAlpha(0.3f);
		}

		return button;
	}

	private String[][] getCurKeyboard() {
		return keyboards.get(curKeyboardIndex);
	}

	private String[][] nextKeyboard() {
		curKeyboardIndex = ++curKeyboardIndex % keyboards.size();
		return keyboards.get(curKeyboardIndex);
	}

	private void switchKeyboardLayout() {
		String[][] curKeyboard = nextKeyboard();
		createKeyboardLayout(curKeyboard);
	}

	private void initLaunchOptions() {
		ScreenUtils.hideBlackBar(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// moveTaskToBack(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void onViewportResize(int width, int height) {
		runOnUiThread(() -> {
			// 刷新虚拟键盘
			screenWidth = width;
			screenHeight = height;
			if (initialized)
				createKeyboardLayout(getCurKeyboard());
		});
	}
}
