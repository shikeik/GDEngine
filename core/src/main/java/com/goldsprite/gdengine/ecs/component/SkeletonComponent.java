package com.goldsprite.gdengine.ecs.component;

import com.goldsprite.gdengine.ecs.skeleton.NeonSkeleton;

/**
 * 骨架组件
 * 职责：将 NeonSkeleton 挂载到 ECS 实体上，并同步 Transform
 */
public class SkeletonComponent extends Component {

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
			skeleton.rootBone.scaleX = transform.scale;
			skeleton.rootBone.scaleY = transform.scale;
		}

		// 然后触发骨骼矩阵计算
		skeleton.update();
	}
}
