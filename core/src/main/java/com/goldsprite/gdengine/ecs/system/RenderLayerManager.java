package com.goldsprite.gdengine.ecs.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渲染层级管理器
 * 职责：管理全局渲染 Layer 的深度、配置。
 */
public class RenderLayerManager {
	private static final List<String> spriteLayers = new ArrayList<>();
	private static final Map<String, Integer> layerDepths = new ConcurrentHashMap<>();
	private static final Map<String, RenderLayerConfig> layerConfigs = new ConcurrentHashMap<>();
	private static boolean needsSort = false;

	static {
		createDefaultGameLayers();
	}

	public static void createDefaultGameLayers() {
		// Name, Depth, UseWorldSpace
		createLayer("Default", 0, true);
		createLayer("Background", 100, true);
		createLayer("Ground", 200, true);
		createLayer("Entity", 300, true);
		createLayer("Player", 301, true);
		createLayer("Effect", 700, true);

		// UI 层 (非 WorldSpace)
		createLayer("UI", 800, false);
		createLayer("Debug", 1100, true);
	}

	public static class RenderLayerConfig {
		public String name;
		public int depth;
		public boolean useWorldSpace;
		public boolean enabled = true;

		public RenderLayerConfig(String name, int depth, boolean useWorldSpace) {
			this.name = name;
			this.depth = depth;
			this.useWorldSpace = useWorldSpace;
		}
	}

	public static synchronized void createLayer(String layerName, int depth, boolean useWorldSpace) {
		if (!layerDepths.containsKey(layerName)) {
			spriteLayers.add(layerName);
			layerDepths.put(layerName, depth);
			layerConfigs.put(layerName, new RenderLayerConfig(layerName, depth, useWorldSpace));
			needsSort = true;
			sortLayersIfNeeded();
		}
	}

	public static synchronized void deleteLayer(String layerName) {
		spriteLayers.remove(layerName);
		layerDepths.remove(layerName);
		layerConfigs.remove(layerName);
	}

	public static List<String> getSortedLayers() {
		sortLayersIfNeeded();
		return new ArrayList<>(spriteLayers);
	}

	public static int getLayerDepth(String layerName) {
		return layerDepths.getOrDefault(layerName, 0);
	}

	public static boolean isLayerWorldSpace(String layerName) {
		RenderLayerConfig config = layerConfigs.get(layerName);
		return config != null && config.useWorldSpace;
	}

	public static RenderLayerConfig getLayerConfig(String layerName) {
		return layerConfigs.get(layerName);
	}

	private static synchronized void sortLayersIfNeeded() {
		if (needsSort) {
			spriteLayers.sort((a, b) -> {
				int depthA = layerDepths.getOrDefault(a, 0);
				int depthB = layerDepths.getOrDefault(b, 0);
				return Integer.compare(depthA, depthB);
			});
			needsSort = false;
		}
	}
}
