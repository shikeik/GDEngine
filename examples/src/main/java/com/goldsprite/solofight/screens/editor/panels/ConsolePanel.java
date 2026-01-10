package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.solofight.screens.editor.EditorContext;

public class ConsolePanel extends BaseEditorPanel {
    private List<String> list;

    public ConsolePanel(Skin skin, EditorContext context) {
        super("Console", skin, context);
    }

    @Override
    protected void initContent() {
        list = new List<>(getSkin());
        getContent().add(new VisScrollPane(list)).grow();
    }
    
    @Override
    public void act(float delta) {
        super.act(delta);
        // Polling logs (inefficient but works for now)
        java.util.List<String> logs = Debug.getLogs();
        if (list.getItems().size != logs.size()) {
            list.setItems(logs.toArray(new String[0]));
            // Scroll to bottom logic if needed
        }
    }
}
