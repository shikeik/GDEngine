package com.goldsprite.gdengine.ecs;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ECS 系统中所有对象的基类 (对应 Unity.Object)
 * 职责：
 * 1. 提供全局唯一的 ID (GID)
 * 2. 提供基础的 name 管理
 * 3. 统一 Update/FixedUpdate 接口定义
 */
public abstract class EcsObject {

	// 全局 ID 种子 (线程安全)
	private static final AtomicInteger GID_SEED = new AtomicInteger(0);

	// 核心数据
	private final int gid;
	protected String name;

	public EcsObject() {
		// 构造即分配 ID，永不重复
		this.gid = GID_SEED.getAndAdd(1);
		// 默认名字为类名 (例如 "TransformComponent")
		this.name = getClass().getSimpleName();
	}
	public EcsObject(String name) {
		this();
		setName(name);
	}

	// --- 核心属性 ---

	public final int getGid() {
		return gid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// --- 生命周期/循环接口 (默认为空，子类按需重写) ---

	public void update(float delta) {
		// Optional override
	}

	public void fixedUpdate(float fixedDelta) {
		// Optional override
	}

	// --- 基础 toString (子类可覆盖) ---
	@Override
	public String toString() {
		// 格式: GID#Name (例如: 101#TransformComponent)
		return String.format("%d#%s", gid, name);
	}
}
