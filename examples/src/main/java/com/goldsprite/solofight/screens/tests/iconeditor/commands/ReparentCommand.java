package com.goldsprite.solofight.screens.tests.iconeditor.commands;

import com.goldsprite.gdengine.core.command.ICommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

public class ReparentCommand implements ICommand {
	private final SceneManager sm;
	private final EditorTarget target;
	private final EditorTarget oldParent, newParent;
	private final int oldIndex, newIndex;

	public ReparentCommand(SceneManager sm, EditorTarget target, EditorTarget newParent, int newIndex) {
		this.sm = sm;
		this.target = target;

		// [修复] 处理顶层节点: 如果 ECS parent 为 null，尝试获取编辑器 Root
		EditorTarget p = target.getParent();
		if (p == null && sm.getRoot() != null && sm.getRoot().getChildren().contains(target, true)) {
			p = sm.getRoot();
		}
		this.oldParent = p;

		if (this.oldParent != null) {
			this.oldIndex = oldParent.getChildren().indexOf(target, true);
		} else {
			this.oldIndex = -1; // 理论上不应发生，除非对象不在场景中
		}

		this.newParent = newParent;
		this.newIndex = newIndex;
	}

	@Override public void execute() { sm.internalMoveNode(target, newParent, newIndex); }

	@Override public void undo() {
		if (oldParent != null) {
			sm.internalMoveNode(target, oldParent, oldIndex);
		}
	}

	@Override public String getName() { return "Move " + target.getName(); }
	@Override public String getSource() { return "Hierarchy"; }
	@Override public String getIcon() { return "M"; }
}
