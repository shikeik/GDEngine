package com.goldsprite.solofight.screens.tests.iconeditor.commands;

import com.goldsprite.gdengine.core.command.ICommand;
import java.util.function.Consumer;

public class PropertyChangeCommand implements ICommand {
	private final String name;
	private final float oldVal, newVal;
	private final Consumer<Float> setter;
	private final Runnable refreshUI;

	public PropertyChangeCommand(String name, float oldVal, float newVal, Consumer<Float> setter, Runnable refreshUI) {
		this.name = name; this.oldVal = oldVal; this.newVal = newVal; this.setter = setter; this.refreshUI = refreshUI;
	}

	@Override public void execute() { setter.accept(newVal); if(refreshUI != null) refreshUI.run(); }
	@Override public void undo() { setter.accept(oldVal); if(refreshUI != null) refreshUI.run(); }
	@Override public String getName() { return "Set " + name; }
	@Override public String getSource() { return "Inspector"; }
	@Override public String getIcon() { return "P"; }
}
