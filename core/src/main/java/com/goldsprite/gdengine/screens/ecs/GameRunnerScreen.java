package com.goldsprite.gdengine.screens.ecs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.core.scripting.ScriptResourceTracker;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.system.SkeletonSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.neonbatch.NeonStage; // [新增]
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameGraphics;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameInput;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;

public class GameRunnerScreen extends GScreen {

	private final IGameScriptEntry gameEntry;
	private GameWorld world;
	private NeonBatch neonBatch;
	private NeonStage uiStage; // [新增] UI层

	public GameRunnerScreen(IGameScriptEntry gameEntry) {
		this.gameEntry = gameEntry;

		// [核心修复] 捕获脚本的 ClassLoader
		if (gameEntry != null) {
			// gameEntry 是由编译器加载的，它的 ClassLoader 就是那个能找到用户代码的 URLClassLoader
			Gd.scriptClassLoader = gameEntry.getClass().getClassLoader();
			Debug.logT("Runner", "注入脚本 ClassLoader: " + Gd.scriptClassLoader);
		}
	}

	@Override
	protected void initViewport() {
		//autoCenterWorldCamera = false; // Default is false now
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.5f : 2.5f;
		worldScale = 0.4f;
		super.initViewport();
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		// [新增] 每次运行前确保清理一次（防止上次异常退出残留）
		ScriptResourceTracker.disposeAll();

		neonBatch = new NeonBatch();

		try {
			GameWorld.autoDispose();
		} catch (Exception ignored) {}

		world = new GameWorld();
		world.setReferences(getUIViewport(), getWorldCamera());

		new SkeletonSystem();
		new WorldRenderSystem(neonBatch, getWorldCamera());

		// 初始化为实机模式(防止从编辑器模式返回后无编辑器实例报错)
		Gd.init(Gd.Mode.EDITOR, Gdx.input, Gdx.graphics, Gd.compiler);

		// [新增] 初始化 UI 层
		initUI();

		if (gameEntry != null) {
			Debug.logT("Runner", "启动脚本逻辑: %s", gameEntry.getClass().getName());
			gameEntry.onStart(world);
		}
	}

	private void initUI() {
		uiStage = new NeonStage(getUIViewport());
		getImp().addProcessor(uiStage);

		Table root = new Table();
		root.setFillParent(true);

		root.top().left().pad(20);
		uiStage.addActor(root);

		VisTextButton btnStop = new VisTextButton("STOP");
		btnStop.setColor(Color.RED);
		btnStop.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				stopGame();
			}
		});
		// 稍微大一点，半透明
		btnStop.getColor().a = 0.6f;
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

		world.render(neonBatch, worldCamera);

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
		if (uiStage != null) uiStage.dispose();

		// [新增] 核心清理：释放所有脚本加载的图片
		ScriptResourceTracker.disposeAll();

		// [核心修复] 注释掉这一行！
		// 在编辑器模式下，我们需要保留这个 ClassLoader，
		// 否则回到编辑器后，Inspector 就无法反射用户组件了。
		// Gd.scriptClassLoader = ClassLoader.getSystemClassLoader();
	}
}
