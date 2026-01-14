package com.goldsprite.gdengine.screens.ecs.editor.mvp;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.ecs.entity.GObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 编辑器事件总线
 * 极简实现，用于模块间解耦。
 */
public class EditorEvents {
	private static EditorEvents instance;
	public static EditorEvents inst() {
		if (instance == null) instance = new EditorEvents();
		return instance;
	}

	// --- 事件定义 ---

	// 选中物体变更
	private final List<Consumer<GObject>> onSelectionChanged = new ArrayList<>();
	// 场景结构变更 (添加/删除/重排物体)
	private final List<Consumer<Void>> onStructureChanged = new ArrayList<>();
	// 属性变更 (Inspector 修改了数值，Scene 需要刷新)
	private final List<Consumer<Void>> onPropertyChanged = new ArrayList<>();
	// 场景加载完毕
	private final List<Consumer<Void>> onSceneLoaded = new ArrayList<>();

	// --- 订阅接口 ---

	public void subscribeSelection(Consumer<GObject> listener) { onSelectionChanged.add(listener); }
	public void subscribeStructure(Consumer<Void> listener) { onStructureChanged.add(listener); }
	public void subscribeProperty(Consumer<Void> listener) { onPropertyChanged.add(listener); }
	public void subscribeSceneLoaded(Consumer<Void> listener) { onSceneLoaded.add(listener); }

	// --- 发布接口 ---

	public void emitSelectionChanged(GObject selection) {
		for (var l : onSelectionChanged) l.accept(selection);
	}

	public void emitStructureChanged() {
		for (var l : onStructureChanged) l.accept(null);
	}

	public void emitPropertyChanged() {
		for (var l : onPropertyChanged) l.accept(null);
	}

	public void emitSceneLoaded() {
		for (var l : onSceneLoaded) l.accept(null);
	}

	public void clear() {
		onSelectionChanged.clear();
		onStructureChanged.clear();
		onPropertyChanged.clear();
		onSceneLoaded.clear();
	}
}
