package com.goldsprite.gdengine.tests; // 确认包名和你的一致

import com.goldsprite.gdengine.ecs.ComponentManager;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.CLogAssert;
import com.goldsprite.gdengine.GdxTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class EcsUnitTestSuite {

	private GameWorld world;

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
		CLogAssert.assertTrue("Awake 应在 Add 时立即触发", comp.isAwakeCalled);
		CLogAssert.assertFalse("Start 不应在 Add 时立即触发", comp.isStartCalled);

		// 2. 运行第一帧
		world.update(0.016f);

		CLogAssert.assertTrue("Start 应在第一帧触发", comp.isStartCalled);
		CLogAssert.assertEquals("Update 在首帧应被跳过(计数0)", 0, comp.updateCount);

		// 3. 运行第二帧
		world.update(0.016f);
		CLogAssert.assertEquals("Update 在次帧应开始执行(计数1)", 1, comp.updateCount);
	}

	@Test
	public void testHierarchyAndDestroy() {
		System.out.println(">>> 正在验证: 层级与销毁逻辑");

		GObject parent = new GObject("Parent");
		GObject child = new GObject("Child");

		child.setParent(parent);

		world.update(0.016f); // Flush

		// 验证层级
		CLogAssert.assertTrue("父物体由 World 管理", world.getRootEntities().contains(parent));
		CLogAssert.assertFalse("子物体不由 World 直接管理", world.getRootEntities().contains(child));
		CLogAssert.assertEquals("父物体有1个孩子", 1, parent.getChildren().size());

		// 销毁
		parent.destroy();
		CLogAssert.assertTrue("父物体进入软销毁状态", parent.isDestroyed());

		world.update(0.016f); // Destroy Task

		// 验证结果
		CLogAssert.assertFalse("父物体已从 World 移除", world.getRootEntities().contains(parent));
		CLogAssert.assertTrue("子物体被递归标记销毁", child.isDestroyed());
		CLogAssert.assertTrue("组件缓存被彻底清空", ComponentManager.getEntitiesWithComponents(TransformComponent.class).isEmpty());
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
