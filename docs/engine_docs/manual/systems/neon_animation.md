# Neon 骨骼动画(未审查)

Neon 是 GDEngine 内置的高性能 2D 骨骼动画系统。

## 核心概念
*   **Skeleton (骨架):** 由 Bone (骨骼) 和 Slot (插槽) 组成的层级结构。
*   **Skin (皮肤):** 插槽上的显示内容 (如图片、几何体)。
*   **Animation (动画):** 控制骨骼属性 (旋转、位移、缩放) 的时间轴数据。

## 代码创建动画
目前支持通过代码动态构建动画（适合程序化动画）：

```java
// 1. 获取组件
NeonAnimatorComponent anim = player.addComponent(NeonAnimatorComponent.class);

// 2. 定义动画
NeonAnimation run = new NeonAnimation("Run", 1.0f, true);

// 3. 添加轨道 (控制 Body 骨骼的旋转)
// 参数: 时间, 值, 时间, 值...
TestAnimationFactory.addTrack(run, "Body", NeonProperty.ROTATION, 
    0.0f, 0f, 
    0.5f, 45f, 
    1.0f, 0f
);

// 4. 注册并播放
anim.addAnimation(run);
anim.play("Run");
```

## JSON 导入
支持从 JSON 格式导入动画数据，配合编辑器的 "Live JSON Edit" 功能可实时预览。
