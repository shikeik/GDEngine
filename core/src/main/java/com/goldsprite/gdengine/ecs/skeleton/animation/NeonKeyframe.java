package com.goldsprite.gdengine.ecs.skeleton.animation;

/**
 * 混合关键帧
 * 既可以存 float (骨骼)，也可以存 Object (帧动画)
 */
public class NeonKeyframe implements Comparable<NeonKeyframe> {
	public float time;
	public NeonCurve curve;

	// 数据槽位 (互斥使用)
	public float floatValue;
	public Object objectValue;

	// --- Float 构造 (骨骼用) ---
	public NeonKeyframe(float time, float value) {
		this(time, value, NeonCurve.LINEAR);
	}

	public NeonKeyframe(float time, float value, NeonCurve curve) {
		this.time = time;
		this.floatValue = value;
		this.curve = curve;
	}

	// --- Object 构造 (帧动画用) ---
	public NeonKeyframe(float time, Object value) {
		this.time = time;
		this.objectValue = value;
		this.curve = NeonCurve.STEPPED; // 对象类型默认只能阶梯跳变
	}

	@Override
	public int compareTo(NeonKeyframe o) {
		return Float.compare(this.time, o.time);
	}
}
