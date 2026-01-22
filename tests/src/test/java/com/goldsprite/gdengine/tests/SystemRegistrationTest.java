package com.goldsprite.gdengine.tests;

import com.goldsprite.gdengine.CLogAssert;
import com.goldsprite.gdengine.GdxTestRunner;
import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.SystemType;
import com.goldsprite.gdengine.ecs.system.BaseSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class SystemRegistrationTest {

	// --- 1. 定义测试用的 Mock 系统 ---

	// A. 纯渲染系统 (模拟 WorldRenderSystem)
	@GameSystemInfo(type = SystemType.RENDER)
	private static class MockRenderSystem extends BaseSystem {}

	// B. 纯逻辑系统
	@GameSystemInfo(type = SystemType.UPDATE)
	private static class MockLogicSystem extends BaseSystem {}

	// C. 混合系统 (Update + Fixed)
	@GameSystemInfo(type = SystemType.UPDATE | SystemType.FIXED_UPDATE)
	private static class MockPhysicsSystem extends BaseSystem {}

	// D. 默认系统 (无注解 -> 应默认为 Update)
	private static class MockDefaultSystem extends BaseSystem {}

	// --- 2. 测试环境设置 ---

	@Before
	public void setUp() {
		// 重置世界，确保干净的环境
		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		new GameWorld(); 
		System.out.println("\n----------- System Registration Test Start -----------");
	}

	@After
	public void tearDown() {
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
		System.out.println("------------------------------------------------------");
	}

	// --- 3. 测试逻辑 ---

	@Test
	public void testSystemClassification() {
		GameWorld world = GameWorld.inst();

		// 实例化系统 (BaseSystem 构造函数会自动注册)
		MockRenderSystem renderSys = new MockRenderSystem();
		MockLogicSystem logicSys = new MockLogicSystem();
		MockPhysicsSystem physSys = new MockPhysicsSystem();
		MockDefaultSystem defSys = new MockDefaultSystem();

		// --- 验证 A: 渲染系统 ---
		System.out.println(">>> 验证 RenderSystem...");
		CLogAssert.assertTrue("MockRenderSystem 应是 Render 类型", renderSys.isRenderSystem());
		CLogAssert.assertFalse("MockRenderSystem 不应是 Update 类型", renderSys.isUpdateSystem());
		
		CLogAssert.assertTrue("Render 列表应包含 MockRenderSystem", world.getRenderSystems().contains(renderSys));
		CLogAssert.assertFalse("Update 列表不应包含 MockRenderSystem", world.getUpdateSystems().contains(renderSys));

		// --- 验证 B: 逻辑系统 ---
		System.out.println(">>> 验证 LogicSystem...");
		CLogAssert.assertTrue("MockLogicSystem 应是 Update 类型", logicSys.isUpdateSystem());
		CLogAssert.assertTrue("Update 列表应包含 MockLogicSystem", world.getUpdateSystems().contains(logicSys));

		// --- 验证 C: 混合系统 ---
		System.out.println(">>> 验证 PhysicsSystem (Mixed)...");
		CLogAssert.assertTrue("Phys 应包含 Update", physSys.isUpdateSystem());
		CLogAssert.assertTrue("Phys 应包含 Fixed", physSys.isFixedSystem());
		
		CLogAssert.assertTrue("Update 列表应包含 Phys", world.getUpdateSystems().contains(physSys));
		CLogAssert.assertTrue("Fixed 列表应包含 Phys", world.getFixedUpdateSystems().contains(physSys));
		CLogAssert.assertFalse("Render 列表不应包含 Phys", world.getRenderSystems().contains(physSys));

		// --- 验证 D: 默认系统 ---
		System.out.println(">>> 验证 DefaultSystem (No Annotation)...");
		CLogAssert.assertTrue("无注解应默认为 Update", defSys.isUpdateSystem());
		CLogAssert.assertTrue("Update 列表应包含 Default", world.getUpdateSystems().contains(defSys));
	}
	
	@Test
	public void testBitmaskLogic() {
		System.out.println(">>> 验证位运算逻辑...");
		
		int renderFlag = SystemType.RENDER; // 4
		int updateFlag = SystemType.UPDATE; // 1
		int mixed = SystemType.RENDER | SystemType.UPDATE; // 5
		
		CLogAssert.assertTrue("Render check failed", SystemType.isRender(renderFlag));
		CLogAssert.assertTrue("Mixed check render failed", SystemType.isRender(mixed));
		CLogAssert.assertTrue("Mixed check update failed", SystemType.isUpdate(mixed));
		CLogAssert.assertFalse("Render check update failed", SystemType.isUpdate(renderFlag));
	}
}