package com.goldsprite.solofight.screens.tests.iconeditor.commands;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.core.command.ICommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.BaseNode;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

public class ColorChangeCommand implements ICommand {
    private final EditorTarget target;
    private final Color oldColor, newColor;
    private final Runnable refreshUI;

    public ColorChangeCommand(EditorTarget target, Color oldVal, Color newVal, Runnable refreshUI) {
        this.target = target; 
        this.oldColor = new Color(oldVal); 
        this.newColor = new Color(newVal);
        this.refreshUI = refreshUI;
    }
    @Override public void execute() { apply(newColor); }
    @Override public void undo() { apply(oldColor); }
    private void apply(Color c) { 
        if(target instanceof BaseNode) ((BaseNode)target).color.set(c);
        if(refreshUI != null) refreshUI.run(); 
    }
    @Override public String getName() { return "Color Change"; }
    @Override public String getSource() { return "Inspector"; }
    @Override public String getIcon() { return "C"; }
}
