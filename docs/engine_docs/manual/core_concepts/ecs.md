# ECS 架构详解

GDEngine 实现了高性能的 ECS (Entity-Component-System) 架构，同时为了易用性，保留了类似 Unity 的 Scene Graph (场景图) 接口。

## 核心三要素

### 1. Entity (实体)
对应类：`com.goldsprite.gdengine.ecs.entity.GObject`

*   **Transform 绑定**：所有 `GObject` 天生拥有 `transform` (`TransformComponent`)，不可移除。
*   **层级系统**：支持 `setParent(GObject parent)`。
    *   子物体的 `worldPosition` = 父物体变换矩阵 * 子物体局部变换。
    *   这使得你可以像传统引擎一样构建复杂的物体层级。

### 2. Component (组件)
对应类：`com.goldsprite.gdengine.ecs.component.Component`

*   **数据载体**：组件主要用于存储数据（如 `SpriteComponent` 存储图片路径和颜色）。
*   **逻辑载体**：组件也可以包含逻辑 (`update`)，但这是为了方便。在纯粹 ECS 中，逻辑应由 System 处理。
*   **自动注入**：组件实例化时，会自动获得 `gobject` 和 `transform` 的引用。

### 3. System (系统)
对应类：`com.goldsprite.gdengine.ecs.system.BaseSystem`

系统负责处理特定的逻辑。它通过筛选器（Filter）来获取它关心的实体。

**示例：编写一个旋转系统**

```java
// 声明：这是一个 UPDATE 类型的系统，且只关心持有 TransformComponent 的物体
@GameSystemInfo(
    type = SystemType.UPDATE,
    interestComponents = { TransformComponent.class }
)
public class RotateSystem extends BaseSystem {
    
    @Override
    public void update(float delta) {
        // getInterestEntities() 利用缓存实现了 O(1) 获取
        List<GObject> targets = getInterestEntities();
        
        for (GObject obj : targets) {
            obj.transform.rotation += 90 * delta;
        }
    }
}
```

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

## 性能优化机制 (实证)

根据 `ComponentManager.java` 源码：
*   **BitSet 掩码**：每个实体都有一个 `BitSet` 签名，表示它拥有哪些组件。
*   **查询缓存**：系统查询的结果会被缓存。当实体的组件发生增删时，管理器会进行**增量更新 (Incremental Update)**，而不是全量重新扫描。


# 重要补充！
当前GameWorld默认内置系统只有SceneSystem与WorldRenderSystem, 如需运行其他逻辑：例如动画更新，或任何自定义系统逻辑请在入口类处自行实例化系统(自动注册)