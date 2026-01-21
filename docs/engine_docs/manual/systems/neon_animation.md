# Neon 骨骼动画(未完全审查)

Neon 是 GDEngine 内置的轻量级 2D 骨骼动画系统，旨在提供高性能的代码驱动动画能力。

## 核心概念

*   **Skeleton (骨架):** 由 Bone (骨骼) 和 Slot (插槽) 组成的层级结构。
*   **Skin (皮肤):** 插槽上的显示内容 (如图片、几何体)。
*   **Animation (动画):** 控制骨骼属性 (旋转、位移、缩放) 的时间轴数据。

## 核心组件

*   `SkeletonComponent`: 维护骨架数据 (`NeonSkeleton`)，负责骨骼层级计算。
*   `NeonAnimatorComponent`: 动画状态机，负责播放 `NeonAnimation` 并混合属性。

## 支持的特性 (实证)

根据 `NeonCurve.java` 和 `NeonProperty.java`：

1.  **插值曲线**:
    *   `LINEAR`: 线性匀速。
    *   `SMOOTH`: 平滑插值 (Ease-in-out)。
    *   `STEPPED`: 阶梯突变 (无过渡)。

2.  **动画属性**:
    *   `X`, `Y`: 局部位移。
    *   `ROTATION`: 局部旋转 (角度制)。
    *   `SCALE_X`, `SCALE_Y`: 局部缩放。
    *   `SPRITE`: **(泛型关键帧)** 切换插槽图片，用于实现帧动画或换装。

## 代码创建动画示例

Neon 最大的特点是可以通过代码动态构建动画（Procedural Animation）：

```java
// 1. 获取组件
NeonAnimatorComponent anim = entity.addComponent(NeonAnimatorComponent.class);

// 2. 创建动画片段
NeonAnimation attack = new NeonAnimation("Attack", 0.5f, false); // 0.5秒，不循环

// 3. 添加时间轴 (控制 "Arm" 骨骼的旋转)
// 参数: 时间点, 属性值, 曲线类型
NeonTimeline armRot = new NeonTimeline("Arm", NeonProperty.ROTATION);
armRot.addKeyframe(0.0f, 0f, NeonCurve.SMOOTH);
armRot.addKeyframe(0.2f, 90f, NeonCurve.SMOOTH); // 挥砍
armRot.addKeyframe(0.5f, 0f, NeonCurve.SMOOTH);  // 收招

attack.addTimeline(armRot);

// 4. 注册并播放
anim.addAnimation(attack);
anim.play("Attack");
```

## 动画混合 (CrossFade)

`NeonAnimatorComponent` 支持在两个动画之间进行平滑过渡：

```java
// 在 0.3秒 内从当前动作混合过渡到 "Idle" 动作
animator.crossFade("Idle", 0.3f);
```

## JSON 导入
支持从 JSON 格式导入动画数据，配合编辑器的 "Live JSON Edit" 功能可实时预览。


## 重要
当前底层实现基本完善，但api封装较为原始，使用繁琐(包括Sprite帧动画也是如此)