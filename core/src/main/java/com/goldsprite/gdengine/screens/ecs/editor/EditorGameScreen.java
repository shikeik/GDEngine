package com.goldsprite.gdengine.screens.ecs.editor;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.log.Debug;

public class EditorGameScreen extends GScreen {
	private EditorController realGame;

	@Override
	protected void initViewport() {
		//uiViewportScale = 1.5f;
		super.initViewport();
	}
	
	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void show() {
		super.show();
		Debug.showDebugUI = false;
	}

	@Override
	public void hide() {
		super.hide();
		Debug.showDebugUI = true;
	}

	@Override
	public void create() {
		realGame = new EditorController(this);
		realGame.create();
	}

	@Override
	public void render(float delta) {
		realGame.render(delta);
	}

	@Override
	public void resize(int width, int height) {
		realGame.resize(width, height);
	}

	@Override
	public void dispose() {
		realGame.dispose();
	}
}


