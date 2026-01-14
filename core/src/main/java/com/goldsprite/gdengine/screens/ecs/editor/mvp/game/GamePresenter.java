package com.goldsprite.gdengine.screens.ecs.editor.mvp.game;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.ViewWidget;

public class GamePresenter {
	private final GamePanel view;
	private final OrthographicCamera camera;
	private Viewport viewport;

	private final NeonBatch neonBatch;

	public GamePresenter(GamePanel view, NeonBatch batch) {
		this.view = view;
		this.neonBatch = batch;
		view.setPresenter(this);

		camera = new OrthographicCamera();
		reloadViewport();
	}

	public void update(float delta) {
		camera.update();
		view.getRenderTarget().renderToFbo(() -> {
			viewport.apply();
			// 这里 GameWorld.render 负责调用 RenderSystem
			GameWorld.inst().render(neonBatch, camera);
		});
	}

	public void setViewportMode(String mode) {
		if(mode.equals("FIT")) {
			Gd.config.viewportType = Gd.ViewportType.FIT;
			view.setWidgetDisplayMode(ViewWidget.DisplayMode.FIT);
		} else if(mode.equals("STRETCH")) {
			Gd.config.viewportType = Gd.ViewportType.STRETCH;
			view.setWidgetDisplayMode(ViewWidget.DisplayMode.STRETCH);
		} else if(mode.equals("EXTEND")) {
			Gd.config.viewportType = Gd.ViewportType.EXTEND;
			view.setWidgetDisplayMode(ViewWidget.DisplayMode.COVER);
		}
		reloadViewport();
	}

	private void reloadViewport() {
		Gd.Config conf = Gd.config;
		if (conf.viewportType == Gd.ViewportType.FIT) viewport = new FitViewport(conf.logicWidth, conf.logicHeight, camera);
		else if (conf.viewportType == Gd.ViewportType.STRETCH) viewport = new StretchViewport(conf.logicWidth, conf.logicHeight, camera);
		else viewport = new ExtendViewport(conf.logicWidth, conf.logicHeight, camera);

		viewport.update(view.getRenderTarget().getFboWidth(), view.getRenderTarget().getFboHeight());

		// 记得更新 GameWorld 引用，虽然 GameWorld 可能存的是静态
		// GameWorld.inst().setReferences(viewport, camera); // 如果需要的话
	}

	public Viewport getViewport() { return viewport; }
	public OrthographicCamera getCamera() { return camera; }
}
