// 文件: ./core/src/main/java/com/goldsprite/solofight/ecs/skeleton/animation/NeonTimeline.java
package com.goldsprite.solofight.ecs.skeleton.animation;

import com.badlogic.gdx.utils.Array;

/**
 * 时间轴
 * 控制 "某根骨头" 的 "某个属性" 随时间的变化
 */
public class NeonTimeline {
	public String boneName;
	public NeonProperty property;
	public final Array<NeonKeyframe> frames = new Array<>();

	public NeonTimeline(String boneName, NeonProperty property) {
		this.boneName = boneName;
		this.property = property;
	}

	public void addKeyframe(float time, float value, NeonCurve curve) {
		frames.add(new NeonKeyframe(time, value, curve));
		frames.sort(); // 保证按时间排序，这是二分查找或遍历的前提
	}

	/**
	 * 核心逻辑：计算给定时间点的数值
	 * @param time 当前动画时间
	 * @return 插值计算后的结果
	 */
	public float evaluate(float time) {
		if (frames.size == 0) return 0;

		// 1. 边界处理：时间小于第一帧，返回第一帧
		NeonKeyframe first = frames.get(0);
		if (time <= first.time) return first.value;

		// 2. 边界处理：时间大于最后一帧，返回最后一帧
		NeonKeyframe last = frames.get(frames.size - 1);
		if (time >= last.time) return last.value;

		// 3. 查找区间 (Frame A -> Frame B)
		// 简单遍历 (对于单个动作一般不超过10-20帧，遍历足够快)
		// 优化点：Animator 可以缓存 lastFrameIndex 避免每次从头找
		NeonKeyframe frameA = first;
		NeonKeyframe frameB = last;

		for (int i = 0; i < frames.size - 1; i++) {
			NeonKeyframe curr = frames.get(i);
			NeonKeyframe next = frames.get(i + 1);
			if (time >= curr.time && time < next.time) {
				frameA = curr;
				frameB = next;
				break;
			}
		}

		// 4. 插值计算
		// 阶梯曲线特殊处理：直接返回 A 的值 (直到 time >= frameB.time 才会变)
		if (frameA.curve == NeonCurve.STEPPED) return frameA.value;

		float duration = frameB.time - frameA.time;
		if (duration <= 0) return frameA.value; // 防止除零

		float t = (time - frameA.time) / duration; // 归一化进度 0~1
		float easeT = frameA.curve.apply(t);       // 应用曲线 (慢进慢出)

		// Lerp 公式: a + (b - a) * t
		return frameA.value + (frameB.value - frameA.value) * easeT;
	}
}
