package com.goldsprite.solofight.tests; // 确认包名和你的一致

import com.goldsprite.gameframeworks.ecs.ComponentManager;
import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.component.Component;
import com.goldsprite.gameframeworks.ecs.component.TransformComponent;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.solofight.GdxTestRunner;

import org.junit.After;
import org.junit.Assert; // 引入原生 Assert
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class EcsUnitTestSuite {

	private GameWorld world;

	// ==========================================
	// 🛠️ 自定义话唠断言工具 (放在最前面方便看)
	// ==========================================
	private static class LogAssert {
		public static void assertTrue(String msg, boolean condition) {
			if (condition) {
				System.out.println("✅ PASS: " + msg);
			} else {
				System.err.println("❌ FAIL: " + msg);
				Assert.assertTrue(msg, false); // 触发 JUnit 失败
			}
		}

		public static void assertFalse(String msg, boolean condition) {
			if (!condition) {
				System.out.println("✅ PASS: " + msg);
			} else {
				System.err.println("❌ FAIL: " + msg + " (Expected False, got True)");
				Assert.assertFalse(msg, true);
			}
		}

		public static void assertEquals(String msg, Object expected, Object actual) {
			if (expected.equals(actual)) {
				System.out.println("✅ PASS: " + msg + " [Value: " + actual + "]");
			} else {
				System.err.println("❌ FAIL: " + msg + " (Expected: " + expected + ", Actual: " + actual + ")");
				Assert.assertEquals(msg, expected, actual);
			}
		}
	}

	// ==========================================
	// 测试生命周期
	// ==========================================

	@Before
	public void setUp() {
		try {
			if (GameWorld.inst() != null) GameWorld.inst().dispose();
		} catch (Exception ignored) {}
		world = new GameWorld();
		System.out.println("\n----------- 开始新测试 -----------");
	}

	@After
	public void tearDown() {
		if (GameWorld.inst() != null) {
			GameWorld.inst().dispose();
		}
		System.out.println("----------------------------------");
	}

	// ==========================================
	// 测试用例
	// ==========================================

	@Test
	public void testLifecycle() {
		System.out.println(">>> 正在验证: 生命周期 (Add -> Awake -> Start -> Update)");

		GObject obj = new GObject("TestObj");
		LifecycleTestComp comp = obj.addComponent(LifecycleTestComp.class);

		// 1. 验证 Awake
		LogAssert.assertTrue("Awake 应在 Add 时立即触发", comp.isAwakeCalled);
		LogAssert.assertFalse("Start 不应在 Add 时立即触发", comp.isStartCalled);

		// 2. 运行第一帧
		world.update(0.016f);

		LogAssert.assertTrue("Start 应在第一帧触发", comp.isStartCalled);
		LogAssert.assertEquals("Update 在首帧应被跳过(计数0)", 0, comp.updateCount);

		// 3. 运行第二帧
		world.update(0.016f);
		LogAssert.assertEquals("Update 在次帧应开始执行(计数1)", 1, comp.updateCount);
	}

	@Test
	public void testHierarchyAndDestroy() {
		System.out.println(">>> 正在验证: 层级与销毁逻辑");

		GObject parent = new GObject("Parent");
		GObject child = new GObject("Child");

		child.setParent(parent);

		world.update(0.016f); // Flush

		// 验证层级
		LogAssert.assertTrue("父物体由 World 管理", world.getRootEntities().contains(parent));
		LogAssert.assertFalse("子物体不由 World 直接管理", world.getRootEntities().contains(child));
		LogAssert.assertEquals("父物体有1个孩子", 1, parent.getChildren().size());

		// 销毁
		parent.destroy();
		LogAssert.assertTrue("父物体进入软销毁状态", parent.isDestroyed());

		world.update(0.016f); // Destroy Task

		// 验证结果
		LogAssert.assertFalse("父物体已从 World 移除", world.getRootEntities().contains(parent));
		LogAssert.assertTrue("子物体被递归标记销毁", child.isDestroyed());
		LogAssert.assertTrue("组件缓存被彻底清空", ComponentManager.getEntitiesWithComponents(TransformComponent.class).isEmpty());
	}

	// --- 辅助组件 ---
	public static class LifecycleTestComp extends Component {
		public boolean isAwakeCalled = false;
		public boolean isStartCalled = false;
		public int updateCount = 0;

		@Override
		protected void onAwake() { isAwakeCalled = true; }

		@Override
		protected void onStart() { isStartCalled = true; }

		@Override
		public void update(float delta) { updateCount++; }
	}
}
