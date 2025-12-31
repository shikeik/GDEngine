package com.goldsprite.solofight.tests;

import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.component.FsmComponent;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.ecs.fsm.State;
import com.goldsprite.solofight.CLogAssert;
import com.goldsprite.solofight.GdxTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** TODO:
 * 1 将话痨LogAssert提取公用
 * 2 创建Fsm测试套件类
 * - 验证高优先级自动上位
 * - 验证低优先级无法打断
 */
@RunWith(GdxTestRunner.class)
public class EcsFsmUnitTestSuite {
	private GameWorld game;

	@Before
	public void setUp() {
		if(GameWorld.inst() != null) GameWorld.inst().dispose();
		System.out.println("---------"+getClass().getSimpleName()+" 测试 开始"+"---------");
		game = new GameWorld();
	}

	@After
	public void tearDown() {
		System.out.println("---------"+getClass().getSimpleName()+" 测试 结束"+"---------");
	}

	@Test
	public void testUpperStateEnter() {
		TestAtkState atkState;
		// 初始化---------
		GObject obj = new GObject("FsmEntity");

		FsmComponent fsm = obj.addComponent(FsmComponent.class);
		fsm.addState(new TestIdleState(), 0);
		fsm.addState(atkState = new TestAtkState(), 10);
		// ---------

		// 断言
		CLogAssert.assertTrue("FsmEntity 应该拥有 状态机 组件", obj.hasComponent(fsm));
		CLogAssert.assertTrue("fsm 应该有 2 个状态", fsm.getFsm().getStates().size() == 2);

		CLogAssert.assertTrue("1=1", 1==1);
	}

	private static class TestIdleState extends State {
		@Override public boolean canEnter() { return true; }
		@Override public boolean canExit() { return false; }
	}
	private static class TestAtkState extends State {
		public boolean enterAtk, exitAtk;
		@Override public boolean canEnter() { return enterAtk; }
		@Override public boolean canExit() { return exitAtk; }
	}
	private static class TestHurtState extends State {}
}
