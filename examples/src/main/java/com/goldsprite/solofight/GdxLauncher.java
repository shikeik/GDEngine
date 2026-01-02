package com.goldsprite.solofight;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.log.Debug;
import com.goldsprite.gameframeworks.assets.VisUIHelper;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.solofight.screens.ExampleSelectScreen;
import com.kotcrab.vis.ui.VisUI;
import com.goldsprite.solofight.core.audio.SynthAudio;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GdxLauncher extends Game {int k16;
	private Application.ApplicationType userType;

	public SpriteBatch batch;
	public Debug debug;

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

		// 4. 设置全局视口
		float scl = 1.2f;
		Viewport uiViewport = new ExtendViewport(540*scl, 960*scl, new OrthographicCamera());

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
		if(Gdx.input.isKeyJustPressed(Input.Keys.K)) {
			Debug.showDebugUI = !Debug.showDebugUI;
		}

		ScreenManager.getInstance().render();
		if (debug != null) debug.render();
	}

	@Override
	public void resize(int width, int height) {
		ScreenManager.getInstance().resize(width, height);
		if(debug != null) debug.resize(width, height);
	}

	@Override
	public void dispose() {
		ScreenManager.getInstance().dispose();
		if (debug != null) debug.dispose();
		batch.dispose();
		VisUI.dispose();
		SynthAudio.dispose(); // [新增] 关闭线程和设备
	}
}
