package com.goldsprite.gdengine.screens.ecs.editor.core;

import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.input.Event;

import java.util.List;

/**
 * 原生编辑器场景管理器
 * 直接操作 GObject，无中间商赚差价。
 */
public class EditorSceneManager {

	private final CommandManager commandManager;
	private GObject selection;

	// 事件
	public final Event<Object> onStructureChanged = new Event<>();
	public final Event<GObject> onSelectionChanged = new Event<>();

	public EditorSceneManager(CommandManager commandManager) {
		this.commandManager = commandManager;

		// 监听 GameWorld 的变动，自动刷新编辑器 UI
		GameWorld.inst().onGObjectRegistered.add(obj -> notifyStructureChanged());
		GameWorld.inst().onGObjectUnregistered.add(obj -> notifyStructureChanged());
	}

	// --- 核心查询 ---

	/**
	 * 获取场景顶层物体 (用于构建 TreeView)
	 */
	public List<GObject> getRootObjects() {
		return GameWorld.inst().getRootEntities();
	}

	public GObject getSelection() {
		return selection;
	}

	public void select(GObject obj) {
		if (this.selection != obj) {
			this.selection = obj;
			onSelectionChanged.invoke(obj);
		}
	}

	public void notifyStructureChanged() {
		onStructureChanged.invoke(null);
	}

	// --- 操作 (将在后续对接 Command) ---

	public void deleteSelection() {
		if (selection != null) {
			// TODO: 接入 DeleteGObjectCommand
			selection.destroyImmediate(); // 暂时直接删
			select(null);
		}
	}

	/**
	 * 核心层级调整
	 * @param target 被拖拽的物体
	 * @param newParent 新父级 (null 表示移动到顶层)
	 */
	public void setParent(GObject target, GObject newParent) {
		if (target == null) return;

		// GObject 内部已经处理了从旧父级移除、添加到新父级、以及世界注册的所有逻辑
		// 我们只需要调用这一句，简直完美。
		target.setParent(newParent);

		// 通知 UI 刷新
		notifyStructureChanged();
	}
}
