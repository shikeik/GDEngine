package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector;

public interface IInspectorView {
	void setPresenter(InspectorPresenter presenter);
	void rebuild(Object target); // GObject -> Object
	void updateValues();
	// void showAddComponentDialog(GObject target); // Deleted
}
