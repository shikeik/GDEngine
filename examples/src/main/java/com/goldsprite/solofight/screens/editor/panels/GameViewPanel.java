package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.goldsprite.solofight.screens.editor.EditorContext;

public class GameViewPanel extends BaseEditorPanel {
    public GameViewPanel(Skin skin, EditorContext context) {
        super("Game", skin, context);
    }

    @Override
    protected void initContent() {
        getContent().add(new Label("Game View (To be implemented)", getSkin()));
    }
}
