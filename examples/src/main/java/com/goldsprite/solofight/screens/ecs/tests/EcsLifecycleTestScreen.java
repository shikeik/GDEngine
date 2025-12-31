package com.goldsprite.solofight.screens.ecs.tests;

import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;

public class EcsLifecycleTestScreen extends ExampleGScreen {

	private NeonBatch neonBatch;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();
	}


}
