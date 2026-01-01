package com.goldsprite.solofight.screens.ecs.tests.skeleton;

import com.goldsprite.solofight.ecs.skeleton.NeonAnimatorComponent;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonTimeline;

public class TestAnimationFactory {

	public static void setupAnimations(NeonAnimatorComponent animComp) {
		// --- 动画 A: Idle ---
		NeonAnimation idle = new NeonAnimation("Idle", 1.5f, true);

		// 身体
		addTrack(idle, "Body", NeonProperty.SCALE_Y, 0f, 1.0f, 0.75f, 1.05f, 1.5f, 1.0f);
		addTrack(idle, "Body", NeonProperty.ROTATION, 0f, 90f, 0.75f, 88f, 1.5f, 90f);

		// 手臂
		addTrack(idle, "Arm_Front_Up", NeonProperty.ROTATION, 0f, -160f, 0.75f, -140f, 1.5f, -160f);
		addTrack(idle, "Arm_Front_Low", NeonProperty.ROTATION, 0f, 10f, 0.75f, 30f, 1.5f, 10f);

		addTrack(idle, "Arm_Back_Up", NeonProperty.ROTATION, 0f, -150f, 0.75f, -170f, 1.5f, -150f);
		addTrack(idle, "Arm_Back_Low", NeonProperty.ROTATION, 0f, 10f, 0.75f, 20f, 1.5f, 10f);

		// 腿部 (这里是 2 帧，之前报错的地方)
		addTrack(idle, "Leg_Front_Up", NeonProperty.ROTATION, 0f, -80f, 1.5f, -80f);
		addTrack(idle, "Leg_Front_Low", NeonProperty.ROTATION, 0f, -10f, 1.5f, -10f);

		addTrack(idle, "Leg_Back_Up", NeonProperty.ROTATION, 0f, -100f, 1.5f, -100f);
		addTrack(idle, "Leg_Back_Low", NeonProperty.ROTATION, 0f, 20f, 1.5f, 20f);

		animComp.addAnimation(idle);


		// --- 动画 B: Attack ---
		NeonAnimation atk = new NeonAnimation("Attack", 0.8f, true);

		// 身体前倾
		addTrack(atk, "Body", NeonProperty.ROTATION, 0f, 90f, 0.2f, 70f, 0.8f, 90f);

		// 前手刺出
		addTrack(atk, "Arm_Front_Up", NeonProperty.ROTATION, 0f, -160f, 0.2f, 0f, 0.5f, 0f, 0.8f, -160f);
		addTrack(atk, "Arm_Front_Low", NeonProperty.ROTATION, 0f, 10f, 0.2f, 0f, 0.5f, 0f, 0.8f, 10f);

		// 后手平衡
		addTrack(atk, "Arm_Back_Up", NeonProperty.ROTATION, 0f, -150f, 0.2f, -200f, 0.8f, -150f);

		// 腿部弓步
		addTrack(atk, "Leg_Front_Up", NeonProperty.ROTATION, 0f, -80f, 0.2f, -50f, 0.8f, -80f);
		addTrack(atk, "Leg_Front_Low", NeonProperty.ROTATION, 0f, -10f, 0.2f, -90f, 0.8f, -10f);

		animComp.addAnimation(atk);
	}

	/**
	 * 通用轨道构建器 (支持任意数量关键帧)
	 * 参数格式: time1, value1, time2, value2, ...
	 */
	private static void addTrack(NeonAnimation anim, String bone, NeonProperty prop, float... keyframes) {
		if (keyframes.length % 2 != 0) {
			throw new IllegalArgumentException("关键帧参数必须成对出现 (time, value)");
		}

		NeonTimeline line = new NeonTimeline(bone, prop);
		for (int i = 0; i < keyframes.length; i += 2) {
			float t = keyframes[i];
			float v = keyframes[i+1];
			// 默认全部使用平滑插值，如果需要精细控制，可以再加重载
			line.addKeyframe(t, v, NeonCurve.SMOOTH);
		}
		anim.addTimeline(line);
	}
}
