package com.goldsprite.solofight.refactor.ecs.system;

import com.goldsprite.solofight.refactor.ecs.ComponentManager;
import com.goldsprite.solofight.refactor.ecs.GameSystemInfo;
import com.goldsprite.solofight.refactor.ecs.GameWorld;
import com.goldsprite.solofight.refactor.ecs.EcsObject;
import com.goldsprite.solofight.refactor.ecs.component.IComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import java.util.List;

/**
 * 系统基类
 * 自动根据 @GameSystemInfo 注解筛选感兴趣的实体
 */
public abstract class BaseSystem implements EcsObject {
	private IRunnableFields fields = new IRunnableFields();

	protected GameWorld world;
	private boolean isEnabled = true;
	// 系统关注的组件类型
	private Class<? extends IComponent>[] interestComponents;

	public BaseSystem() {
		initIRunnable();
		this.world = GameWorld.inst();

		// 1. 解析注解配置
		GameSystemInfo info = this.getClass().getAnnotation(GameSystemInfo.class);
		if (info != null) {
			this.interestComponents = info.interestComponents();
		}

		// 2. 自动注册到世界
		GameWorld.inst().registerSystem(this);
	}

	@Override
	public EcsObject.IRunnableFields getIRunnableFields() {
		return fields;
	}

	/**
	 * 获取当前系统关心的实体列表 (O(1) 高效查询)
	 */
	protected List<GObject> getInterestEntities() {
		if (interestComponents != null && interestComponents.length > 0) {
			return ComponentManager.getEntitiesWithComponents(interestComponents);
		}
		// 如果没定义感兴趣的组件，默认返回所有实体 (慎用，性能较低)
		return world.getAllEntities();
	}

	public void awake() {}

	@Override public void fixedUpdate(float fixedDelta){}
	@Override public void update(float delta){}

	public boolean isEnabled() { return isEnabled; }
	public void setEnabled(boolean enabled) { isEnabled = enabled; }

	public String getSystemName() {
		return this.getClass().getSimpleName();
	}

	public GameSystemInfo getSystemInfo() {
		return getClass().getAnnotation(GameSystemInfo.class);
	}
}
