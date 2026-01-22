package com.goldsprite.gdengine.ecs.skeleton.animation;

import com.badlogic.gdx.utils.Array;

/**
 * 动画片段
 * 一个完整的动作 (e.g. "Attack"), 包含多条轨道
 */
public class NeonAnimation {
	public String name;
	public float duration; // 总时长
	public boolean looping; // 是否循环 (供 Animator 使用)

	public final Array<NeonTimeline> timelines = new Array<>();

	public NeonAnimation(String name, float duration, boolean looping) {
		this.name = name;
		this.duration = duration;
		this.looping = looping;
	}

	public void addTimeline(NeonTimeline timeline) {
		timelines.add(timeline);
	}
}
