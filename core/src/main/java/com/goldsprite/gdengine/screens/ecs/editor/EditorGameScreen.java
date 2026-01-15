package com.goldsprite.gdengine.screens.ecs.editor;

import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.log.Debug;

public class EditorGameScreen extends GScreen {
	private EditorController realGame;

	@Override
	protected void initViewport() {
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.65f : 2.1f;
		super.initViewport();
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		realGame = new EditorController(this);
		realGame.create(uiViewport);
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


