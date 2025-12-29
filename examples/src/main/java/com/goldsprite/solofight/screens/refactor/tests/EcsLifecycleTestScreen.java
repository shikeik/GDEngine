package com.goldsprite.solofight.screens.refactor.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.component.LifecycleLogComponent;
import com.goldsprite.solofight.refactor.ecs.component.TestRotatorComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class EcsLifecycleTestScreen extends ExampleGScreen {

    private SpriteBatch batch;
    private NeonBatch neonBatch;
    private Stage stage;
    private BitmapFont debugFont;

    private GObject rootA, rootB;
    private GObject childUnit;
    private GObject greenMover;

    @Override
    public String getIntroduction() {
        return "生命周期与层级测试 v2\n可视化调试 | 换父级测试 | 压力测试";
    }
	@Override
	protected void initViewport() {
		float scl = 1.2f;
		uiViewport = new ExtendViewport(960 * scl, 540 * scl);
		setWorldScale(2f);
	}
	@Override
	public void show() {
		super.show();
		//getScreenManager().setOrientation(ScreenManager.Orientation.LANDSCAPE);
	}
	@Override
	public void hide() {
		super.hide();
		//getScreenManager().setOrientation(ScreenManager.Orientation.PORTRAIT);
	}

    @Override
    public void create() {
        batch = new SpriteBatch();
        neonBatch = new NeonBatch(batch);
        debugFont = FontUtils.generate(16); // 用于在物体头顶画名字

        if (GameWorld.inst() != null) GameWorld.inst().dispose();
        new GameWorld();

        initUI();
        spawnInitialSetup();
    }

    private void spawnInitialSetup() {
        // 1. 创建两个“基地” (Root A, Root B)
        rootA = new GObject("Base_A");
        rootA.getTransform().setPosition(300, 400);
        rootA.addComponent(LifecycleLogComponent.class);

        rootB = new GObject("Base_B");
        rootB.getTransform().setPosition(900, 400);
        rootB.addComponent(LifecycleLogComponent.class);

        // 2. 创建一个独立的移动物体 (绿球)，限制范围防止出界
        greenMover = new GObject("Green_Mover");
        greenMover.getTransform().setPosition(600, 200);
        greenMover.addComponent(LifecycleLogComponent.class);
        // 设置范围 200，保证在 400~800 之间移动，完全在视野内
        greenMover.addComponent(TestRotatorComponent.class).setConfig(150f, 200f);
    }

    private void initUI() {
        stage = new Stage(getUIViewport(), batch);
        getImp().addProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.left().top().pad(20);
        stage.addActor(root);

        root.defaults().width(240).height(40).padBottom(8);

        root.add(new VisLabel("--- 层级操作 ---")).row();

        // 1. 生成子物体
        VisTextButton btnSpawnChild = new VisTextButton("1. Spawn Child at Base A");
        btnSpawnChild.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (childUnit != null && !childUnit.isDestroyed()) {
						DebugUI.log("Child already exists!");
						return;
					}
					childUnit = new GObject("Unit_Child");
					childUnit.getTransform().setPosition(300, 300); // 初始位置在 A 下方
					childUnit.addComponent(LifecycleLogComponent.class);
					rootA.addChild(childUnit);
					DebugUI.log("Spawned Child under Base A");
				}
			});
        root.add(btnSpawnChild).row();

        // 2. 换父级 (核心测试点)
        VisTextButton btnMoveToB = new VisTextButton("2. Move Child to Base B");
        btnMoveToB.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (validateChild()) {
						rootB.addChild(childUnit); // 自动从 A 移除并加入 B
						// 简单的视觉移动动画模拟 (瞬移)
						childUnit.getTransform().setPosition(900, 300);
						DebugUI.log("Moved Child to Base B");
					}
				}
			});
        root.add(btnMoveToB).row();

        // 3. 换回 A
        VisTextButton btnMoveToA = new VisTextButton("3. Move Child to Base A");
        btnMoveToA.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (validateChild()) {
						rootA.addChild(childUnit);
						childUnit.getTransform().setPosition(300, 300);
						DebugUI.log("Moved Child back to Base A");
					}
				}
			});
        root.add(btnMoveToA).row();

        // 4. 独立 (Detach)
        VisTextButton btnDetach = new VisTextButton("4. Detach (Become Orphan)");
        btnDetach.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (validateChild()) {
						childUnit.setParent(null); // 测试 setParent(null) 逻辑
						childUnit.getTransform().setPosition(600, 500); // 移到中间顶部
						DebugUI.log("Detached Child (Now Independent)");
					}
				}
			});
        root.add(btnDetach).row();

        root.add(new VisLabel("--- 压力测试 ---")).padTop(10).row();

        // 5. 压力测试
        VisTextButton btnStress = new VisTextButton("5. Spawn 100 Particles");
        btnStress.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					for (int i = 0; i < 100; i++) {
						GObject p = new GObject("P_" + i);
						p.getTransform().setPosition(
							MathUtils.random(100, 1100),
							MathUtils.random(100, 600)
						);
						// 5秒后自动销毁的逻辑需要额外写组件，这里为了演示简单，仅生成
						// 实际上我们可以给它们加个 Rotator 看看性能
						// p.addComponent(TestRotatorComponent.class); 
					}
					DebugUI.log("Spawned 100 entities.");
				}
			});
        root.add(btnStress).row();

        VisTextButton btnClearAll = new VisTextButton("6. Destroy All Entities");
        btnClearAll.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// 暴力清空，测试 GameWorld 稳定性
					for(GObject obj : GameWorld.inst().getAllEntities()) {
						obj.destroy();
					}
					rootA = null; rootB = null; childUnit = null; greenMover = null;
					DebugUI.log("Requested destroy for ALL entities.");
				}
			});
        root.add(btnClearAll).row();
    }

    private boolean validateChild() {
        if (childUnit == null || childUnit.isDestroyed()) {
            DebugUI.log("Child does not exist!");
            return false;
        }
        return true;
    }

    @Override
    public void render0(float delta) {
        GameWorld.inst().update(delta);

        // --- 可视化调试绘制 ---
        batch.setProjectionMatrix(getWorldCamera().combined);
        neonBatch.begin();

		neonBatch.drawRect(getWorldCenter().x, getWorldCenter().y, getWorldSize().x, getWorldSize().y, 0, 20, Color.RED, false);
		
        // 绘制网格背景
        drawGrid(neonBatch);

        // 遍历所有实体绘制 Debug 图形
        // 注意：这里 getAllEntities 只包含根节点，所以需要递归或者 ComponentManager 帮忙
        // 为了演示连线，我们手动处理已知对象，或者遍历 ComponentManager
        // 简单起见，我们遍历 GameWorld 的根节点，如果有子节点则画线

        for (GObject obj : GameWorld.inst().getAllEntities()) {
            drawEntityDebug(obj);
        }

        neonBatch.end();

        // 绘制文字标签 (需要 batch 单独 begin)
        batch.begin();
        for (GObject obj : GameWorld.inst().getAllEntities()) {
            drawEntityLabel(obj);
        }
        batch.end();

        stage.act(delta);
        stage.draw();

        DebugUI.info("Entities (Roots): %d", GameWorld.inst().getAllEntities().size());
        DebugUI.info("Components: %d", ComponentManager.getRegisteredComponentCount());
    }

    // 递归绘制实体及其连线
    private void drawEntityDebug(GObject obj) {
        if (obj.isDestroyed()) return;

        float x = obj.getTransform().position.x;
        float y = obj.getTransform().position.y;

        // 不同对象不同颜色
        Color color = Color.WHITE;
        if (obj == rootA) color = Color.CYAN;
        else if (obj == rootB) color = Color.MAGENTA;
        else if (obj == childUnit) color = Color.YELLOW;
        else if (obj == greenMover) color = Color.GREEN;

        neonBatch.drawCircle(x, y, 20, 0, color, 16, true);

        // 画子节点连线
        for (GObject child : obj.getChildren()) {
            if (!child.isDestroyed()) {
                float cx = child.getTransform().position.x;
                float cy = child.getTransform().position.y;
                neonBatch.drawLine(x, y, cx, cy, 2, Color.LIGHT_GRAY);

                // 递归绘制子节点
                drawEntityDebug(child); 
            }
        }
    }

    // 递归绘制文字
    private void drawEntityLabel(GObject obj) {
        if (obj.isDestroyed()) return;
        float x = obj.getTransform().position.x;
        float y = obj.getTransform().position.y;

        debugFont.draw(batch, obj.getName(), x - 20, y + 40);

        for (GObject child : obj.getChildren()) {
            drawEntityLabel(child);
        }
    }

    private void drawGrid(NeonBatch b) {
        Color c = new Color(1, 1, 1, 0.1f);
        for (int i = 0; i < 2000; i += 100) {
            b.drawLine(i, 0, i, 1000, 1, c);
            b.drawLine(0, i, 2000, i, 1, c);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (debugFont != null) debugFont.dispose();
        if (GameWorld.inst() != null) GameWorld.inst().dispose();
    }
}
