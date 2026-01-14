package com.goldsprite.gdengine.screens.ecs.editor.mvp.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.ViewTarget;
import com.goldsprite.gdengine.screens.ecs.editor.ViewWidget;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class ScenePanel extends EditorPanel implements ISceneView {

	private ScenePresenter presenter;
	private ViewWidget sceneWidget;
	private ViewTarget renderTarget;

	// UI Buttons
	private VisTextButton btnMove, btnRotate, btnScale;

	// [新增] 引用 Save/Load 按钮
	private VisTextButton btnSave, btnLoad;

	public ScenePanel() {
		super("Scene");

		// 初始化 FBO 目标 (1280x720 物理分辨率)
		renderTarget = new ViewTarget(1280, 720);
		sceneWidget = new ViewWidget(renderTarget);
		sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER); // 场景默认铺满

		// 布局：Stack (底层是画面，上层是工具栏)
		Stack stack = new Stack();
		stack.add(sceneWidget);

		Table overlay = new Table();
		overlay.top().left().pad(5);
		createToolbar(overlay);
		stack.add(overlay);

		addContent(stack);
	}

	private void createToolbar(Table t) {
		// [修改] 赋值给成员变量
		btnSave = addToolBtn(t, "Save", () -> presenter.saveScene(), Color.WHITE);
		btnLoad = addToolBtn(t, "Load", () -> presenter.loadScene(), Color.WHITE);

		t.add().width(20);

		// Gizmo Modes
		btnMove = addToolBtn(t, "M", () -> presenter.setGizmoMode(EditorGizmoSystem.Mode.MOVE), Color.GRAY);
		btnRotate = addToolBtn(t, "R", () -> presenter.setGizmoMode(EditorGizmoSystem.Mode.ROTATE), Color.GRAY);
		btnScale = addToolBtn(t, "S", () -> presenter.setGizmoMode(EditorGizmoSystem.Mode.SCALE), Color.GRAY);
	}

	@Override
	public void setStorageEnabled(boolean enabled) {
		// 变灰 + 禁用触摸
		Color c = enabled ? Color.WHITE : Color.GRAY;
		Touchable t = enabled ? Touchable.enabled : Touchable.disabled;

		if (btnSave != null) {
			btnSave.setColor(c);
			btnSave.setTouchable(t);
		}
		if (btnLoad != null) {
			btnLoad.setColor(c);
			btnLoad.setTouchable(t);
		}
	}

	private VisTextButton addToolBtn(Table t, String text, Runnable act, Color c) {
		VisTextButton b = new VisTextButton(text);
		b.setColor(c);
		b.addListener(new ClickListener() {
			@Override public void clicked(InputEvent e, float x, float y) { act.run(); }
		});
		t.add(b).padRight(5);
		return b;
	}

	@Override
	public void setPresenter(ScenePresenter presenter) {
		this.presenter = presenter;
		updateGizmoModeUI(EditorGizmoSystem.Mode.MOVE.ordinal()); // 默认高亮
	}

	@Override
	public ViewTarget getRenderTarget() {
		return renderTarget;
	}

	@Override
	public Vector2 screenToWorld(float screenX, float screenY, OrthographicCamera camera) {
		return sceneWidget.screenToWorld(screenX, screenY, camera);
	}

	@Override
	public void updateGizmoModeUI(int modeOrdinal) {
		// 简单的高亮逻辑：选中的是 Cyan，未选中的是 Gray
		Color active = Color.CYAN;
		Color inactive = Color.GRAY;

		btnMove.setColor(inactive);
		btnRotate.setColor(inactive);
		btnScale.setColor(inactive);

		if (modeOrdinal == EditorGizmoSystem.Mode.MOVE.ordinal()) btnMove.setColor(active);
		else if (modeOrdinal == EditorGizmoSystem.Mode.ROTATE.ordinal()) btnRotate.setColor(active);
		else if (modeOrdinal == EditorGizmoSystem.Mode.SCALE.ordinal()) btnScale.setColor(active);
	}

	public void dispose() {
		if (renderTarget != null) renderTarget.dispose();
	}

	// 公开 Widget 供 EditorController 绑定 DragAndDrop
	public Actor getDropTargetActor() {
		return sceneWidget;
	}

	@Override
	public boolean isMouseOver() {
		return hasFocus; // 直接返回 EditorPanel 维护的焦点状态
	}
}
