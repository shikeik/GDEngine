package com.goldsprite.gameframeworks.ecs.fsm;

import com.goldsprite.gameframeworks.ecs.entity.GObject;

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

    protected void changeState(State state) {
        if (state == currentState) return;

        if(currentState != null){
            // Debug.logT("Fsm", "%s 退出 %s", getName(), currentState.getClass().getSimpleName());
            currentState.exit();
        }
        currentState = state;
        // Debug.logT("Fsm", "%s 进入 %s", getName(), currentState.getClass().getSimpleName());
        currentState.enter();
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
     * [核心算法复刻]
     * 逻辑：如果当前状态不可退出，则门槛提升至当前优先级。
     * 必须找到一个优先级 >= 门槛，且能 Enter 的状态。
     */
    private StateInfo findNextState() {
        StateInfo bestStateInfo = null;
        int bestPriority = -1;

        // 关键逻辑还原：
        // 如果当前状态不愿意退出 (!canExit)，则只有 Priority >= 当前优先级的状态才有资格打断。
        // 否则 (canExit)，任何 Priority >= -1 的状态都有资格 (即所有状态)。
        if (currentState != null && !currentState.canExit()) {
            bestPriority = getCurrentStatePriority(currentState);
        }

        for (StateInfo stateInfo : states.values()) {
            if (stateInfo.state == currentState) continue;

            // 筛选：
            // 1. 优先级必须 >= 当前门槛 (bestPriority)
            // 2. 状态本身必须满足进入条件 (canEnter)
            // 3. (隐式逻辑) 循环会不断抬高 bestPriority，确保最终找到的是最高优先级的那个
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
