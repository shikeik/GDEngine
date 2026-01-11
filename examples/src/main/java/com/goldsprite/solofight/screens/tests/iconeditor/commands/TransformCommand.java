package com.goldsprite.solofight.screens.tests.iconeditor.commands;

import com.goldsprite.gdengine.core.command.ICommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

public class TransformCommand implements ICommand {
	private final EditorTarget target;
	private final float oldX, oldY, oldRot, oldSX, oldSY;
	private final float newX, newY, newRot, newSX, newSY;
	private final Runnable refreshUI;

	public TransformCommand(EditorTarget t, float ox, float oy, float or, float osx, float osy, Runnable refreshUI) {
		this.target = t;
		this.oldX = ox; this.oldY = oy; this.oldRot = or; this.oldSX = osx; this.oldSY = osy;
		this.newX = t.getX(); this.newY = t.getY(); this.newRot = t.getRotation(); this.newSX = t.getScaleX(); this.newSY = t.getScaleY();
		this.refreshUI = refreshUI;
	}

	@Override public void execute() { apply(newX, newY, newRot, newSX, newSY); }
	@Override public void undo() { apply(oldX, oldY, oldRot, oldSX, oldSY); }
	@Override public String getName() { return "Transform " + target.getName(); }
	@Override public String getSource() { return "Gizmo"; }
	@Override public String getIcon() { return "T"; }

	private void apply(float x, float y, float r, float sx, float sy) {
		target.setX(x); target.setY(y); target.setRotation(r); target.setScaleX(sx); target.setScaleY(sy);
		if(refreshUI != null) refreshUI.run();
	}
}
