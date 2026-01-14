package com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy;

import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;

public class HierarchyPresenter {
	private final IHierarchyView view;
	private final EditorSceneManager sceneManager; // 这里暂时还得依赖旧的核心逻辑，逐步替换

	public HierarchyPresenter(IHierarchyView view, EditorSceneManager sceneManager) {
		this.view = view;
		this.sceneManager = sceneManager;
		view.setPresenter(this);

		// 订阅事件
		EditorEvents.inst().subscribeStructure(v -> refresh());
		EditorEvents.inst().subscribeSceneLoaded(v -> refresh());
	}

	public void refresh() {
		// 获取根节点数据
		view.showNodes(GameWorld.inst().getRootEntities());
	}

	public void onNodeSelected(GObject obj) {
		sceneManager.select(obj);
		// 发送事件，Inspector 会听到
		EditorEvents.inst().emitSelectionChanged(obj);
	}

	// ... 拖拽、重排逻辑后续搬运到这里
}
