package com.goldsprite.gdengine.screens.ecs.hub.mvp;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameScreen;

/**
 * Hub 逻辑控制器 (Presenter)
 * 职责：处理业务逻辑，响应用户操作，指挥 View 更新。
 */
public class HubPresenter {

	private final IHubView view;
	private final ProjectService service;

	public HubPresenter(IHubView view) {
		this.view = view;
		this.service = ProjectService.inst();

		// 告诉 View：我是你的指挥官
		view.setPresenter(this);
	}

	/**
	 * 启动逻辑 (通常在 Screen.show 调用)
	 */
	public void start() {
		refreshProjectList();
	}

	/**
	 * 业务：刷新列表
	 */
	public void refreshProjectList() {
		Array<FileHandle> projects = service.listProjects();
		view.showProjects(projects);
	}

	// ==========================
	// 响应 View 的用户操作
	// ==========================

	public void onProjectOpenRequest(FileHandle project) {
		if (project == null || !project.exists()) {
			view.showError("Project path invalid!");
			return;
		}

		Debug.logT("Hub", "Opening project: %s", project.path());

		// 1. 设置当前项目状态
		service.setCurrentProject(project);

		// 2. 跳转场景 (Screen 跳转逻辑暂时还在这里，未来可以用 Router 解耦)
		ScreenManager.getInstance().turnNewScreen(EditorGameScreen.class);
	}

	public void onProjectDeleteRequest(FileHandle project) {
		service.deleteProject(project);
		view.showToast("Project Deleted: " + project.name());
		refreshProjectList(); // 删完刷新界面
	}

	// [新增] 处理升级请求
	public void onProjectUpgradeRequest(FileHandle project) {
		service.upgradeProject(project);
		view.showToast("Project Upgraded to " + com.goldsprite.gdengine.BuildConfig.DEV_VERSION);
		refreshProjectList(); // 刷新列表以显示新版本号
	}

	public void onProjectCreateRequest(TemplateInfo tmpl, String name, String pkg) {
		String error = service.createProject(tmpl, name, pkg);
		if (error == null) {
			view.showToast("Created: " + name);
			refreshProjectList();
		} else {
			view.showError(error);
		}
	}
}
