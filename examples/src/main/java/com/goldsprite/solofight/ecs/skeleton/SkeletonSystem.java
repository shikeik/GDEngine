// 文件: ./core/src/main/java/com/goldsprite/solofight/ecs/skeleton/SkeletonSystem.java
package com.goldsprite.solofight.ecs.skeleton;

import com.goldsprite.gameframeworks.ecs.GameSystemInfo;
import com.goldsprite.gameframeworks.ecs.component.TransformComponent;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.ecs.system.BaseSystem;
import java.util.List;

/**
 * 骨骼更新系统
 * 职责：在逻辑帧的末尾（渲染前），统一同步 Transform 并计算骨骼矩阵。
 * 必须在 SceneSystem 之后运行。
 */
@GameSystemInfo(interestComponents = {SkeletonComponent.class})
public class SkeletonSystem extends BaseSystem {

	@Override
	public void update(float delta) {
		List<GObject> entities = getInterestEntities();
		for (GObject entity : entities) {
			SkeletonComponent skelComp = entity.getComponent(SkeletonComponent.class);
			if (skelComp == null || !skelComp.isEnable()) continue;

			NeonSkeleton skeleton = skelComp.getSkeleton();
			TransformComponent transform = entity.transform;

			// 1. 同步 ECS Transform 到 RootBone
			if (transform != null) {
				skeleton.rootBone.x = transform.position.x;
				skeleton.rootBone.y = transform.position.y;
				skeleton.rootBone.rotation = transform.rotation;
				skeleton.rootBone.scaleX = transform.scale.x;
				skeleton.rootBone.scaleY = transform.scale.y;
			}

			// 2. 触发矩阵计算 (Recursive)
			skeleton.update();
		}
	}
}
