package com.goldsprite.solofight.screens.refactor.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.Debug;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.component.TestRotatorComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;

public class EcsBasicTestScreen extends ExampleGScreen {

	private SpriteBatch batch;
	private NeonBatch neonBatch;
	private GObject testEntity;

	@Override
	public String getIntroduction() {
		return "ECS 基础循环测试\n验证: World Init, Entity Creation, Update Loop";
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);

		// 1. 初始化世界
		// 注意：GameWorld 是单例，重入时需要确保状态干净，这里简单起见每次 new 或者 reset
		// 目前 GameWorld 构造函数里初始化了 System
		if (GameWorld.inst() != null) {
			GameWorld.inst().dispose();
		}
		new GameWorld();

		// 2. 创建测试实体
		testEntity = new GObject("Test_Player");
		testEntity.getTransform().setPosition(200, 300); // 设置初始位置

		// 3. 挂载测试组件
		testEntity.addComponent(TestRotatorComponent.class);

		Debug.log("ECS Environment Created.");
	}

	@Override
	public void render0(float delta) {
		Debug.info("相机位置: %s", getWorldCamera().position);

		// 1. 驱动世界循环
		GameWorld.inst().update(delta);

		// 2. 可视化绘制 (暂时手动绘制，RenderSystem 还没写)
		batch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		// 画个圈代表实体
		float x = testEntity.getTransform().position.x;
		float y = testEntity.getTransform().position.y;
		neonBatch.drawCircle(x, y, 20, 0, Color.CYAN, 16, true);

		// 画个地平线
		neonBatch.drawLine(0, 200, 1000, 200, 2, Color.GRAY);

		neonBatch.end();

		// 3. Debug 信息面板
		Debug.info("World Entities: %d", GameWorld.inst().getAllEntities().size());
		Debug.info("Registered Components: %d", ComponentManager.getRegisteredComponentCount()); // 需要在ComponentManager加个getter验证
		Debug.info("Target Pos: (%.1f, %.1f)", x, y);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (batch != null) batch.dispose();
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
	}
}
