# ECS 架构详解

GDEngine 的 ECS (Entity-Component-System) 实现了数据与逻辑的分离，但保留了类似 Unity 的层级图（Scene Graph）易用性。

## 1. Entity (实体)
**对应类：** `com.goldsprite.gdengine.ecs.entity.GObject`

实体是场景中的基本单元。
*   **强制组件：** 每个 `GObject` 在构造时会自动添加一个 `TransformComponent`，不可移除。
*   **层级关系：** 支持父子嵌套 (`setParent`)。子物体的变换（位移/旋转/缩放）是相对于父物体的。
*   **生命周期：** 由 `GameWorld` 的 `rootEntities` 列表管理顶层物体，子物体由父级递归驱动。

## 2. Component (组件)
**对应类：** `com.goldsprite.gdengine.ecs.component.Component`

组件是数据的载体，也可以包含针对单个物体的逻辑。
*   **依赖注入：** 组件被添加时，会自动注入 `gobject` 和 `transform` 引用。
*   **多态支持：** `getComponent(Class)` 支持查找子类组件。
*   **核心组件：**
	*   `TransformComponent`: 核心变换，维护 Local 和 World 两套矩阵。
	*   `SpriteComponent`: 基础图片渲染。
	*   `NeonAnimatorComponent`: 骨骼动画控制器。

## 3. System (系统)
**对应类：** `com.goldsprite.gdengine.ecs.system.BaseSystem`

系统负责处理特定类型的组件集合。

### 注册与筛选
系统在实例化时会自动注册到 `GameWorld`。系统必须通过注解声明它关心的组件：

```java
@GameSystemInfo(
    type = SystemType.UPDATE,
    interestComponents = { SkeletonComponent.class }
)
public class SkeletonSystem extends BaseSystem {
    @Override
    public void update(float delta) {
        // 这里的 getInterestEntities() 会利用 ComponentManager 的缓存
        // 仅返回持有 SkeletonComponent 的实体，O(1) 复杂度
        List<GObject> targets = getInterestEntities();
        for (GObject obj : targets) {
            // ...
        }
    }
}
```

### 内置核心系统
*   **SceneSystem:** 驱动所有 `GObject` 的 `update` 和生命周期（Start/Destroy）。
*   **WorldRenderSystem:** 收集所有 `RenderComponent`，按 Layer 排序并绘制。


### 常用 API
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
