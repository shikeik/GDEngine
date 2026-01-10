package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTree;
import com.goldsprite.solofight.screens.editor.EditorContext;
import com.goldsprite.solofight.screens.editor.ui.FileNode;

public class FileTreePanel extends BaseEditorPanel {
    private VisTree<FileNode, FileHandle> tree;
    private static final String ASSETS_DIR = "assets";

    public FileTreePanel(Skin skin, EditorContext context) {
        super("Project", skin, context);
    }

    @Override
    protected void initContent() {
        Table toolbar = new Table();
        TextButton btnRefresh = new TextButton("Refresh", getSkin());
        btnRefresh.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                reload();
            }
        });
        toolbar.add(btnRefresh);
        
        getContent().add(toolbar).left().row();
        
        tree = new VisTree<>();
        getContent().add(new VisScrollPane(tree)).grow();
        
        reload();
    }

    private void reload() {
        tree.clearChildren();
        FileHandle root = Gdx.files.internal("."); // Start from root to find assets
        // Actually usually assets is the root of internal
        // Let's try listing internal root
        FileHandle assets = Gdx.files.internal(".");
        
        // If we are in project root, assets might be a child
        if (assets.child(ASSETS_DIR).exists()) {
            assets = assets.child(ASSETS_DIR);
        }
        
        if (assets.exists()) {
            addNode(assets, null);
        }
    }

    private void addNode(FileHandle file, FileNode parent) {
        FileNode node = new FileNode(file);
        if (parent == null) tree.add(node);
        else parent.add(node);

        if (file.isDirectory()) {
            for (FileHandle child : file.list()) {
                addNode(child, node);
            }
        }
    }
}
