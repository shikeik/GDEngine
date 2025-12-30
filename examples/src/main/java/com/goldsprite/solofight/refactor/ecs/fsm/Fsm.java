package com.goldsprite.solofight.refactor.ecs.fsm;

import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import java.util.LinkedHashMap;
import java.util.Map;

public class Fsm {
	protected String name;
	private State currentState;
	// private State defaultState; // 暂时用不到，保留结构
	private final Map<Class<? extends State>, StateInfo> states = new LinkedHashMap<>();
	private GObject owner;

	public Fsm(GObject owner) {
		this.owner = owner;
		setName("Fsm_" + owner.getName());
	}

	public boolean isState(Class<? extends State> stateClazz) {
		if(currentState == null) return false;
		return stateClazz.isAssignableFrom(currentState.getClass());
	}

	public String getCurrentStateName() {
		return currentState == null ? "None" : currentState.getClass().getSimpleName();
	}

	public void addState(State state, int priority) {
		// [修改] 注入上下文
		state.setContext(this, owner);

		states.put(state.getClass(), new StateInfo(state, priority));

		// 如果是第一个添加的状态，默认进入
		if(currentState == null){
			changeState(state);
			// defaultState = currentState;
		}
	}

	public <T extends State> T getState(Class<T> key) {
		if(!states.containsKey(key)) return null;
		return (T) states.get(key).state;
	}

	protected void changeState(State state) {
		if(currentState != null){
			// DebugUI.log("FSM: %s Exit %s", getName(), currentState.getClass().getSimpleName());
			currentState.exit();
		}
		currentState = state;
		// DebugUI.log("FSM: %s Enter %s", getName(), currentState.getClass().getSimpleName());
		currentState.enter();
	}

	public void updateFsm(float delta) {
		// 1. 执行当前状态逻辑
		if (currentState != null) {
			currentState.running(delta);
		}

		// 2. 检查是否有更高优先级的状态想要接管
		StateInfo nextStateInfo = findNextState();

		if (nextStateInfo != null) {
			changeState(nextStateInfo.state);
		}
	}

	private StateInfo findNextState() {
		StateInfo bestStateInfo = null;
		int bestPriority = -1;

		// 如果当前状态锁死（不可退出），则不进行任何切换查找
		if(currentState != null && !currentState.canExit()) return null;

		// 如果当前状态可退出，基准优先级为当前状态的优先级
		// 意思是：只有比当前优先级 更高 或 相等 的状态才有资格打断
		if(currentState != null) {
			bestPriority = getCurrentStatePriority(currentState);
		}

		for (StateInfo stateInfo : states.values()) {
			// 跳过自己
			if(stateInfo.state == currentState) continue;

			// 核心轮询逻辑：
			// 1. 优先级 >= 当前 (高级打断低级，或者同级切换)
			// 2. 优先级 >= 已找到的候选者 (找最高级的)
			// 3. 满足进入条件 (canEnter)
			if (stateInfo.priority >= bestPriority && stateInfo.state.canEnter()) {
				bestStateInfo = stateInfo;
				bestPriority = stateInfo.priority;
			}
		}

		return bestStateInfo;
	}

	private int getCurrentStatePriority(State state) {
		StateInfo stateInfo = states.get(state.getClass());
		if(stateInfo == null) return -1;
		return stateInfo.priority;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	private static class StateInfo {
		State state;
		int priority;

		StateInfo(State state, int priority) {
			this.state = state;
			this.priority = priority;
		}
	}
}
