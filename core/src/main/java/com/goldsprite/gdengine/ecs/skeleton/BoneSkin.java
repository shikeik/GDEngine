package com.goldsprite.gdengine.ecs.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 骨骼皮肤接口
 * 职责：定义骨骼在屏幕上的视觉表现（形状、贴图、特效）
 */
public interface BoneSkin {
	/**
	 * 绘制方法
	 * @param batch 渲染批处理
	 * @param transform 骨骼的世界变换矩阵（已包含位置、旋转、缩放）
	 * @param length 骨骼的逻辑长度（用于确定形状的尺寸参考）
	 * @param color Slot 传入的叠加颜色（通常用于状态变色，如受击变白）
	 */
	void draw(NeonBatch batch, Affine2 transform, float length, Color color);
}
