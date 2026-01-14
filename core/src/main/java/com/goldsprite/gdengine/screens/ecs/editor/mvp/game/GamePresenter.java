package com.goldsprite.gdengine.screens.ecs.editor.mvp.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.EditorState;
import com.goldsprite.gdengine.screens.ecs.editor.ViewWidget;

public class GamePresenter {
	private final GamePanel view;
	private final OrthographicCamera camera;
	private Viewport viewport;

	private final NeonBatch neonBatch;
	private final BitmapFont font; // 用于绘制提示文字

	// [新增] 当前编辑器状态
	private EditorState currentState = EditorState.CLEAN;

	public GamePresenter(GamePanel view, NeonBatch batch) {
		this.view = view;
		this.neonBatch = batch;
		view.setPresenter(this);

		camera = new OrthographicCamera();
		// 从 Skin 获取字体，或者使用 FontUtils
		font = FontUtils.generateAutoClarity(40);
		reloadViewport();
	}

	// [新增] 状态设置接口
	public void setEditorState(EditorState state) {
		this.currentState = state;
	}

	public void update(float delta) {
		camera.update();
		view.getRenderTarget().renderToFbo(() -> {
			viewport.apply();

			// [核心逻辑] 根据状态决定渲染内容
			if (currentState == EditorState.CLEAN) {
				// 正常渲染游戏画面
				GameWorld.inst().render(neonBatch, camera);
			} else {
				renderWarningOverlay();
			}
		});
	}

	GlyphLayout layout = new GlyphLayout();
	private void renderWarningOverlay() {
		// 1. 清屏 (深红色表示 Dirty，深灰色表示 Compiling)
		float r = (currentState == EditorState.DIRTY) ? 0.2f : 0.1f;
		Gdx.gl.glClearColor(r, 0.1f, 0.1f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 2. 绘制提示文字
		neonBatch.setProjectionMatrix(camera.combined);
		neonBatch.begin();

		String msg = (currentState == EditorState.DIRTY)
			? "WARNING: Code is Dirty!\nPlease Build Project."
			: "Compiling Scripts...\nPlease Wait.";

		layout.setText(font, msg);

		Color c = (currentState == EditorState.DIRTY) ? Color.ORANGE : Color.CYAN;

		font.setColor(c);

		// 居中绘制警告文字
		font.draw(neonBatch, msg, camera.position.x - layout.width / 2, camera.position.y + layout.height / 2, layout.width, Align.center, true);

		neonBatch.end();
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
