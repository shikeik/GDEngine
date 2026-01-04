package com.goldsprite.gdengine.screens.ecs;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.system.SkeletonRenderSystem;
import com.goldsprite.gdengine.ecs.system.SkeletonSystem;
import com.goldsprite.gdengine.ecs.system.SpriteSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.GScreen;

/**
 * 通用 ECS 游戏运行容器
 * 职责：
 * 1. 提供标准的 ECS GameWorld 环境
 * 2. 预装核心渲染系统 (Sprite/Skeleton)
 * 3. 驱动 IGameScriptEntry 的生命周期
 */
public class GameRunnerScreen extends GScreen {

	private final IGameScriptEntry gameEntry;
	private GameWorld world;
	private NeonBatch neonBatch;

	public GameRunnerScreen(IGameScriptEntry gameEntry) {
		this.gameEntry = gameEntry;
	}

	@Override
	protected void initViewport() {
		autoCenterWorldCamera = false; // 取消初始相机居中, 因为游戏世界没有居中的说法，他没有边界，而且测试代码创建在0,0处
		uiViewportScale = 1.5f; // 将视口视野放大包括ui与世界相机
		super.initViewport();
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		// 1. 基础设施初始化
		neonBatch = new NeonBatch();

		// 2. 重置并初始化世界
		try {
			GameWorld.autoDispose(); // 使用智能释放方法
		} catch (Exception ignored) {}

		world = new GameWorld();
		world.setReferences(getUIViewport(), getWorldCamera());

		// 3. 预装常用系统 (渲染层)
		// 这样脚本只需要负责创建 Entity，不用管怎么画
		//所有系统自动注册，无需显式调用
		new SpriteSystem(neonBatch, getWorldCamera());
		new SkeletonSystem();
		new SkeletonRenderSystem(neonBatch, getWorldCamera());

		// 4. 【核心】移交权柄给脚本
		if (gameEntry != null) {
			Debug.logT("Runner", "启动脚本逻辑: %s", gameEntry.getClass().getName());
			gameEntry.onStart(world);
		}
	}

	@Override
	public void render0(float delta) {
		// 1. 绘制背景网格 (辅助线)
		drawGrid();

		// 2. 驱动脚本的 Update (可选)
		if (gameEntry != null) {
			gameEntry.onUpdate(delta);
		}

		// 3. 驱动 ECS 世界 (System Update & Render)
		// 注意：渲染系统是在 ECS update 流程中被调用的
		world.update(delta);
	}

	private void drawGrid() {
		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		neonBatch.setColor(1, 1, 1, 0.1f);
		neonBatch.drawLine(-1000, 0, 1000, 0, 2, Color.GRAY); // 地平线
		neonBatch.drawLine(0, -1000, 0, 1000, 2, Color.GRAY); // 垂直线
		neonBatch.setColor(Color.WHITE);
		neonBatch.end();
	}

	@Override
	public void dispose() {
		if (world != null) world.dispose();
		if (neonBatch != null) neonBatch.dispose();
	}
}
