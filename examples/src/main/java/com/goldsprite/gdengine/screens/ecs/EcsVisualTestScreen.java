package com.goldsprite.gdengine.screens.ecs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gdengine.ecs.ComponentManager;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.FsmComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.fsm.State;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.neonbatch.NeonStage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.List;

import com.goldsprite.gdengine.tests.ShapeRendererComponent;

public class EcsVisualTestScreen extends ExampleGScreen {

	private NeonBatch neonBatch;
	private SpriteBatch uiBatch;
	private BitmapFont font;
	private Stage uiStage;

	private GameWorld world;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public String getIntroduction() {
		return "ECS 可视化测试\n验证: 矩阵变换(父子级)、TimeScale、FSM";
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();
		uiBatch = new SpriteBatch();
		font = new BitmapFont();

		// 1. 初始化世界 (每次进入先清理)
		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		world = new GameWorld();
		world.setReferences(getUIViewport(), worldCamera);

		// 2. 创建场景 (太阳系)
		createSolarSystem();

		// 3. 创建 UI
		createUI();
	}

	private void createSolarSystem() {
		// [修改] 迁移到中心坐标系 (0,0)
		float cx = 0;
		float cy = 0;

		// --- 1. 太阳 (Root) ---
		GObject sun = new GObject("Sun");
		sun.transform.setPosition(cx, cy);
		sun.addComponent(ShapeRendererComponent.class)
			.set(ShapeRendererComponent.ShapeType.CIRCLE, Color.ORANGE, 100f);
		// 太阳自转 (45度/秒)
		sun.getComponent(ShapeRendererComponent.class).rotateSpeed = 45f;

		// --- 2. 地球 (Child of Sun) ---
		GObject earth = new GObject("Earth");
		earth.setParent(sun); // 【关键】设置父级
		// 局部坐标：在太阳右边 300 像素
		earth.transform.setPosition(300, 0);
		earth.addComponent(ShapeRendererComponent.class)
			 .set(ShapeRendererComponent.ShapeType.BOX, Color.CYAN, 50f);
		// 地球自转更快
		earth.getComponent(ShapeRendererComponent.class).rotateSpeed = 180f;

		// 给地球加 FSM (测试状态机)
		FsmComponent fsm = earth.addComponent(FsmComponent.class);
		fsm.addState(new IdleState(), 0);
		fsm.addState(new FastSpinState(), 10);

		// --- 3. 月球 (Child of Earth) ---
		GObject moon = new GObject("Moon");
		moon.setParent(earth); // 【关键】设置父级
		// 局部坐标：在地球右边 80 像素
		moon.transform.setPosition(80, 0);
		moon.addComponent(ShapeRendererComponent.class)
			.set(ShapeRendererComponent.ShapeType.TRIANGLE, Color.LIGHT_GRAY, 20f);
		// 月球疯狂自转
		moon.getComponent(ShapeRendererComponent.class).rotateSpeed = -360f;
	}

	private void createUI() {
		uiStage = new NeonStage(getUIViewport());
		getImp().addProcessor(uiStage);

		Table root = new Table();
		root.setFillParent(true);
		root.left().top().pad(20);
		uiStage.addActor(root);

		// TimeScale
		root.add(new VisLabel("Time Scale:")).left().row();
		VisSlider slider = new VisSlider(0, 2, 0.1f, false);
		slider.setValue(1.0f);
		slider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				GameWorld.timeScale = slider.getValue();
			}
		});
		root.add(slider).width(300).padBottom(20).row();

		// FSM Toggle
		VisTextButton btnFsm = new VisTextButton("Toggle Earth FSM (Fast/Idle)");
		btnFsm.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				// 这是一个简单的黑客式测试：直接通过静态变量控制状态
				FastSpinState.trigger = !FastSpinState.trigger;
			}
		});
		root.add(btnFsm).width(300).row();
	}

	@Override
	public void render0(float delta) {
		// 1. 驱动 ECS
		world.update(delta);

		// 2. 渲染 (手动遍历 ShapeComponent)
		List<GObject> renderables = ComponentManager.getEntitiesWithComponents(ShapeRendererComponent.class);

		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		drawGrid();
		for (GObject obj : renderables) {
			ShapeRendererComponent shape = obj.getComponent(ShapeRendererComponent.class);
			if (shape != null && shape.isEnable()) {
				shape.draw(neonBatch);
			}
		}
		neonBatch.end();

		// 3. 画文字 Debug (FSM 状态)
		uiBatch.setProjectionMatrix(getWorldCamera().combined);
		uiBatch.begin();
		for (GObject obj : renderables) {
			FsmComponent fsm = obj.getComponent(FsmComponent.class);
			if (fsm != null) {
				float x = obj.transform.worldPosition.x;
				float y = obj.transform.worldPosition.y;
				font.draw(uiBatch, fsm.getCurrentStateName(), x - 20, y + 60);
			}
		}
		uiBatch.end();

		// 4. 画 UI
		uiStage.act(delta);
		uiStage.draw();
	}

	private void drawGrid() {
		neonBatch.setColor(1, 1, 1, 0.1f);
		float cx = getWorldCenter().x;
		float cy = getWorldCenter().y;
		neonBatch.drawLine(cx - 500, cy, cx + 500, cy, 1, Color.GRAY);
		neonBatch.drawLine(cx, cy - 500, cx, cy + 500, 1, Color.GRAY);
		neonBatch.setColor(Color.WHITE);
	}

	@Override
	public void dispose() {
		if (world != null) world.dispose();
		if (neonBatch != null) neonBatch.dispose();
		if (uiBatch != null) uiBatch.dispose();
		if (font != null) font.dispose();
		if (uiStage != null) uiStage.dispose();
	}

	// --- 内部状态类 ---
	public static class IdleState extends State {
		@Override
		public void onUpdate(float delta) {
			entity.getComponent(ShapeRendererComponent.class).rotateSpeed = 100f;
		}
	}

	public static class FastSpinState extends State {
		public static boolean trigger = false;
		@Override public boolean canEnter() { return trigger; }

		@Override public void enter() {
			entity.getComponent(ShapeRendererComponent.class).color.set(Color.RED);
			entity.getComponent(ShapeRendererComponent.class).rotateSpeed = 800f;
		}
		@Override public void exit() {
			entity.getComponent(ShapeRendererComponent.class).color.set(Color.CYAN);
		}

		@Override
		public void onUpdate(float delta) {
			// [新增] 核心修复：当触发器关闭时，主动降级回 Idle
			if (!trigger) {
				// 使用 fsm.changeState 手动切换
				fsm.changeState(IdleState.class);
			}
		}
	}
}
