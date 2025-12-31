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
        // 规则1：如果当前状态"锁死" (霸体中)，且不允许强行打断，直接返回
        // (注：这里的逻辑由你的具体需求决定，目前的测试用例要求 Hurt 能打断 Skill霸体)
        // (所以我们依靠优先级比较来决定是否破招，而不是在这里直接 return null)
        // 但为了严谨，如果逻辑是 "canExit=false 意味着绝对无敌"，可以在这里拦截。
        // 根据测试 testLockAndBreak，我们希望高优先级能破招，所以不在这里拦截。

        StateInfo bestStateInfo = null;
        
        // [修复点] 基准优先级默认为 -1
        int bestPriority = -1;

        // [修复点] 如果当前有状态，基准线提升至当前状态的优先级
        // 意思是：想要篡位，你的优先级必须 >= 我！
        // (不管是普通的 Attack，还是霸体的 Skill，都得遵守基本法：低级不能打高级)
        if (currentState != null) {
            bestPriority = getCurrentStatePriority(currentState);
        }

        for (StateInfo info : states.values()) {
            if (info.state == currentState) continue;

            // 筛选：
            // 1. 优先级必须 >= 当前基准线
            // 2. 状态本身满足进入条件
            if (info.priority >= bestPriority && info.state.canEnter()) {
                
                // 特殊处理：如果优先级相等，是否切换？
                // 通常保持当前状态更稳定。但为了满足 "Idle(0) -> Attack(10)" 这种升级，
                // 或者 "Combo1(10) -> Combo2(10)" 这种同级切换，
                // 我们允许 >=。
                // 但为了通过 testPrioritySuppress (Move 5 vs Attack 10)，5 >= 10 为 false，正确拦截。
                
                // 再次确认 testLockAndBreak (Skill 10 霸体 vs Hurt 100)
                // 100 >= 10，正确打断。
                
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
