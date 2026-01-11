package com.goldsprite.solofight.screens.tests.iconeditor.commands;

import com.goldsprite.gdengine.core.command.ICommand;
import java.util.function.Consumer;

public class GenericPropertyChangeCommand<T> implements ICommand {
	private final String name;
	private final T oldVal, newVal;
	private final Consumer<T> setter;
	private final Runnable refreshUI;

	public GenericPropertyChangeCommand(String name, T oldVal, T newVal, Consumer<T> setter, Runnable refreshUI) {
		this.name = name; this.oldVal = oldVal; this.newVal = newVal; this.setter = setter; this.refreshUI = refreshUI;
	}

	@Override public void execute() { setter.accept(newVal); if(refreshUI != null) refreshUI.run(); }
	@Override public void undo() { setter.accept(oldVal); if(refreshUI != null) refreshUI.run(); }
	@Override public String getName() { return "Set " + name; }
	@Override public String getSource() { return "Inspector"; }
	@Override public String getIcon() { return "P"; }
}
