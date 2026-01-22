package com.goldsprite.gdengine.core;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.core.platform.DesktopFiles;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.core.web.IWebBrowser;

/**
 * 引擎核心 API 入口 (GDEngine Facade)
 * <p>
 * 这是访问 GDEngine 核心功能的全局入口点。它不仅代理了 LibGDX 的基础模块（如 {@link #input}, {@link #graphics}），
 * 还提供了引擎特有的功能（如脚本编译、配置管理）。
 * </p>
 *
 * <h3>快速索引：</h3>
 * <ul>
 *   <li>详细指南与架构说明: <a href="file:E:\WorkSpaces\Libgdx_WSpace\Projs\GDEngine\docs\manual\core\engine_context.md">docs/manual/core/engine_context.md</a></li>
 *   <li>获取当前运行模式: {@link #mode}</li>
 * </ul>
 *
 * <h3>Code Example:</h3>
 * <pre>
 * // 检查是否在编辑器模式下运行
 * if (Gd.mode == Gd.Mode.EDITOR) {
 *     Gd.app.log("Engine", "Running in Editor Mode");
 * }
 *
 * // 获取逻辑设计分辨率
 * float width = Gd.config.logicWidth;
 * </pre>
 */
public class Gd {

	// =============================================================
	// 1. 基础模块 (默认透传 LibGDX，保持 API 一致性)
	// =============================================================
	public static Files files = Gdx.files;
	public static Application app = Gdx.app;
	public static Audio audio = Gdx.audio;

	// =============================================================
	// 2. 核心代理 (可被编辑器 Hook)
	// =============================================================
	/** 输入系统接口 (实机为 Gdx.input，编辑器为 EditorGameInput) */
	public static Input input = Gdx.input;

	/** 图形系统接口 (实机为 Gdx.graphics，编辑器为 EditorGameGraphics) */
	public static Graphics graphics = Gdx.graphics;

	// =============================================================
	// 3. 引擎特有模块
	// =============================================================
	/** 运行时脚本编译器 */
	public static IScriptCompiler compiler;

	/** 全局配置中心 */
	public static final Config config = new Config();

	/** 引擎偏好设置 (可能为 null，表示未初始化引导) */
	public static GDEngineConfig engineConfig;

	/** 当前运行模式 */
	public static Mode mode = Mode.RELEASE;

	/**
	 * 动态脚本类加载器<br>
	 * 默认是系统加载器。<br>
	 * 当 GameRunner 启动或编辑器加载项目时，这会被替换为包含用户代码的加载器。<br>
	 * [修复] 默认使用加载当前 Gd 类的加载器 (Android下为 PathClassLoader)，确保能找到引擎内置组件。<br>
	 * 不要使用 getSystemClassLoader()，它在 Android 上看不到 APK 里的类。
	 */
	public static ClassLoader scriptClassLoader = Gd.class.getClassLoader();

	/** 全局浏览器服务 */
	public static IWebBrowser browser; // [New]

	/**
	 * 初始化引擎环境 (依赖注入)
	 * <p>
	 * 调用此方法时，具体的 Input/Graphics 实现类应已由启动器或编辑器创建完毕。
	 * </p>
	 *
	 * @param runMode 运行模式 (EDITOR / RELEASE)
	 * @param inputImpl 具体的输入实现 (Nullable, 默认为 Gdx.input)
	 * @param graphicsImpl 具体的图形实现 (Nullable, 默认为 Gdx.graphics)
	 * @param compilerImpl 脚本编译器 (Nullable)
	 */
	public static void init(Mode runMode, Input inputImpl, Graphics graphicsImpl, IScriptCompiler compilerImpl) {
		mode = runMode;

		if (inputImpl != null) input = inputImpl;
		else input = Gdx.input;
		if (graphicsImpl != null) graphics = graphicsImpl;
		else graphics = Gdx.graphics;
		if (!PlatformImpl.isAndroidUser()) files = new DesktopFiles(Gdx.files); // Desktop 使用代理，解决 JAR 包 list 问题
		else files = Gdx.files; // Android 原生支持良好，直接透传

		// 刷新基础引用，防止 Gdx 上下文重建后引用失效
		app = Gdx.app;
		audio = Gdx.audio;

		if (compilerImpl != null) compiler = compilerImpl;

		// 尝试静默加载配置
		if (GDEngineConfig.tryLoad()) {
			engineConfig = GDEngineConfig.getInstance();
		}

		// 默认实现：兜底使用 Gdx.net.openURI (防空指针)
		if (browser == null) {
			browser = new IWebBrowser() {
				@Override public void openUrl(String url, String title) { Gdx.net.openURI(url); }
				@Override public void close() {}
				@Override public boolean isEmbedded() { return false; }
			};
		}
	}

	// [新增] 允许平台层注入实现
	public static void setWebBrowser(IWebBrowser browserImpl) {
		browser = browserImpl;
	}

	// =============================================================
	// 4. 定义与枚举
	// =============================================================

	public enum Mode {
		RELEASE, // 实机运行模式
		EDITOR   // 编辑器预览模式
	}

	public static class Config {
		// 逻辑设计分辨率 (默认 960x540)
		public float logicWidth = 960;
		public float logicHeight = 540;

		// 视口适配策略
		public ViewportType viewportType = ViewportType.FIT;
	}

	public enum ViewportType {
		FIT, EXTEND, STRETCH
	}
}
