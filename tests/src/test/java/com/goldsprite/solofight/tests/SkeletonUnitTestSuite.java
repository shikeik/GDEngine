package com.goldsprite.solofight.tests;

import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.solofight.CLogAssert;
import com.goldsprite.solofight.GdxTestRunner;
import com.goldsprite.gdengine.ecs.skeleton.NeonBone;
import com.goldsprite.gdengine.ecs.skeleton.NeonSkeleton;
import com.goldsprite.gdengine.ecs.skeleton.NeonSlot;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class SkeletonUnitTestSuite {

	@Test
	public void testBoneHierarchy() {
		System.out.println(">>> 验证: 骨骼层级矩阵计算 (Parent-Child Transform)");

		NeonSkeleton skeleton = new NeonSkeleton();

		// 1. 创建层级: Body -> Arm
		// Body 在 (100, 100)
		NeonBone body = skeleton.createBone("Body", "root", 50, null);
		body.x = 100;
		body.y = 100;

		// Arm 在 Body 的 (10, 0) 处
		NeonBone arm = skeleton.createBone("Arm", "Body", 20, null);
		arm.x = 10;
		arm.y = 0;

		// 2. 更新矩阵
		skeleton.update();

		// 验证初始世界坐标 (没有旋转)
		// Arm World X = 100 + 10 = 110
		// Arm World Y = 100 + 0 = 100
		CLogAssert.assertEquals("Arm初始世界X应为110", 110f, arm.worldTransform.m02);
		CLogAssert.assertEquals("Arm初始世界Y应为100", 100f, arm.worldTransform.m12);

		// 3. 旋转 Body 90度 (逆时针)
		System.out.println("  -> 旋转 Body 90度...");
		body.rotation = 90f;
		skeleton.update();

		// 验证旋转后的坐标
		// Arm 应该在 Body 的上方 (100, 100+10)
		// 允许微小误差 (float precision)
		float armWx = arm.worldTransform.m02;
		float armWy = arm.worldTransform.m12;

		CLogAssert.assertTrue("Arm X 应接近 100 (实际: " + armWx + ")", MathUtils.isEqual(armWx, 100f, 0.01f));
		CLogAssert.assertTrue("Arm Y 应接近 110 (实际: " + armWy + ")", MathUtils.isEqual(armWy, 110f, 0.01f));
	}

	@Test
	public void testDrawOrder() {
		System.out.println(">>> 验证: 渲染队列 (Draw Order)");

		NeonSkeleton skeleton = new NeonSkeleton();

		// 依次创建 A, B, C
		skeleton.createBone("A", "root", 0, null);
		skeleton.createBone("B", "root", 0, null);
		skeleton.createBone("C", "root", 0, null);

		// 1. 默认顺序: A, B, C (先进先出)
		NeonSlot slot0 = skeleton.getDrawOrder().get(0);
		NeonSlot slot2 = skeleton.getDrawOrder().get(2);

		CLogAssert.assertEquals("第0个应该是 A", "A", slot0.name);
		CLogAssert.assertEquals("第2个应该是 C", "C", slot2.name);

		// 2. 手动调整: 把 C 放到最前面 (Index 0)
		System.out.println("  -> 把 C 移到最底层 (Index 0)");
		skeleton.setSlotOrder("C", 0);

		// 期望顺序: C, A, B
		CLogAssert.assertEquals("现在第0个应该是 C", "C", skeleton.getDrawOrder().get(0).name);
		CLogAssert.assertEquals("现在第1个应该是 A", "A", skeleton.getDrawOrder().get(1).name);
		CLogAssert.assertEquals("现在第2个应该是 B", "B", skeleton.getDrawOrder().get(2).name);
	}
}
