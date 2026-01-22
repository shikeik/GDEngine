package com.goldsprite.gdengine.tests;

import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.CLogAssert;
import com.goldsprite.gdengine.GdxTestRunner;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class TransformMatrixTest {

	private static final float EPSILON = 0.01f;

	@Test
	public void testBasicTRS() {
		System.out.println(">>> 验证: 基础 TRS (平移/旋转/缩放)");
		TransformComponent t = new TransformComponent();

		t.setPosition(100, 50);
		t.setRotation(90);
		t.setScale(2, 2);

		// 更新矩阵 (无父级)
		t.updateWorldTransform(null);

		// 验证缓存值
		CLogAssert.assertTrue("WorldX 应该是 100", MathUtils.isEqual(t.worldPosition.x, 100, EPSILON));
		CLogAssert.assertTrue("WorldY 应该是 50", MathUtils.isEqual(t.worldPosition.y, 50, EPSILON));
		CLogAssert.assertTrue("WorldRot 应该是 90", MathUtils.isEqual(t.worldRotation, 90, EPSILON));
		CLogAssert.assertTrue("WorldScaleX 应该是 2", MathUtils.isEqual(t.worldScale.x, 2, EPSILON));
	}

	@Test
	public void testHierarchyPosition() {
		System.out.println(">>> 验证: 层级位移 (父级旋转带动子级)");

		GObject parent = new GObject("Parent");
		GObject child = new GObject("Child");
		child.setParent(parent);

		// 父级: (0,0), 旋转 90度
		parent.transform.setPosition(0, 0);
		parent.transform.setRotation(90);

		// 子级: 本地 (100, 0) -> 也就是父级的"右边"
		// 因为父级转了90度，世界坐标里子级应该在 (0, 100)
		child.transform.setPosition(100, 0);

		// 手动更新链条 (模拟 GameWorld.update)
		parent.transform.updateWorldTransform(null);
		child.transform.updateWorldTransform(parent.transform);

		float wx = child.transform.worldPosition.x;
		float wy = child.transform.worldPosition.y;

		System.out.println(String.format("Child World: (%.2f, %.2f)", wx, wy));

		CLogAssert.assertTrue("Child World X 应接近 0", MathUtils.isEqual(wx, 0, EPSILON));
		CLogAssert.assertTrue("Child World Y 应接近 100", MathUtils.isEqual(wy, 100, EPSILON));
		CLogAssert.assertTrue("Child World Rot 应叠加为 90", MathUtils.isEqual(child.transform.worldRotation, 90, EPSILON));
	}

	@Test
	public void testHierarchyScale() {
		System.out.println(">>> 验证: 层级缩放 (父级缩放影响子级位置和大小)");

		GObject parent = new GObject("Parent");
		GObject child = new GObject("Child");
		child.setParent(parent);

		// 父级: 放大 2 倍
		parent.transform.setScale(2, 2);

		// 子级: 本地 (50, 0)
		child.transform.setPosition(50, 0);

		// 手动更新
		parent.transform.updateWorldTransform(null);
		child.transform.updateWorldTransform(parent.transform);

		// 验证位置: 50 * 2 = 100
		float wx = child.transform.worldPosition.x;
		CLogAssert.assertTrue("Child World X 应被放大为 100", MathUtils.isEqual(wx, 100, EPSILON));

		// 验证缩放: 1 * 2 = 2
		float ws = child.transform.worldScale.x;
		CLogAssert.assertTrue("Child World Scale 应为 2", MathUtils.isEqual(ws, 2, EPSILON));
	}

	@Test
	public void testSetWorldPosition() {
		System.out.println(">>> 验证: 逆向设置世界坐标 (SetWorldPosition)");

		GObject parent = new GObject("Parent");
		GObject child = new GObject("Child");
		child.setParent(parent);

		// 父级: (100, 100)
		parent.transform.setPosition(100, 100);
		// 先更新父级矩阵，因为 setWorldPosition 需要用到父级矩阵的逆
		parent.transform.updateWorldTransform(null);

		// 我们希望子级在世界坐标 (200, 100)
		// 理论上 Local 应该是 (100, 0)
		child.transform.setWorldPosition(200, 100);

		float lx = child.transform.position.x;
		float ly = child.transform.position.y;

		System.out.println(String.format("Calculated Local: (%.2f, %.2f)", lx, ly));

		CLogAssert.assertTrue("Local X 应为 100", MathUtils.isEqual(lx, 100, EPSILON));
		CLogAssert.assertTrue("Local Y 应为 0", MathUtils.isEqual(ly, 0, EPSILON));
	}

	@Test
	public void testSetWorldRotation() {
		System.out.println(">>> 验证: 逆向设置世界旋转");

		GObject parent = new GObject("P");
		GObject child = new GObject("C");
		child.setParent(parent);

		parent.transform.setRotation(90);
		parent.transform.updateWorldTransform(null); // 必须先更新父级

		// 希望子级世界旋转为 0 (竖直)
		// 父级 90，local 应该是 -90
		child.transform.setWorldRotation(0);

		CLogAssert.assertTrue("Local Rot 应为 -90", MathUtils.isEqual(child.transform.rotation, -90, EPSILON));
	}
}
