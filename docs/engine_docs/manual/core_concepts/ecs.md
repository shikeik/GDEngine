# ECS 架构详解

GDEngine 采用标准的 **Entity-Component-System** 架构，与 Unity 的 GameObject/MonoBehaviour 类似，但更轻量。

## 1. Entity (实体)
对应类：`GObject`
*   场景中的基本对象。
*   本身没有逻辑，只是组件的容器。
*   支持层级关系 (`setParent`), 也就是 Scene Graph。

## 2. Component (组件)
对应类：`Component`
*   **数据**的主要载体。
*   继承自 `Component`。
*   常用内置组件：
	*   `TransformComponent`: 位置、旋转、缩放。
	*   `SpriteComponent`: 显示图片。
	*   `NeonAnimatorComponent`: 播放骨骼动画。

## 3. System (系统)
对应类：`BaseSystem`
*   **逻辑**的执行者。
*   系统每帧遍历所有包含特定组件的实体，并执行逻辑。
*   例如：`SkeletonSystem` 会自动查找所有带有 `SkeletonComponent` 的实体并更新骨骼矩阵。

## 4. 常用 API
```java
// 创建实体
GObject player = new GObject("Player");

// transform操作
player.transform.position.set(100, 100);

// 添加组件
SpriteComponent sprite = player.addComponent(SpriteComponent.class);

// 获取组件
TransformComponent trans = player.getComponent(SpriteComponent.class);
```
