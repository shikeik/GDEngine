package com.goldsprite.gdengine.ecs.fsm;

import com.goldsprite.gdengine.ecs.entity.GObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Fsm {
	protected String name;
	private final GObject owner; // [适配] 持有者

	private State currentState;
	private final Map<Class<? extends State>, StateInfo> states = new LinkedHashMap<>();

	public Fsm(GObject owner) {
		this.owner = owner;
		this.name = "FSM_" + owner.getName();
	}

	public boolean isState(Class<? extends State> stateClazz) {
		if(currentState == null) return false;
		return stateClazz.isAssignableFrom(currentState.getClass());
	}

	public void addState(State state, int priority) {
		state.setContext(this, owner); // [适配] 注入上下文
		states.put(state.getClass(), new StateInfo(state, priority));

		// 默认进入第一个添加的状态
		if(currentState == null){
			changeState(state);
		}
	}

	public <T extends State> T getState(Class<T> key) {
		StateInfo info = states.get(key);
		return info != null ? (T)info.state : null;
	}

	public List<StateInfo> getStates() {
		return new ArrayList<>(states.values());
	}

	public void changeState(State state) {
		if (state == currentState) return;

		if(currentState != null){
			// Debug.logT("Fsm", "%s 退出 %s", getName(), currentState.getClass().getSimpleName());
			currentState.exit();
		}
		currentState = state;
		// Debug.logT("Fsm", "%s 进入 %s", getName(), currentState.getClass().getSimpleName());
		currentState.enter();
	}

	/**
	 * [新增] 手动切换状态 (API)
	 * 通常在 State 内部逻辑中使用，例如：攻击结束切回 Idle
	 */
	public void changeState(Class<? extends State> stateType) {
		StateInfo info = states.get(stateType);
		if (info != null) {
			changeState(info.state);
		} else {
			// 可选：打印警告，说明试图切换到一个未注册的状态
			// Debug.log("FSM Warning: 状态未注册 " + stateType.getSimpleName());
		}
	}

	public void update(float delta) {
		// 1. 运行当前状态
		if (currentState != null) {
			currentState.onUpdate(delta);
		}

		// 2. 轮询查找下一状态
		StateInfo nextStateInfo = findNextState();

		if (nextStateInfo != null) {
			changeState(nextStateInfo.state);
		}
	}

	/**
	 * [核心修复] 查找逻辑
	 */
	private StateInfo findNextState() {
		StateInfo bestStateInfo = null;
		int bestPriority = -1;
		if(!currentState.canExit()) bestPriority = getCurrentStatePriority(currentState);

		for (StateInfo info : states.values()) {
			if (info.state == currentState) continue;
			if (info.priority >= bestPriority && info.state.canEnter()) {
				bestStateInfo = info;
				bestPriority = info.priority;
			}
		}

		return bestStateInfo;
	}

	private int getCurrentStatePriority(State state) {
		StateInfo stateInfo = states.get(state.getClass());
		if(stateInfo == null) return -1;
		return stateInfo.priority;
	}

	public String getName() { return name; }
	public String getCurrentStateName() {
		return currentState != null ? currentState.getClass().getSimpleName() : "None";
	}

	public static class StateInfo {
		State state;
		int priority;

		StateInfo(State state, int priority) {
			this.state = state;
			this.priority = priority;
		}
	}
}
