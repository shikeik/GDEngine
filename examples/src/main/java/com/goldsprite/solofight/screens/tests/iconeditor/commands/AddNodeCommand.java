package com.goldsprite.solofight.screens.tests.iconeditor.commands;

import com.goldsprite.gdengine.core.command.ICommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

public class AddNodeCommand implements ICommand {
	private final SceneManager sm;
	private final EditorTarget parent;
	private final String type;
	private EditorTarget createdNode;

	public AddNodeCommand(SceneManager sm, EditorTarget parent, String type) {
		this.sm = sm; this.parent = parent; this.type = type;
	}
	@Override public void execute() {
		if (createdNode == null) {
			createdNode = sm.internalAddNode(parent, type);
		} else {
			sm.internalAttachNode(createdNode, parent, -1);
		}
	}
	@Override public void undo() {
		if (createdNode != null) sm.internalDeleteNode(createdNode);
	}
	@Override public String getName() { return "Add " + type; }
	@Override public String getSource() { return "Hierarchy"; }
	@Override public String getIcon() { return "+"; }
}
