package com.goldsprite;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.audio.SynthAudio;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.log.DebugConsole;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.screens.ExampleSelectScreen;
import com.kotcrab.vis.ui.VisUI;
import com.goldsprite.gdengine.utils.ThreadedDownload;

public class GdxLauncher extends Game {int k60;
	private IScriptCompiler scriptCompiler; // 去掉 final，允许后期注入
	private Stage toastStage;
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
		// 如果是 Android 端且没有编译器，说明权限还在申请中
		// 我们先暂停初始化，防止读取资源报错
		if (userType == Application.ApplicationType.Android && scriptCompiler == null) return;
		initGameContent();
	}
	// Android 端拿到权限后调用此方法注入编译器并启动
	public void onAndroidReady(IScriptCompiler compiler) {
		this.scriptCompiler = compiler;
		// 在 GL 线程执行初始化
		Gdx.app.postRunnable(this::initGameContent);
	}

	// 真正的初始化逻辑提取出来
	private void initGameContent() {
		if (isInitialized) return;

		VisUIHelper.loadWithChineseFont();
		SynthAudio.init();
		userType = Gdx.app.getType();
		debug = Debug.getInstance();
		debug.initUI();
		toastStage = new Stage(new ScreenViewport());
		toastStage.addActor(ToastUI.inst());

		// 注入原生实现和编译器
		Gd.init(Gd.Mode.RELEASE, Gdx.input, Gdx.graphics, scriptCompiler);
		Debug.logT("Engine", "[GREEN]Gd initialized. Compiler available: %b", (scriptCompiler != null));

		new ScreenManager()
			.addScreen(new ExampleSelectScreen())
			.setLaunchScreen(ExampleSelectScreen.class);

		isInitialized = true;
		
		ToastUI.inst().show("开始自动下载引擎文档");
		ThreadedDownload.download(()->{
			ToastUI.inst().show("完成引擎文档下载");
		});
	}

	@Override
	public void render() {
		// [核心] 如果还没初始化，只清屏，不跑逻辑
		if (!isInitialized) {
			ScreenUtils.clear(0, 0, 0, 1);
			return;
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
//			Debug.showDebugUI = !Debug.showDebugUI;
			if(Debug.shortcuts) DebugConsole.autoSwitchState();
		}

		ScreenManager.getInstance().render();
		if (debug != null) debug.render();

		// toast
		toastStage.act();
		toastStage.draw();
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
		VisUI.dispose();
		SynthAudio.dispose();
	}
}
