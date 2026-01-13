package com.goldsprite.gdengine.ecs.system;

import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.SystemType;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import java.util.ArrayList;
import java.util.List;

/**
 * 场景系统 (对应 Unity 的核心驱动层)
 * 职责：
 * 1. <b>Start 管理</b>: 收集并执行组件的 Start 方法。
 * 2. <b>Update 驱动</b>: 遍历所有 GObject 并调用其 Update。
 * 3. <b>Destroy 管理</b>: 收集并执行销毁任务。
 */
@GameSystemInfo(type = SystemType.BOTH_UPDATE)
public class SceneSystem extends BaseSystem {

	// 死亡名单
	private final List<GObject> destroyGObjects = new ArrayList<>();
	private final List<Component> destroyComponents = new ArrayList<>();

	// 待 Start 名单
	private final List<Component> pendingStarts = new ArrayList<>();

	// ==========================================
	// 1. Start 逻辑 (Unity Style)
	// ==========================================

	/** 注册需要执行 Start 的组件 (由 Component.awake 调用) */
	public void registerStart(Component component) {
		// 简单去重，防止同一个组件 Awake 多次导致多次 Start
		if (!pendingStarts.contains(component)) {
			pendingStarts.add(component);
		}
	}

	/** 统一执行所有注册组件的 Start 方法 */
	public void executeStartTask() {
		if (pendingStarts.isEmpty()) return;

		// 拷贝副本，因为 Start() 内部可能会 new 新物体，导致 pendingStarts 变动
		// 避免 ConcurrentModificationException
		List<Component> temp = new ArrayList<>(pendingStarts);
		pendingStarts.clear();

		for (Component c : temp) {
			// 防御：组件可能在等待 Start 的过程中被销毁了，或者物体失活了
			if (!c.isDestroyed() && c.getGObject() != null && c.getGObject().isActive()) {
				c.start();
			}
		}
	}

	// ==========================================
	// 2. Update 驱动 (遍历顶层实体)
	// ==========================================

	@Override
	public void fixedUpdate(float fixedDelta) {
		// 获取所有顶层实体 (子物体由父级递归驱动)
		List<GObject> roots = world.getRootEntities();
		for (int i = 0; i < roots.size(); i++) {
			GObject obj = roots.get(i);
			// 只有激活且未销毁的物体才执行
			if (obj.isActive() && !obj.isDestroyed()) {
				obj.fixedUpdate(fixedDelta);
			}
		}
	}

	@Override
	public void update(float delta) {
		List<GObject> roots = world.getRootEntities();
		for (int i = 0; i < roots.size(); i++) {
			GObject obj = roots.get(i);
			if (obj.isActive() && !obj.isDestroyed()) {
				obj.update(delta);
			}
		}
	}

	// ==========================================
	// 3. 销毁管理 (收尸)
	// ==========================================

	/** 执行所有销毁请求 */
	public void executeDestroyTask() {
		// 1. 销毁组件
		if (!destroyComponents.isEmpty()) {
			// 倒序遍历，防止索引问题
			for (int i = destroyComponents.size() - 1; i >= 0; i--) {
				Component comp = destroyComponents.get(i);
				if (comp != null) comp.destroyImmediate();
			}
			destroyComponents.clear();
		}

		// 2. 销毁物体
		if (!destroyGObjects.isEmpty()) {
			for (int i = destroyGObjects.size() - 1; i >= 0; i--) {
				GObject obj = destroyGObjects.get(i);
				if (obj != null) obj.destroyImmediate();
			}
			destroyGObjects.clear();
		}
	}

	// --- 接收请求 ---

	public void addDestroyGObject(GObject gobject) {
		if (!destroyGObjects.contains(gobject))
			destroyGObjects.add(gobject);
	}

	public void addDestroyComponent(Component component) {
		if (!destroyComponents.contains(component))
			destroyComponents.add(component);
	}
}
