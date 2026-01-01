// 文件: ./examples/src/main/java/com/goldsprite/solofight/screens/ecs/tests/SkeletonVisualScreen.java
package com.goldsprite.solofight.screens.ecs.tests.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.ecs.system.SceneSystem;
import com.goldsprite.gameframeworks.log.Debug;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;
import com.goldsprite.solofight.core.neonbatch.NeonStage;
import com.goldsprite.solofight.ecs.skeleton.*;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class SkeletonVisualScreen extends ExampleGScreen {

	private GameWorld world;
	private NeonBatch neonBatch;
	private NeonStage uiStage;

	private NeonAnimatorComponent playerAnimator;
	private float mixDuration = 0.5f;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public String getIntroduction() {
		return "骨骼动画集成测试 (v1.5.6)\n[封装版]";
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

		// 1. ECS Init
		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		world = new GameWorld();
		world.setReferences(getUIViewport(), worldCamera);

		world.registerSystem(new SceneSystem());
		world.registerSystem(new SkeletonSystem());
		world.registerSystem(new SkeletonRenderSystem(neonBatch, getWorldCamera()));

		// 2. Entity Init (使用工厂)
		createTestEntity();

		// 3. UI Init
		initUI();
	}

	private void createTestEntity() {
		GObject player = new GObject("Player");
		player.transform.setPosition(0, -150);
		player.transform.setScale(1.5f);

		SkeletonComponent skelComp = player.addComponent(SkeletonComponent.class);
		playerAnimator = player.addComponent(NeonAnimatorComponent.class);

		// 调用工厂方法填充数据
		TestSkeletonFactory.buildStickman(skelComp.getSkeleton());
		TestAnimationFactory.setupAnimations(playerAnimator);

		playerAnimator.play("Idle");
	}

	private void initUI() {
		uiStage = new NeonStage(getUIViewport());
		getImp().addProcessor(uiStage);

		Table root = new Table();
		root.setFillParent(true);
		root.left().top().pad(20);
		uiStage.addActor(root);

		// UI Controls
		root.add(new VisLabel("Animation Controls")).colspan(2).left().padBottom(10).row();

		Table btnTable = new Table();
		VisTextButton btnIdle = new VisTextButton("CrossFade: Idle");
		btnIdle.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				playerAnimator.crossFade("Idle", mixDuration);
			}
		});

		VisTextButton btnAtk = new VisTextButton("CrossFade: Attack");
		btnAtk.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				playerAnimator.crossFade("Attack", mixDuration);
			}
		});

		btnTable.add(btnIdle).width(150).padRight(10);
		btnTable.add(btnAtk).width(150);
		root.add(btnTable).colspan(2).left().padBottom(20).row();

		root.add(new VisLabel("Mix Duration (s): ")).left();
		VisLabel lblMixVal = new VisLabel("0.5");
		VisSlider slMix = new VisSlider(0f, 2.0f, 0.1f, false);
		slMix.setValue(0.5f);
		slMix.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				mixDuration = slMix.getValue();
				lblMixVal.setText(String.format("%.1f", mixDuration));
			}
		});
		root.add(lblMixVal).padRight(10);
		root.add(slMix).width(200).row();

		root.add(new VisLabel("Time Scale: ")).left();
		VisLabel lblTimeVal = new VisLabel("1.0");
		VisSlider slTime = new VisSlider(0f, 2.0f, 0.1f, false);
		slTime.setValue(1.0f);
		slTime.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				GameWorld.timeScale = slTime.getValue();
				lblTimeVal.setText(String.format("%.1f", GameWorld.timeScale));
			}
		});
		root.add(lblTimeVal).padRight(10);
		root.add(slTime).width(200).row();
	}

	@Override
	public void render0(float delta) {
		world.update(delta);

		worldCamera.position.set(0, 0, 0);
		worldCamera.update();

		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		neonBatch.drawLine(-1000, 0, 1000, 0, 2, Color.GRAY);
		neonBatch.end();

		world.getSystem(SkeletonRenderSystem.class).update(delta);

		uiStage.act(delta);
		uiStage.draw();

		Debug.info("Anim: State Switching Test");
	}

	@Override
	public void dispose() {
		if(world!=null) world.dispose();
		if(neonBatch!=null) neonBatch.dispose();
		if(uiStage!=null) uiStage.dispose();
	}
}
