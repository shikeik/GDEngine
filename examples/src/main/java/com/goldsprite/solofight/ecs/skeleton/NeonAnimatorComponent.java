// 文件: ./core/src/main/java/com/goldsprite/solofight/ecs/skeleton/NeonAnimatorComponent.java
package com.goldsprite.solofight.ecs.skeleton;

import com.badlogic.gdx.utils.ObjectMap;
import com.goldsprite.gameframeworks.ecs.component.Component;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.solofight.ecs.skeleton.animation.NeonTimeline;

/**
 * 霓虹动画控制器
 * 职责：管理动画状态机，驱动骨骼变换
 */
public class NeonAnimatorComponent extends Component {

	// 依赖
	private SkeletonComponent skeletonComp;

	// 动画库 (名字 -> 数据)
	private final ObjectMap<String, NeonAnimation> animationLibrary = new ObjectMap<>();

	// 播放状态
	private NeonAnimation currentAnim;
	private float timer = 0f;
	private float speed = 1.0f;
	private boolean isPlaying = false;

	@Override
	protected void onAwake() {
		// 获取同实体的骨架组件
		skeletonComp = getComponent(SkeletonComponent.class);
		if (skeletonComp == null) {
			throw new RuntimeException("NeonAnimator 需要 SkeletonComponent 才能工作！");
		}
	}

	/** 注册动画数据 */
	public void addAnimation(NeonAnimation anim) {
		animationLibrary.put(anim.name, anim);
	}

	/** 播放动画 (立即切换) */
	public void play(String name) {
		NeonAnimation anim = animationLibrary.get(name);
		if (anim == null) {
			System.err.println("Animator: 未找到动画 " + name);
			return;
		}

		// 如果已经在播放同一个动画，且是循环的，就不重置
		if (currentAnim == anim && isPlaying && anim.looping) return;

		this.currentAnim = anim;
		this.timer = 0f;
		this.isPlaying = true;
	}

	/** 停止播放 */
	public void stop() {
		this.isPlaying = false;
	}

	@Override
	public void update(float delta) {
		if (!isPlaying || currentAnim == null || skeletonComp == null) return;

		// 1. 更新时间
		timer += delta * speed;

		// 2. 处理循环
		if (timer >= currentAnim.duration) {
			if (currentAnim.looping) {
				timer %= currentAnim.duration; // 循环：取余
			} else {
				timer = currentAnim.duration; // 不循环：定格在最后
				// isPlaying = false; // 可选：自动停止
			}
		}

		// 3. 驱动骨骼 (Apply)
		applyAnimationToSkeleton();
	}

	private void applyAnimationToSkeleton() {
		NeonSkeleton skeleton = skeletonComp.getSkeleton();

		// 遍历当前动画的所有时间轴
		for (NeonTimeline timeline : currentAnim.timelines) {
			// A. 找到目标骨骼
			NeonBone bone = skeleton.getBone(timeline.boneName);
			if (bone == null) continue; // 骨架里没这根骨头，忽略

			// B. 计算当前值
			float value = timeline.evaluate(timer);

			// C. 写入属性 (Property Mapping)
			switch (timeline.property) {
				case X: bone.x = value; break;
				case Y: bone.y = value; break;
				case ROTATION: bone.rotation = value; break;
				case SCALE_X: bone.scaleX = value; break;
				case SCALE_Y: bone.scaleY = value; break;
			}
		}

		// 注意：这里只改了 x/y/rot，
		// 最终的 worldTransform 计算由 SkeletonComponent.update() 在稍后统一执行
	}
}
