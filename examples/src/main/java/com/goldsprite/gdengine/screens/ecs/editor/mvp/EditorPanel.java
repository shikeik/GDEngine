package com.goldsprite.gdengine.screens.ecs.editor.mvp;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

public abstract class EditorPanel extends VisTable {

	protected VisTable contentTable;
	protected VisLabel titleLabel;

	public EditorPanel(String title) {
		setBackground("window-bg");

		// 1. Title Bar
		VisTable titleBar = new VisTable();
		titleBar.setBackground("button"); // 稍亮的标题栏

		titleLabel = new VisLabel(title);
		titleLabel.setAlignment(Align.left);
		titleLabel.setColor(Color.LIGHT_GRAY);

		titleBar.add(titleLabel).expandX().fillX().pad(2, 5, 2, 5);
		addTitleButtons(titleBar); // 钩子

		add(titleBar).growX().height(26).row();

		// 2. Content
		contentTable = new VisTable();
		add(contentTable).grow();
	}

	protected void addTitleButtons(Table titleBar) {}

	protected void addContent(Actor content) {
		contentTable.add(content).grow();
	}
}
