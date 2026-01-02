// 文件: ./examples/src/main/java/com/goldsprite/solofight/screens/ecs/tests/TestSkeletonFactory.java
package com.goldsprite.gdengine.screens.ecs.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.ecs.skeleton.NeonBone;
import com.goldsprite.gdengine.ecs.skeleton.NeonGeometrySkin;
import com.goldsprite.gdengine.ecs.skeleton.NeonSkeleton;

public class TestSkeletonFactory {

	// --- 样式配置 ---
	private static final float W_BODY = 25f;
	private static final float W_HEAD = 22f;
	private static final float W_LIMB = 11f;
	private static final float W_HAND = 9f;

	// 剑的尺寸
	private static final float W_BLADE = 6f;
	private static final float L_BLADE = 110f;
	private static final float W_HILT = 5f;
	private static final float L_HILT = 25f; // 手柄长度
	private static final float W_GUARD = 15f; // 护手单侧长度 (总长30)
	private static final float H_GUARD = 4f;  // 护手厚度

	// 配色
	private static final Color C_SHADOW = new Color(0.15f, 0.15f, 0.2f, 1f);
	private static final Color C_BODY = new Color(0.3f, 0.3f, 0.35f, 1f);
	private static final Color C_ARMOR = Color.valueOf("00eaff");
	private static final Color C_SKIN = Color.valueOf("e0e0e0");
	private static final Color C_SWORD_BLADE = Color.valueOf("ff3300"); // 氖红
	private static final Color C_SWORD_METAL = Color.valueOf("808080"); // 灰铁

	public static void buildStickman(NeonSkeleton skel) {
		// 1. Root & Body
		NeonBone body = create(skel, "Body", "root", 90, W_BODY, C_BODY);
		body.rotation = 90f;

		// 2. Head
		NeonBone head = create(skel, "Head", "Body", 0, W_HEAD, C_ARMOR);
		head.x = 90;
		skel.getSlot("Head").skin = new NeonGeometrySkin(NeonGeometrySkin.Shape.CIRCLE, 22f, true);

		// --- 肢体构建 (整形关键点) ---
		// 肩膀高度下调 (75 -> 60)
		float shoulderY = 60f;
		float hipY = 5f;
		// 肩膀宽度偏移 (Body是90度，LocalY 是垂直于脊柱的方向)
		float shoulderOffset = 8f;

		// Back Layer (Shadow)
		buildLeg(skel, "Leg_Back", "root", hipY, C_SHADOW);
		skel.getBone("Leg_Back_Up").rotation += 5;

		// 后手：LocalY 正向偏移 (视觉上的左侧/后侧)
		buildArm(skel, "Arm_Back", "Body", shoulderY, shoulderOffset, C_SHADOW);

		// Front Layer (Bright)
		buildLeg(skel, "Leg_Front", "root", hipY, C_ARMOR);
		skel.getBone("Leg_Front_Up").rotation -= 5;
		// 前手：LocalY 负向偏移 (视觉上的右侧/前侧)
		NeonBone handR = buildArm(skel, "Arm_Front", "Body", shoulderY, -shoulderOffset, C_SKIN);

		// --- 武器系统 (对称护手版) ---

		// 1. 剑根 (隐形锚点，位置不变，代表手心)
		NeonBone swordRoot = skel.createBone("Weapon_Root", "Arm_Front_Hand", 0, null);
		swordRoot.x = 5;
		swordRoot.rotation = 90;

		// [新增] 握持偏移量：把剑整体往“上”推 15px
		// 这样手心(Root)就会位于剑柄下方 15px 的位置，而不是紧贴护手
		float gripOffset = 10f;

		// 2. 剑刃 (Blade) - 从偏移后的护手处开始延伸
		NeonBone blade = create(skel, "Weapon_Blade", "Weapon_Root", L_BLADE, W_BLADE, C_SWORD_BLADE);
		blade.x = H_GUARD/2 + gripOffset;

		// 3. 剑柄 (Hilt) - 同样从偏移处开始，向下画
		// 因为 rotation=180，它会往下画，刚好穿过手心(Root)，露出一截在手后面
		NeonBone hilt = create(skel, "Weapon_Hilt", "Weapon_Root", L_HILT, W_HILT, C_SWORD_METAL);
		hilt.rotation = 180;
		hilt.x = H_GUARD/2 + gripOffset;

		// 4. 护手 (Guard) - 位于偏移点
		NeonBone guardL = create(skel, "Weapon_Guard_L", "Weapon_Root", W_GUARD, H_GUARD, C_SWORD_METAL);
		guardL.x = gripOffset; // 加上偏移
		guardL.rotation = 90;

		NeonBone guardR = create(skel, "Weapon_Guard_R", "Weapon_Root", W_GUARD, H_GUARD, C_SWORD_METAL);
		guardR.x = gripOffset; // 加上偏移
		guardR.rotation = -90;

		// --- 渲染层级调整 ---
		int i = 0;
		// Back
		setOrder(skel, "Leg_Back_Up", i++); setOrder(skel, "Leg_Back_Low", i++); setOrder(skel, "Leg_Back_Foot", i++);
		setOrder(skel, "Arm_Back_Up", i++); setOrder(skel, "Arm_Back_Low", i++); setOrder(skel, "Arm_Back_Hand", i++);

		setOrder(skel, "Weapon_Hilt", i++); // 剑柄如果不被手包住，就放后面

		// Body
		setOrder(skel, "Body", i++);
		setOrder(skel, "Head", i++);

		// Front
		setOrder(skel, "Leg_Front_Up", i++); setOrder(skel, "Leg_Front_Low", i++); setOrder(skel, "Leg_Front_Foot", i++);
		setOrder(skel, "Arm_Front_Up", i++); setOrder(skel, "Arm_Front_Low", i++);

		setOrder(skel, "Arm_Front_Hand", i++); // 手掌

		// 武器部件最上层
		setOrder(skel, "Weapon_Guard_L", i++);
		setOrder(skel, "Weapon_Guard_R", i++);
		setOrder(skel, "Weapon_Blade", i++);
	}

	private static void buildLeg(NeonSkeleton skel, String prefix, String parent, float parentY, Color color) {
		NeonBone up = create(skel, prefix + "_Up", parent, 45, W_LIMB, color);
		up.x = parentY;
		up.rotation = -90;
		NeonBone low = create(skel, prefix + "_Low", prefix + "_Up", 45, W_LIMB, color);
		low.x = 45;
		NeonBone foot = create(skel, prefix + "_Foot", prefix + "_Low", 15, W_LIMB, color);
		foot.x = 45;
		foot.rotation = 90;
	}

	// 更新 buildArm：增加 offsetY 参数，控制肩膀宽度
	private static NeonBone buildArm(NeonSkeleton skel, String prefix, String parent, float parentX, float parentY, Color color) {
		NeonBone up = create(skel, prefix + "_Up", parent, 35, W_LIMB, color);
		up.x = parentX; // 沿脊柱高度
		up.y = parentY; // 偏离脊柱宽度

		NeonBone low = create(skel, prefix + "_Low", prefix + "_Up", 35, W_LIMB, color);
		low.x = 35;
		NeonBone hand = create(skel, prefix + "_Hand", prefix + "_Low", 12, W_HAND, color);
		hand.x = 35;
		return hand;
	}

	private static NeonBone create(NeonSkeleton skel, String name, String pName, float len, float width, Color c) {
		NeonBone b = skel.createBone(name, pName, len, new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, width, true));
		skel.getSlot(name).color.set(c);
		return b;
	}

	private static void setOrder(NeonSkeleton skel, String name, int idx) {
		skel.setSlotOrder(name, idx);
	}
}
