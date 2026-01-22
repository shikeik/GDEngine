package com.goldsprite.gdengine.ecs.skeleton.animation;

import com.badlogic.gdx.utils.Array;

/**
 * 时间轴
 * 控制 "某根骨头" 的 "某个属性" 随时间的变化
 */
public class NeonTimeline {
	public String boneName; // 对于帧动画，这里可以是 "self" 或者组件名
	public NeonProperty property;
	public final Array<NeonKeyframe> frames = new Array<>();

	public NeonTimeline(String boneName, NeonProperty property) {
		this.boneName = boneName;
		this.property = property;
	}

	// 添加 Float 帧
	public void addKeyframe(float time, float value, NeonCurve curve) {
		frames.add(new NeonKeyframe(time, value, curve));
		frames.sort();
	}

	// [新增] 添加 Object 帧
	public void addKeyframe(float time, Object value) {
		frames.add(new NeonKeyframe(time, value));
		frames.sort();
	}

	/**
	 * 计算浮点数值 (Lerp / Bezier)
	 */
	public float evaluate(float time) {
		if (frames.size == 0) return 0;
		NeonKeyframe first = frames.get(0);
		if (time <= first.time) return first.floatValue;
		NeonKeyframe last = frames.get(frames.size - 1);
		if (time >= last.time) return last.floatValue;

		// 查找区间
		NeonKeyframe frameA = first;
		NeonKeyframe frameB = last;
		for (int i = 0; i < frames.size - 1; i++) {
			if (time >= frames.get(i).time && time < frames.get(i+1).time) {
				frameA = frames.get(i);
				frameB = frames.get(i+1);
				break;
			}
		}

		if (frameA.curve == NeonCurve.STEPPED) return frameA.floatValue;

		float duration = frameB.time - frameA.time;
		if (duration <= 0) return frameA.floatValue;

		float t = (time - frameA.time) / duration;
		float easeT = frameA.curve.apply(t);

		return frameA.floatValue + (frameB.floatValue - frameA.floatValue) * easeT;
	}

	/**
	 * [新增] 计算对象数值 (Stepped Only)
	 * 逻辑：返回 "当前时间点之前最近的一帧" 的值
	 */
	public Object evaluateObject(float time) {
		if (frames.size == 0) return null;

		// 如果小于第一帧，返回第一帧 (或者 null? 通常返回第一帧作为默认状态)
		if (time <= frames.get(0).time) return frames.get(0).objectValue;

		// 倒序查找，找到第一个 time >= frame.time 的帧
		// 或者正序查找最后一个 <= time 的帧
		// 这里沿用正序逻辑
		Object result = frames.get(frames.size - 1).objectValue; // 默认最后一帧

		for (int i = 0; i < frames.size - 1; i++) {
			NeonKeyframe curr = frames.get(i);
			NeonKeyframe next = frames.get(i+1);

			// 落在区间 [curr, next)
			if (time >= curr.time && time < next.time) {
				return curr.objectValue;
			}
		}

		return result;
	}
}
