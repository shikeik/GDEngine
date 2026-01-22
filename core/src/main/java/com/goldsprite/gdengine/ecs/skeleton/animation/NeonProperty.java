package com.goldsprite.gdengine.ecs.skeleton.animation;

/**
 * 动画属性枚举
 * 定义了 Timeline 可以控制骨骼的哪些属性
 */
public enum NeonProperty {
	X,          // 位移 X
	Y,          // 位移 Y
	ROTATION,   // 旋转 (角度制)
	SCALE_X,    // 缩放 X
	SCALE_Y,     // 缩放 Y

	// Render (Object)
	SPRITE; // [新增] 贴图纹理

	/** 是否是浮点数属性 (用于优化路径) */
	public boolean isFloat() {
		return this != SPRITE;
	}
}
