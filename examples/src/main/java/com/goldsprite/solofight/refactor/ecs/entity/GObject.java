package com.goldsprite.solofight.refactor.ecs.entity;

import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.EcsObject;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.component.Component;
import com.goldsprite.solofight.refactor.ecs.component.TransformComponent;
import com.goldsprite.solofight.refactor.ecs.enums.ManageMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 游戏实体 (对应 Unity.GameObject)
 * 职责：
 * 1. 维护组件列表 (支持同类多个)
 * 2. 维护父子层级关系
 * 3. 转发生命周期 (Awake, Update, Destroy)
 */
public class GObject extends EcsObject {

    // ==========================================
    // 1. 核心属性
    // ==========================================
    private boolean isActive = true;      // 对应 .activeSelf
    private boolean isDestroyed = false;
    private String tag = "Untagged";      // 对应 .tag
    private int layer = 0;                // 对应 .layer (物理/逻辑层)

    // 核心组件：每个实体必有 Transform
    public final TransformComponent transform;

    // ==========================================
    // 2. 容器
    // ==========================================
    // 组件容器: Map<类对象, 组件列表>。使用 List 允许同一种组件挂多个。
    private final Map<Class<?>, List<Component>> components = new LinkedHashMap<>();

    // 层级容器
    private GObject parent;
    private final List<GObject> children = new ArrayList<>();

    // ==========================================
    // 3. 构造与初始化
    // ==========================================
    public GObject(String name) {
        super(); // 分配 GID
        setName(name);

        // 1. 立即创建并绑定 Transform
        // 注意：这里手动 new 并赋值，确保 addComponent 内部能引用到它
        this.transform = new TransformComponent();
        // 2. 将 Transform 作为普通组件加入管理
        // 特殊处理：先赋值 field，再 add，防止 add 内部回调时 transform 为空
        addComponentInternal(this.transform);

        // 3. 注册到世界 (默认加入 Update 列表)
        GameWorld.manageGObject(this, ManageMode.ADD);
    }

    public GObject() {
        this("GObject");
    }

    // ==========================================
    // 4. 组件管理 (CRUD)
    // ==========================================

    /** 添加组件 (通过类) */
    public <T extends Component> T addComponent(Class<T> clazz) {
        try {
            T comp = clazz.getDeclaredConstructor().newInstance();
            return addComponent(comp);
        } catch (Exception e) {
            throw new RuntimeException("AddComponent Failed: " + clazz.getSimpleName(), e);
        }
    }

    /** 添加组件 (通过实例) */
    public <T extends Component> T addComponent(T component) {
        // 防止重复添加同一个实例
        if (component.getGObject() == this) return component;
        return addComponentInternal(component);
    }

    private <T extends Component> T addComponentInternal(T component) {
        Class<?> type = component.getClass();

        // 1. 放入 Map
        List<Component> list = components.computeIfAbsent(type, k -> new ArrayList<>());
        list.add(component);

        // 2. 绑定关系 (这一步会自动让组件获取 transform 引用)
        component.setGObject(this);

        // 3. 触发生命周期
        component.awake();
        // 如果游戏已经运行了一段时间，新加的组件可能需要补调 Start (暂时不加，按我们约定的 Awake 模式)

        return component;
    }

    /** 移除组件 */
    public void removeComponent(Component component) {
        if (component == null) return;
        Class<?> type = component.getClass();

        // 1. 从 Map 移除
        List<Component> list = components.get(type);
        if (list != null) {
            list.remove(component);
            if (list.isEmpty()) components.remove(type);
        }

        // 2. 管理器注销 (ComponentManager 缓存脏标记)
        // 注意：这里不负责调 destroy，那是 Component 自己的事，这里只负责解绑关系
        ComponentManager.unregisterComponent(this, component.getClass(), component);
    }

    // ==========================================
    // 5. 组件查找 (核心算法)
    // ==========================================

    /** 查找逻辑：优先精准匹配，其次遍历子类 */
    public <T extends Component> T getComponent(Class<T> type) {
        // 1. 精准查表 (O(1))
        List<Component> list = components.get(type);
        if (list != null && !list.isEmpty()) {
            return (T) list.get(0);
        }

        // 2. 接口/父类查找 (O(N)) - 模拟 Unity 行为
        // 比如 getComponent(Collider.class) 应该返回 BoxCollider
        for (List<Component> comps : components.values()) {
            if (!comps.isEmpty()) {
                Component c = comps.get(0);
                if (type.isAssignableFrom(c.getClass())) {
                    return (T) c;
                }
            }
        }
        return null;
    }

    /** 按名字查找 (O(N)) */
    public <T extends Component> T getComponent(Class<T> type, String name) {
        // 遍历所有组件，找到类型匹配且名字匹配的
        for (List<Component> comps : components.values()) {
            for (Component c : comps) {
                if (type.isAssignableFrom(c.getClass()) && c.getName().equals(name)) {
                    return (T) c;
                }
            }
        }
        return null;
    }

    /** 按索引查找 */
    public <T extends Component> T getComponent(Class<T> type, int index) {
        List<T> all = getComponents(type);
        if (all != null && index >= 0 && index < all.size()) {
            return all.get(index);
        }
        return null;
    }

    /** 获取所有同类组件 */
    public <T extends Component> List<T> getComponents(Class<T> type) {
        List<T> result = new ArrayList<>();
        // 同样需要支持继承查找
        for (List<Component> comps : components.values()) {
            if (!comps.isEmpty()) {
                // 检查列表里的第一个元素即可判断类型
                if (type.isAssignableFrom(comps.get(0).getClass())) {
                    for (Component c : comps) {
                        result.add((T) c);
                    }
                }
            }
        }
        return result;
    }

    // ==========================================
    // 6. 层级管理 (Parent-Child)
    // ==========================================

    public void setParent(GObject newParent) {
        if (this.parent == newParent) return;

        // 1. 从旧父级移除
        if (this.parent != null) {
            this.parent.children.remove(this);
        } else {
            // 旧父级是 null，说明之前是世界顶层物体，现在要认爹了
            // 从世界顶层列表中移除 (由父级驱动 Update)
            GameWorld.manageGObject(this, ManageMode.REMOVE);
        }

        this.parent = newParent;

        // 2. 加入新父级
        if (newParent != null) {
            newParent.children.add(this);
        } else {
            // 新父级是 null，说明变成了孤儿（顶层物体）
            // 重新加入世界顶层列表
            GameWorld.manageGObject(this, ManageMode.ADD);
        }
    }

    public void addChild(GObject child) {
        if (child != null) child.setParent(this);
    }

    public GObject getParent() { return parent; }
    public List<GObject> getChildren() { return children; }

    // ==========================================
    // 7. 生命周期循环
    // ==========================================

    @Override
    public void update(float delta) {
        if (!isActive || isDestroyed) return;

        // 1. 更新自己的组件
        // 遍历 Map.values() 时需要小心并发修改异常 (如果 Update 里 Add/Remove 组件)
        // 简单起见，这里假设 Update 不会频繁增删组件结构，或者使用 CopyOnWrite
        // 为了性能，暂直接遍历
        for (List<Component> list : components.values()) {
            for (int i = 0; i < list.size(); i++) {
                Component c = list.get(i);
                if (c.isEnable() && !c.isDestroyed()) {
                    c.update(delta);
                }
            }
        }

        // 2. 递归更新子物体
        for (int i = 0; i < children.size(); i++) {
            children.get(i).update(delta);
        }
    }

    public void awake() {
        // 唤醒所有组件
        for (List<Component> list : components.values()) {
            for (Component c : list) c.awake();
        }
        // 递归唤醒子物体
        for (GObject child : children) child.awake();
    }

    // ==========================================
    // 8. 销毁逻辑
    // ==========================================

    public void destroy() {
        if (isDestroyed) return;
        isDestroyed = true;
        GameWorld.inst().addDestroyGObject(this); // 软销毁
    }

    /** 级联硬销毁 */
    public void destroyImmediate() {
        // 1. 先杀孩子 (倒序)
        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).destroyImmediate();
        }
        children.clear();

        // 2. 再杀组件
        for (List<Component> list : components.values()) {
            for (Component c : list) {
                // 组件硬销毁会调用 onDestroy 并解绑
                // 但这里我们手写逻辑避免 ConcurrentModificationException
                c.onDisable();
                c.onDestroy();
                ComponentManager.unregisterComponent(this, c.getClass(), c);
            }
        }
        components.clear();

        // 3. 处理父级关系
        if (parent != null) {
            parent.children.remove(this);
        } else {
            GameWorld.manageGObject(this, ManageMode.REMOVE);
        }

        parent = null;
        // transform = null; // final 字段无法置空，但对象已无引用
    }

    // ==========================================
    // 9. Getters / Setters
    // ==========================================
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { 
        this.isActive = active;
        // Unity 中 SetActive 会触发组件的 OnEnable/OnDisable
        for (List<Component> list : components.values()) {
            for (Component c : list) {
                if (c.isEnable()) { // 只有组件本身 Enable 时，物体开关才有效
                    if (active) c.onEnable();
                    else c.onDisable();
                }
            }
        }
    }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public int getLayer() { return layer; }
    public void setLayer(int layer) { this.layer = layer; }

    // [补充] 提供给 ComponentManager 使用的后门
    public Map<Class<?>, List<Component>> getComponentsMap() {
        return components;
    }
}
