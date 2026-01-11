package com.goldsprite.solofight.screens.tests.iconeditor.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

public abstract class BaseNode implements EditorTarget {
	public String name;
	public float x, y, rotation = 0, scaleX = 1, scaleY = 1;
	public Color color = new Color(Color.WHITE);
	// [新增] 父节点引用
	protected transient EditorTarget parent;
	public Array<EditorTarget> children = new Array<>();

	public BaseNode(String name) { this.name = name; }
	public BaseNode() { this("Node"); }

	@Override public String getName() { return name; }
	@Override public void setName(String name) { this.name = name; }
	@Override public float getX() { return x; }
	@Override public void setX(float v) { x = v; }
	@Override public float getY() { return y; }
	@Override public void setY(float v) { y = v; }
	@Override public float getRotation() { return rotation; }
	@Override public void setRotation(float v) { rotation = v; }
	@Override public float getScaleX() { return scaleX; }
	@Override public void setScaleX(float v) { scaleX = v; }
	@Override public float getScaleY() { return scaleY; }
	@Override public void setScaleY(float v) { scaleY = v; }
	@Override public Array<EditorTarget> getChildren() { return children; }
	@Override public void addChild(EditorTarget child) { children.add(child); }
	
	/**
	 * 仅用于恢复父节点引用（例如反序列化后），不触发逻辑也不修改父节点的子列表。
	 */
	public void restoreParentLink(EditorTarget parent) {
		this.parent = parent;
	}

	// [新增] 实现亲缘管理
	@Override public EditorTarget getParent() { return parent; }

	@Override public void setParent(EditorTarget newParent) {
		if (this.parent == newParent) return;

		// 1. 从旧父级移除
		if (this.parent != null) {
			this.parent.getChildren().removeValue(this, true);
		}

		this.parent = newParent;

		// 2. 加入新父级
		if (newParent != null) {
			newParent.addChild(this);
		}
	}

	@Override public void removeFromParent() {
		setParent(null);
	}

	@Override public boolean hitTest(float x, float y) { return false; }
}
