package com.goldsprite.solofight;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.VisUIHelper;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleSelectScreen;
import com.kotcrab.vis.ui.VisUI;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.goldsprite.solofight.core.audio.SynthAudio;

public class GdxLauncher extends Game {int k53;
	private Application.ApplicationType userType;

	public SpriteBatch batch;
	public DebugUI debugUI;

	@Override
	public void create() {
		userType = Gdx.app.getType();
		batch = new SpriteBatch();

		// 初始化 DebugUI
		debugUI = DebugUI.getInstance();

		SynthAudio.init(); // [新增] 启动音频线程

		// 1. 初始化 VisUI (注入中文字体)
		VisUIHelper.loadWithChineseFont();

		// 3. 初始化屏幕管理器
		ScreenManager.getInstance();

		// 4. 设置全局视口
		OrthographicCamera camera = new OrthographicCamera();
		float scl = 1.2f;
		ExtendViewport viewport = new ExtendViewport(540*scl, 960*scl, camera);
		ScreenManager.getInstance().setViewport(viewport);

		// 5. 进入演示列表
		ScreenManager.getInstance()
			.addScreen(new ExampleSelectScreen())
			.setLaunchScreen(ExampleSelectScreen.class);
	}

	@Override
	public void render() {
		GScreen curScreen = (GScreen)ScreenManager.getInstance().getCurScreen();
		curScreen.getViewport().apply();
		ScreenManager.getInstance().render();

		if(Gdx.input.isKeyJustPressed(Input.Keys.K)) {
			DebugUI.showDebugUI = !DebugUI.showDebugUI;
		}
		if (debugUI != null) debugUI.render();
	}

	@Override
	public void resize(int width, int height) {
		ScreenManager.getInstance().resize(width, height);
		if(debugUI != null)debugUI.resize(width, height);
	}

	@Override
	public void dispose() {
		ScreenManager.getInstance().dispose();
		if (debugUI != null) debugUI.dispose();
		batch.dispose();
		VisUI.dispose();
		SynthAudio.dispose(); // [新增] 关闭线程和设备
	}
}
