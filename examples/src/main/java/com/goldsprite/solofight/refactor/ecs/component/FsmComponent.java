package com.goldsprite.solofight.refactor.ecs.component;

import com.goldsprite.solofight.refactor.ecs.fsm.Fsm;
import com.goldsprite.solofight.refactor.ecs.fsm.State;

public class FsmComponent extends Component {
	private Fsm fsm;

	@Override
	public void onAwake() {
		super.awake();
		// 初始化 FSM，注入 owner
		fsm = new Fsm(getGObject());
	}

	@Override
	public void update(float delta) {
		super.update(delta);
		if (fsm != null) {
			fsm.updateFsm(delta);
		}
	}
	
	// --- API ---
	
	public void registerState(State state, int priority) {
		if (fsm == null) fsm = new Fsm(getGObject()); // 容错
		fsm.addState(state, priority);
	}
	
	public String getCurrentStateName() {
		return fsm != null ? fsm.getCurrentStateName() : "Null";
	}
	
	public Fsm getFsm() { return fsm; }
}
