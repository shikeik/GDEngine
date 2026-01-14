package com.goldsprite.gdengine.screens.ecs.hub.mvp;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

/**
 * Hub 视图接口 (View)
 * 职责：定义 Hub 界面能做什么显示操作。
 * 实现者：HubViewImpl (具体的 UI 代码)
 */
public interface IHubView {

	/** 绑定指挥官 */
	void setPresenter(HubPresenter presenter);

	/**
	 * 显示项目列表
	 * @param projects 数据源
	 */
	void showProjects(Array<FileHandle> projects);

	/** 显示通用消息提示 (Toast) */
	void showToast(String msg);

	/** 显示错误弹窗/提示 */
	void showError(String msg);
}
