package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers;

import com.kotcrab.vis.ui.widget.VisTable;

/**
 * 顶级检查器绘制接口
 * 负责绘制整个选中的对象 (GObject, File, etc.)
 */
public interface IInspectorDrawer<T> {
	/** 检查是否支持该对象 */
	boolean accept(Object target);

	/**
	 * 在容器中绘制 UI
	 * @param target 选中的对象
	 * @param container UI 容器
	 */
	void draw(T target, VisTable container);
}
