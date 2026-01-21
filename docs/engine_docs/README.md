# GDEngine 开发者中心

**GDEngine** 是一个基于 LibGDX 的现代化 2D 游戏引擎，专注于**跨平台热重载开发**与**原生级性能**。

### 核心特性
*   **双端开发：** 支持 Windows (PC) 与 Android (手机) 直接运行编辑器。
*   **ECS 架构：** 基于实体-组件-系统的高性能架构。
*   **Neon 动画：** 内置高性能骨骼动画系统。
*   **热重载：** 修改 Java 代码后，无需重启即可在编辑器内实时生效。

### 快速导航
*   [🚀 快速开始](manual/getting_started/install.md)
*   [🧩 核心概念](manual/core_concepts/ecs.md)
*   [💀 骨骼动画](manual/systems/neon_animation.md)
*   [📚 API 参考](javadoc/index.html)
*   [📅 更新日志](changelog/README.md)


## 公告处

### 文档当前基本为AI撰写, 幻觉问题请注意: 实际以代码为准

---

[当前最全的引擎概览(AI撰写)](manual/GDEngine_项目概览报表.md)

### 当前bug罗列
- [已解决] ~~用户项目必须含有一个自定义组件继承任何Component及其子类否则AddCompDialog清单将不触发刷新，显示为空~~