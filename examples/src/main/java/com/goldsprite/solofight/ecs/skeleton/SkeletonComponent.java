// 文件: ./core/src/main/java/com/goldsprite/solofight/ecs/skeleton/SkeletonComponent.java
package com.goldsprite.solofight.ecs.skeleton;

import com.goldsprite.gameframeworks.ecs.component.Component;

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
			skeleton.rootBone.scaleX = transform.scale.x;
			skeleton.rootBone.scaleY = transform.scale.y;
		}

		// 然后触发骨骼矩阵计算
		skeleton.update();
	}
}
