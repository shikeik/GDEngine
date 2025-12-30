package com.goldsprite.solofight.refactor.ecs.system;

import com.goldsprite.solofight.core.Debug;
import com.goldsprite.solofight.refactor.ecs.GameSystemInfo;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.component.IComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import com.goldsprite.solofight.refactor.ecs.enums.ManageMode;

import java.util.ArrayList;
import java.util.List;

/**
 * 场景系统：负责实体的通用 Update 和 销毁任务
 * 它不关心具体组件，而是维护所有 GObject 的生命周期
 */
@GameSystemInfo(type = GameSystemInfo.SystemType.BOTH)
public class SceneSystem extends BaseSystem {

	private final List<GObject> destroyGObjects = new ArrayList<>();
	private final List<IComponent> destroyComponents = new ArrayList<>();

	public void awakeScene() {
		// 唤醒所有已有实体
		for(GObject object : world.getAllEntities()) {
			object.awake();
		}
	}

	@Override
	public void fixedUpdate(float fixedDelta){
		// 传统的 GObject.fixedUpdate 调用（如果不想用 System 纯 ECS，可保留此逻辑）
		List<GObject> gobjects = world.getAllEntities();
		for (int i = gobjects.size() - 1; i >= 0; i--) {
			GObject obj = gobjects.get(i);
			if (!obj.isDestroyed() && obj.isEnable()) {
				obj.fixedUpdate(fixedDelta);
			}
		}
	}

	@Override
	public void update(float delta) {
		// 传统的 GObject.update 调用
		List<GObject> gobjects = world.getAllEntities();
		for (int i = gobjects.size() - 1; i >= 0; i--) {
			GObject obj = gobjects.get(i);
			if (!obj.isDestroyed() && obj.isEnable()) {
				obj.update(delta);
			}
		}
	}

	public void executeDestroyTask() {
		// 1. 销毁实体
		if (!destroyGObjects.isEmpty()) {
			for (int i = destroyGObjects.size() - 1; i >= 0; i--) {
				GObject obj = destroyGObjects.get(i);
				if (obj != null) {
					Debug.log("Destroy GObject: " + obj.getName());
					obj.destroyImmediate();
					GameWorld.manageGObject(obj, ManageMode.REMOVE);
				}
			}
			destroyGObjects.clear();
		}

		// 2. 销毁组件
		if (!destroyComponents.isEmpty()) {
			for (int i = destroyComponents.size() - 1; i >= 0; i--) {
				IComponent comp = destroyComponents.get(i);
				if (comp != null) {
					comp.destroyImmediate();
				}
			}
			destroyComponents.clear();
		}
	}

	public void addDestroyGObject(GObject gobject) {
		if (!destroyGObjects.contains(gobject))
			destroyGObjects.add(gobject);
	}

	public void addDestroyComponent(IComponent component) {
		if (!destroyComponents.contains(component))
			destroyComponents.add(component);
	}
}

