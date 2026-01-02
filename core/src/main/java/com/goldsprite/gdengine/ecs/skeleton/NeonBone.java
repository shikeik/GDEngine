package com.goldsprite.gdengine.ecs.skeleton;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;

/**
 * 骨骼节点 (逻辑层)
 * 职责：维护父子级关系，计算变换矩阵
 */
public class NeonBone {
	public final String name;

	// 树状结构
	public NeonBone parent;
	public final Array<NeonBone> children = new Array<>();

	// 局部变换 (相对于父级)
	public float x, y;
	public float rotation; // 角度
	public float scaleX = 1f, scaleY = 1f;

	// 固有属性 (长度，用于辅助绘图)
	public float length;

	// 矩阵缓存
	public final Affine2 localTransform = new Affine2();
	public final Affine2 worldTransform = new Affine2();

	public NeonBone(String name, float length) {
		this.name = name;
		this.length = length;
	}

	/** 更新世界矩阵 (递归) */
	public void updateWorldTransform() {
		// 1. 更新局部矩阵
		localTransform.setToTrnRotScl(x, y, rotation, scaleX, scaleY);

		// 2. 结合父级矩阵
		if (parent != null) {
			worldTransform.set(parent.worldTransform).mul(localTransform);
		} else {
			worldTransform.set(localTransform);
		}

		// 3. 递归更新子级
		for (NeonBone child : children) {
			child.updateWorldTransform();
		}
	}

	public void addChild(NeonBone child) {
		if (child.parent != null) child.parent.children.removeValue(child, true);
		child.parent = this;
		children.add(child);
	}
}
