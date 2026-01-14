package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector;

import com.goldsprite.gdengine.ecs.entity.GObject;

public interface IInspectorView {
	void setPresenter(InspectorPresenter presenter);

	/** 重建整个面板 (当选中物体改变或组件增删时) */
	void rebuild(GObject target);

	/** 仅刷新数值 (用于每帧更新，保持 UI 与数据同步) */
	void updateValues();

	/** 显示添加组件弹窗 */
	void showAddComponentDialog(GObject target);
}
