package com.goldsprite.solofight.refactor.ecs;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.solofight.core.Debug;
import com.goldsprite.solofight.refactor.ecs.component.Component; // [变更] 引用新类
import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import com.goldsprite.solofight.refactor.ecs.enums.ManageMode;
import com.goldsprite.solofight.refactor.ecs.system.BaseSystem;
import com.goldsprite.solofight.refactor.ecs.system.SceneSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 游戏世界容器 (ECS 环境上下文)
 * 职责：
 * 1. 维护全局单例 (Instance)。
 * 2. 维护系统列表与实体列表。
 * 3. 核心主循环 (Game Loop): 处理时间步长，调度 System 更新。
 */
public class GameWorld {
    private static GameWorld instance;

    // ==========================================
    // 1. 核心状态与时间
    // ==========================================
    private boolean paused = false;
    private boolean awaked = false;

    // 物理模拟步长 (60Hz)
    // 无论屏幕刷新率是 30fps 还是 144fps，物理逻辑永远按 0.016s 一步来跑，保证结果确定性。
    public static final float FIXED_DELTA_TIME = 1f / 60f; 
    private float fixedUpdateAccumulator = 0f; // 时间累加器

    // 全局时间记录
    private static float totalDeltaTime;

    // ==========================================
    // 2. 容器
    // ==========================================
    // 所有系统
    private final List<BaseSystem> systems = new CopyOnWriteArrayList<>();
    // 系统类型映射 (方便 getSystem)
    private final Map<Class<? extends BaseSystem>, BaseSystem> systemMap = new HashMap<>();

    // 分组缓存 (避免每帧遍历判断类型)
    private final List<BaseSystem> updateSystems = new ArrayList<>();
    private final List<BaseSystem> fixedUpdateSystems = new ArrayList<>();

    // 所有**顶层**实体 (只有 Root Entity 在这里，子物体不在)
    private final List<GObject> allEntities = new CopyOnWriteArrayList<>();

    // ==========================================
    // 3. 内置核心系统
    // ==========================================
    public SceneSystem sceneSystem;
    // public PhysicsSystem physicsSystem; // 预留
    // public RenderSystem renderSystem;   // 预留

    // ==========================================
    // 4. 全局视口引用 (方便 System 访问)
    // ==========================================
    private Viewport uiViewport;
    private OrthographicCamera worldCamera;

    public GameWorld() {
        if (instance != null) throw new RuntimeException("GameWorld 只能有一个实例!");
        instance = this;

        initializeCoreSystems();
    }

    public static GameWorld inst() {
        return instance;
    }

    private void initializeCoreSystems() {
        Debug.log("GameWorld: 初始化核心系统...");

        // 1. 初始化场景系统 (负责生命周期驱动)
        // 注意：new 的时候，BaseSystem 构造函数会自动调用 GameWorld.registerSystem(this)
        sceneSystem = new SceneSystem();

        // 2. TODO: 初始化物理、渲染系统
        // physicsSystem = new PhysicsSystem();

        Debug.log("GameWorld: 初始化完成, 系统数: " + systems.size());
    }

    /** 设置视口引用 (通常在 Screen.create 或 resize 中调用) */
    public void setViewport(Viewport uiViewport, OrthographicCamera worldCamera) {
		this.uiViewport = uiViewport;
		this.worldCamera = worldCamera;
    }
	
	public Viewport getUIViewport() {
		return uiViewport;
	}
	
	public OrthographicCamera getWorldCamera() {
		return worldCamera;
	}

    // ==========================================
    // 5. 游戏主循环 (The Game Loop)
    // ==========================================

    public void update(float delta) {
        totalDeltaTime += delta;

        // 0. 世界首次唤醒
        if (!awaked) {
            awaked = true;
            // 唤醒所有系统
            for(BaseSystem system : systems) system.awake();
            // 唤醒场景中已有的实体
            sceneSystem.awakeScene();
            Debug.log("GameWorld: 世界已唤醒");
            return; // 第一帧仅唤醒，不执行逻辑，防止 dt 过大
        }

        if (paused) return;

        // 1. Fixed Update (物理/逻辑)
        // 追赶式更新：如果画面卡顿了(delta很大)，循环多跑几次物理，保证物理时间跟上现实时间
        fixedUpdateAccumulator += delta;

        // 防止螺旋死循环 (Spiral of Death)：如果 delta 实在太大(断点调试)，限制最大追赶次数
        if (fixedUpdateAccumulator > 0.2f) fixedUpdateAccumulator = 0.2f;

        while (fixedUpdateAccumulator >= FIXED_DELTA_TIME) {
            // 执行所有注册了 FIXED_UPDATE 的系统
            for (BaseSystem sys : fixedUpdateSystems) {
                if (sys.isEnabled()) sys.fixedUpdate(FIXED_DELTA_TIME);
            }
            fixedUpdateAccumulator -= FIXED_DELTA_TIME;
        }

        // 2. Variable Update (渲染/输入/动画)
        // 执行所有注册了 UPDATE 的系统
        for (BaseSystem sys : updateSystems) {
            if (sys.isEnabled()) sys.update(delta);
        }

        // 3. Late Update / Cleanup (收尸)
        // 放在最后，确保本帧内被 Destroy 的对象能活过 Update，在帧末统一消失
        sceneSystem.executeDestroyTask();
    }

    // ==========================================
    // 6. 管理 API
    // ==========================================

    /** 系统注册 (由 BaseSystem 构造函数自动调用) */
    public void registerSystem(BaseSystem system) {
        if (!systemMap.containsKey(system.getClass())) {
            systems.add(system);
            systemMap.put(system.getClass(), system);

            // 根据注解归类到不同的更新列表
            GameSystemInfo info = system.getSystemInfo();
            if (info != null) {
                if (info.type().has(GameSystemInfo.SystemType.UPDATE)) updateSystems.add(system);
                if (info.type().has(GameSystemInfo.SystemType.FIXED_UPDATE)) fixedUpdateSystems.add(system);
            } else {
                // 默认归为 Update
                updateSystems.add(system);
            }

            Debug.log("System Registered: " + system.getSystemName());
        }
    }

    /** 实体管理 (由 GObject 构造/setParent 自动调用) */
    public static void manageGObject(GObject gobject, ManageMode mode) {
        if (instance != null) {
            if (mode == ManageMode.ADD) {
                // 使用 CopyOnWriteArrayList 的 addIfAbsent (虽然这里手动写了 check)
                if (!instance.allEntities.contains(gobject)) {
                    instance.allEntities.add(gobject);
                }
            } else {
                instance.allEntities.remove(gobject);
            }
        }
    }

    // --- 销毁请求转发 ---
    public void addDestroyGObject(GObject gobject) {
        sceneSystem.addDestroyGObject(gobject);
    }

    public void addDestroyComponent(Component component) {
        sceneSystem.addDestroyComponent(component);
    }

    // --- Getters ---

    /** 获取所有顶层实体 (线程安全列表) */
    public List<GObject> getAllEntities() {
        return allEntities; 
    }

    public static float getTotalDeltaTime() {
        return totalDeltaTime;
    }

    /** 释放资源 */
    public void dispose() {
        allEntities.clear();
        systems.clear();
        systemMap.clear();
        ComponentManager.clearCache();
        instance = null;
    }
}
