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
        this.sm = sm; this.target = target;
        this.parent = target.getParent();
        this.index = parent.getChildren().indexOf(target, true);
    }
    @Override public void execute() { sm.internalDeleteNode(target); }
    @Override public void undo() { sm.internalAttachNode(target, parent, index); }
    @Override public String getName() { return "Delete " + target.getName(); }
    @Override public String getSource() { return "Hierarchy"; }
    @Override public String getIcon() { return "X"; }
}
