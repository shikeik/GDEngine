package com.goldsprite.gdengine.screens.ecs.editor;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;

public class RealGameScreen extends GScreen {
	private RealGame realGame;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		realGame = new RealGame();
		realGame.create();
	}

	@Override
	public void render(float delta) {
		realGame.render();
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


