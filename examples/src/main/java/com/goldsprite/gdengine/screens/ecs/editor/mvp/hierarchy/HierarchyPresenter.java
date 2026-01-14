package com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy;

import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;

public class HierarchyPresenter {
	private final IHierarchyView view;
	private final EditorSceneManager sceneManager;

	public HierarchyPresenter(IHierarchyView view, EditorSceneManager sceneManager) {
		this.view = view;
		this.sceneManager = sceneManager;
		view.setPresenter(this);

		// 监听全局事件刷新 UI
		EditorEvents.inst().subscribeStructure(v -> refresh());

		// 初始刷新
		refresh();
	}

	public void refresh() {
		view.showNodes(GameWorld.inst().getRootEntities());
	}

	// --- 业务操作 ---

	public void selectObject(GObject obj) {
		sceneManager.select(obj);
		EditorEvents.inst().emitSelectionChanged(obj);
	}

	public void createObject(GObject parent) {
		GObject obj = new GObject("GameObject");
		if (parent != null) obj.setParent(parent);

		// 通知发生结构变化
		EditorEvents.inst().emitStructureChanged();
		selectObject(obj);
	}

	public void deleteObject(GObject obj) {
		if (obj != null) {
			obj.destroyImmediate();
			EditorEvents.inst().emitStructureChanged();
			// 如果删除了当前选中的，清空选中
			if (sceneManager.getSelection() == obj) {
				selectObject(null);
			}
		}
	}

	public void moveEntity(GObject target, GObject newParent, int index) {
		sceneManager.moveEntity(target, newParent, index);
		EditorEvents.inst().emitStructureChanged();
	}
}
