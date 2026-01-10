package com.goldsprite.solofight.screens.editor.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.solofight.screens.editor.EditorContext;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

public class GObjectNode extends Tree.Node<GObjectNode, GObject, VisTable> {
    public GObjectNode(GObject gobject, EditorContext context) {
        super(new VisTable());
        setValue(gobject);
        
        VisTable table = getActor();
        VisLabel label = new VisLabel(gobject.getName());
        table.add(label).growX();
        
        label.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                context.setSelection(gobject);
            }
        });
    }
}
