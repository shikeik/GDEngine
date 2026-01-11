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
		
		// Use local storage "GDEngine/" as requested
		FileHandle root = Gdx.files.local("GDEngine/");
		if (!root.exists()) {
			root.mkdirs();
		}
		
		addNode(root, null);
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
