package com.goldsprite.solofight.refactor.ecs;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一运行接口，支持 Update/FixedUpdate 和全局唯一 ID 生成
 */
public interface IRunnable {
	void update(float delta);
	void fixedUpdate(float fixedDelta);

	// 全局运行实例 ID 种子
	public static AtomicInteger runnableGidSeed = new AtomicInteger(0);

	IRunnableFields getIRunnableFields();

	default void initIRunnable() {
		int newId = runnableGidSeed.getAndAdd(1);
		// DebugUI.log("IRunnable: 新对象 %s, Gid: %s", getName(), newId);
		initRunnableGid(newId);
	}

	default void initRunnableGid(int gid) {
		if(getIRunnableFields().runnableGid != -1) return;
		getIRunnableFields().runnableGid = gid;
	}

	default int getRunnableGid() {
		return getIRunnableFields().runnableGid;
	}

	default <T> T setName(String name) {
		getIRunnableFields().name = name;
		return (T)this;
	}

	default String getName() {
		if(getIRunnableFields().name == null || getIRunnableFields().name.isBlank())
			getIRunnableFields().name = getClass().getSimpleName();
		return getIRunnableFields().name;
	}

	public static class IRunnableFields {
		public String name;
		protected int runnableGid = -1;
	}
}
