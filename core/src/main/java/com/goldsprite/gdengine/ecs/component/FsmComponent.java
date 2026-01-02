package com.goldsprite.gdengine.ecs.component;

import com.goldsprite.gdengine.ecs.fsm.Fsm;
import com.goldsprite.gdengine.ecs.fsm.State;

public class FsmComponent extends Component {

	private Fsm fsm;

	@Override
	protected void onAwake() {
		// 创建 FSM 实例
		fsm = new Fsm(getGObject());
	}

	@Override
	public void update(float delta) {
		if (fsm != null) {
			fsm.update(delta);
		}
	}

	// --- 代理方法 ---

	public void addState(State state, int priority) {
		if (fsm == null) fsm = new Fsm(getGObject());
		fsm.addState(state, priority);
	}

	public Fsm getFsm() { return fsm; }

	public String getCurrentStateName() {
		return fsm != null ? fsm.getCurrentStateName() : "None";
	}

	@Override
	public String toString() {
		return String.format("%s [State:%s]", super.toString(), getCurrentStateName());
	}
}
