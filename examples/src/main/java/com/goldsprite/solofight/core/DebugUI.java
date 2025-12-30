package com.goldsprite.solofight.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.solofight.BuildConfig;
import com.goldsprite.solofight.core.ui.DebugConsole;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugUI {
	private static DebugUI instance;

	// 数据层 (构造时即可用)
	private List<String> logs = new ArrayList<>();
	private List<String> debugInfos = new ArrayList<>();
	public static boolean showDebugUI = true;
	static int maxLogsCache = 100;

	// 表现层 (延后初始化)
	private Stage stage;
	private DebugConsole console;

	// 视口配置
	static float scl = 1.4f;
	private static final float LOGICAL_SHORT = 540f*scl;
	private static final float LOGICAL_LONG = 960f*scl;

	// [修改] 构造函数只做最基础的数据准备，绝对不碰 UI
	public DebugUI() {
		instance = this;
		// 移除 Stage 和 Console 的创建代码
	}

	public static DebugUI getInstance() {
		if (instance == null) new DebugUI();
		return instance;
	}

	/**
	 * [新增] 显式 UI 初始化方法
	 * 必须在 VisUIHelper.loadWithChineseFont() 之后调用
	 */
	public void initUI() {
		if (stage != null) return; // 防止重复初始化

		// [修改] 初始默认横屏
		stage = new Stage(new ExtendViewport(LOGICAL_LONG, LOGICAL_SHORT));

		console = new DebugConsole();
		stage.addActor(console);

		registerInput();

		// 打印一条调试信息验证顺序
		log("DebugUI UI Initialized.");
	}

	private void registerInput() {
		Gdx.app.postRunnable(() -> {
			try {
				ScreenManager sm = ScreenManager.getInstance();
				if (sm != null && sm.getImp() != null) {
					sm.getImp().addProcessor(0, stage);
					log("DebugUI Input Registered at Top.");
				} else {
					log("Warning: ScreenManager not ready for DebugUI input.");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	// --- 数据接口 ---

	public static List<String> getLogs() {
		return getInstance().logs;
	}

	public static String getInfoString() {
		StringBuilder sb = new StringBuilder();
		sb.append(BuildConfig.PROJECT_NAME).append(": V").append(BuildConfig.DEV_VERSION);
		sb.append("\nHeap: ").append(Gdx.app.getJavaHeap() / 1024 / 1024).append("MB");
		sb.append("\nFPS: ").append(Gdx.graphics.getFramesPerSecond());int k;

		if (!getInstance().debugInfos.isEmpty()) {
			sb.append("\n--- Monitors ---\n");
			sb.append(String.join("\n", getInstance().debugInfos));
		}
		return sb.toString();
	}
	public static void clearInfo() {
		if(getInstance() != null) getInstance().debugInfos.clear();
	}

	public static void log(Object... value) {
		if (value == null || value.length == 0) return;
		String msg;
		if (value.length == 1) {
			msg = String.valueOf(value[0]);
		} else {
			try {
				String format = String.valueOf(value[0]);
				Object[] args = Arrays.copyOfRange(value, 1, value.length);
				msg = String.format(format, args);
			} catch (Exception e) {
				msg = String.valueOf(value[0]) + " <FmtErr> " + Arrays.toString(Arrays.copyOfRange(value, 1, value.length));
			}
		}
		getInstance().logs.add(msg);
		if (getInstance().logs.size() > maxLogsCache) getInstance().logs.remove(0);
		Gdx.app.log("DEBUG", msg);
	}

	public static void info(Object... value) {
		if (value == null || value.length == 0) return;
		String finalStr;
		if (value.length == 1) {
			finalStr = String.valueOf(value[0]);
		} else {
			try {
				String format = String.valueOf(value[0]);
				Object[] args = Arrays.copyOfRange(value, 1, value.length);
				finalStr = String.format(format, args);
			} catch (Exception e) {
				finalStr = String.valueOf(value[0]);
			}
		}
		getInstance().debugInfos.add(finalStr);
	}

	public static void setIntros(String text) {
		// [修改] 增加判空，防止在 initUI 之前调用导致的崩溃
		if (getInstance().console != null) {
			getInstance().console.setIntros(text);
		}
	}

	public void render() {
		// [修改] 如果 UI 还没初始化，直接跳过渲染，但数据收集依然正常工作
		if (!showDebugUI || stage == null) return;

		stage.getViewport().apply(true);int k;
		stage.act();
		stage.draw();
	}

	public void resize(int w, int h) {
		if (stage == null) return;

		ExtendViewport vp = (ExtendViewport) stage.getViewport();
		if (h > w) {
			vp.setMinWorldWidth(LOGICAL_SHORT);
			vp.setMinWorldHeight(LOGICAL_LONG);
		} else {
			vp.setMinWorldWidth(LOGICAL_LONG);
			vp.setMinWorldHeight(LOGICAL_SHORT);
		}
		vp.update(w, h, true);
	}

	public void dispose() {
		if (stage != null) stage.dispose();
		if (console != null) console.dispose();
	}
}
