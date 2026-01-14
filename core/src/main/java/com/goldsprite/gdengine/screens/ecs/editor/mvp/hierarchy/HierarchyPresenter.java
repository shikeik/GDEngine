package com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy;

import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;

public class HierarchyPresenter {
	private final IHierarchyView view;
	private final EditorSceneManager sceneManager;
	private final float REFRESH_RATE = 1f / 30f; // 限制为 30FPS 刷新
	// [新增] 节流阀状态，防止一帧内多次刷新导致闪烁和性能浪费
	private boolean isDirty = false;
	private float timer = 0f;
	// [新增] 记录当前选中，用于刷新后恢复
	private GObject currentSelection;

	public HierarchyPresenter(IHierarchyView view, EditorSceneManager sceneManager) {
		this.view = view;
		this.sceneManager = sceneManager;
		view.setPresenter(this);

		// 监听事件：只标记脏状态，不立即刷新
		EditorEvents.inst().subscribeStructure(v -> isDirty = true);
		EditorEvents.inst().subscribeSceneLoaded(v -> isDirty = true);

		// [新增] 监听选中事件，同步状态
		EditorEvents.inst().subscribeSelection(obj -> {
			// 这里需要判断类型, 因为现在检查器支持任何类型
			if(obj instanceof GObject gObj) this.currentSelection = gObj;
		});

		// 初始强制刷新一次
		refresh();
	}

	// [新增] 每帧调用，处理延迟刷新
	public void update(float delta) {
		if (isDirty) {
			timer += delta;
			if (timer >= REFRESH_RATE) {
				refresh();
				isDirty = false;
				timer = 0f;
			}
		}
	}

	private void refresh() {
		// 获取最新的根节点列表并重建树
		view.showNodes(GameWorld.inst().getRootEntities());
		// [核心修复] 刷新后尝试恢复选中状态 (Item 2 & 4)
		if (currentSelection != null) {
			view.selectNode(currentSelection);
		}
	}

	// --- 业务操作 ---

	public void selectObject(GObject obj) {
		sceneManager.select(obj);
		EditorEvents.inst().emitSelectionChanged(obj);
	}

	public void createObject(GObject parent) {
		GObject obj = new GObject("GameObject");
		if (parent != null) obj.setParent(parent);

		// [核心修复] 立即更新选中状态
		this.currentSelection = obj;

		// 发送结构变化事件，这会将 isDirty 设为 true
		EditorEvents.inst().emitStructureChanged();
		selectObject(obj);
	}

	public void deleteObject(GObject obj) {
		if (obj != null) {
			obj.destroyImmediate();
			EditorEvents.inst().emitStructureChanged();
			if (sceneManager.getSelection() == obj) {
				selectObject(null);
			}
		}
	}

	public void moveEntity(GObject target, GObject newParent, int index) {
		sceneManager.moveEntity(target, newParent, index);
		EditorEvents.inst().emitStructureChanged();
	}
}
