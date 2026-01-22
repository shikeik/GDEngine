package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector;

import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;

public class InspectorPresenter {
	private final IInspectorView view;
	private final EditorSceneManager sceneManager;
	private Object currentSelection; // [修改] 泛型化

	public InspectorPresenter(IInspectorView view, EditorSceneManager sceneManager) {
		this.view = view;
		this.sceneManager = sceneManager;
		view.setPresenter(this);

		EditorEvents.inst().subscribeSelection(this::onSelectionChanged);
		EditorEvents.inst().subscribeStructure(v -> refresh());
	}

	private void onSelectionChanged(Object selection) {
		this.currentSelection = selection;
		view.rebuild(selection);
	}

	public void refresh() {
		if (currentSelection != null) {
			view.rebuild(currentSelection);
		}
	}
}
