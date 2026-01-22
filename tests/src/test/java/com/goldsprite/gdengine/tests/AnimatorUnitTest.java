package com.goldsprite.gdengine.tests;

import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.CLogAssert;
import com.goldsprite.gdengine.GdxTestRunner;
import com.goldsprite.gdengine.ecs.component.NeonAnimatorComponent;
import com.goldsprite.gdengine.ecs.skeleton.NeonBone;
import com.goldsprite.gdengine.ecs.component.SkeletonComponent;
import com.goldsprite.gdengine.ecs.system.SkeletonSystem; // 新增
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;

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

		// 【关键】注册骨骼更新系统
		// 它会在 SceneSystem 之后运行，确保矩阵计算使用的是最新的动画数据
		new SkeletonSystem();
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

		// 2. 初始化骨架
		NeonBone arm = skelComp.getSkeleton().createBone("Arm", "root", 100, null);
		arm.rotation = 0;

		// 3. 手写动画 ("Wave": 0s->0, 1s->90)
		NeonAnimation anim = new NeonAnimation("Wave", 1.0f, false);
		NeonTimeline timeline = new NeonTimeline("Arm", NeonProperty.ROTATION);
		timeline.addKeyframe(0.0f, 0f, NeonCurve.LINEAR);
		timeline.addKeyframe(1.0f, 90f, NeonCurve.LINEAR);
		anim.addTimeline(timeline);

		// 4. 播放
		world.update(0);
		animComp.addAnimation(anim);
		animComp.play("Wave");

		// --- 测试开始 ---

		// A. 0s
		CLogAssert.assertEquals("初始角度应为0", 0f, arm.rotation);

		// B. 0.5s
		world.update(0.5f);
		System.out.println("Current Arm Rotation: " + arm.rotation);
		CLogAssert.assertEquals("0.5s 时角度应为 45", 45f, arm.rotation);

		// C. 1.0s
		world.update(0.5f);
		CLogAssert.assertEquals("1.0s 时角度应为 90", 90f, arm.rotation);

		// D. 验证矩阵同步
		// 这次应该通过了，因为 SkeletonSystem 在 Animator 之后重新计算了矩阵
		float m00 = arm.localTransform.m00; // cos(90)
		float m10 = arm.localTransform.m10; // sin(90)

		System.out.println(String.format("Matrix: m00=%.4f, m10=%.4f", m00, m10));

		CLogAssert.assertTrue("矩阵 cos90 应接近 0", Math.abs(m00) < 0.001f);
		CLogAssert.assertTrue("矩阵 sin90 应接近 1", Math.abs(m10 - 1) < 0.001f);
	}
}
