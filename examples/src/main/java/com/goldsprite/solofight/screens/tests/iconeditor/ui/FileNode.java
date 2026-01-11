package com.goldsprite.solofight.screens.tests.iconeditor.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.widget.VisLabel;

public class FileNode extends Tree.Node<FileNode, FileHandle, VisLabel> {
	public FileNode(FileHandle file) {
		super(new VisLabel(file.name()));
		setValue(file);
		VisLabel label = getActor();
		if (file.isDirectory()) label.setColor(Color.GOLD);
	}

	public void setDirty(boolean dirty) {
		VisLabel label = getActor();
		String name = getValue().name();
		if (dirty) label.setText(name + " *");
		else label.setText(name);
	}
}
