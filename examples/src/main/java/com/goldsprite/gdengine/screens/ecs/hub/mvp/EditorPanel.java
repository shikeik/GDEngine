package com.goldsprite.gdengine.screens.ecs.editor.mvp;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * 编辑器面板基类
 * 包含统一的 Title Bar 和 Content Container
 */
public abstract class EditorPanel extends VisTable {

	protected VisTable contentTable;
	protected VisLabel titleLabel;

	public EditorPanel(String title) {
		setBackground("window-bg"); // 统一深色背景

		// 1. Title Bar
		VisTable titleBar = new VisTable();
		titleBar.setBackground("button"); // 稍亮的标题栏背景

		titleLabel = new VisLabel(title);
		titleLabel.setAlignment(Align.left);
		titleLabel.setColor(Color.LIGHT_GRAY);

		titleBar.add(titleLabel).expandX().fillX().pad(2, 5, 2, 5);

		// 添加额外按钮的钩子 (比如 Inspector 的锁或者是 Hierarchy 的加号)
		addTitleButtons(titleBar);

		add(titleBar).growX().height(26).row();

		// 2. Content
		contentTable = new VisTable();
		// contentTable.setBackground("field-bg"); // 可选：内容区背景

		add(contentTable).grow();
	}

	/** 子类重写此方法在标题栏右侧添加按钮 */
	protected void addTitleButtons(Table titleBar) {}

	/** 子类调用此方法填充内容 */
	protected void addContent(Table content) {
		contentTable.add(content).grow();
	}

	public void setTitle(String title) {
		titleLabel.setText(title);
	}
}
