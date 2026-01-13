package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.Camera;
import com.goldsprite.gdengine.ecs.skeleton.NeonSkeleton;
import com.goldsprite.gdengine.ecs.skeleton.NeonSlot;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 骨架组件
 * 职责：将 NeonSkeleton 挂载到 ECS 实体上，并同步 Transform
 */
// [修改] 继承 RenderComponent
public class SkeletonComponent extends RenderComponent {

	private final NeonSkeleton skeleton;

	public SkeletonComponent() {
		this.skeleton = new NeonSkeleton();
	}

	public NeonSkeleton getSkeleton() {
		return skeleton;
	}

	@Override
	public void update(float delta) {
		// 每一帧先同步 ECS 实体的 Transform 到骨架的 Root
		if (transform != null) {
			skeleton.rootBone.x = transform.position.x;
			skeleton.rootBone.y = transform.position.y;
			skeleton.rootBone.rotation = transform.rotation;
			skeleton.rootBone.scaleX = transform.worldScale.x;
			skeleton.rootBone.scaleY = transform.worldScale.y;
		}

		// 然后触发骨骼矩阵计算
		skeleton.update();
	}
	
	// [新增] 迁移自 SkeletonRenderSystem
	@Override
	public void render(NeonBatch batch, Camera camera) {
		if (!isEnable() || skeleton == null) return;

		for (NeonSlot slot : skeleton.getDrawOrder()) {
			slot.draw(batch);
		}
	}

	// [新增] 简单的命中检测 (基于 RootBone 周围半径)
	@Override
	public boolean contains(float x, float y) {
		if (transform == null) return false;

		// 简单判定：距离中心点 50 像素范围内
		// 实际上骨骼形状复杂，OBB检测很难，这里用一个经验值
		float dist = com.badlogic.gdx.math.Vector2.dst(x, y, transform.worldPosition.x, transform.worldPosition.y);
		float threshold = 50f * Math.max(Math.abs(transform.worldScale.x), Math.abs(transform.worldScale.y));

		return dist < threshold;
	}
}
