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
        this.sm = sm; this.target = target;
        this.oldParent = target.getParent();
        this.oldIndex = oldParent.getChildren().indexOf(target, true);
        this.newParent = newParent;
        this.newIndex = newIndex;
    }
    @Override public void execute() { sm.internalMoveNode(target, newParent, newIndex); }
    @Override public void undo() { sm.internalMoveNode(target, oldParent, oldIndex); }
    @Override public String getName() { return "Move " + target.getName(); }
    @Override public String getSource() { return "Hierarchy"; }
    @Override public String getIcon() { return "M"; }
}
