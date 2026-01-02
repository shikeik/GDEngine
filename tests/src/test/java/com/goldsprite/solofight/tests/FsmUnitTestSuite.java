package com.goldsprite.solofight.tests;

import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.FsmComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.fsm.State;
import com.goldsprite.solofight.CLogAssert; // 引用你提取的工具类
import com.goldsprite.solofight.GdxTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class FsmUnitTestSuite {

	private GameWorld world;
	private GObject player;
	private FsmComponent fsm;

	@Before
	public void setUp() {
		// 1. 重置世界
		try {
			if (GameWorld.inst() != null) GameWorld.inst().dispose();
		} catch (Exception ignored) {}
		world = new GameWorld();

		// 2. 创建带 FSM 的主角
		player = new GObject("Player");
		fsm = player.addComponent(FsmComponent.class);

		// 3. 跑一帧让 FSM Awake (初始化)
		world.update(0.016f);

		System.out.println("\n----------- FSM 测试开始 -----------");
	}

	@After
	public void tearDown() {
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
		System.out.println("------------------------------------");
	}

	// ==========================================
	// 测试用例
	// ==========================================

	@Test
	public void testPriorityInterrupt() {
		System.out.println(">>> 场景 1: 高优先级打断低优先级 (Idle -> Attack)");

		// 1. 准备状态
		Idle idle = new Idle();     // P: 0
		Attack attack = new Attack(); // P: 10

		fsm.addState(idle, 0);
		fsm.addState(attack, 10);

		// 2. 初始条件: 只允许 Idle
		idle.setCondition(true);
		attack.setCondition(false);

		world.update(0.016f);
		CLogAssert.assertEquals("初始应为 Idle", "Idle", fsm.getCurrentStateName());

		// 3. 触发攻击: 两个都满足条件，但 Attack(10) > Idle(0)
		attack.setCondition(true);

		world.update(0.016f);
		CLogAssert.assertEquals("Attack 应打断 Idle", "Attack", fsm.getCurrentStateName());

		// 4. 攻击结束: Attack 条件不满足，应回落到 Idle
		attack.setCondition(false);

		world.update(0.016f);
		CLogAssert.assertEquals("Attack 结束后应回落 Idle", "Idle", fsm.getCurrentStateName());
	}

	@Test
	public void testPrioritySuppress() {
		System.out.println(">>> 场景 2: 等级压制 (Attack 运行中，Move 无法打断)");

		Attack attack = new Attack(); // P: 10
		Move move = new Move();       // P: 5

		fsm.addState(attack, 10);
		fsm.addState(move, 5);

		// 让 Attack 运行
		attack.setCondition(true);
		move.setCondition(false);
		world.update(0.016f);

		// 尝试触发 Move
		move.setCondition(true); // 此时 Attack(10) 和 Move(5) 都满足条件

		world.update(0.016f);
		CLogAssert.assertEquals("低优先级 Move 不应打断 Attack", "Attack", fsm.getCurrentStateName());
	}

	@Test
	public void testLockAndBreak() {
		System.out.println(">>> 场景 3: 霸体与破招 (不可退出状态)");

		Skill skill = new Skill(); // P: 10 (霸体)
		Move move = new Move();    // P: 5
		Hurt hurt = new Hurt();    // P: 100

		fsm.addState(skill, 10);
		fsm.addState(move, 5);
		fsm.addState(hurt, 100);

		// 1. 进入技能状态，并开启霸体 (canExit = false)
		skill.setCondition(true);
		skill.setLocked(true);
		world.update(0.016f);
		CLogAssert.assertEquals("当前是 Skill", "Skill", fsm.getCurrentStateName());

		// 2. 尝试移动 (Move P:5 < Skill P:10)
		// 即使 Skill 不是霸体，Move 也打断不了。这里验证基本流程。
		move.setCondition(true);
		world.update(0.016f);
		CLogAssert.assertEquals("Move 无法打断", "Skill", fsm.getCurrentStateName());

		// 3. 尝试受击 (Hurt P:100 > Skill P:10)
		// 核心验证：即使 Skill 锁死 (!canExit)，但 Hurt 优先级极高，必须能强行破招
		// 逻辑依据：findNextState 里 bestPriority 提升到了 currentPriority，但 100 >= 10，满足条件。
		hurt.setCondition(true);
		world.update(0.016f);
		CLogAssert.assertEquals("Hurt 应无视霸体强行打断", "Hurt", fsm.getCurrentStateName());
	}

	// ==========================================
	// 🎭 演员状态类 (Mock Classes)
	// 为了让 fsm.getCurrentStateName() 返回不同的名字，
	// 我们必须定义具体的子类。
	// ==========================================

	// 1. 通用基类 (控制逻辑)
	private static class MockState extends State {
		boolean condition = false; // 控制 canEnter
		boolean locked = false;    // 控制 canExit (true表示不可退出)

		public void setCondition(boolean v) { this.condition = v; }
		public void setLocked(boolean v) { this.locked = v; }

		@Override public boolean canEnter() { return condition; }
		@Override public boolean canExit() { return !locked; }

		// 调试打印
		@Override public void enter() { System.out.println("  -> Enter: " + getClass().getSimpleName()); }
		@Override public void exit() { System.out.println("  <- Exit: " + getClass().getSimpleName()); }
	}

	// 2. 具体状态 (名字不同)
	private static class Idle extends MockState {}
	private static class Move extends MockState {}
	private static class Attack extends MockState {}
	private static class Skill extends MockState {}
	private static class Hurt extends MockState {}
}
