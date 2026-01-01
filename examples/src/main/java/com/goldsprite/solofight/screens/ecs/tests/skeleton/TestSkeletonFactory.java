// 文件: ./examples/src/main/java/com/goldsprite/solofight/screens/ecs/tests/TestSkeletonFactory.java
package com.goldsprite.solofight.screens.ecs.tests.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.solofight.ecs.skeleton.NeonBone;
import com.goldsprite.solofight.ecs.skeleton.NeonGeometrySkin;
import com.goldsprite.solofight.ecs.skeleton.NeonSkeleton;

public class TestSkeletonFactory {

	public static void buildStickman(NeonSkeleton skel) {
		// --- 样式定义 ---
		float wLimb = 12f;
		float wBody = 25f;

		// 颜色分层 (Back: Dark, Front: Bright)
		Color cBackUp = Color.valueOf("4b0082"); // 深紫
		Color cBackLow = Color.valueOf("800080"); // 紫
		Color cBody = Color.LIGHT_GRAY;
		Color cHead = Color.YELLOW;
		Color cFrontUp = Color.valueOf("00ced1"); // 亮青
		Color cFrontLow = Color.CYAN;
		Color cAtkUp = Color.valueOf("ff1493");   // 亮粉 (攻击手)
		Color cAtkLow = Color.valueOf("ff69b4");

		// --- 1. 创建骨骼 (Hierarchy) ---

		// Root & Body
		NeonBone body = skel.createBone("Body", "root", 100,
			new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wBody, true));
		body.rotation = 90f;

		// Head
		NeonBone head = skel.createBone("Head", "Body", 0,
			new NeonGeometrySkin(NeonGeometrySkin.Shape.CIRCLE, 25f, true));
		head.x = 100;

		// --- 后肢 (Back) ---
		skel.createBone("Leg_Back_Up", "root", 50, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		NeonBone legBL_Low = skel.createBone("Leg_Back_Low", "Leg_Back_Up", 50, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		legBL_Low.x = 50;

		NeonBone armBL = skel.createBone("Arm_Back_Up", "Body", 40, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		armBL.x = 90;
		NeonBone armBL_Low = skel.createBone("Arm_Back_Low", "Arm_Back_Up", 40, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		armBL_Low.x = 40;

		// --- 前肢 (Front) ---
		skel.createBone("Leg_Front_Up", "root", 50, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		NeonBone legFL_Low = skel.createBone("Leg_Front_Low", "Leg_Front_Up", 50, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		legFL_Low.x = 50;

		NeonBone armFL = skel.createBone("Arm_Front_Up", "Body", 40, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		armFL.x = 90;
		NeonBone armFL_Low = skel.createBone("Arm_Front_Low", "Arm_Front_Up", 40, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, wLimb, true));
		armFL_Low.x = 40;

		// --- 2. 设置颜色 (Skin Color) ---
		skel.getSlot("Body").color.set(cBody);
		skel.getSlot("Head").color.set(cHead);

		skel.getSlot("Leg_Back_Up").color.set(cBackUp); skel.getSlot("Leg_Back_Low").color.set(cBackLow);
		skel.getSlot("Arm_Back_Up").color.set(cBackUp); skel.getSlot("Arm_Back_Low").color.set(cBackLow);

		skel.getSlot("Leg_Front_Up").color.set(cFrontUp); skel.getSlot("Leg_Front_Low").color.set(cFrontLow);
		skel.getSlot("Arm_Front_Up").color.set(cAtkUp); skel.getSlot("Arm_Front_Low").color.set(cAtkLow);

		// --- 3. 调整渲染层级 (Draw Order) ---
		int idx = 0;
		skel.setSlotOrder("Leg_Back_Low", idx++); skel.setSlotOrder("Leg_Back_Up", idx++);
		skel.setSlotOrder("Arm_Back_Low", idx++); skel.setSlotOrder("Arm_Back_Up", idx++);
		skel.setSlotOrder("Body", idx++); skel.setSlotOrder("Head", idx++);
		skel.setSlotOrder("Leg_Front_Low", idx++); skel.setSlotOrder("Leg_Front_Up", idx++);
		skel.setSlotOrder("Arm_Front_Low", idx++); skel.setSlotOrder("Arm_Front_Up", idx++);
	}
}
