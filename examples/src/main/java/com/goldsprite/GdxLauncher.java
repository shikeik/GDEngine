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
		// 【补漏】视口变化时通知 Gd 配置 (虽然 Release 模式下由 GameWorld 自己管，但保持同步是个好习惯)
		// Gd.config.logicWidth = ...;
	}

	@Override
	public void dispose() {
		ScreenManager.getInstance().dispose();
		if (debug != null) debug.dispose();
		batch.dispose();
		VisUI.dispose();
		SynthAudio.dispose(); // [新增] 关闭线程和设备
	}


	private void testAndroidScript() {
		new Thread(() -> {
			try {
				// 注意：使用转义的双引号 \"
				String code =
					"package com.test;" +
						"public class HelloAndroid {" +
						"    public String greet() {" +
						"        return \"Hello from Dynamic Dex! Time: " + System.currentTimeMillis() + "\";" +
						"    }" +
						"}";

				// 编译！
				Class<?> cls = Gd.compiler.compile("com.test.HelloAndroid", code);

				if (cls != null) {
					Object obj = cls.newInstance();
					// 反射调用方法
					java.lang.reflect.Method m = cls.getMethod("greet");
					String result = (String) m.invoke(obj);

					Gdx.app.postRunnable(() -> {
						Debug.log("SCRIPT: 脚本运行结果: " + result);
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
				Debug.log("testCode编译异常: " + e.getCause());
			}
		}).start();
	}
}
