package com.goldsprite;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils; // 记得引入这个
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.audio.SynthAudio;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.screens.ExampleSelectScreen;
import com.kotcrab.vis.ui.VisUI;

public class GdxLauncher extends Game {
	private IScriptCompiler scriptCompiler; // 去掉 final，允许后期注入
	public SpriteBatch batch;
	public Debug debug;
	private Application.ApplicationType userType;

	// [新增] 标记是否已初始化完成
	private boolean isInitialized = false;

	public GdxLauncher() {
		this(null);
	}

	public GdxLauncher(IScriptCompiler scriptCompiler) {
		this.scriptCompiler = scriptCompiler;
	}

	@Override
	public void create() {
		userType = Gdx.app.getType();

		float scl = 1.2f;
		Viewport uiViewport = new ExtendViewport(540 * scl, 960 * scl, new OrthographicCamera());
		new ScreenManager(uiViewport);

		// 如果是 Android 端且没有编译器，说明权限还在申请中
		// 我们先暂停初始化，防止读取资源报错
		if (userType == Application.ApplicationType.Android && scriptCompiler == null) {
			return;
		}

		initGameContent();
	}

	// [新增] 真正的初始化逻辑提取出来
	private void initGameContent() {
		if (isInitialized) return;

		batch = new SpriteBatch();
		VisUIHelper.loadWithChineseFont();

		debug = Debug.getInstance();
		debug.initUI();

		SynthAudio.init();

		// 【修改点】注入原生实现和编译器
		Gd.init(Gd.Mode.RELEASE, Gdx.input, Gdx.graphics, scriptCompiler);
		Debug.logT("Engine", "Gd initialized. Compiler available: %b", (scriptCompiler != null));

		ScreenManager.getInstance()
			.addScreen(new ExampleSelectScreen())
			.setLaunchScreen(ExampleSelectScreen.class);

		isInitialized = true;
	}

	// [新增] Android 端拿到权限后调用此方法注入编译器并启动
	public void onAndroidReady(IScriptCompiler compiler) {
		this.scriptCompiler = compiler;
		// 在 GL 线程执行初始化
		Gdx.app.postRunnable(this::initGameContent);
	}

	@Override
	public void render() {
		// [核心] 如果还没初始化，只清屏，不跑逻辑
		if (!isInitialized) {
			ScreenUtils.clear(0, 0, 0, 1);
			return;
		}

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
}
