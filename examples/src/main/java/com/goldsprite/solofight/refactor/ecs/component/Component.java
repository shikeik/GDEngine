package com.goldsprite.solofight.refactor.ecs.component;

import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.EcsObject;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;

public abstract class Component extends EcsObject {

    // ==========================================
    // 1. 核心引用区
    // ==========================================
    protected GObject gameObject;
    protected TransformComponent transform; // 极高频使用的快捷引用

    // ==========================================
    // 2. 状态标志位
    // ==========================================
    protected boolean isEnabled = true; // 逻辑开关 (对应 Unity .enabled)
    protected boolean isAwake = false;  // 防止重复唤醒
    protected boolean isStarted = false; // 防止重复 Start
    protected boolean isDestroyed = false;

    // ==========================================
    // 3. 构造阶段 (Phase 1)
    // ==========================================
    public Component() {
        super(); // 调用 EcsObject 构造，自动分配 GID
    }

    // ==========================================
    // 4. 苏醒阶段 (Phase 2: Awake)
    // ==========================================
    /** 
     * 引擎调用的入口：处理底层注册逻辑 
     * 千万不要重写这个方法，去重写 onAwake()
     */
    public final void awake() { 
        if (isAwake) return; // 只有第一次有效
        isAwake = true;

        // 自动将自己注册到 ComponentManager，这样查询系统就能找到它了
        if (gameObject != null) {
            ComponentManager.registerComponent(gameObject, this.getClass(), this);
            ComponentManager.updateEntityComponentMask(gameObject);
        }

        onAwake(); // -> 执行你的业务逻辑

        // 如果出生时就是开启状态，顺便触发一次 OnEnable
        if (isEnabled) onEnable(); 
    }

    /** 用户逻辑入口：获取自身组件 (GetComponent), 初始化变量 */
    protected void onAwake() {}

    // ==========================================
    // 5. 开始阶段 (Phase 3: Start)
    // ==========================================
    /** 引擎调用的入口 */
    public final void start() {
        if (isStarted) return;
        isStarted = true;
        onStart(); // -> 执行你的业务逻辑
    }

    /** 用户逻辑入口：获取跨物体引用 (FindObject), 复杂初始化 */
    protected void onStart() {}

    // ==========================================
    // 6. 状态回调 (Enable/Disable/Destroy)
    // ==========================================
    public void onEnable() {}  // 当组件启用时
    public void onDisable() {} // 当组件禁用时
    public void onDestroy() {} // 当组件销毁前

    // 编辑器/调试绘图接口
    public void onDrawGizmos() {}

    // ==========================================
    // 7. 销毁逻辑
    // ==========================================
    /** 软销毁：标记并在帧末移除 (推荐) */
    public final void destroy() {
        if (isDestroyed) return;
        isDestroyed = true;
        GameWorld.inst().addDestroyComponent(this); // 扔进垃圾桶，稍后倒掉
    }

    /** 硬销毁：立即切断所有联系 (系统内部使用) */
    public final void destroyImmediate() {
        if (gameObject != null) {
            // 临死前最后一口气
            if (isEnabled) onDisable();
            onDestroy();

            // 物理移除引用
            gameObject.removeComponent(this); 
            ComponentManager.unregisterComponent(gameObject, this.getClass(), this);

            gameObject = null;
            transform = null;
        }
    }

    // ==========================================
    // 8. 属性与快捷访问
    // ==========================================
    /** 仅供 GObject 添加组件时调用，自动绑定 Transform */
    public void setGObject(GObject gObject) {
        this.gameObject = gObject;
        this.transform = gObject.transform; // 模仿 Unity，自动缓存 transform 引用
    }

    public GObject getGObject() { return gameObject; }
    public TransformComponent getTransform() { return transform; }

    /** 开关控制 */
    public void setEnable(boolean enable) {
        if (this.isEnabled != enable) {
            this.isEnabled = enable;
            // 状态切换时触发回调
            if (enable) onEnable();
            else onDisable();
        }
    }
    public boolean isEnable() { return isEnabled; }
    public boolean isDestroyed() { return isDestroyed; }

    // --- 快捷查找 (代理给 GObject) ---

    /** 最常用：获取第一个匹配类型的组件 */
    public <T extends Component> T getComponent(Class<T> type) {
        if (gameObject == null) return null;
        return gameObject.getComponent(type);
    }

    /** 语义查找：获取指定名字的组件 (如 "HitBox") */
    public <T extends Component> T getComponent(Class<T> type, String name) {
        if (gameObject == null) return null;
        return gameObject.getComponent(type, name);
    }

    /** 索引查找：获取第 N 个匹配类型的组件 (如 第2个武器) */
    public <T extends Component> T getComponent(Class<T> type, int index) {
        if (gameObject == null) return null;
        return gameObject.getComponent(type, index);
    }

    /** 批量查找：获取所有匹配类型的组件 (如 所有渲染器) */
    public <T extends Component> java.util.List<T> getComponents(Class<T> type) {
        if (gameObject == null) return null;
        return gameObject.getComponents(type);
    }

    // ==========================================
    // 9. 调试信息
    // ==========================================
    @Override
    public String toString() {
        String objName = (gameObject != null) ? gameObject.getName() : "Null";
        // 格式: 101#Player.Rigidbody
        return String.format("%d#%s.%s", getGid(), objName, getName());
    }
}
