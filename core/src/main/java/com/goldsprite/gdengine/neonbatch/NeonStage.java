package com.goldsprite.gdengine.neonbatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class NeonStage extends Stage {
	public NeonStage() {
		this(new ScreenViewport());
	}

	public NeonStage(Viewport viewport) {
		this(viewport, new NeonBatch());
	}

	public NeonStage(Viewport viewport, NeonBatch batch) {
		super(viewport, batch);
	}
}
