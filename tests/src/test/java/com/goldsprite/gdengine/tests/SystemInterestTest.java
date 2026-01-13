package com.goldsprite.gdengine.tests;

import com.goldsprite.gdengine.CLogAssert;
import com.goldsprite.gdengine.GdxTestRunner;
import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.BaseSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(GdxTestRunner.class)
public class SystemInterestTest {

	// 模拟继承结构
	static abstract class BaseRenderComp extends Component {}
	static class SubSpriteComp extends BaseRenderComp {}

	// 系统只关心基类
	@GameSystemInfo(interestComponents = {BaseRenderComp.class})
	static class TestRenderSystem extends BaseSystem {
		public List<GObject> getEntities() {
			return getInterestEntities();
		}
	}

	@Before
	public void setUp() {
		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		new GameWorld();
	}

	@After
	public void tearDown() {
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
	}

	@Test
	public void testPolymorphicInterest() {
		System.out.println(">>> 验证: 系统多态组件筛选 (SubClass -> SuperClass Interest)");

		TestRenderSystem system = new TestRenderSystem();

		GObject obj = new GObject("TestObj");
		// 添加子类组件
		obj.addComponent(SubSpriteComp.class);

		// 验证：系统能否通过基类 BaseRenderComp 找到持有 SubSpriteComp 的实体
		List<GObject> targets = system.getEntities();
		System.out.println("Entities found: " + targets.size());

		CLogAssert.assertEquals("应能找到持有子类组件的实体", 1, targets.size());
		CLogAssert.assertTrue("实体应为 obj", targets.contains(obj));
	}
}
