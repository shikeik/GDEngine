package com.goldsprite.solofight.refactor.ecs.entity;

import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.GameWorld; // 稍后提供
import com.goldsprite.solofight.refactor.ecs.IRunnable;
import com.goldsprite.solofight.refactor.ecs.component.Component;
import com.goldsprite.solofight.refactor.ecs.component.IComponent;
import com.goldsprite.solofight.refactor.ecs.component.TransformComponent;
import com.goldsprite.solofight.refactor.ecs.enums.ManageMode; // 稍后提供

import java.lang.reflect.Field;
import java.util.*;

/**
 * 游戏实体 (Entity)
 * 混合模式：既是 ECS 的 ID 容器，又保留了组件引用的 Map
 */
public class GObject implements IRunnable {
	private IRunnableFields fields = new IRunnableFields();
	private String tag = "";
	private boolean isDestroyed;
	private boolean isEnabled = true;

	// 核心组件直接引用
	public TransformComponent transform = null;
	private final transient Field transformField;

	// 组件容器
	private final Map<String, List<IComponent>> components = new LinkedHashMap<>();
	private final List<GObject> childGObjects = new ArrayList<>();
	private GObject parent;

	@Override
	public IRunnable.IRunnableFields getIRunnableFields() {
		return fields;
	}

	public GObject(String name) {
		initIRunnable();
		setName(name);
		try {
			transformField = getClass().getField("transform");
			transformField.setAccessible(true);

			// 自动加入世界管理
			GameWorld.manageGObject(this, ManageMode.ADD);

			// 默认挂载 Transform
			setTransform();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public GObject() {
		this("GObject");
	}

	// [修复] 补充 getComponents 方法供 ComponentManager 使用
	public Map<String, List<IComponent>> getComponents() {
		return components;
	}

	public GObject setTransform() {
		if (transform == null) {
			addComponent(new TransformComponent());
		}
		return this;
	}

	public <T extends IComponent> T addComponent(Class<T> clazz) {
		try {
			T comp = clazz.getConstructor().newInstance();
			return addComponent(comp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Add Component Failed: " + clazz.getSimpleName());
		}
	}

	public <T extends IComponent> T addComponent(T component) {
		Class clazz = component.getClass();
		if (TransformComponent.class.isAssignableFrom(clazz) && components.containsKey(clazz.getName())) {
			// Transform 是单例
			return (T) components.get(clazz.getName()).get(0);
		}

		List<IComponent> list = components.computeIfAbsent(clazz.getName(), k -> new ArrayList<>());

		if (!list.contains(component)) {
			// 注入 transform 字段引用
			if (component instanceof TransformComponent) {
				try {
					transformField.set(this, component);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			list.add(component);
			component.setGObject(this);

			// 立即唤醒组件 (注册到 ComponentManager)
			component.awake();
		}
		return component;
	}

	public void removeComponent(IComponent component) {
		List<IComponent> list = components.get(component.getClass().getName());
		if (list != null) {
			list.remove(component);
		}
		ComponentManager.unregisterComponent(this, component.getClass(), component);
		ComponentManager.updateEntityComponentMask(this);
	}

	public <T extends IComponent> T getComponent(Class<T> type) {
		return getComponent(type, 0);
	}

	public <T extends IComponent> T getComponent(Class<T> type, int index) {
		if (type == null) return null;
		List<IComponent> list = components.get(type.getName());
		if (list != null && index < list.size()) {
			return (T) list.get(index);
		}
		// 查找子类
		for (List<IComponent> comps : components.values()) {
			if (!comps.isEmpty()) {
				if (type.isAssignableFrom(comps.get(0).getClass())) {
					if (index < comps.size()) return (T) comps.get(index);
				}
			}
		}
		return null;
	}

	public void awake() {
		DebugUI.log("GObject Awake: %s", getName());
		for(List<IComponent> compList : components.values()) {
			for(IComponent comp : compList) {
				comp.awake();
			}
		}
	}

	@Override
	public void fixedUpdate(float fixedDelta) {
		// 实体本身不需要 Update 逻辑，逻辑应该在 System 中
		// 但为了兼容旧模式，这里保留对 Component 的调用
		// 实际上有了 System，这里可以留空，或者只作为 fallback
	}

	@Override
	public void update(float delta) {
		if (isEnabled && !isDestroyed) {
			for (List<IComponent> list : components.values()) {
				for (IComponent component : list) {
					component.update(delta);
				}
			}
			for(GObject child : childGObjects) {
				child.update(delta);
			}
		}
	}

	public void destroy() {
		if (isDestroyed) return;
		isDestroyed = true;
		GameWorld.inst().addDestroyGObject(this);
	}

	public void destroyImmediate() {
		for (List<IComponent> list : components.values()) {
			for (int i = list.size() - 1; i >= 0; i--) {
				list.get(i).destroyImmediate();
			}
		}
		components.clear();
		ComponentManager.removeEntity(this);
		GameWorld.manageGObject(this, ManageMode.REMOVE);
	}

	public boolean isDestroyed() { return isDestroyed; }
	public boolean isEnable() { return isEnabled; }
	public void setEnable(boolean isEnable) { this.isEnabled = isEnable; }
	public String getTag() { return tag; }
	public void setTag(String tag) { this.tag = tag; }

	public void addChild(GObject child) {
		if(!childGObjects.contains(child)) {
			childGObjects.add(child);
			child.parent = this;
			GameWorld.manageGObject(child, ManageMode.REMOVE); // 从顶层移除，由父级管理
		}
	}
	public void removeChild(GObject child) {
		if(childGObjects.remove(child)) {
			child.parent = null;
			GameWorld.manageGObject(child, ManageMode.ADD); // 回到顶层
		}
	}
}
