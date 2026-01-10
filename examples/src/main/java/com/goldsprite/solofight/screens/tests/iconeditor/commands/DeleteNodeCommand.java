package com.goldsprite.solofight.screens.tests.iconeditor.commands;

import com.goldsprite.gdengine.core.command.ICommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

public class DeleteNodeCommand implements ICommand {
	private final SceneManager sm;
	private final EditorTarget target;
	private final EditorTarget parent;
	private final int index;

	public DeleteNodeCommand(SceneManager sm, EditorTarget target) {
		this.sm = sm;
		this.target = target;

		// [修复] 处理顶层节点删除
		EditorTarget p = target.getParent();
		if (p == null && sm.getRoot() != null && sm.getRoot().getChildren().contains(target, true)) {
			p = sm.getRoot();
		}
		this.parent = p;

		if (this.parent != null) {
			this.index = parent.getChildren().indexOf(target, true);
		} else {
			this.index = -1;
		}
	}

	@Override public void execute() { sm.internalDeleteNode(target); }

	@Override public void undo() {
		if (parent != null) {
			sm.internalAttachNode(target, parent, index);
		}
	}

	@Override public String getName() { return "Delete " + target.getName(); }
	@Override public String getSource() { return "Hierarchy"; }
	@Override public String getIcon() { return "X"; }
}
