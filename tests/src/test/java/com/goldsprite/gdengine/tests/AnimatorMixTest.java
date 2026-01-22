package com.goldsprite.gdengine.tests;

import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.component.NeonAnimatorComponent;
import com.goldsprite.gdengine.ecs.skeleton.NeonBone;
import com.goldsprite.gdengine.ecs.component.SkeletonComponent;
import com.goldsprite.gdengine.ecs.system.SkeletonSystem;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;
import com.goldsprite.gdengine.CLogAssert;
import com.goldsprite.gdengine.GdxTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class AnimatorMixTest {

	private GameWorld world;

	@Before
	public void setUp() {
		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		world = new GameWorld();
		new SkeletonSystem(); // 记得注册系统！
	}

	@After
	public void tearDown() {
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
	}

	@Test
	public void testCrossFade() {
		System.out.println(">>> 验证: 动画混合 (CrossFade)");

		// 1. Setup
		GObject entity = new GObject("Player");
		SkeletonComponent skel = entity.addComponent(SkeletonComponent.class);
		NeonAnimatorComponent anim = entity.addComponent(NeonAnimatorComponent.class);
		NeonBone arm = skel.getSkeleton().createBone("Arm", "root", 10, null);

		// 2. 制作两个静态动画 (方便算数)
		// Anim A: 永远 0度
		NeonAnimation animA = new NeonAnimation("Idle", 1f, true);
		NeonTimeline lineA = new NeonTimeline("Arm", NeonProperty.ROTATION);
		lineA.addKeyframe(0f, 0f, NeonCurve.LINEAR);
		lineA.addKeyframe(1f, 0f, NeonCurve.LINEAR);
		animA.addTimeline(lineA);

		// Anim B: 永远 100度
		NeonAnimation animB = new NeonAnimation("Attack", 1f, true);
		NeonTimeline lineB = new NeonTimeline("Arm", NeonProperty.ROTATION);
		lineB.addKeyframe(0f, 100f, NeonCurve.LINEAR);
		lineB.addKeyframe(1f, 100f, NeonCurve.LINEAR);
		animB.addTimeline(lineB);

		// 3. 注册
		world.update(0); // Awake
		anim.addAnimation(animA);
		anim.addAnimation(animB);

		// 4. 先播放 A
		anim.play("Idle");
		world.update(0.1f);
		CLogAssert.assertEquals("Idle状态应为0度", 0f, arm.rotation);

		// 5. 开始混合 (CrossFade 1.0s)
		System.out.println("  -> 开始 CrossFade (Duration=1.0s)...");
		anim.crossFade("Attack", 1.0f);

		// 6. 验证中间态 (0.5s 后)
		// 此时混合进度 50%，值应该是 (0 + 100) / 2 = 50
		world.update(0.5f);
		System.out.println("0.5s Rotation: " + arm.rotation);
		CLogAssert.assertEquals("混合50%应为50度", 50f, arm.rotation);

		// 7. 验证结束态 (再过 0.6s -> 总计 1.1s，混合结束)
		world.update(0.6f);
		System.out.println("1.1s Rotation: " + arm.rotation);
		CLogAssert.assertEquals("混合结束应为100度", 100f, arm.rotation);
	}
}
