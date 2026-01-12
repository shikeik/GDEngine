package com.goldsprite.gdengine.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.BuildConfig;
import com.goldsprite.GdxLauncher;

/**
 * Launches the desktop (LWJGL3) application.
 */
public class Lwjgl3Launcher {
	// 定义基础尺寸
	static float scl = 1.5f;
	public static final float WORLD_WIDTH = 960 * scl;
	public static final float WORLD_HEIGHT = 540 * scl;

	public static void main(String[] args) {
		if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
		createApplication();
	}

	private static Lwjgl3Application createApplication() {
		// [修改] 创建 PC 端编译器实例
		DesktopScriptCompiler compiler = new DesktopScriptCompiler();

		// [修改] 注入到游戏主入口
		return new Lwjgl3Application(new GdxLauncher(compiler), getDefaultConfiguration());
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
		configuration.setTitle(BuildConfig.PROJECT_NAME+" - V" + BuildConfig.DEV_VERSION);
		Graphics.DisplayMode primaryMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
		configuration.setWindowedMode((int) WORLD_WIDTH, (int) WORLD_HEIGHT);
		configuration.setDecorated(true);
		configuration.setResizable(true);

		//实现退出接口
		ScreenManager.exitGame.add(() -> {
			Gdx.app.exit();
		});

		// 实现全屏接口
		PlatformImpl.fullScreenEvent = (fullScreen) -> {
			if (fullScreen) {
				// 切换到全屏
				Gdx.graphics.setFullscreenMode(primaryMode);
				configuration.setDecorated(false); // 全屏模式下通常禁用窗口装饰
				configuration.setResizable(false); // 全屏模式下通常不允许调整大小
			} else {
				// 切换到窗口模式
				Gdx.graphics.setWindowedMode((int) WORLD_WIDTH, (int) WORLD_HEIGHT);
				configuration.setDecorated(true);
				configuration.setResizable(true);
			}
		};

		// 可选：设置全屏模式下的其他参数
		configuration.setInitialVisible(true);
		configuration.setAutoIconify(true); // 当失去焦点时最小化

		// 全屏模式下建议启用垂直同步以减少屏幕撕裂
		configuration.useVsync(false);
		// 全屏模式下使用显示器刷新率
//		configuration.setForegroundFPS(primaryMode.refreshRate);
		configuration.setForegroundFPS(240);

		//// 如果你想要窗口最大化模式：
		// configuration.setMaximized(true);

		//// You can change these files; they are in lwjgl3/src/main/resources/ .
		configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

		configuration.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4); // r, g, b, a, depth, stencil, samples=4

		return configuration;
	}
}
