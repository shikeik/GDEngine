package com.goldsprite.gdengine.screens.ecs.editor.mvp;

import com.goldsprite.gdengine.ecs.entity.GObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 编辑器全局事件总线
 * 职责：解耦各个面板，Hierarchy 选中物体不需要直接调用 Inspector 的刷新方法，而是发送事件。
 */
public class EditorEvents {
	private static EditorEvents instance;
	public static EditorEvents inst() {
		if (instance == null) instance = new EditorEvents();
		return instance;
	}

	// --- 事件定义 ---
	private final List<Consumer<GObject>> onSelectionChanged = new ArrayList<>();
	private final List<Consumer<Void>> onStructureChanged = new ArrayList<>();
	// [新增] 属性变更 (Inspector 修改了数值，Scene 需要刷新)
	private final List<Consumer<Void>> onPropertyChanged = new ArrayList<>();
	// [新增] 场景加载完毕
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

	/** 当物体被添加、删除、重排父子关系时调用 */
	public void emitStructureChanged() {
		for (var l : onStructureChanged) l.accept(null);
	}

	/** 当组件属性被修改时调用 (比如拖拽 Gizmo) */
	public void emitPropertyChanged() {
		for (var l : onPropertyChanged) l.accept(null);
	}

	/** 当场景文件加载完毕时调用 */
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
