package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector;

import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;

public class InspectorPresenter {
	private final IInspectorView view;
	private final EditorSceneManager sceneManager;
	private GObject currentSelection;

	public InspectorPresenter(IInspectorView view, EditorSceneManager sceneManager) {
		this.view = view;
		this.sceneManager = sceneManager;
		view.setPresenter(this);

		// 监听选中事件
		EditorEvents.inst().subscribeSelection(this::onSelectionChanged);

		// 监听结构变化 (比如组件被删除了，或者名字改了)
		EditorEvents.inst().subscribeStructure(v -> refresh());
	}

	private void onSelectionChanged(GObject selection) {
		this.currentSelection = selection;
		view.rebuild(selection);
	}

	public void refresh() {
		if (currentSelection != null) {
			view.rebuild(currentSelection);
		}
	}

	// --- 业务操作 ---

	public void changeName(String newName) {
		if (currentSelection != null) {
			currentSelection.setName(newName);
			// 改名属于结构变化，通知 Hierarchy 刷新
			EditorEvents.inst().emitStructureChanged();
		}
	}

	public void changeTag(String newTag) {
		if (currentSelection != null) {
			currentSelection.setTag(newTag);
		}
	}

	public void removeComponent(Component c) {
		if (c != null) {
			c.destroyImmediate();
			// 组件删除也是结构变化
			EditorEvents.inst().emitStructureChanged();
			// 强制刷新当前面板
			refresh();
		}
	}

	public void onAddComponentClicked() {
		if (currentSelection != null) {
			view.showAddComponentDialog(currentSelection);
		}
	}

	public void onComponentAdded() {
		EditorEvents.inst().emitStructureChanged();
		refresh();
	}
}
