// 文件: ./core/src/main/java/com/goldsprite/solofight/ecs/skeleton/animation/NeonKeyframe.java
package com.goldsprite.solofight.ecs.skeleton.animation;

/**
 * 关键帧
 * 记录某一时刻的目标值
 */
public class NeonKeyframe implements Comparable<NeonKeyframe> {
	public float time;      // 时间点 (秒)
	public float value;     // 目标值
	public NeonCurve curve; // 到达 下一帧 的插值方式

	public NeonKeyframe(float time, float value) {
		this(time, value, NeonCurve.LINEAR);
	}

	public NeonKeyframe(float time, float value, NeonCurve curve) {
		this.time = time;
		this.value = value;
		this.curve = curve;
	}

	@Override
	public int compareTo(NeonKeyframe o) {
		return Float.compare(this.time, o.time);
	}
}
