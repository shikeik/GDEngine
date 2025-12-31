package com.goldsprite.solofight.tests; // 测试包名可以自定义

// 引入你重构后的框架包
import com.goldsprite.gameframeworks.ecs.ComponentManager;
import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.component.Component;
import com.goldsprite.gameframeworks.ecs.component.TransformComponent;
import com.goldsprite.gameframeworks.ecs.entity.GObject;

// 引入 GDX 测试运行器
import com.goldsprite.solofight.GdxTestRunner; // 假设 Runner 还在原来的位置，或者你也把它移到了 frameworks？

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(GdxTestRunner.class)
public class EcsUnitTestSuite {

	private GameWorld world;

	@Before
	public void setUp() {
		// 防止单例污染，每次测试前重置
		// 注意：如果 dispose() 把 instance 置空了，需要确保能重新 new
		try {
			if (GameWorld.inst() != null) GameWorld.inst().dispose();
		} catch (Exception ignored) {}

		world = new GameWorld();
	}

	@After
	public void tearDown() {
		if (GameWorld.inst() != null) {
			GameWorld.inst().dispose();
		}
	}

	// --- 测试用例 1: 基础生命周期 ---
	@Test
	public void testLifecycle() {
		System.out.println("--- Test: Lifecycle (Add -> Awake -> Start -> Update) ---");

		GObject obj = new GObject("TestObj");
		LifecycleTestComp comp = obj.addComponent(LifecycleTestComp.class);

		// 1. Awake 应立即触发
		assertTrue("Awake should trigger on add", comp.isAwakeCalled);
		assertFalse("Start should NOT trigger immediately", comp.isStartCalled);

		// 2. 运行第一帧 (Init + Start Only)
		// GameWorld 设计特性：第一帧用于初始化和执行 Start，不执行 Update
		world.update(0.016f);

		assertTrue("Start should trigger on 1st frame", comp.isStartCalled);

		// [修正预期] 第一帧 Update 逻辑被跳过，所以计数应为 0
		assertEquals("Update count should be 0 on init frame", 0, comp.updateCount);

		// 3. 运行第二帧 (Normal Loop)
		world.update(0.016f);

		// [修正预期] 现在才开始执行第一次 Update
		assertEquals("Update count should be 1", 1, comp.updateCount);
	}

	// --- 测试用例 2: 父子层级与递归销毁 ---
	@Test
	public void testHierarchyAndDestroy() {
		System.out.println("--- Test: Hierarchy & Destroy ---");

		GObject parent = new GObject("Parent");
		GObject child = new GObject("Child");

		// 建立父子关系
		child.setParent(parent);

		// 跑一帧，让状态同步 (Flush)
		world.update(0.016f);

		// 验证层级
		assertTrue("Parent should be in World roots", world.getRootEntities().contains(parent));
		assertFalse("Child should NOT be in World roots (managed by parent)", world.getRootEntities().contains(child));
		assertEquals("Parent should have 1 child", 1, parent.getChildren().size());

		// 销毁父物体
		parent.destroy();

		// 验证：软销毁状态
		assertTrue("Parent isDestroyed", parent.isDestroyed());
		// 此时还没执行 destroyImmediate，所以引用还在

		// 跑一帧 (执行 Destroy Task + Late Flush)
		world.update(0.016f);

		// 验证：硬销毁结果
		// 1. 父物体应从世界移除
		assertFalse("Parent removed from world", world.getRootEntities().contains(parent));
		// 2. 子物体应该被递归销毁
		assertTrue("Child isDestroyed", child.isDestroyed());

		// 3. 组件缓存应该被清空
		// 在 assertTrue 之前加入：
		List<GObject> leftovers = ComponentManager.getEntitiesWithComponents(TransformComponent.class);
		if (!leftovers.isEmpty()) {
			System.err.println("残留实体: " + leftovers.get(0).getName() +
				", IsDestroyed=" + leftovers.get(0).isDestroyed());
		}
		assertTrue("Component Cache Cleared", leftovers.isEmpty());
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
