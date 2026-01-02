package com.goldsprite.gdengine.screens.ecs.editor;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class SceneViewDemoScreen extends GScreen {
	private NeonBatch neonBatch;
	
	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();
	}

	@Override
	public void render0(float delta) {
	}
	
}
