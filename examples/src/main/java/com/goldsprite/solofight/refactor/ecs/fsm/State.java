package com.goldsprite.solofight.refactor.ecs.fsm;

import com.goldsprite.solofight.refactor.ecs.entity.GObject;

public abstract class State {
	protected Fsm fsm;
	protected GObject entity; // [新增] 持有实体引用

	public void setContext(Fsm fsm, GObject entity) {
		this.fsm = fsm;
		this.entity = entity;
		init();
	}

	protected void init(){}

	// 核心判定：当前能否进入此状态？(例如：有输入信号、CD冷却完毕)
	public boolean canEnter() {
		return true;
	}
	
	// 核心判定：当前能否退出此状态？(例如：攻击后摇是否结束)
	public boolean canExit() {
		return true;
	}

	public void enter() {}

	public void exit() {}

	public void running(float delta) {} // [修改] 传入 delta
}