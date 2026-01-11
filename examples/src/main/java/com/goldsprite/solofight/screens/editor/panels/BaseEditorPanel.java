package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.goldsprite.dockablewindow.core.DockableWindow;
import com.goldsprite.solofight.screens.editor.EditorContext;

public abstract class BaseEditorPanel extends DockableWindow {
	protected final EditorContext context;

	public BaseEditorPanel(String title, Skin skin, EditorContext context) {
		super(title, skin);
		this.context = context;
		initContent();
	}

	protected abstract void initContent();
}
