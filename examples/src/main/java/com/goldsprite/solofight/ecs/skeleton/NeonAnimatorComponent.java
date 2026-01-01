// 文件: ./core/src/main/java/com/goldsprite/solofight/ecs/skeleton/NeonAnimatorComponent.java
package com.goldsprite.solofight.ecs.skeleton;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.goldsprite.gameframeworks.ecs.component.Component;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonTimeline;

/**
 * 霓虹动画控制器 (v2: 支持 CrossFade)
 * 职责：管理动画状态机，驱动骨骼变换
 */
public class NeonAnimatorComponent extends Component {

	// 依赖
	private SkeletonComponent skeletonComp;

	// 动画库 (名字 -> 数据)
	private final ObjectMap<String, NeonAnimation> animationLibrary = new ObjectMap<>();

	// --- 主轨道 (Current) ---
	private NeonAnimation currentAnim;
	private float currentTime = 0f;

	// --- 混合轨道 (Previous) ---
	private NeonAnimation prevAnim;
	private float prevTime = 0f;

	// --- 混合控制 ---
	private float mixTimer = 0f;
	private float mixDuration = 0f;
	private boolean inTransition = false; // 是否处于混合过程中

	private float speed = 1.0f;
	private boolean isPlaying = false;

	@Override
	protected void onAwake() {
		// 获取同实体的骨架组件
		skeletonComp = getComponent(SkeletonComponent.class);
	}

	public void addAnimation(NeonAnimation anim) {
		animationLibrary.put(anim.name, anim);
	}

	/** 硬切播放 */
	public void play(String name) {
		startAnimation(name, 0f);
	}

	/** 混合播放 (CrossFade) */
	public void crossFade(String name, float duration) {
		startAnimation(name, duration);
	}

	private void startAnimation(String name, float transitionTime) {
		NeonAnimation nextAnim = animationLibrary.get(name);
		if (nextAnim == null) return;
		if (currentAnim == nextAnim && isPlaying && currentAnim.looping) return;

		if (transitionTime <= 0 || currentAnim == null) {
			// 无过渡：直接硬切
			this.currentAnim = nextAnim;
			this.currentTime = 0f;
			this.inTransition = false;
			this.prevAnim = null;
		} else {
			// 有过渡：将当前降级为旧动画，新动画上位
			this.prevAnim = this.currentAnim;
			this.prevTime = this.currentTime; // 记录旧动画停在哪一刻 (或者让它继续走? 通常继续走一点点更自然)

			this.currentAnim = nextAnim;
			this.currentTime = 0f;

			this.mixDuration = transitionTime;
			this.mixTimer = 0f;
			this.inTransition = true;
		}
		this.isPlaying = true;
	}

	@Override
	public void update(float delta) {
		if (!isPlaying || currentAnim == null || skeletonComp == null) return;

		float dt = delta * speed;

		// 1. 更新主动画时间
		currentTime = updateTime(currentAnim, currentTime, dt);

		// 2. 更新混合逻辑
		float alpha = 1.0f; // 默认为 100% 新动画
		if (inTransition) {
			mixTimer += dt;
			// 更新旧动画时间 (让旧动作惯性继续走一会，比定格更自然)
			if (prevAnim != null) {
				prevTime = updateTime(prevAnim, prevTime, dt);
			}

			if (mixTimer >= mixDuration) {
				// 混合结束
				inTransition = false;
				prevAnim = null;
			} else {
				// 计算权重 (0~1)
				alpha = mixTimer / mixDuration;
			}
		}

		// 3. 驱动骨骼
		applyToSkeleton(alpha);
	}

	// 辅助：处理时间循环
	private float updateTime(NeonAnimation anim, float time, float dt) {
		time += dt;
		if (time >= anim.duration) {
			if (anim.looping) time %= anim.duration;
			else time = anim.duration;
		}
		return time;
	}

	private void applyToSkeleton(float alpha) {
		NeonSkeleton skeleton = skeletonComp.getSkeleton();

		// 策略：
		// 1. 先计算 Prev 的值 (如果有)
		// 2. 再计算 Curr 的值
		// 3. 结果 = Lerp(Prev, Curr, alpha)

		// 这里的难点是：Prev 和 Curr 可能控制不同的骨骼集合。
		// 简单起见，我们假设它们控制相同的骨骼，或者以 Curr 为主。
		// 为了健壮性，我们可以先遍历 Curr 的 Timeline。

		for (NeonTimeline timeline : currentAnim.timelines) {
			NeonBone bone = skeleton.getBone(timeline.boneName);
			if (bone == null) continue;

			// 获取新动画的目标值
			float valCurr = timeline.evaluate(currentTime);
			float finalVal = valCurr;

			// 如果在混合，且旧动画也有这条轨道，则进行混合
			if (inTransition && prevAnim != null) {
				NeonTimeline prevLine = findTimeline(prevAnim, timeline.boneName, timeline.property); // 查找同名同属性轨道
				if (prevLine != null) {
					float valPrev = prevLine.evaluate(prevTime);
					// 插值公式: prev + (curr - prev) * alpha
					finalVal = MathUtils.lerp(valPrev, valCurr, alpha);
				}
			}

			// 写入
			switch (timeline.property) {
				case X: bone.x = finalVal; break;
				case Y: bone.y = finalVal; break;
				case ROTATION: bone.rotation = finalVal; break;
				case SCALE_X: bone.scaleX = finalVal; break;
				case SCALE_Y: bone.scaleY = finalVal; break;
			}
		}
	}

	// 简单的线性查找 (量少不慢，量多可优化为Map)
	private NeonTimeline findTimeline(NeonAnimation anim, String bone, com.goldsprite.solofight.ecs.skeleton.animation.NeonProperty prop) {
		for(NeonTimeline t : anim.timelines) {
			if(t.boneName.equals(bone) && t.property == prop) return t;
		}
		return null;
	}
}
