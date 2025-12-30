package com.goldsprite.solofight.screens.refactor.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.component.FsmComponent;
import com.goldsprite.solofight.refactor.ecs.component.TransformComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import com.goldsprite.solofight.refactor.ecs.fsm.State;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.math.MathUtils;

public class EcsFsmTestScreen extends ExampleGScreen {

	private NeonBatch neonBatch;
	private Stage stage;
	private GObject player;
	
	// 模拟输入信号
	private boolean inputMove = false;
	private boolean inputAttack = false;

	@Override
	public String getIntroduction() {
		return "FSM 状态机测试\nPriority: Attack(2) > Move(1) > Idle(0)";
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();
		
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
		new GameWorld();

		initUI();
		initEntity();
	}

	private void initEntity() {
		player = new GObject("PlayerFighter");
		player.getTransform().setPosition(400, 300);
		
		FsmComponent fsm = player.addComponent(FsmComponent.class);
		
		// 注册状态
		fsm.registerState(new TestIdleState(), 0);   // P0
		fsm.registerState(new TestMoveState(), 1);   // P1
		fsm.registerState(new TestAttackState(), 2); // P2
	}

	private void initUI() {
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		Table root = new Table();
		root.setFillParent(true);
		root.left().bottom().pad(50);
		stage.addActor(root);

		// 模拟按住 Move 键
		final VisTextButton btnMove = new VisTextButton("Hold Move (P1)");
		btnMove.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
			public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
				inputMove = true; return true;
			}
			public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
				inputMove = false;
			}
		});
		root.add(btnMove).size(150, 60).padRight(20);

		// 模拟按住 Attack 键
		final VisTextButton btnAtk = new VisTextButton("Hold Attack (P2)");
		btnAtk.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
			public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
				inputAttack = true; return true;
			}
			public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
				inputAttack = false;
			}
		});
		root.add(btnAtk).size(150, 60);
	}

	@Override
	public void render0(float delta) {
		GameWorld.inst().update(delta);

		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		
		neonBatch.drawRect(0, 0, getWorldSize().x, getWorldSize().y, 0, 5, Color.CYAN, false);
		
		// 绘制玩家，根据状态变色
		FsmComponent fsm = player.getComponent(FsmComponent.class);
		String stateName = fsm.getCurrentStateName();
		Color c = Color.GRAY;
		if (stateName.contains("Idle")) c = Color.CYAN;
		if (stateName.contains("Move")) c = Color.GREEN;
		if (stateName.contains("Attack")) c = Color.RED;
		
		TransformComponent t = player.getTransform();
		neonBatch.drawCircle(t.position.x, t.position.y, 40, 0, c, 16, true);
		
		neonBatch.end();

		stage.act();
		stage.draw();
		
		DebugUI.info("State: %s", stateName);
		DebugUI.info("Input: Move=%b, Atk=%b", inputMove, inputAttack);
	}

	// --- 内部测试状态类 ---

	class TestIdleState extends State {
		@Override public boolean canEnter() { return true; } // 总是可以进入作为兜底
		@Override public void running(float delta) {
			// Idle 逻辑：轻微呼吸动画
		}
	}

	class TestMoveState extends State {
		@Override public boolean canEnter() { return inputMove; } // 只有按下 Move 且优先级够高时进入
		@Override public void running(float delta) {
			// Move 逻辑：移动坐标
			entity.getTransform().position.x += 100 * delta;
			if (entity.getTransform().position.x > 800) entity.getTransform().position.x = 200;
		}
	}

	class TestAttackState extends State {
		@Override public boolean canEnter() { return inputAttack; }
		@Override public void running(float delta) {
			// Attack 逻辑：原地震动
			float shake = MathUtils.sin(GameWorld.getTotalDeltaTime() * 50) * 5;
			entity.getTransform().position.y = 300 + shake;
		}
		@Override public void exit() {
			entity.getTransform().position.y = 300; // 归位
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (stage != null) stage.dispose();
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
	}
}
