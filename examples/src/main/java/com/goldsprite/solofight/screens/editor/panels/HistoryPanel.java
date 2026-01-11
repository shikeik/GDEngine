package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.goldsprite.solofight.screens.editor.EditorContext;
import com.goldsprite.solofight.ui.widget.CommandHistoryUI;

public class HistoryPanel extends BaseEditorPanel {
	public HistoryPanel(Skin skin, EditorContext context) {
		super("History", skin, context);
	}

	@Override
	protected void initContent() {
		CommandHistoryUI historyUI = new CommandHistoryUI(context.commandManager);
		historyUI.setAutoPosition(false);
		// Ensure it fills the panel
		// Note: CommandHistoryUI has its own internal scrolling and layout.
		// We might need to adjust it to fill available space.
		// Since CommandHistoryUI is a Group, we rely on Table layout to size it.
		getContent().add(historyUI).grow();
	}
}
