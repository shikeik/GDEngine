package com.goldsprite.solofight.screens.tests.iconeditor.system;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.AddNodeCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.DeleteNodeCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.ReparentCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.BaseNode;
import com.goldsprite.solofight.screens.tests.iconeditor.model.CircleShape;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.model.GroupNode;
import com.goldsprite.solofight.screens.tests.iconeditor.model.RectShape;

public class SceneManager {
	public interface ShapeFactory {
		EditorTarget create(String name);
	}

	private EditorTarget rootNode;
	private EditorTarget selection;
	private final List<EditorListener> listeners = new ArrayList<>();
	private final Map<String, ShapeFactory> shapeRegistry = new LinkedHashMap<>();
	private final CommandManager commandManager;
	
	public SceneManager(CommandManager cm) {
		this.commandManager = cm;
		registerDefaultShapes();
	}
	
	private void registerDefaultShapes() {
		shapeRegistry.put("Group", GroupNode::new);
		shapeRegistry.put("Rectangle", name -> {
			RectShape r = new RectShape(name);
			r.width = 80; r.height = 80;
			return r;
		});
		shapeRegistry.put("Circle", name -> {
			CircleShape c = new CircleShape(name);
			c.radius = 40;
			return c;
		});
	}
	
	public void setRoot(EditorTarget root) {
		this.rootNode = root;
		notifyStructureChanged();
	}
	
	public EditorTarget getRoot() { return rootNode; }
	public EditorTarget getSelection() { return selection; }
	public Map<String, ShapeFactory> getShapeRegistry() { return shapeRegistry; }
	
	public void addListener(EditorListener l) { listeners.add(l); }
	public void removeListener(EditorListener l) { listeners.remove(l); }
	
	public void notifyStructureChanged() {
		for(EditorListener l : listeners) l.onStructureChanged();
	}
	
	public void selectNode(EditorTarget node) {
		this.selection = node;
		for(EditorListener l : listeners) l.onSelectionChanged(node);
	}
	
	public void deleteNode(EditorTarget node) {
		if (node == null || node == rootNode) return;
		commandManager.execute(new DeleteNodeCommand(this, node));
	}

	public void internalDeleteNode(EditorTarget node) {
		if (node == null || node == rootNode) return;
		node.removeFromParent();
		if (selection == node) selectNode(null);
		notifyStructureChanged();
	}
	
	public void addNode(EditorTarget parent, String type) {
		if (parent == null) parent = rootNode;
		commandManager.execute(new AddNodeCommand(this, parent, type));
	}
	
	public EditorTarget internalAddNode(EditorTarget parent, String type) {
		if (parent == null) parent = rootNode;
		ShapeFactory factory = shapeRegistry.get(type);
		if (factory == null) return null;
		
		String name = getUniqueName(parent, "New " + type);
		EditorTarget newShape = factory.create(name);
		
		if (newShape != null) {
			newShape.setX(MathUtils.random(-20, 20));
			newShape.setY(MathUtils.random(-20, 20));
			newShape.setParent(parent);
			notifyStructureChanged();
			selectNode(newShape);
		}
		return newShape;
	}

	public void internalAttachNode(EditorTarget node, EditorTarget parent, int index) {
		if (node == null || parent == null) return;
		node.setParent(parent);
		if (index >= 0 && index <= parent.getChildren().size) {
			parent.getChildren().removeValue(node, true);
			parent.getChildren().insert(index, node);
		}
		notifyStructureChanged();
		selectNode(node);
	}
	
	public void moveNode(EditorTarget node, EditorTarget newParent, int index) {
		if (node == null || newParent == null) return;
		commandManager.execute(new ReparentCommand(this, node, newParent, index));
	}
	
	public void internalMoveNode(EditorTarget node, EditorTarget newParent, int index) {
		if (node == null || newParent == null) return;
		
		node.setParent(newParent);
		
		if (index >= 0) {
			newParent.getChildren().removeValue(node, true);
			if (index > newParent.getChildren().size) index = newParent.getChildren().size;
			newParent.getChildren().insert(index, node);
		}
		
		notifyStructureChanged();
		selectNode(node);
	}
	
	public void changeNodeType(EditorTarget target, String newType) {
		if (target == null || target == rootNode) return;
		ShapeFactory factory = shapeRegistry.get(newType);
		if (factory == null) return;
		
		EditorTarget newNode = factory.create(target.getName());
		
		if (target instanceof BaseNode && newNode instanceof BaseNode) {
			BaseNode t = (BaseNode) target;
			BaseNode n = (BaseNode) newNode;
			n.x = t.x; n.y = t.y; n.rotation = t.rotation;
			n.scaleX = t.scaleX; n.scaleY = t.scaleY;
			n.color.set(t.color);
		}
		
		Array<EditorTarget> children = new Array<>(target.getChildren());
		for (EditorTarget child : children) child.setParent(newNode);
		
		EditorTarget parent = target.getParent();
		if (parent != null) {
			int idx = parent.getChildren().indexOf(target, true);
			target.removeFromParent();
			newNode.setParent(parent);
			parent.getChildren().removeValue(newNode, true);
			parent.getChildren().insert(idx, newNode);
		}
		
		notifyStructureChanged();
		selectNode(newNode);
	}
	
	private String getUniqueName(EditorTarget parent, String baseName) {
		boolean exists = false;
		for (EditorTarget child : parent.getChildren()) {
			if (child.getName().equals(baseName)) {
				exists = true;
				break;
			}
		}
		if (!exists) return baseName;
		
		int i = 1;
		while (true) {
			String newName = baseName + "_" + i;
			boolean found = false;
			for (EditorTarget child : parent.getChildren()) {
				if (child.getName().equals(newName)) {
					found = true;
					break;
				}
			}
			if (!found) return newName;
			i++;
		}
	}
}
