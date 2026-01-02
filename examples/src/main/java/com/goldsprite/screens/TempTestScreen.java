package com.goldsprite.screens;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.badlogic.gdx.graphics.Color;

public class TempTestScreen extends GScreen {

	private NeonBatch neonBatch;


	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	protected void initViewport() {
		uiViewportScale = 0.3f;
		viewSizeShort = 540;
		viewSizeLong = 960;
		super.initViewport();
		//先ui视口再world相机
		autoCenterWorldCamera = true;
		setWorldScale(1.0f);
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();
	}

	float k;
	@Override
	public void render0(float delta) {
		neonBatch.setProjectionMatrix(worldCamera.combined);
		neonBatch.begin();

		k-=delta * 40;
		neonBatch.drawRegularPolygon(getWorldCenter().x, getWorldCenter().y, 50, 7, k, 4, Color.RED, false);

		neonBatch.end();
	}
}
