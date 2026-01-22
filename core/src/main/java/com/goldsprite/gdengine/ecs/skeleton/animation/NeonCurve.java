package com.goldsprite.gdengine.ecs.skeleton.animation;

import com.badlogic.gdx.math.Interpolation;

/**
 * 插值曲线类型
 * 决定了两个关键帧之间的过渡方式
 */
public enum NeonCurve {
	LINEAR,     // 线性 (匀速)
	STEPPED,    // 阶梯 (瞬变，无过渡)
	SMOOTH;     // 平滑 (EaseInOut，慢进慢出)

	/**
	 * 计算插值进度
	 * @param t 输入进度 (0.0 ~ 1.0)
	 * @return 输出进度 (0.0 ~ 1.0)
	 */
	public float apply(float t) {
		switch (this) {
			case STEPPED: return 0f; // 只要不到 1.0，全是 0 (即保持上一帧的值)
			case SMOOTH: return Interpolation.smooth.apply(t);
			case LINEAR:
			default: return t;
		}
	}
}
