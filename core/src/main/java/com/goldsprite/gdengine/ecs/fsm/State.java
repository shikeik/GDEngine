package com.goldsprite.gdengine.ecs.fsm;

import com.goldsprite.gdengine.ecs.entity.GObject;

public abstract class State {

	protected Fsm fsm;
	protected GObject entity; // [适配] 注入实体引用

	/** [适配] 上下文注入 */
	public void setContext(Fsm fsm, GObject entity) {
		this.fsm = fsm;
		this.entity = entity;
		init();
	}

	protected void init() {}

	// --- 核心逻辑 ---

	public boolean canEnter() {
		return true;
	}

	/**
	 * 返回 false 表示"不愿被打断"，会抬高打断优先级门槛。
	 * 但如果有更高优先级的状态满足条件，依然会被强制退出。
	 */
	public boolean canExit() {
		return true;
	}

	public void enter() {}

	public void exit() {}

	/** [适配] 增加 delta 时间参数 */
	public void onUpdate(float delta) {}
}
