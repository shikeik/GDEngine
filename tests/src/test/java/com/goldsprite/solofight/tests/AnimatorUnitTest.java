// 文件: ./tests/src/test/java/com/goldsprite/solofight/tests/AnimatorUnitTest.java
package com.goldsprite.solofight.tests;

import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.solofight.CLogAssert;
import com.goldsprite.solofight.GdxTestRunner;
import com.goldsprite.solofight.ecs.skeleton.NeonAnimatorComponent;
import com.goldsprite.solofight.ecs.skeleton.NeonBone;
import com.goldsprite.solofight.ecs.skeleton.SkeletonComponent;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonTimeline;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class AnimatorUnitTest {

	private GameWorld world;

	@Before
	public void setUp() {
		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		world = new GameWorld();
	}

	@After
	public void tearDown() {
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
	}

	@Test
	public void testAnimationDriveSkeleton() {
		System.out.println(">>> 验证: 动画器驱动骨骼 (Animator -> Skeleton)");

		// 1. 创建实体和组件
		GObject entity = new GObject("Player");
		SkeletonComponent skelComp = entity.addComponent(SkeletonComponent.class);
		NeonAnimatorComponent animComp = entity.addComponent(NeonAnimatorComponent.class);

		// 2. 初始化骨架 (创建一根 "Arm" 骨头)
		NeonBone arm = skelComp.getSkeleton().createBone("Arm", "root", 100, null);
		arm.rotation = 0; // 初始 0度

		// 3. 手写一个动画数据 ("Wave": 1秒内，手臂从 0度 转到 90度)
		NeonAnimation anim = new NeonAnimation("Wave", 1.0f, false);
		NeonTimeline timeline = new NeonTimeline("Arm", NeonProperty.ROTATION);
		timeline.addKeyframe(0.0f, 0f, NeonCurve.LINEAR);
		timeline.addKeyframe(1.0f, 90f, NeonCurve.LINEAR);
		anim.addTimeline(timeline);

		// 4. 注册并播放
		// 这一步会触发 Awake -> 绑定引用
		world.update(0);
		animComp.addAnimation(anim);
		animComp.play("Wave");

		// --- 测试开始 ---

		// A. 初始状态 (0s)
		CLogAssert.assertEquals("初始角度应为0", 0f, arm.rotation);

		// B. 运行半秒 (0.5s)
		world.update(0.5f);
		// Animator.update() -> 改 arm.rotation = 45
		// Skeleton.update() -> 算 worldTransform

		System.out.println("Current Arm Rotation: " + arm.rotation);
		CLogAssert.assertEquals("0.5s 时角度应为 45", 45f, arm.rotation);

		// C. 运行到结束 (1.0s)
		world.update(0.5f); // +0.5 = 1.0s
		CLogAssert.assertEquals("1.0s 时角度应为 90", 90f, arm.rotation);

		// D. 验证矩阵是否同步更新 (SkeletonComponent 负责)
		// 旋转90度，局部矩阵的 m00 (cos90) 应该是 0，m10 (sin90) 应该是 1
		float m00 = arm.localTransform.m00;
		float m10 = arm.localTransform.m10;
		CLogAssert.assertTrue("矩阵 cos90 应接近 0", Math.abs(m00) < 0.001f);
		CLogAssert.assertTrue("矩阵 sin90 应接近 1", Math.abs(m10 - 1) < 0.001f);
	}
}
