package com.goldsprite.gdengine.screens.ecs.skeleton;

import com.goldsprite.gdengine.ecs.component.NeonAnimatorComponent;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;

public class TestAnimationFactory {

	public static void setupAnimations(NeonAnimatorComponent animComp) {
		setupIdle(animComp);
		setupCombatAnimations(animComp);
	}

	public static void setupIdle(NeonAnimatorComponent animComp) {
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

	// [新增] 战斗大片专用动作
	private static void setupCombatAnimations(NeonAnimatorComponent animComp) {

		// 1. Run_Drag (拖刀跑)
		NeonAnimation run = new NeonAnimation("Run_Drag", 0.6f, true);
		// 身体极大前倾
		addTrack(run, "Body", NeonProperty.ROTATION, 0f, 60f, 0.3f, 55f, 0.6f, 60f);
		// 右手(持剑)向后拖
		addTrack(run, "Arm_Front_Up", NeonProperty.ROTATION, 0f, -220f, 0.6f, -220f);
		addTrack(run, "Arm_Front_Low", NeonProperty.ROTATION, 0f, 10f, 0.6f, 10f);
		addTrack(run, "Arm_Front_Hand", NeonProperty.ROTATION, 0f, 40f, 0.6f, 40f); // 手腕反扣
		// 左手护在胸前
		addTrack(run, "Arm_Back_Up", NeonProperty.ROTATION, 0f, -80f, 0.3f, -60f, 0.6f, -80f);
		addTrack(run, "Arm_Back_Low", NeonProperty.ROTATION, 0f, 120f, 0.3f, 100f, 0.6f, 120f);
		// 腿部奔跑 (交替)
		addTrack(run, "Leg_Front_Up", NeonProperty.ROTATION, 0f, -30f, 0.3f, -90f, 0.6f, -30f);
		addTrack(run, "Leg_Front_Low", NeonProperty.ROTATION, 0f, -10f, 0.3f, -100f, 0.6f, -10f);
		addTrack(run, "Leg_Back_Up", NeonProperty.ROTATION, 0f, -90f, 0.3f, -30f, 0.6f, -90f);
		addTrack(run, "Leg_Back_Low", NeonProperty.ROTATION, 0f, -100f, 0.3f, -10f, 0.6f, -100f);
		animComp.addAnimation(run);

		// 2. Atk_Launcher (上挑)
		NeonAnimation launch = new NeonAnimation("Atk_Launcher", 0.4f, false);
		// 0.0s 蓄力 -> 0.2s 挑飞
		addTrack(launch, "Body", NeonProperty.ROTATION, 0f, 50f, 0.2f, 110f, 0.4f, 100f); // 身体后仰
		// 右手从后(-220) 甩到 头顶(-10)
		addTrack(launch, "Arm_Front_Up", NeonProperty.ROTATION, 0f, -220f, 0.2f, 10f, 0.4f, -10f);
		addTrack(launch, "Arm_Front_Low", NeonProperty.ROTATION, 0f, 10f, 0.2f, 0f, 0.4f, 0f); // 伸直
		// 腿部蹬地
		addTrack(launch, "Leg_Front_Up", NeonProperty.ROTATION, 0f, -60f, 0.2f, -90f);
		animComp.addAnimation(launch);

		// 3. Atk_Air (空中乱斩)
		NeonAnimation air = new NeonAnimation("Atk_Air", 0.2f, true); // 极快循环
		// 手臂疯狂挥舞
		addTrack(air, "Arm_Front_Up", NeonProperty.ROTATION, 0f, 40f, 0.1f, -40f, 0.2f, 40f);
		addTrack(air, "Arm_Front_Low", NeonProperty.ROTATION, 0f, 30f, 0.1f, 0f, 0.2f, 30f);
		// 身体蜷缩
		addTrack(air, "Body", NeonProperty.ROTATION, 0f, 80f, 0.2f, 80f);
		addTrack(air, "Leg_Front_Up", NeonProperty.ROTATION, 0f, -40f, 0.2f, -40f); // 收腿
		addTrack(air, "Leg_Back_Up", NeonProperty.ROTATION, 0f, -50f, 0.2f, -50f);
		animComp.addAnimation(air);

		// 4. Atk_Smash (下劈)
		NeonAnimation smash = new NeonAnimation("Atk_Smash", 0.5f, false);
		// 0.0s 举过头顶 -> 0.2s 砸下
		addTrack(smash, "Body", NeonProperty.ROTATION, 0f, 110f, 0.2f, 40f, 0.5f, 60f); // 猛烈前压
		addTrack(smash, "Arm_Front_Up", NeonProperty.ROTATION, 0f, 160f, 0.2f, -100f, 0.5f, -80f);
		addTrack(smash, "Arm_Front_Low", NeonProperty.ROTATION, 0f, 10f, 0.2f, 0f, 0.5f, 0f);
		animComp.addAnimation(smash);

		// 5. Pose_Back (收刀装逼)
		NeonAnimation pose = new NeonAnimation("Pose_Back", 2.0f, true);
		// 转身背对 (这里我们假装背对，主要靠姿势)
		addTrack(pose, "Body", NeonProperty.ROTATION, 0f, 90f, 2.0f, 90f);
		// 右手背在身后
		addTrack(pose, "Arm_Front_Up", NeonProperty.ROTATION, 0f, -200f, 2.0f, -200f);
		addTrack(pose, "Arm_Front_Low", NeonProperty.ROTATION, 0f, 100f, 2.0f, 100f);
		// 剑稍微调整角度
		addTrack(pose, "Arm_Front_Hand", NeonProperty.ROTATION, 0f, 45f, 2.0f, 45f);
		// 抬头望天
		addTrack(pose, "Head", NeonProperty.ROTATION, 0f, 20f, 2.0f, 20f);
		animComp.addAnimation(pose);
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
