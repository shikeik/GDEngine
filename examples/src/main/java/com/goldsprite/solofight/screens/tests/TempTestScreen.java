package com.goldsprite.solofight.screens.tests;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;
import com.badlogic.gdx.graphics.Color;

public class TempTestScreen extends ExampleGScreen {

	private NeonBatch neonBatch;
	
	
	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}
	
	@Override
	public void create() {
		autoCenterWorldCamera = true;
		
		neonBatch = new NeonBatch();
	}

	float k;
	@Override
	public void render0(float delta) {
		neonBatch.setProjectionMatrix(worldCamera.combined);
		neonBatch.begin();
		
		neonBatch.drawRegularPolygon(getWorldCenter().x, getWorldCenter().y, 50, 7, k+=delta, 4, Color.RED, false);
		
		neonBatch.end();
	}
}
