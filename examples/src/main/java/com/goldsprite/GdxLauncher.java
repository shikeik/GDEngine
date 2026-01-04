package com.goldsprite;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.audio.SynthAudio;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.ecs.editor.Gd;
import com.goldsprite.screens.ExampleSelectScreen;
import com.kotcrab.vis.ui.VisUI;

import java.lang.reflect.Method;

public class GdxLauncher extends Game {
	// 【新增】保存编译器引用
	private final IScriptCompiler scriptCompiler;
	public SpriteBatch batch;
	public Debug debug;
	private Application.ApplicationType userType;

	public GdxLauncher() {
		this(null);
	}

	// 【新增】构造函数，强制要求传入编译器
	// 如果是纯实机运行不需要编译功能，可以传 null
	public GdxLauncher(IScriptCompiler scriptCompiler) {
		this.scriptCompiler = scriptCompiler;
	}

	@Override
	public void create() {
		userType = Gdx.app.getType();
		batch = new SpriteBatch();

		// 1. 初始化 VisUI (注入中文字体)
		VisUIHelper.loadWithChineseFont();

		// 初始化 DebugUI
		debug = Debug.getInstance();
		debug.initUI();

		SynthAudio.init(); // [新增] 启动音频线程

		// 2. 【核心】初始化引擎全局代理 (Gd)
		// 启动时默认为 RELEASE 模式，传入我们从 Launcher 带来的编译器
		Gd.init(Gd.Mode.RELEASE, null, null, scriptCompiler);
		Debug.logT("Engine", "Gd initialized. Compiler available: %b", (scriptCompiler != null));

		// 3. 测试脚本运行
		testAndroidScript();

		// 4. 设置全局视口
		float scl = 1.2f;
		Viewport uiViewport = new ExtendViewport(540 * scl, 960 * scl, new OrthographicCamera());

		// 3. 初始化屏幕管理器
		new ScreenManager(uiViewport);
		Debug.log("Main: initViewport: %.0f, %.0f", uiViewport.getWorldWidth(), uiViewport.getWorldHeight());


		// 5. 进入演示列表
		ScreenManager.getInstance()
			.addScreen(new ExampleSelectScreen())
			.setLaunchScreen(ExampleSelectScreen.class);
	}

	@Override
	public void render() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
			Debug.showDebugUI = !Debug.showDebugUI;
		}

		ScreenManager.getInstance().render();
		if (debug != null) debug.render();
	}

	@Override
	public void resize(int width, int height) {
		ScreenManager.getInstance().resize(width, height);
		if (debug != null) debug.resize(width, height);
	}

	@Override
	public void dispose() {
		ScreenManager.getInstance().dispose();
		if (debug != null) debug.dispose();
		batch.dispose();
		VisUI.dispose();
		SynthAudio.dispose();
	}

	private void testAndroidScript() {
		if (scriptCompiler == null) return;

		new Thread(() -> {
			// 稍微延时，确保UI初始化完毕，能看到Log
			try { Thread.sleep(500); } catch (InterruptedException ignored) {}

			try {
				// 1. 获取真实路径 (Android Private Storage)
				// 对应手动创建的: Projects/HelloTest/Scripts/com/game/Main.java
				String projectPath = Gdx.files.local("Projects/HelloTest").file().getAbsolutePath();
				String mainClass = "com.game.Main";

				Gdx.app.postRunnable(() -> Debug.logT("Engine", "开始编译脚本项目: %s", projectPath));

				// 2. 编译并加载
				Class<?> cls = Gd.compiler.compile(mainClass, projectPath);

				if (cls != null) {
					// 3. 反射调用 main 方法
					Gdx.app.postRunnable(() -> Debug.logT("Engine", "✅ 编译成功! 正在执行 main()..."));

					Method method = cls.getMethod("main", String[].class);
					// 静态方法传 null, 参数数组转为 Object 避免变长参数歧义
					method.invoke(null, (Object) new String[0]);

					Gdx.app.postRunnable(() -> Debug.logT("Engine", "执行完毕."));
				} else {
					Gdx.app.postRunnable(() -> Debug.logT("Engine", "❌ 编译失败 (Cls is null)"));
				}
			} catch (Exception e) {
				e.printStackTrace();
				Gdx.app.postRunnable(() -> Debug.logT("Engine", "❌ 脚本测试异常: %s", e.getCause()));
			}
		}).start();
	}
}
