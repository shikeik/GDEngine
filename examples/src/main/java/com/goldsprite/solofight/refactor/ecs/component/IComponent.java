package com.goldsprite.solofight.refactor.ecs.component;

import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.EcsObject;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;

public interface IComponent extends EcsObject {
	ComponentField getCompFields();

	@Override
	default EcsObject.IRunnableFields getIRunnableFields() {
		return getCompFields();
	}

	default boolean isShowGizmos() { return getCompFields().showGizmos; }

	// 预留给 NeonBatch 的 Gizmos 绘制接口
	default void drawGizmos() {}

	default void initIComponent() {
		initIRunnable();
	}

	default boolean isAwake() { return getCompFields().awaked; }

	default boolean isEnable() { return getCompFields().enabled; }
	default void setEnable(boolean isEnable) { getCompFields().enabled = isEnable; }

	default boolean isDestroyed() { return getCompFields().destroyed; }

	default String getTag() { return getCompFields().tag; }
	default void setTag(String tag) { getCompFields().tag = tag; }

	default GObject getGObject() { return getCompFields().gobject; }
	default void setGObject(GObject gObject) {
		getCompFields().gobject = gObject;
		getCompFields().transform = gObject.transform;
	}

	default TransformComponent getTransform() { return getCompFields().transform; }

	default void awake(){
		if(isAwake()) return;
		getCompFields().awaked = true;

		if (getGObject() != null) {
			ComponentManager.registerComponent(getGObject(), this.getClass(), this);
			ComponentManager.updateEntityComponentMask(getGObject());
		}
	}

	@Override default void fixedUpdate(float fixedDelta){}
	@Override default void update(float delta) {}

	// --- 快捷访问 ---
	default <T extends IComponent> T getComponent(Class<T> type) {
		return getGObject().getComponent(type);
	}

	default void destroy() {
		if (isDestroyed()) return;
		getCompFields().destroyed = true;
		// GameWorld 稍后实现
		GameWorld.inst().addDestroyComponent(this);
	}

	default void destroyImmediate() {
		if (getGObject() != null) {
			getGObject().removeComponent(this);
			ComponentManager.unregisterComponent(getGObject(), this.getClass(), this);
		}
	}

	public static class ComponentField extends IRunnableFields {
		public GObject gobject;
		public TransformComponent transform;
		protected boolean awaked = false;
		protected boolean enabled = true;
		protected boolean destroyed = false;
		protected boolean showGizmos = true;
		protected String tag;
	}
}
