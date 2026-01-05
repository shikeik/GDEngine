package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.goldsprite.gdengine.core.scripting.ScriptResourceTracker;
import com.goldsprite.gdengine.ecs.skeleton.NeonBone;
import com.goldsprite.gdengine.ecs.skeleton.NeonSkeleton;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;

public class NeonAnimatorComponent extends Component {

	private SkeletonComponent skeletonComp;
	// [新增] 引用 SpriteComponent
	private SpriteComponent spriteComp;

	private final ObjectMap<String, NeonAnimation> animationLibrary = new ObjectMap<>();

	private NeonAnimation currentAnim;
	private float currentTime = 0f;

	private NeonAnimation prevAnim;
	private float prevTime = 0f;

	private float mixTimer = 0f;
	private float mixDuration = 0f;
	private boolean inTransition = false;

	private float speed = 1.0f;
	private boolean isPlaying = false;

	@Override
	protected void onAwake() {
		skeletonComp = getComponent(SkeletonComponent.class);
		// [新增] 获取 Sprite 组件 (它是可选的，不是所有物体都有)
		spriteComp = getComponent(SpriteComponent.class);
	}

	public void addAnimation(NeonAnimation anim) {
		animationLibrary.put(anim.name, anim);
	}

	public void play(String name) {
		startAnimation(name, 0f);
	}

	public void crossFade(String name, float duration) {
		startAnimation(name, duration);
	}

	private void startAnimation(String name, float transitionTime) {
		NeonAnimation nextAnim = animationLibrary.get(name);
		if (nextAnim == null) return;
		if (currentAnim == nextAnim && isPlaying && currentAnim.looping) return;

		if (transitionTime <= 0 || currentAnim == null) {
			this.currentAnim = nextAnim;
			this.currentTime = 0f;
			this.inTransition = false;
			this.prevAnim = null;
		} else {
			this.prevAnim = this.currentAnim;
			this.prevTime = this.currentTime;
			this.currentAnim = nextAnim;
			this.currentTime = 0f;
			this.mixDuration = transitionTime;
			this.mixTimer = 0f;
			this.inTransition = true;
		}
		this.isPlaying = true;
	}

	/**
	 * [新增] 脚本快捷方法：创建简单的序列帧动画
	 *
	 * 使用示例:
	 * anim.createSimpleAnim("Run", 0.8f, true, "run_01.png", "run_02.png", "run_03.png");
	 *
	 * @param name 动画名 (用于 play)
	 * @param duration 总时长 (秒)
	 * @param looping 是否循环
	 * @param fileNames 图片文件名列表
	 */
	public void createSimpleAnim(String name, float duration, boolean looping, String... fileNames) {
		if (fileNames == null || fileNames.length == 0) return;

		NeonAnimation anim = new NeonAnimation(name, duration, looping);

		// 创建针对自身的 Sprite 属性轨道
		NeonTimeline timeline = new NeonTimeline("self", NeonProperty.SPRITE);

		float frameDuration = duration / fileNames.length;

		for (int i = 0; i < fileNames.length; i++) {
			// 自动加载每一帧
			TextureRegion reg = ScriptResourceTracker.loadRegion(fileNames[i]);
			if (reg != null) {
				// 在对应时间点插入关键帧
				timeline.addKeyframe(i * frameDuration, reg);
			}
		}

		anim.addTimeline(timeline);
		addAnimation(anim);
	}

	@Override
	public void update(float delta) {
		if (!isPlaying || currentAnim == null) return; // 移除 skeletonComp == null 的强制检查，因为它可能只驱动 Sprite

		float dt = delta * speed;
		currentTime = updateTime(currentAnim, currentTime, dt);

		float alpha = 1.0f;
		if (inTransition) {
			mixTimer += dt;
			if (prevAnim != null) prevTime = updateTime(prevAnim, prevTime, dt);

			if (mixTimer >= mixDuration) {
				inTransition = false;
				prevAnim = null;
			} else {
				alpha = mixTimer / mixDuration;
			}
		}

		applyToEntity(alpha);
	}

	private float updateTime(NeonAnimation anim, float time, float dt) {
		time += dt;
		if (time >= anim.duration) {
			if (anim.looping) time %= anim.duration;
			else time = anim.duration;
		}
		return time;
	}

	private void applyToEntity(float alpha) {
		// 遍历当前动画的所有时间轴
		for (NeonTimeline timeline : currentAnim.timelines) {

			// --- 分支 1: 处理浮点数属性 (骨骼) ---
			if (timeline.property.isFloat()) {
				if (skeletonComp == null) continue; // 没骨架就跳过

				NeonSkeleton skeleton = skeletonComp.getSkeleton();
				NeonBone bone = skeleton.getBone(timeline.boneName);
				if (bone == null) continue;

				float valCurr = timeline.evaluate(currentTime);
				float finalVal = valCurr;

				// 混合逻辑 (仅针对 float)
				if (inTransition && prevAnim != null) {
					NeonTimeline prevLine = findTimeline(prevAnim, timeline.boneName, timeline.property);
					if (prevLine != null) {
						float valPrev = prevLine.evaluate(prevTime);
						finalVal = MathUtils.lerp(valPrev, valCurr, alpha);
					}
				}

				switch (timeline.property) {
					case X: bone.x = finalVal; break;
					case Y: bone.y = finalVal; break;
					case ROTATION: bone.rotation = finalVal; break;
					case SCALE_X: bone.scaleX = finalVal; break;
					case SCALE_Y: bone.scaleY = finalVal; break;
				}
			}
			// --- 分支 2: 处理对象属性 (Sprite) ---
			else if (timeline.property == NeonProperty.SPRITE) {
				if (spriteComp == null) continue; // 没 Sprite 组件就跳过

				// 帧动画不支持混合，直接取当前值
				// 只有当 alpha > 0.5 (或者是立即?) 时切换?
				// 通常帧动画是立即响应当前的 Animation，Prev Anim 的贴图不应该残留

				Object val = timeline.evaluateObject(currentTime);
				if (val instanceof TextureRegion) {
					spriteComp.setRegion((TextureRegion) val);
				}
			}
		}
	}

	private NeonTimeline findTimeline(NeonAnimation anim, String bone, NeonProperty prop) {
		for(NeonTimeline t : anim.timelines) {
			if(t.boneName.equals(bone) && t.property == prop) return t;
		}
		return null;
	}
}
