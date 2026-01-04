package com.goldsprite.gdengine.screens.ecs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.system.SkeletonRenderSystem;
import com.goldsprite.gdengine.ecs.system.SkeletonSystem;
import com.goldsprite.gdengine.ecs.system.SpriteSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.neonbatch.NeonStage; // [新增]
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class GameRunnerScreen extends GScreen {

	private final IGameScriptEntry gameEntry;
	private GameWorld world;
	private NeonBatch neonBatch;
	private NeonStage uiStage; // [新增] UI层

	public GameRunnerScreen(IGameScriptEntry gameEntry) {
		this.gameEntry = gameEntry;
	}

	@Override
	protected void initViewport() {
		autoCenterWorldCamera = false;
		uiViewportScale = 1.5f;
		super.initViewport();
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

		try {
			GameWorld.autoDispose();
		} catch (Exception ignored) {}

		world = new GameWorld();
		world.setReferences(getUIViewport(), getWorldCamera());

		new SpriteSystem(neonBatch, getWorldCamera());
		new SkeletonSystem();
		new SkeletonRenderSystem(neonBatch, getWorldCamera());

		// [新增] 初始化 UI 层
		initUI();

		if (gameEntry != null) {
			Debug.logT("Runner", "启动脚本逻辑: %s", gameEntry.getClass().getName());
			gameEntry.onStart(world);
		}
	}

	private void initUI() {
		uiStage = new NeonStage(getUIViewport());
		getImp().addProcessor(uiStage); // 确保 UI 能接收输入

		Table root = new Table();
		root.setFillParent(true);
		root.top().right().pad(20);
		uiStage.addActor(root);

		// 红色停止按钮
		VisTextButton btnStop = new VisTextButton("STOP");
		btnStop.setColor(Color.RED);
		btnStop.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				stopGame();
			}
		});
		root.add(btnStop).width(80).height(40);
	}

	private void stopGame() {
		Debug.logT("Runner", "停止运行.");
		getScreenManager().popLastScreen(); // 返回编辑器
	}

	@Override
	public void render0(float delta) {
		drawGrid();

		if (gameEntry != null) {
			gameEntry.onUpdate(delta);
		}

		world.update(delta);

		// [新增] 绘制 UI
		uiStage.act(delta);
		uiStage.draw();
	}

	private void drawGrid() {
		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		neonBatch.setColor(1, 1, 1, 0.1f);
		neonBatch.drawLine(-1000, 0, 1000, 0, 2, Color.GRAY);
		neonBatch.drawLine(0, -1000, 0, 1000, 2, Color.GRAY);
		neonBatch.setColor(Color.WHITE);
		neonBatch.end();
	}

	@Override
	public void dispose() {
		if (world != null) world.dispose();
		if (neonBatch != null) neonBatch.dispose();
		if (uiStage != null) uiStage.dispose(); // [新增]
	}
}
