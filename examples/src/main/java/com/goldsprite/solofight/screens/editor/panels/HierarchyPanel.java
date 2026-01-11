package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTree;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.solofight.screens.editor.EditorContext;
import com.goldsprite.solofight.screens.editor.ui.GObjectNode;

public class HierarchyPanel extends BaseEditorPanel {
	private VisTree<GObjectNode, GObject> tree;

	public HierarchyPanel(Skin skin, EditorContext context) {
		super("Hierarchy", skin, context);
	}

	@Override
	protected void initContent() {
		tree = new VisTree<>();
		getContent().add(new VisScrollPane(tree)).grow();

		// Listeners
		context.gameWorld.onGObjectRegistered.add(this::onGObjectAdded);
		context.gameWorld.onGObjectUnregistered.add(this::onGObjectRemoved);
		
		rebuildTree();
	}

	private void rebuildTree() {
		tree.clearChildren();
		for (GObject root : context.gameWorld.getRootEntities()) {
			addNode(root, null);
		}
	}

	private void addNode(GObject gobject, GObjectNode parentNode) {
		GObjectNode node = new GObjectNode(gobject, context);
		if (parentNode == null) tree.add(node);
		else parentNode.add(node);

		for (GObject child : gobject.getChildren()) {
			addNode(child, node);
		}
		node.setExpanded(true);
	}

	private void onGObjectAdded(GObject gobject) {
		// Only if it's a root entity (no parent)
		if (gobject.getParent() == null) {
			 addNode(gobject, null);
		}
	}

	private void onGObjectRemoved(GObject gobject) {
		GObjectNode node = tree.findNode(gobject);
		if (node != null) {
			node.remove();
		}
	}
}
