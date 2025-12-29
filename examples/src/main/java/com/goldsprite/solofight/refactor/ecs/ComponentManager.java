package com.goldsprite.solofight.refactor.ecs;

import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.refactor.ecs.component.IComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 组件管理器 (核心)
 * 职责：
 * 1. 维护组件类型 ID (BitSet Index)
 * 2. 维护实体的组件掩码 (Component Mask)
 * 3. 缓存查询结果，加速 System 的实体筛选 (O(1)读取)
 */
public class ComponentManager {
	private static int nextComponentId = 0;
	// 组件类型 -> ID 映射
	private static final Map<Class<? extends IComponent>, Integer> componentIds = new ConcurrentHashMap<>();
	// 组件类型 -> 所有实例池
	private static final Map<Class<? extends IComponent>, List<IComponent>> componentPools = new ConcurrentHashMap<>();
	// 查询掩码 -> 实体列表缓存 (System 查询加速)
	private static final Map<BitSet, List<GObject>> entityCache = new ConcurrentHashMap<>();
	// 实体 -> 该实体拥有的组件掩码
	private static final Map<GObject, BitSet> entityComponentMasks = new ConcurrentHashMap<>();

	/**
	 * 获取或生成组件类型的唯一 ID
	 */
	public static <T extends IComponent> int getComponentId(Class<T> componentType) {
		// 检查是否已注册父类或接口，如果有继承关系处理逻辑可在此扩展
		// 这里简化处理，直接为每个具体类分配 ID
		return componentIds.computeIfAbsent(componentType, k -> nextComponentId++);
	}
	
	public static <T extends IComponent> int preRegisterComponentType(Class<T> componentType) {
		return getComponentId(componentType);
	}

	/**
	 * 注册组件 (当 Component.awake 时调用)
	 */
	public static <T extends IComponent> void registerComponent(GObject entity, Class<T> componentType, IComponent component) {
		// 1. 加入组件池
		List<IComponent> pool = componentPools.computeIfAbsent(componentType, k -> new CopyOnWriteArrayList<>());
		if (!pool.contains(component)) {
			pool.add(component);
		}

		// 2. 更新实体的掩码
		BitSet mask = entityComponentMasks.computeIfAbsent(entity, k -> new BitSet());
		mask.set(getComponentId(componentType));

		// 3. 标记缓存脏 (这是最激进的策略，保证正确性)
		// 优化思路：只清除受影响的 queryMask，但目前规模 clear 全部没问题
		entityCache.clear();
	}

	public static <T extends IComponent> void unregisterComponent(GObject entity, Class<T> componentType, IComponent component) {
		// 1. 从组件池移除
		List<IComponent> pool = componentPools.get(componentType);
		if (pool != null) {
			pool.remove(component);
		}

		// 2. 更新掩码
		BitSet mask = entityComponentMasks.get(entity);
		if (mask != null) {
			mask.clear(getComponentId(componentType));
			if (mask.isEmpty()) {
				entityComponentMasks.remove(entity);
			}
		}

		// 3. 清除缓存
		entityCache.clear();
	}

	/**
	 * System 调用的核心方法：获取拥有特定组件的所有实体
	 */
	@SafeVarargs
	public static List<GObject> getEntitiesWithComponents(Class<? extends IComponent>... componentTypes) {
		if (componentTypes.length == 0) {
			return new ArrayList<>(entityComponentMasks.keySet());
		}

		// 构建查询掩码
		BitSet queryMask = createComponentMask(componentTypes);

		// 命中缓存直接返回 (O(1))
		if (entityCache.containsKey(queryMask)) {
			return new ArrayList<>(entityCache.get(queryMask));
		}

		// 缓存未命中，执行筛选 (O(N))
		List<GObject> result = new ArrayList<>();
		for (Map.Entry<GObject, BitSet> entry : entityComponentMasks.entrySet()) {
			BitSet entityMask = entry.getValue();
			// 检查实体是否包含所有请求的组件位
			if (containsAllBits(entityMask, queryMask)) {
				result.add(entry.getKey());
			}
		}

		// 写入缓存
		entityCache.put(queryMask, new CopyOnWriteArrayList<>(result));
		return result;
	}

	private static BitSet createComponentMask(Class<? extends IComponent>... componentTypes) {
		BitSet mask = new BitSet();
		for (Class<? extends IComponent> type : componentTypes) {
			mask.set(getComponentId(type));
		}
		return mask;
	}

	// 检查 source 是否包含 target 的所有位
	private static boolean containsAllBits(BitSet source, BitSet target) {
		// 优化：BitSet.intersects 只能查交集，我们需要包含关系
		// 逻辑： (source & target) == target
		BitSet temp = (BitSet) target.clone();
		temp.and(source);
		return temp.equals(target);
	}

	public static void updateEntityComponentMask(GObject entity) {
		BitSet mask = new BitSet();
		for (List<IComponent> components : entity.getComponents().values()) {
			for (IComponent comp : components) {
				mask.set(getComponentId(comp.getClass()));
			}
		}

		if (!mask.isEmpty()) {
			entityComponentMasks.put(entity, mask);
		} else {
			entityComponentMasks.remove(entity);
		}
		entityCache.clear();
	}

	public static void removeEntity(GObject entity) {
		// 从池中清理
		for (List<IComponent> components : entity.getComponents().values()) {
			for (IComponent comp : components) {
				for (List<IComponent> pool : componentPools.values()) {
					pool.remove(comp);
				}
			}
		}
		entityComponentMasks.remove(entity);
		entityCache.clear();
	}

	// [修复] 补充 clearCache 方法
	public static void clearCache() {
		entityCache.clear();
	}
	
	public static int getRegisteredComponentCount() {
        return componentPools.size();
    }

	public static void debugInfo() {
		DebugUI.log("=== ComponentManager Debug ===");
		DebugUI.log("Entities: %d, Cached Queries: %d", entityComponentMasks.size(), entityCache.size());
	}
}
