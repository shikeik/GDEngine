package com.goldsprite.solofight.refactor.ecs.system;

import com.goldsprite.solofight.core.Debug;
import com.goldsprite.solofight.refactor.ecs.GameSystemInfo;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.component.Component; // 注意引用新的 Component
import com.goldsprite.solofight.refactor.ecs.entity.GObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 场景系统 (对应 Unity 的 PlayerLoop 核心循环)
 * 职责：
 * 1. 驱动所有 GameObject 的 Update/FixedUpdate (脚本逻辑)。
 * 2. 帧末统一处理销毁 (垃圾回收)。
 */
@GameSystemInfo(type = GameSystemInfo.SystemType.BOTH) // 既跑 Update 也跑 FixedUpdate
public class SceneSystem extends BaseSystem {

    // 死亡名单 (缓存一帧内的销毁请求)
    private final List<GObject> destroyGObjects = new ArrayList<>();
    private final List<Component> destroyComponents = new ArrayList<>();

    // ==========================================
    // 1. 驱动逻辑 (Driver)
    // ==========================================

    @Override
    public void fixedUpdate(float fixedDelta) {
        // 获取所有顶层实体 (由 GameWorld 维护)
        // 注意：我们只驱动顶层，GObject 内部会递归驱动子物体
        List<GObject> roots = world.getAllEntities();

        // 倒序遍历？不，Update通常正序。
        // 为了防止 Update 过程中数组变动导致异常，通常建议用 CopyOnWriteArrayList (GameWorld里已用)
        // 或者简单的 fori 循环
        for (int i = 0; i < roots.size(); i++) {
            GObject obj = roots.get(i);
            // 只有激活且未销毁的物体才执行逻辑
            if (obj.isActive() && !obj.isDestroyed()) {
                obj.fixedUpdate(fixedDelta);
            }
        }
    }

    @Override
    public void update(float delta) {
        List<GObject> roots = world.getAllEntities();
        for (int i = 0; i < roots.size(); i++) {
            GObject obj = roots.get(i);
            if (obj.isActive() && !obj.isDestroyed()) {
                obj.update(delta);
            }
        }
    }

    // ==========================================
    // 2. 生命周期管理 (Lifecycle)
    // ==========================================

    public void awakeScene() {
        Debug.log("SceneSystem: Waking up all entities...");
        List<GObject> roots = world.getAllEntities();
        for (GObject obj : roots) {
            obj.awake();
        }
    }

    /**
     * 执行销毁任务 (在 GameWorld.update 的最后调用)
     */
    public void executeDestroyTask() {
        boolean hasDestroy = false;

        // 1. 销毁组件
        if (!destroyComponents.isEmpty()) {
            hasDestroy = true;
            // 倒序删除，安全
            for (int i = destroyComponents.size() - 1; i >= 0; i--) {
                Component comp = destroyComponents.get(i);
                // 二次检查：防止同一帧被重复销毁
                if (comp != null) {
                    comp.destroyImmediate();
                }
            }
            destroyComponents.clear();
        }

        // 2. 销毁物体
        if (!destroyGObjects.isEmpty()) {
            hasDestroy = true;
            for (int i = destroyGObjects.size() - 1; i >= 0; i--) {
                GObject obj = destroyGObjects.get(i);
                if (obj != null) {
                    // Debug.log("♻️ GC: " + obj.getName());
                    obj.destroyImmediate();
                }
            }
            destroyGObjects.clear();
        }

        // 如果有销毁发生，可能需要触发一次 GC (可选，通常交给 JVM)
    }

    // --- 接口：接收销毁请求 ---

    public void addDestroyGObject(GObject gobject) {
        if (!destroyGObjects.contains(gobject))
            destroyGObjects.add(gobject);
    }

    public void addDestroyComponent(Component component) {
        if (!destroyComponents.contains(component))
            destroyComponents.add(component);
    }
}
