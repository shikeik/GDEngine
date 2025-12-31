package com.goldsprite.gameframeworks.ecs;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gameframeworks.log.Debug;
import com.goldsprite.gameframeworks.ecs.component.Component;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.ecs.system.BaseSystem;
import com.goldsprite.gameframeworks.ecs.system.SceneSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏世界容器 (ECS 上下文 & 核心循环)
 * <p>
 * <b>职责：</b>
 * <ol>
 *     <li><b>时间管理</b>: 控制物理步长(FixedUpdate)和时间缩放(TimeScale)。</li>
 *     <li><b>实体管理</b>: 维护顶层实体列表，处理增删缓冲 (Flush)。</li>
 *     <li><b>系统调度</b>: 驱动所有 System 的生命周期。</li>
 * </ol>
 * </p>
 */
public class GameWorld {
    private static GameWorld instance;

    // ==========================================
    // 1. 全局配置与时间状态
    // ==========================================

    /** 全局时间缩放 (1.0 = 正常, 0.5 = 慢动作, 0 = 暂停) */
    public static float timeScale = 1.0f;

    /** 物理模拟步长 (60Hz = 0.0166s)，保证物理逻辑的确定性 */
    public static final float FIXED_DELTA_TIME = 1f / 60f;

    /** 最大物理追赶时间 (0.2s)，防止卡顿后死循环追赶 */
    private static final float MAX_FIXED_STEP_TIME = 0.2f;

    // 运行状态
    private boolean paused = false;
    private boolean awaked = false;
    private float fixedUpdateAccumulator = 0f;

    // 时间快照 (供 System 每一帧访问)
    private static float deltaTime;      // 缩放后的帧时间
    private static float unscaledDelta;  // 真实流逝时间
    private static float totalTime;      // 游戏启动总时长

    // ==========================================
    // 2. 实体容器 (顶层物体管理)
    // ==========================================

    /**
     * 活跃的顶层实体列表 (无父级的 GObject)
     * <p>只有在这个列表里的物体，才会由 GameWorld 驱动 Update。
     * 有父级的物体由父级驱动。</p>
     */
    private final List<GObject> rootEntities = new ArrayList<>(512);

    /** 待添加缓冲队列：防止遍历 rootEntities 时添加新物体导致 Crash */
    private final List<GObject> pendingAdds = new ArrayList<>(64);

    /** 待移除缓冲队列：防止遍历 rootEntities 时移除物体导致 Crash */
    private final List<GObject> pendingRemoves = new ArrayList<>(64);

    // ==========================================
    // 3. 系统容器
    // ==========================================
    private final List<BaseSystem> systems = new ArrayList<>();
    private final Map<Class<? extends BaseSystem>, BaseSystem> systemMap = new HashMap<>();

    // 性能优化：按类型分组，避免每帧 update 时做 instanceof 判断
    private final List<BaseSystem> updateSystems = new ArrayList<>();
    private final List<BaseSystem> fixedUpdateSystems = new ArrayList<>();

    /** 核心系统：负责驱动 GObject 的生命周期 (Unity 兼容层) */
    public SceneSystem sceneSystem;

    // ==========================================
    // 4. 全局服务引用
    // ==========================================
    // UI 视口 (用于 Input 处理: screen -> world)
    public static Viewport uiViewport;
    // 世界相机 (用于 System 获取位置: culling / physics)
    public static Camera worldCamera;

    // ==========================================
    // 构造与初始化
    // ==========================================

    public GameWorld() {
        if (instance != null) throw new RuntimeException("GameWorld 实例已存在! 请确保单例唯一性。");
        instance = this;

        initializeCoreSystems();
    }

    public static GameWorld inst() { return instance; }

    private void initializeCoreSystems() {
        Debug.log("GameWorld: 正在初始化核心系统...");
        // 初始化场景系统，它会自动调用 registerSystem 把自己注册进来
        sceneSystem = new SceneSystem();
    }

    /**
     * 设置全局视口引用 (通常在 Screen.create 或 resize 中调用)
     */
    public void setReferences(Viewport uiViewport, Camera worldCamera) {
        GameWorld.uiViewport = uiViewport;
        GameWorld.worldCamera = worldCamera;
    }

    // ==========================================
    // 5. 核心主循环 (The Game Loop)
    // ==========================================

    /**
     * 每帧调用，驱动整个世界
     * @param rawDelta Gdx.graphics.getDeltaTime() 传入的原始时间
     */
    public void update(float rawDelta) {
        // 1. 时间计算
        unscaledDelta = rawDelta;
        deltaTime = rawDelta * timeScale;
        totalTime += deltaTime;

        // 2. [Early Flush] 结构变更处理 (核心)
        // 处理上一帧产生的 add/remove 请求。
        // 确保本帧 Update 开始时，所有新出生的物体都在 rootEntities 列表中。
        flushEntities();

        // 3. [Awake Phase] 世界首次启动检查
        if (!awaked) {
            awaked = true;
            // 唤醒所有系统
            for(BaseSystem sys : systems) sys.awake();

            // 注意：这里删除了 sceneSystem.awakeScene()，因为组件在 add 时已自动 awake。

            // 确保第一帧也能执行 Start 逻辑
            sceneSystem.executeStartTask();
            Debug.log("GameWorld: 逻辑循环已启动");
            return; // 第一帧通常 delta 不稳定，跳过逻辑运行
        }

        if (paused) return;

        // 4. [Start Phase] 统一执行 Start
        // 任何刚 Awake 但还没 Start 的组件，在这里统一初始化跨对象逻辑
        sceneSystem.executeStartTask();

        // 5. [Fixed Update] 物理循环
        fixedUpdateAccumulator += deltaTime;
        // 螺旋死循环防护
        if (fixedUpdateAccumulator > MAX_FIXED_STEP_TIME) fixedUpdateAccumulator = MAX_FIXED_STEP_TIME;

        while (fixedUpdateAccumulator >= FIXED_DELTA_TIME) {
            for (int i = 0; i < fixedUpdateSystems.size(); i++) {
                BaseSystem sys = fixedUpdateSystems.get(i);
                if (sys.isEnabled()) sys.fixedUpdate(FIXED_DELTA_TIME);
            }
            fixedUpdateAccumulator -= FIXED_DELTA_TIME;
        }

        // 6. [Update] 逻辑循环
        for (int i = 0; i < updateSystems.size(); i++) {
            BaseSystem sys = updateSystems.get(i);
            if (sys.isEnabled()) sys.update(deltaTime);
        }

        // 7. [Destroy] 帧末清理 (收尸)
        // 这一步会调用 GObject.destroyImmediate，触发 unregisterGObject
        // 导致死亡物体进入 pendingRemoves 队列
        sceneSystem.executeDestroyTask();

        // 8. [Late Flush] 立即移除刚刚销毁的物体
        // 这样 rootEntities 列表在帧结束时就是干净的，引用断开，利于 GC 尽快回收
        flushEntities();
    }

    /** 将缓冲队列应用到主列表 */
    private void flushEntities() {
        if (!pendingAdds.isEmpty()) {
            rootEntities.addAll(pendingAdds);
            pendingAdds.clear();
        }
        if (!pendingRemoves.isEmpty()) {
            rootEntities.removeAll(pendingRemoves);
            pendingRemoves.clear();
        }
    }

    // ==========================================
    // 6. 实体管理 (内部API)
    // ==========================================

    /**
     * 注册顶层实体 (由 GObject 构造函数调用)
     * 意味着该物体没有父级，需要由 GameWorld 驱动
     */
    public static void registerGObject(GObject gobject) {
        if (instance == null) return;
        // 防御性检查：不在列表中才添加，且如果正在待删除队列中则救回
        if (!instance.pendingAdds.contains(gobject) && !instance.rootEntities.contains(gobject)) {
            instance.pendingAdds.add(gobject);
            instance.pendingRemoves.remove(gobject);
        }
    }

    /**
     * 注销顶层实体 (由 GObject.setParent 或 destroyImmediate 调用)
     * 意味着该物体有了父级(由父级驱动)，或者被销毁了
     */
    public static void unregisterGObject(GObject gobject) {
        if (instance == null) return;
        instance.pendingRemoves.add(gobject);
        instance.pendingAdds.remove(gobject);
    }

    // --- 销毁请求转发 (代理给 SceneSystem) ---
    public void addDestroyGObject(GObject gobject) {
        if (sceneSystem != null) sceneSystem.addDestroyGObject(gobject);
    }

    public void addDestroyComponent(Component component) {
        if (sceneSystem != null) sceneSystem.addDestroyComponent(component);
    }

    /** 获取所有顶层实体 (供 SceneSystem 遍历) */
    public List<GObject> getRootEntities() {
        return rootEntities;
    }

    // ==========================================
    // 7. 系统管理
    // ==========================================

    public void registerSystem(BaseSystem system) {
        if (!systemMap.containsKey(system.getClass())) {
            systems.add(system);
            systemMap.put(system.getClass(), system);

            // 根据注解分类
            GameSystemInfo info = system.getSystemInfo();
            boolean isUpdate = true;
            boolean isFixed = false;

            if (info != null) {
                isUpdate = info.type().has(GameSystemInfo.SystemType.UPDATE);
                isFixed = info.type().has(GameSystemInfo.SystemType.FIXED_UPDATE);
            }

            if (isUpdate) updateSystems.add(system);
            if (isFixed) fixedUpdateSystems.add(system);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseSystem> T getSystem(Class<T> type) {
        return (T) systemMap.get(type);
    }

    // ==========================================
    // 静态工具 (Time)
    // ==========================================

    public static float getDeltaTime() { return deltaTime; }
    public static float getUnscaledDeltaTime() { return unscaledDelta; }
    public static float getTotalTime() { return totalTime; }

    /** 资源释放与重置 */
    public void dispose() {
        Debug.log("GameWorld: Disposing...");
        rootEntities.clear();
        pendingAdds.clear();
        pendingRemoves.clear();

        systems.clear();
        updateSystems.clear();
        fixedUpdateSystems.clear();
        systemMap.clear();

        ComponentManager.clearCache(); // 清理静态缓存
        instance = null;
    }
}
