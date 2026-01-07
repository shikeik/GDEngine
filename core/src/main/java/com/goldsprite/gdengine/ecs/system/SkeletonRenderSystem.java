package com.goldsprite.gdengine.ecs.system;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.component.SkeletonComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.skeleton.NeonSkeleton;
import com.goldsprite.gdengine.ecs.skeleton.NeonSlot;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import java.util.List;

/**
 * 骨架渲染系统
 * 职责：遍历所有 SkeletonComponent 并进行绘制
 */
@GameSystemInfo(interestComponents = {SkeletonComponent.class})
public class SkeletonRenderSystem extends BaseSystem {

	private final NeonBatch batch;
	private OrthographicCamera camera;

	public SkeletonRenderSystem(NeonBatch batch, OrthographicCamera camera) {
		this.batch = batch;
		this.camera = camera;
	}

	public void setCamera(OrthographicCamera camera) {
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
