package com.goldsprite.gdengine.ecs.entity;

import com.goldsprite.gdengine.core.annotations.ExecuteInEditMode;
import com.goldsprite.gdengine.ecs.ComponentManager;
import com.goldsprite.gdengine.ecs.EcsObject;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.TransformComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap; // [修正1] 使用 LinkedHashMap
import java.util.List;
import java.util.Map;

/**
 * 游戏实体 (对应 Unity.GameObject)
 */
public class GObject extends EcsObject {

	// ==========================================
	// 属性
	// ==========================================
	private boolean isActive = true;
	private boolean isDestroyed = false;
	// [新增] 切换场景时不销毁 (DontDestroyOnLoad)
	private boolean dontDestroyOnLoad = false;

	private String tag = "Untagged";
	private int layer = 0;

	public final TransformComponent transform;

	// [修正1] 使用 LinkedHashMap 保证遍历顺序与添加顺序一致
	private final Map<Class<?>, List<Component>> components = new LinkedHashMap<>();

	private GObject parent;
	private final List<GObject> children = new ArrayList<>();

	// ==========================================
	// 构造
	// ==========================================
	public GObject(String name) {
		super(name);

		// 核心组件初始化
		this.transform = new TransformComponent();
		// 手动注入，不走 addComponent 避免递归死循环 (如果 addComponent 里有特殊逻辑的话)
		addComponentInternal(this.transform);

		// 注册到世界
		GameWorld.registerGObject(this);
	}

	public GObject() {
		this("GObject");
	}

	// ==========================================
	// 组件管理
	// ==========================================

	public <T extends Component> T addComponent(Class<T> clazz) {
		try {
			// [修改后] 获取构造器 -> 强行赋予权限 -> 实例化
			java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true); // <--- 关键！告诉 JVM "别管权限，让我用"
			T comp = constructor.newInstance();

			return addComponent(comp);
		} catch (Exception e) {
			throw new RuntimeException("AddComponent Failed: " + clazz.getSimpleName(), e);
		}
	}

	public <T extends Component> T addComponent(T component) {
		if (component.getGObject() == this) return component;
		return addComponentInternal(component);
	}

	private <T extends Component> T addComponentInternal(T component) {
		Class<?> type = component.getClass();

		// 1. 存入容器
		List<Component> list = components.computeIfAbsent(type, k -> new ArrayList<>());
		list.add(component);

		// 2. 绑定
		component.setGObject(this);

		// 3. 唤醒 (Phase 2: Awake)
		// [说明] Awake 是“自身初始化”。
		// Start (Phase 3) 将在 Component.update() 首次运行时惰性触发 (Lazy Start)
		// 或者由 System 在下一帧统一触发。目前我们在 Component 类里没写 Lazy Start，
		// 暂时假设 System 会处理，或者组件只用 Awake。
		component.awake();

		return component;
	}

	public void removeComponent(Component component) {
		if (component == null) return;
		Class<?> type = component.getClass();

		List<Component> list = components.get(type);
		if (list != null) {
			list.remove(component);
			if (list.isEmpty()) components.remove(type);
		}

		// [修正8] 移除 = 解绑。
		// 只有调用 destroy() 才会触发 onDestroy 回调。
		// 这里只是单纯地把组件从物体上剥离。
		ComponentManager.unregisterComponent(this, component.getClass(), component);
	}

	public boolean hasComponent(Class type) {
		return components.containsKey(type);
	}

	public boolean hasComponent(Component component) {
		if (!hasComponent(component.getClass())) return false;
		return components.get(component.getClass()).contains(component);
	}

	// ==========================================
	// 查找 (保持不变，省略部分代码以节省篇幅)
	// ==========================================
	// ... getComponent(Class), getComponents(Class) 等逻辑 ...
	// 这里完全沿用之前的实现，逻辑是正确的。

	public <T extends Component> T getComponent(Class<T> type) {
		List<Component> list = components.get(type);
		if (list != null && !list.isEmpty()) return (T) list.get(0);

		for (List<Component> comps : components.values()) {
			if (!comps.isEmpty() && type.isAssignableFrom(comps.get(0).getClass())) {
				return (T) comps.get(0);
			}
		}
		return null;
	}

	public <T extends Component> T getComponent(Class<T> type, String name) {
		for (List<Component> comps : components.values()) {
			for (Component c : comps) {
				if (type.isAssignableFrom(c.getClass()) && c.getName().equals(name)) {
					return (T) c;
				}
			}
		}
		return null;
	}

	public <T extends Component> T getComponent(Class<T> type, int index) {
		List<T> all = getComponents(type);
		if (all != null && index >= 0 && index < all.size()) return all.get(index);
		return null;
	}

	public <T extends Component> List<T> getComponents(Class<T> type) {
		List<T> result = new ArrayList<>();
		for (List<Component> comps : components.values()) {
			if (!comps.isEmpty() && type.isAssignableFrom(comps.get(0).getClass())) {
				for (Component c : comps) result.add((T) c);
			}
		}
		return result;
	}

	// ==========================================
	// 层级管理 (保持不变)
	// ==========================================
	public void setParent(GObject newParent) {
		if (this.parent == newParent) return;

		// 1. 从旧父级移除
		if (this.parent != null) {
			this.parent.children.remove(this);
		} else {
			// 之前是顶层 -> 现在要变成子级 -> 从世界顶层移除
			// 旧: GameWorld.manageGObject(this, ManageMode.REMOVE);
			// 新:
			GameWorld.unregisterGObject(this);
		}

		this.parent = newParent;

		// 2. 加入新父级
		if (newParent != null) {
			newParent.children.add(this);
		} else {
			// 现在没有父级 -> 变成顶层 -> 注册到世界
			// 旧: GameWorld.manageGObject(this, ManageMode.ADD);
			// 新:
			GameWorld.registerGObject(this);
		}
	}

	public void addChild(GObject child) { if (child != null) child.setParent(this); }
	public void removeChild(GObject child) { if (child != null && child.parent == this) child.setParent(null); }
	public GObject getParent() { return parent; }
	public List<GObject> getChildren() { return children; }

	// ==========================================
	// 循环
	// ==========================================

	@Override
	public void update(float delta) {
		if (!isActive || isDestroyed) return;

		// [新增] 1. 计算这一帧的变换矩阵
		// 获取父级的 Transform 组件
		TransformComponent parentTrans = (parent != null) ? parent.transform : null;
		// 执行矩阵乘法
		this.transform.updateWorldTransform(parentTrans);

		// [新增] 获取当前模式
		boolean isPlayMode = GameWorld.inst().isPlayMode();

		// 2. 更新组件逻辑 (逻辑可能会修改 transform.local，下一帧生效)
		for (List<Component> list : components.values()) {
			for (Component c : list) {
				if (c.isEnable() && !c.isDestroyed()) {
					// [核心修改] 生命周期过滤
					// 如果是 PLAY 模式 -> 执行
					// 如果是 EDIT 模式 -> 只有带 @ExecuteInEditMode 的才执行
					// TransformComponent 不需要注解，因为它只是数据，且上面已经手动 update 了矩阵
					// (注意：TransformComponent 的 update 方法是空的，所以调了也没事，但为了严谨...)

					if (isPlayMode || shouldRunInEditor(c)) {
						c.update(delta);
					}
				}
			}
		}

		// 3. 递归子物体 (子物体会拿到我刚刚算好的 transform 作为 parentTrans)
		for (int i = 0; i < children.size(); i++) {
			children.get(i).update(delta);
		}
	}

	// 辅助判断
	private boolean shouldRunInEditor(Component c) {
		// 缓存类的注解检查结果会更快，这里暂用反射
		return c.getClass().isAnnotationPresent(ExecuteInEditMode.class);
	}

	public void awake() {
		for (List<Component> list : components.values()) {
			for (Component c : list) c.awake();
		}
		for (GObject child : children) child.awake();
	}

	// ==========================================
	// 销毁 (修正10: 流程清晰化)
	// ==========================================

	public void destroy() {
		if (isDestroyed) return;
		isDestroyed = true;
		// 1. 标记自己
		GameWorld.inst().addDestroyGObject(this);
		// 2. 标记所有组件 (可选，为了让组件能触发 OnDisable 等)
		// 但通常 SceneSystem 的 destroyImmediate 会统一处理
	}

	public void destroyImmediate() {
		// [新增] 即使没调过 destroy() (比如被父级递归销毁)，这里也要强制标记死亡状态
		if (!isDestroyed) isDestroyed = true;

		// 1. 先杀孩子 (倒序)
		for (int i = children.size() - 1; i >= 0; i--) {
			children.get(i).destroyImmediate();
		}
		children.clear();

		// 2. 杀组件 (倒序安全)
		// 需要把 Map 铺平成 List 倒序删，避免 Map 遍历时 remove
		List<Component> allComps = new ArrayList<>();
		for (List<Component> list : components.values()) allComps.addAll(list);

		for (int i = allComps.size() - 1; i >= 0; i--) {
			allComps.get(i).destroyImmediate(); // 这里面会调 removeComponent
		}
		// components.clear(); // removeComponent 里会删，这里不需要了

		// 3. 处理父级关系
		if (parent != null) {
			parent.children.remove(this);
		} else {
			// 顶层物体销毁，从世界注销
			// 旧: GameWorld.manageGObject(this, ManageMode.REMOVE);
			// 新:
			GameWorld.unregisterGObject(this);
		}

		parent = null;
	}

	// ==========================================
	// 状态控制 (修正11: SetActive 联动)
	// ==========================================

	public void setActive(boolean active) {
		if (this.isActive == active) return;
		this.isActive = active;

		// 触发组件的 OnEnable / OnDisable
		for (List<Component> list : components.values()) {
			for (Component c : list) {
				// 只有当组件本身开关是开着的时候，物体的开关才会影响它
				if (c.isEnable()) {
					if (active) c.onEnable();
					else c.onDisable();
				}
			}
		}

		// 递归子物体？
		// Unity 逻辑：子物体的 activeSelf 不变，但 activeInHierarchy 会变。
		// 我们简化处理：子物体如果 activeSelf 是 true，那就也会收到 Update 调用。
		// Update 循环里有 check isActive，所以不需要递归改 active 变量。
		// 但需要递归触发 OnEnable/Disable 吗？Unity 会。
		for (GObject child : children) {
			if (child.isActive()) { // 只有本来就是开着的子物体，才会被这一级影响
				child.setActiveRecursivelyEffect(active);
			}
		}
	}

	// 内部辅助：仅触发回调，不改 activeSelf 值
	private void setActiveRecursivelyEffect(boolean active) {
		for (List<Component> list : components.values()) {
			for (Component c : list) {
				if (c.isEnable()) {
					if (active) c.onEnable();
					else c.onDisable();
				}
			}
		}
		for (GObject child : children) {
			if (child.isActive()) child.setActiveRecursivelyEffect(active);
		}
	}

	// [新增] DDOL 接口
	public void setDontDestroyOnLoad(boolean value) {
		this.dontDestroyOnLoad = value;
	}

	public boolean isDontDestroyOnLoad() {
		return dontDestroyOnLoad;
	}

	// Getters
	public boolean isActive() { return isActive; }
	public boolean isDestroyed() { return isDestroyed; }
	public String getTag() { return tag; }
	public void setTag(String tag) { this.tag = tag; }
	public int getLayer() { return layer; }
	public void setLayer(int layer) { this.layer = layer; }

	public Map<Class<?>, List<Component>> getComponentsMap() { return components; }
}
