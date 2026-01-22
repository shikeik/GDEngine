package com.goldsprite.gdengine.tests;

import com.goldsprite.gdengine.CLogAssert;
import com.goldsprite.gdengine.GdxTestRunner;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class AnimationDataTest {

	@Test
	public void testLinearInterpolation() {
		System.out.println(">>> 验证: 线性插值 (Linear Interpolation)");

		NeonTimeline timeline = new NeonTimeline("Arm", NeonProperty.ROTATION);

		// 0.0s -> 0度
		// 1.0s -> 100度
		timeline.addKeyframe(0.0f, 0f, NeonCurve.LINEAR);
		timeline.addKeyframe(1.0f, 100f, NeonCurve.LINEAR);

		// 测试点 1: 中间点 (0.5s)
		float valMid = timeline.evaluate(0.5f);
		CLogAssert.assertEquals("0.5s 应为 50度", 50f, valMid);

		// 测试点 2: 越界前 ( -1.0s ) -> 应该 Clamp 到第一帧
		float valPre = timeline.evaluate(-1.0f);
		CLogAssert.assertEquals("负时间应保持第一帧", 0f, valPre);

		// 测试点 3: 越界后 ( 2.0s ) -> 应该 Clamp 到最后一帧
		float valPost = timeline.evaluate(2.0f);
		CLogAssert.assertEquals("超时应保持最后一帧", 100f, valPost);
	}

	@Test
	public void testSteppedInterpolation() {
		System.out.println(">>> 验证: 阶梯插值 (Stepped Interpolation)");

		NeonTimeline timeline = new NeonTimeline("Light", NeonProperty.X);

		// 0.0s -> 0 (Curve: STEPPED)
		// 1.0s -> 10
		timeline.addKeyframe(0.0f, 0f, NeonCurve.STEPPED);
		timeline.addKeyframe(1.0f, 10f, NeonCurve.LINEAR);

		// 测试点: 0.99s -> 应该是 0 (还没到下一帧)
		float valBefore = timeline.evaluate(0.99f);
		CLogAssert.assertEquals("0.99s 仍应为 0 (未跳变)", 0f, valBefore);

		// 测试点: 1.0s -> 变成了 10
		float valAt = timeline.evaluate(1.0f);
		CLogAssert.assertEquals("1.0s 应跳变为 10", 10f, valAt);
	}

	@Test
	public void testSmoothInterpolation() {
		System.out.println(">>> 验证: 平滑插值 (Smooth/Bezier)");

		NeonTimeline timeline = new NeonTimeline("Head", NeonProperty.Y);
		timeline.addKeyframe(0.0f, 0f, NeonCurve.SMOOTH);
		timeline.addKeyframe(1.0f, 100f, NeonCurve.LINEAR);

		// Linear 的 0.25 应该是 25
		// Smooth 的 0.25 应该是 "慢进"，所以数值应该 < 25
		float valEarly = timeline.evaluate(0.25f);
		System.out.println("Smooth(0.25) = " + valEarly);
		CLogAssert.assertTrue("慢进阶段数值应小于线性值", valEarly < 25f);

		// Linear 的 0.75 应该是 75
		// Smooth 的 0.75 应该是 "慢出"，所以数值应该 > 75 (因为它中间加速追赶过了)
		float valLate = timeline.evaluate(0.75f);
		System.out.println("Smooth(0.75) = " + valLate);
		CLogAssert.assertTrue("慢出阶段数值应大于线性值", valLate > 75f);
	}
}
