package com.goldsprite.solofight.ecs.skeleton;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.goldsprite.gameframeworks.ecs.GameSystemInfo;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.ecs.system.BaseSystem;
import com.goldsprite.gameframeworks.neonbatch.NeonBatch;
import java.util.List;

/**
 * 骨架渲染系统
 * 职责：遍历所有 SkeletonComponent 并进行绘制
 */
@GameSystemInfo(interestComponents = {SkeletonComponent.class})
public class SkeletonRenderSystem extends BaseSystem {

	private final NeonBatch batch;
	private final OrthographicCamera camera;

	public SkeletonRenderSystem(NeonBatch batch, OrthographicCamera camera) {
		this.batch = batch;
		this.camera = camera;
	}

	@Override
	public void update(float delta) {
		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		List<GObject> entities = getInterestEntities();
		for (GObject entity : entities) {
			SkeletonComponent skelComp = entity.getComponent(SkeletonComponent.class);
			if (skelComp != null && skelComp.isEnable()) {
				drawSkeleton(skelComp.getSkeleton());
			}
		}

		batch.end();
	}

	private void drawSkeleton(NeonSkeleton skeleton) {
		// 核心：只遍历 DrawOrder 列表
		for (NeonSlot slot : skeleton.getDrawOrder()) {
			slot.draw(batch);
		}
	}
}
