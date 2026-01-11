package com.goldsprite.solofight.screens.tests.iconeditor.model;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 编辑器对象接口
 * <p>
 * 这是编辑器与具体对象之间的桥梁。
 * 为了支持 ECS Entity 或 GObject，可以创建适配器来实现此接口。
 * </p>
 */
public interface EditorTarget {
	String getName();
	void setName(String name);
	String getTypeName();
	
	float getX(); void setX(float v);
	float getY(); void setY(float v);
	float getRotation(); void setRotation(float v);
	float getScaleX(); void setScaleX(float v);
	float getScaleY(); void setScaleY(float v);
	
	// 亲缘关系
	EditorTarget getParent();
	void setParent(EditorTarget parent);
	void removeFromParent(); // 从父级移除自己
	Array<EditorTarget> getChildren();
	void addChild(EditorTarget child); // 仅添加数据，不处理逻辑
	
	boolean hitTest(float x, float y);
	void render(NeonBatch batch);
}
