package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.Camera;
import com.goldsprite.gdengine.ecs.ComponentManager;
import com.goldsprite.gdengine.ecs.EcsObject;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public abstract class Component extends EcsObject {

	// ==========================================
	// 1. 核心引用区 (不序列化)
	// ==========================================
	protected transient GObject gobject;
	protected transient TransformComponent transform;

	// ==========================================
	// 2. 状态标志位
	// ==========================================

	// [保留] 启用状态应该被保存 (比如你在编辑器里关掉了一个组件)
	protected boolean isEnabled = true;
	protected boolean isDestroyed = false;

	// [修复] 核心生命周期标记必须是 transient (不保存)
	// 否则加载回来 isAwake=true 会导致 awake() 方法直接 return，跳过初始化
	protected transient boolean isAwake = false;
	protected transient boolean isStarted = false;

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
	 * 当组件被 addComponent 添加到物体时立即调用
	 * 千万不要重写这个方法，去重写 onAwake()
	 */
	public final void awake() {
		if (isAwake) return;
		isAwake = true;

		if (gobject != null) {
			ComponentManager.registerComponent(gobject, this.getClass(), this);
			ComponentManager.updateEntityComponentMask(gobject);
		}

		onAwake();

		// 主动向 SceneSystem 报名参加 Start 仪式
		// 只有没 Start 过且启用的组件才需要 Start
		if (!isStarted) {
			// 防御性判空：反序列化过程中 GameWorld 可能还没准备好，或者这是一个单纯的数据对象
			if (GameWorld.inst() != null && GameWorld.inst().sceneSystem != null) {
				GameWorld.inst().sceneSystem.registerStart(this);
			}
		}

		if (isEnabled) onEnable();
	}

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
	
	// 渲染钩子
	public void onRender(NeonBatch batch, Camera camera) {}

	// ==========================================
	// 7. 销毁逻辑
	// ==========================================
	/** 软销毁：标记并在帧末移除 (推荐) */
	public final void destroy() {
		if (isDestroyed) return;
		isDestroyed = true;
		if (GameWorld.inst() != null) {
			GameWorld.inst().addDestroyComponent(this);
		}
	}

	/** 硬销毁：立即切断所有联系 (系统内部使用) */
	public final void destroyImmediate() {
		if (gobject != null) {
			// 临死前最后一口气
			if (isEnabled) onDisable();
			onDestroy();

			// 物理移除引用
			gobject.removeComponent(this);
			ComponentManager.unregisterComponent(gobject, this.getClass(), this);

			gobject = null;
			transform = null;
		}
	}

	// ==========================================
	// 8. 属性与快捷访问
	// ==========================================
	/** 仅供 GObject 添加组件时调用，自动绑定 Transform */
	public void setGObject(GObject gObject) {
		this.gobject = gObject;
		if (gObject != null) {
			this.transform = gObject.transform;
		}
	}

	public GObject getGObject() { return gobject; }
	public TransformComponent getTransform() { return transform; }

	/** 开关控制 */
	public void setEnable(boolean enable) {
		if (isEnabled != enable) {
			isEnabled = enable;
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
		if (gobject == null) return null;
		return gobject.getComponent(type);
	}

	/** 语义查找：获取指定名字的组件 (如 "HitBox") */
	public <T extends Component> T getComponent(Class<T> type, String name) {
		if (gobject == null) return null;
		return gobject.getComponent(type, name);
	}

	/** 索引查找：获取第 N 个匹配类型的组件 (如 第2个武器) */
	public <T extends Component> T getComponent(Class<T> type, int index) {
		if (gobject == null) return null;
		return gobject.getComponent(type, index);
	}

	/** 批量查找：获取所有匹配类型的组件 (如 所有渲染器) */
	public <T extends Component> java.util.List<T> getComponents(Class<T> type) {
		if (gobject == null) return null;
		return gobject.getComponents(type);
	}

	// ==========================================
	// 9. 调试信息
	// ==========================================
	@Override
	public String toString() {
		String objName = (gobject != null) ? gobject.getName() : "Null";
		// 格式: 101#Player.Rigidbody
		return String.format("%d#%s.%s", getGid(), objName, getName());
	}
}
