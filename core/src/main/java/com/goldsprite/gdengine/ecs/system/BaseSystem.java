package com.goldsprite.gdengine.ecs.system;

import com.goldsprite.gdengine.ecs.ComponentManager;
import com.goldsprite.gdengine.ecs.EcsObject; // [变更]
import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component; // [变更]
import com.goldsprite.gdengine.ecs.entity.GObject;
import java.util.List;

/**
 * 系统基类
 */
public abstract class BaseSystem extends EcsObject {

	protected GameWorld world;
	private boolean isEnabled = true;

	// 系统关注的组件类型
	private Class<? extends Component>[] interestComponents;

	public BaseSystem() {
		super(); // 分配 ID
		this.world = GameWorld.inst();

		// 解析注解
		GameSystemInfo info = this.getClass().getAnnotation(GameSystemInfo.class);
		if (info != null) {
			this.interestComponents = info.interestComponents();
		}

		// 自动注册到世界 (构造即生效)
		GameWorld.inst().registerSystem(this);
	}

	/**
	 * 获取当前系统关心的实体列表 (O(1) 高效查询)
	 * 利用 ComponentManager 的缓存
	 */
	protected List<GObject> getInterestEntities() {
		if (interestComponents != null && interestComponents.length > 0) {
			return ComponentManager.getEntitiesWithComponents(interestComponents);
		}
		// 如果没定义感兴趣的组件，默认返回所有(顶层)实体
		// 注意：这可能不是你想要的，通常建议 System 明确声明 interest
		return world.getRootEntities();
	}

	public void awake() {}

	// 默认空实现，子类按需覆盖
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
