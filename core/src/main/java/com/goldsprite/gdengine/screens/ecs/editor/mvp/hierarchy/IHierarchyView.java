package com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy;

import com.goldsprite.gdengine.ecs.entity.GObject;
import java.util.List;

public interface IHierarchyView {
	void setPresenter(HierarchyPresenter presenter);

	/** 重建整个树结构 */
	void showNodes(List<GObject> roots);

	/** [新增] 选中指定物体对应的树节点 */
	void selectNode(GObject target);

	/** 获取拖拽管理器 (用于绑定 Scene View 的接收) */
	com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop getDragAndDrop();
}
