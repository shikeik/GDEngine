package com.goldsprite.solofight.screens.refactor.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.component.LifecycleLogComponent;
import com.goldsprite.solofight.refactor.ecs.component.TestRotatorComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class EcsLifecycleTestScreen extends ExampleGScreen {

    private SpriteBatch batch;
    private NeonBatch neonBatch;
    private Stage stage;

    private GObject parentObj;
    private GObject childObj;
    private GObject independentObj;

    @Override
    public String getIntroduction() {
        return "生命周期与层级测试\n验证: Destroy Recursion, Dynamic Add/Remove";
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        neonBatch = new NeonBatch(batch);

        // 1. 初始化 ECS 环境
        if (GameWorld.inst() != null) GameWorld.inst().dispose();
        new GameWorld();

        // 2. 初始化 UI
        initUI();

        // 3. 创建一个独立的常驻对象
        independentObj = new GObject("Independent_Obj");
        independentObj.getTransform().setPosition(800, 300);
        independentObj.addComponent(LifecycleLogComponent.class);
    }

    private void initUI() {
        stage = new Stage(getUIViewport(), batch);
        getImp().addProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.left().top().pad(20);
        stage.addActor(root);

        root.defaults().width(220).height(50).padBottom(10);

        // --- 测试组 1: 父子层级 ---
        VisTextButton btnSpawn = new VisTextButton("1. Spawn Parent & Child");
        btnSpawn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					spawnHierarchy();
				}
			});
        root.add(btnSpawn).row();

        VisTextButton btnKillParent = new VisTextButton("2. Destroy Parent (Cascading)");
        btnKillParent.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (parentObj != null && !parentObj.isDestroyed()) {
						DebugUI.log(">>> Requesting Parent Destroy...");
						parentObj.destroy();
						parentObj = null; 
						childObj = null; // 子物体引用也应视为无效
					} else {
						DebugUI.log("Parent not exist!");
					}
				}
			});
        root.add(btnKillParent).row();

        root.add().height(20).row(); // Spacer

        // --- 测试组 2: 动态组件 ---
        VisTextButton btnAddComp = new VisTextButton("3. Add Rotator to Independent");
        btnAddComp.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (independentObj != null && !independentObj.hasComponent(TestRotatorComponent.class)) {
						independentObj.addComponent(TestRotatorComponent.class);
						DebugUI.log("Added Rotator Component dynamically.");
					}
				}
			});
        root.add(btnAddComp).row();

        VisTextButton btnRemoveComp = new VisTextButton("4. Remove Rotator");
        btnRemoveComp.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (independentObj != null) {
						TestRotatorComponent comp = independentObj.getComponent(TestRotatorComponent.class);
						if (comp != null) {
							independentObj.removeComponent(comp);
							DebugUI.log("Removed Rotator Component dynamically.");
						}
					}
				}
			});
        root.add(btnRemoveComp).row();
    }

    private void spawnHierarchy() {
        if (parentObj != null && !parentObj.isDestroyed()) {
            DebugUI.log("Already spawned!");
            return;
        }

        // 创建父物体
        parentObj = new GObject("Parent_Unit");
        parentObj.getTransform().setPosition(400, 300);
        parentObj.addComponent(LifecycleLogComponent.class);

        // 创建子物体
        childObj = new GObject("Child_Unit");
        childObj.getTransform().setPosition(400, 400); // 相对还是绝对？目前 TransformComponent 是简单的 x,y，尚无层级计算，所以是世界坐标
        childObj.addComponent(LifecycleLogComponent.class);

        // 建立关系
        parentObj.addChild(childObj);
        DebugUI.log("Hierarchy Created: Parent -> Child");
    }

    @Override
    public void render0(float delta) {
        // 驱动 ECS
        GameWorld.inst().update(delta);

        // 绘制 (Debug View)
        batch.setProjectionMatrix(getWorldCamera().combined);
        neonBatch.begin();

        // 绘制父物体 (Blue)
        if (parentObj != null && !parentObj.isDestroyed()) {
            float px = parentObj.getTransform().position.x;
            float py = parentObj.getTransform().position.y;
            neonBatch.drawCircle(px, py, 30, 0, Color.CYAN, 16, true);

            // 简单模拟子物体跟随 (因为 TransformComponent 还没写层级矩阵计算)
            if (childObj != null && !childObj.isDestroyed()) {
                // 让子物体绕父物体转 (验证 update 是否执行)
                float time = GameWorld.getTotalDeltaTime(); // 需在 GameWorld 补 getter
                float cx = px + MathUtils.cos(time * 2) * 80;
                float cy = py + MathUtils.sin(time * 2) * 80;
                childObj.getTransform().setPosition(cx, cy);

                neonBatch.drawCircle(cx, cy, 15, 0, Color.RED, 16, true);
                neonBatch.drawLine(px, py, cx, cy, 2, Color.GRAY);
            }
        }

        // 绘制独立物体 (Green)
        if (independentObj != null && !independentObj.isDestroyed()) {
            float ix = independentObj.getTransform().position.x;
            float iy = independentObj.getTransform().position.y;
            neonBatch.drawCircle(ix, iy, 25, 0, Color.GREEN, 16, true);
        }

        neonBatch.end();

        stage.act(delta);
        stage.draw();

        // Debug Info
        DebugUI.info("Entities: %d", GameWorld.inst().getAllEntities().size());
        DebugUI.info("Components: %d", ComponentManager.getRegisteredComponentCount());
    }

    @Override
    public void dispose() {
        super.dispose();
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (GameWorld.inst() != null) GameWorld.inst().dispose();
    }
}
