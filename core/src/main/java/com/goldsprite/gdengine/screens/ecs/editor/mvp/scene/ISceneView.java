package com.goldsprite.gdengine.screens.ecs.editor.mvp.scene;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.screens.ecs.editor.ViewTarget;

public interface ISceneView {
	void setPresenter(ScenePresenter presenter);

	/** 获取用于渲染的 FBO 目标 (供 Presenter 的 render 方法使用) */
	ViewTarget getRenderTarget();

	/** 屏幕坐标转世界坐标 (封装 ViewWidget 的映射逻辑) */
	Vector2 screenToWorld(float screenX, float screenY, OrthographicCamera camera);

	/** 设置 Gizmo 模式按钮的高亮状态 */
	void updateGizmoModeUI(int mode);

	boolean isMouseOver(); // [新增]

	// [新增] 控制存档功能的可用性 (运行时禁用)
	void setStorageEnabled(boolean enabled);
}
