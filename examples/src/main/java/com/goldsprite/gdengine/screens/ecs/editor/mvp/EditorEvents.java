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

	// --- 订阅接口 ---
	public void subscribeSelection(Consumer<GObject> listener) { onSelectionChanged.add(listener); }
	public void subscribeStructure(Consumer<Void> listener) { onStructureChanged.add(listener); }

	// --- 发布接口 ---
	public void emitSelectionChanged(GObject selection) {
		for (var l : onSelectionChanged) l.accept(selection);
	}

	/** 当物体被添加、删除、重排父子关系时调用 */
	public void emitStructureChanged() {
		for (var l : onStructureChanged) l.accept(null);
	}

	public void clear() {
		onSelectionChanged.clear();
		onStructureChanged.clear();
	}
}
