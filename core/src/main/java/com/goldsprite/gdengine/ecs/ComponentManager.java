package com.goldsprite.gdengine.ecs;

import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 组件管理器 (ECS 核心索引模块)
 * <p>
 * 职责：
 * 1. <b>类型映射</b>: 为每种 Component 类分配唯一的整数 ID (0, 1, 2...)。
 * 2. <b>档案管理</b>: 维护每个实体拥有的组件列表 (BitSet 掩码)。
 * 3. <b>查询加速</b>: 缓存 System 查询的结果，提供 O(1) 的查询性能。
 * </p>
 */
public class ComponentManager {

	// ==========================================
	// 1. 类型 ID 分配系统
	// ==========================================

	/** 全局组件类型计数器 */
	private static int nextComponentId = 0;

	/**
	 * 组件类 -> 唯一整数ID 的映射缓存
	 * 例如: TransformComponent.class -> 0, Collider.class -> 1
	 */
	private static final Map<Class<? extends Component>, Integer> componentIds = new ConcurrentHashMap<>();

	// ==========================================
	// 2. 数据存储 (Database)
	// ==========================================

	/**
	 * 组件池: Class -> [所有该类型的组件实例]
	 * 作用: 方便进行全量类型查询 (例如获取世界上所有的 Collider)
	 */
	private static final Map<Class<? extends Component>, List<Component>> componentPools = new ConcurrentHashMap<>();

	/**
	 * 实体档案表: GObject -> ComponentMask (BitSet)
	 * 作用: 记录每个实体拥有哪些组件。
	 * Key: 实体对象
	 * Value: 位掩码 (如 {0, 2, 5} 表示拥有 ID为0,2,5 的组件)
	 */
	private static final Map<GObject, BitSet> entityComponentMasks = new ConcurrentHashMap<>();

	// ==========================================
	// 3. 查询缓存 (Cache)
	// ==========================================

	/**
	 * 查询缓存: QueryMask -> EntityList
	 * <p>
	 * Key: 查询条件的掩码 (例如 "需要组件 0 和 1")
	 * Value: 符合该条件的所有实体列表
	 * </p>
	 * 优化策略: 使用<b>增量更新 (Incremental Update)</b>。
	 * 当实体组件变动时，不清除整个缓存，而是遍历所有 Key，检查该实体是否应加入或移除。
	 */
	private static final Map<BitSet, List<GObject>> entityCache = new ConcurrentHashMap<>();

	// ----------------------------------------------------------------

	/**
	 * 获取组件类型的唯一 ID (线程安全懒加载)
	 * @param componentType 组件类对象
	 * @return 0 到 N 的整数 ID
	 */
	public static int getComponentId(Class<? extends Component> componentType) {
		return componentIds.computeIfAbsent(componentType, k -> nextComponentId++);
	}

	/**
	 * 注册组件到管理器
	 * <p>通常在 {@link Component#awake()} 时调用。</p>
	 *
	 * @param entity 组件所属的实体
	 * @param componentType 组件的类型 (通常是 getClass())
	 * @param component 组件实例
	 */
	public static void registerComponent(GObject entity, Class<? extends Component> componentType, Component component) {
		// 1. 加入全量组件池 (方便 getComponents(Type) 查询)
		// 使用 CopyOnWriteArrayList 保证迭代安全，虽写慢读快，但注册操作相对低频
		List<Component> pool = componentPools.computeIfAbsent(componentType, k -> new CopyOnWriteArrayList<>());
		if (!pool.contains(component)) {
			pool.add(component);
		}

		// 2. 更新该实体的“档案”(Mask)
		// 如果是新实体，创建一个新的 BitSet
		BitSet mask = entityComponentMasks.computeIfAbsent(entity, k -> new BitSet());
		// 在对应 ID 的位置打勾
		mask.set(getComponentId(componentType));

		// 3. [核心优化] 增量更新缓存
		// 告诉缓存系统：这个人的档案变了，请检查他是否符合各个查询条件
		updateCacheForEntity(entity, mask);
	}

	/**
	 * 从管理器注销组件
	 * <p>通常在 {@link Component#destroyImmediate()} 时调用。</p>
	 */
	public static void unregisterComponent(GObject entity, Class<? extends Component> componentType, Component component) {
		// 1. 从全量组件池移除
		List<Component> pool = componentPools.get(componentType);
		if (pool != null) {
			pool.remove(component);
		}

		// 2. 更新实体档案
		BitSet mask = entityComponentMasks.get(entity);
		if (mask != null) {
			// 清除对应位的勾
			mask.clear(getComponentId(componentType));

			// 如果实体身上一个组件都没了，从管理表中移除该实体
			if (mask.isEmpty()) {
				entityComponentMasks.remove(entity);
			}

			// 3. [核心优化] 增量更新缓存
			updateCacheForEntity(entity, mask);
		} else {
			// 防御性编程：如果档案都没了，说明实体可能已经彻底移除了
			// 确保它不在任何缓存列表里
			removeFromAllCaches(entity);
		}
	}

	/**
	 * [核心优化算法] 仅更新特定实体的缓存归属
	 * <p>
	 * 复杂度: O(M)，其中 M 为<b>当前活跃的查询条件数量</b> (即 System 的数量)。
	 * 相比全量重建缓存 O(N * M) (N为实体总数)，性能有数量级提升。
	 * </p>
	 *
	 * @param entity 发生变动的实体
	 * @param entityMask 实体当前最新的组件掩码
	 */
	private static void updateCacheForEntity(GObject entity, BitSet entityMask) {
		// 遍历缓存中的每一个查询条件 (Query Mask)
		// 例如：SystemA 查 {0,1}, SystemB 查 {2}
		for (Map.Entry<BitSet, List<GObject>> entry : entityCache.entrySet()) {
			BitSet queryMask = entry.getKey();
			List<GObject> resultList = entry.getValue();

			// 判断：实体的当前配置(entityMask) 是否满足 查询条件(queryMask)
			// 逻辑: (Entity 和 Query) == Query
			boolean isMatch = containsAllBits(entityMask, queryMask);

			if (isMatch) {
				// 情况A: 满足条件，且列表里没有它 -> 加进去
				// (CopyOnWriteArrayList.contains 是 O(N)，但结果列表通常是缓存的，读多写少)
				if (!resultList.contains(entity)) {
					resultList.add(entity);
				}
			} else {
				// 情况B: 不满足条件，但列表里有它 -> 踢出去
				// 比如以前有Collider现在移除了，就不该在 PhysicsSystem 的列表里了
				resultList.remove(entity);
			}
		}
	}

	/**
	 * [辅助] 当实体彻底销毁时，从所有缓存列表里暴力移除它
	 */
	private static void removeFromAllCaches(GObject entity) {
		for (List<GObject> list : entityCache.values()) {
			list.remove(entity);
		}
	}

	/**
	 * <b>核心查询方法</b>：获取拥有特定组件集合的所有实体
	 * <p>System 主要调用此方法来筛选它关心的实体。</p>
	 *
	 * @param componentTypes 需要包含的组件类型列表
	 * @return 符合条件的实体列表 (只读引用或副本)
	 */
	@SafeVarargs
	public static List<GObject> getEntitiesWithComponents(Class<? extends Component>... componentTypes) {
		// 如果没传参数，返回所有有组件的实体
		if (componentTypes.length == 0) {
			return new ArrayList<>(entityComponentMasks.keySet());
		}

		// 1. 构建查询掩码 (我要找有 0号 和 5号 组件的人 -> Mask: ...00100001)
		BitSet queryMask = createComponentMask(componentTypes);

		// 2. 查缓存 (O(1))
		// 如果这个查询条件之前有人查过，直接返回结果
		if (entityCache.containsKey(queryMask)) {
			return entityCache.get(queryMask);
		}

		// 3. 缓存未命中 (这是一次新的查询类型)
		// 执行全量扫描 (O(N))，并将结果存入缓存
		List<GObject> result = new CopyOnWriteArrayList<>(); // 使用线程安全 List

		for (Map.Entry<GObject, BitSet> entry : entityComponentMasks.entrySet()) {
			// 检查包含关系
			if (containsAllBits(entry.getValue(), queryMask)) {
				result.add(entry.getKey());
			}
		}

		// 4. 存入缓存，下次就快了
		entityCache.put(queryMask, result);
		return result;
	}

	// --- 内部辅助工具 ---

	private static BitSet createComponentMask(Class<? extends Component>... componentTypes) {
		BitSet mask = new BitSet();
		for (Class<? extends Component> type : componentTypes) {
			mask.set(getComponentId(type));
		}
		return mask;
	}

	/**
	 * 位运算判断：source 是否包含 target 的所有位
	 * <br>逻辑: (source AND target) == target
	 */
	private static boolean containsAllBits(BitSet source, BitSet target) {
		if (source == null || target == null) return false;
		// 必须 clone，因为 BitSet.and 会修改自身
		BitSet temp = (BitSet) target.clone();
		temp.and(source); // 取交集
		return temp.equals(target); // 如果交集等于目标，说明全包含
	}

	/**
	 * 强制刷新实体掩码 (当 addComponent 批量操作或初始化时调用)
	 * 会重新扫描实体身上的所有组件并更新缓存
	 */
	public static void updateEntityComponentMask(GObject entity) {
		BitSet mask = new BitSet();

		// 遍历实体所有组件
		for (List<Component> components : entity.getComponentsMap().values()) {
			for (Component comp : components) {
				// [修复核心] 支持多态：不仅注册当前类，还注册所有父类组件
				Class<?> clazz = comp.getClass();
				while (clazz != null && Component.class.isAssignableFrom(clazz)) {
					// 只要是 Component 的子类（包括 RenderComponent），都打上标记
					mask.set(getComponentId((Class<? extends Component>) clazz));
					clazz = clazz.getSuperclass();
				}
			}
		}

		if (!mask.isEmpty()) {
			entityComponentMasks.put(entity, mask);
			// [优化] 增量更新缓存
			updateCacheForEntity(entity, mask);
		} else {
			entityComponentMasks.remove(entity);
			removeFromAllCaches(entity);
		}
	}

	/**
	 * 彻底移除实体 (当 GObject.destroyImmediate 时调用)
	 */
	public static void removeEntity(GObject entity) {
		// 1. 清理组件池中的引用 (O(C) C=组件数)
		// 虽然组件 destroy 时会自己调 unregister，但这里作为最后一道保险
		for (List<Component> components : entity.getComponentsMap().values()) {
			for (Component comp : components) {
				List<Component> pool = componentPools.get(comp.getClass());
				if (pool != null) pool.remove(comp);
			}
		}

		// 2. 移除档案
		entityComponentMasks.remove(entity);

		// 3. 从所有缓存列表中剔除
		removeFromAllCaches(entity);
	}

	/**
	 * [修复] 彻底重置管理器状态 (用于测试环境或游戏完全重启)
	 * 清除所有 ID 映射、实体档案、组件池和查询缓存。
	 */
	public static void dispose() {
		componentIds.clear();
		componentPools.clear();
		entityComponentMasks.clear();
		entityCache.clear();
		nextComponentId = 0; // ID 计数器归零，保证测试确定性
	}

	public static void debugInfo() {
		Debug.log("=== ComponentManager Debug ===");
		Debug.log("Registered Types: %d", componentIds.size());
		Debug.log("Tracked Entities: %d", entityComponentMasks.size());
		Debug.log("Cached Queries: %d", entityCache.size());
	}
}
