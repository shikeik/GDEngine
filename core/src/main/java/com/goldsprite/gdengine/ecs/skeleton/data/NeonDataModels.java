package com.goldsprite.gdengine.ecs.skeleton.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯数据结构，用于 JSON 序列化
 * 对应 NeonAnimation, NeonTimeline, NeonKeyframe
 */
public class NeonDataModels {

	public static class AnimData {
		public String name;
		public float duration;
		public boolean looping;
		public List<TimelineData> timelines = new ArrayList<>();
	}

	public static class TimelineData {
		public String bone;     // boneName
		public String prop;     // NeonProperty name (X, Y, ROTATION...)
		public List<KeyframeData> keys = new ArrayList<>();
	}

	public static class KeyframeData {
		public float t;         // time
		public float v;         // float value (用于骨骼)
		public String s;        // sprite name (用于帧动画, Optional)
		public String c;        // curve type (LINEAR, SMOOTH, STEPPED)

		public KeyframeData() {}

		// 便捷构造
		public KeyframeData(float t, float v, String c) {
			this.t = t; this.v = v; this.c = c;
		}
	}
}
