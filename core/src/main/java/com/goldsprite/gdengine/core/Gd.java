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

/**
 * 引擎核心 API 入口 (GDEngine Facade)
 * <p>
 * 职责：
 * 1. 提供全局唯一的静态访问点 (类似 Gdx)。
 * 2. 通过依赖注入 (init) 持有具体的输入/图形实现。
 * 3. 维护引擎级配置 (Config)。
 * </p>
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

	/** 引擎偏好设置 (Editor Preferences) [新增] */
	public static GDEngineConfig engineConfig;

	/** 当前运行模式 */
	public static Mode mode = Mode.RELEASE;

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

		// Files 注入逻辑
		if (PlatformImpl.isAndroidUser()) files = Gdx.files; // Android 原生支持良好，直接透传
		else files = new DesktopFiles(Gdx.files); // Desktop 使用代理，解决 JAR 包 list 问题

		if (compilerImpl != null) compiler = compilerImpl;

		// 刷新基础引用，防止 Gdx 上下文重建后引用失效
		app = Gdx.app;
		audio = Gdx.audio;

		// 加载引擎配置
		engineConfig = GDEngineConfig.load();
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
