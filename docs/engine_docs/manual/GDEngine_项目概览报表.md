# GDEngine 项目概览报表 (修正版)

## 一、项目基本信息

| 属性 | 内容 | 实证来源 |
| :--- | :--- | :--- |
| **项目名称** | GDEngine | `settings.gradle` |
| **项目类型** | 基于 LibGDX 的跨平台 2D 游戏引擎 | `build.gradle` dependencies |
| **当前版本** | **v1.10.12.21** | `gradle.properties`: `projectVersion=1.10.12.21` |
| **开发状态** | 活跃开发中 | `changelog/README.md` 含 "In Development" |
| **JDK 版本** | **JDK 17** | `gradle.properties`: `jdkVersion=17` |

### 核心定位
专注于**跨平台（PC/Android）热重载开发**与**原生级性能**的 ECS 2D 引擎。

---

## 二、项目目录结构分析

### 2.1 顶层目录结构

| 目录/文件 | 功能说明 | 关键内容验证 |
| :--- | :--- | :--- |
| `android/` | Android 平台实现 | `AndroidGdxLauncher`, `AndroidScriptCompiler` (D8 dexing) |
| `core/` | 引擎核心代码 | ECS, Neon Animation, Editor MVP, UI Widgets |
| `lwjgl3/` | PC 桌面端实现 | `Lwjgl3Launcher`, `DesktopScriptCompiler` (ECJ+URLClassLoader) |
| `assets/` | 引擎内置资源 | 包含 `engine/libs` (核心库Jar), `templates` (工程模板) |
| `tests/` | 单元测试 | `GdxTestRunner`, ECS/FSM/Skeleton 覆盖测试 |
| `GDEngine/` | 运行时工作区 | `UserProjects`, `LocalTemplates`, `engine_config.json` |

### 2.2 核心代码结构修正 (`core/src/main/java/com/goldsprite/gdengine/`)

*   **`assets/`**: 包含 `CustomAtlasLoader` (支持 `#region` 语法) 和 `VisUIHelper` (中文字体加载)。
*   **`core/`**:
    *   `Gd.java`: 引擎 Facade，持有 `Input`, `Graphics`, `IScriptCompiler` 代理。
    *   `ComponentRegistry.java`: **基于 `project.index` 文件**的组件发现机制（非全量扫描）。
*   **`ecs/`**:
    *   `ComponentManager.java`: 使用 `BitSet` 和 `CopyOnWriteArrayList` 实现 O(1) 实体查询缓存。
    *   `GObject.java`: 使用 `LinkedHashMap` 存储组件以保持顺序。
*   **`neonbatch/`**: 自定义 `BaseShapeBatch`，支持多边形、贝塞尔曲线、Miter Join 描边。

> **【修正注记】**：原报表未提及 `neonbatch` 包含贝塞尔曲线和 Miter Join 算法，实际代码 `BaseShapeBatch.java` 中包含 `pathStroke` 和 `computeMiterOffset` 逻辑。

---

## 三、核心模块详细分析

### 3.1 ECS 架构模块

**核心类文件**：`core/.../ecs/`

| 类名 | 功能特性 | 代码实证 |
| :--- | :--- | :--- |
| `GObject` | 实体，构造时强制添加 `TransformComponent`。支持 `setParent` 层级。 | `GObject.java`: 构造函数内 `this.transform = new TransformComponent()` |
| `Component` | 组件基类，生命周期包括 `awake`, `start`, `update`, `destroy`。 | `Component.java`: `isAwake`, `isStarted` 均为 `transient` |
| `TransformComponent` | 维护 `localTransform` 和 `worldTransform` (Affine2 矩阵)。 | `TransformComponent.java`: `updateWorldTransform` 包含矩阵乘法 |
| `SystemType` | 位掩码枚举：`UPDATE`, `FIXED_UPDATE`, `RENDER`。 | `SystemType.java`: 使用位运算 `1 << 0` 等 |

**核心系统**：
*   **`SceneSystem`**: 处理 Start 队列、Update 循环、Destroy 队列。
*   **`SkeletonSystem`**: 专门负责在 Update 后更新骨骼矩阵。
*   **`WorldRenderSystem`**: 按 `sortingLayer` (LayerDepth) + `orderInLayer` 排序绘制。

### 3.2 Neon 骨骼动画模块

**核心类文件**：`core/.../ecs/skeleton/`

| 模块 | 修正后的特性 | 实证 (代码文件) |
| :--- | :--- | :--- |
| **曲线类型** | **LINEAR, STEPPED, SMOOTH** | `NeonCurve.java`: 枚举仅定义了这三种，**原报表提及的 CUBIC 为幻觉**。 |
| **动画属性** | X, Y, ROTATION, SCALE_X, SCALE_Y, **SPRITE** | `NeonProperty.java`: 包含 `SPRITE` 用于帧动画切换。 |
| **关键帧** | 支持 `float` (骨骼) 和 `Object` (Sprite) 混合存储 | `NeonKeyframe.java`: 含 `floatValue` 和 `objectValue`。 |
| **混合** | 支持 `CrossFade` | `NeonAnimatorComponent.java`: `mixTimer`, `applyToEntity` 含 lerp 逻辑。 |

### 3.3 编辑器模块 (MVP)

**核心类文件**：`core/.../screens/ecs/editor/`

**MVP 架构实证**：
*   **View**: `ScenePanel`, `HierarchyPanel` (负责 UI 构建, 继承 `EditorPanel`)。
*   **Presenter**: `ScenePresenter`, `HierarchyPresenter` (负责业务逻辑, 监听 `EditorEvents`)。
*   **Model**: `EditorSceneManager` (管理选中状态 `selection` 和命令 `CommandManager`)。

**编辑器特性修正**：
*   **FBO 渲染**：`ViewWidget` 配合 `ViewTarget` 实现将游戏画面渲染到 ImGUI 风格的窗口内，支持 `FIT/STRETCH/COVER` 模式。
*   **输入映射**：`EditorGameInput` 将屏幕坐标重映射回 FBO 像素坐标，实现“画中画”输入。
*   **状态管理**：`EditorState` (CLEAN, DIRTY, COMPILING) 控制编译按钮状态。

### 3.4 脚本编译模块 (热重载核心)

**核心类文件**：
*   `lwjgl3/.../DesktopScriptCompiler.java`
*   `android/.../AndroidScriptCompiler.java`

| 平台 | 编译流程 (实证) | 关键技术 |
| :--- | :--- | :--- |
| **PC (Desktop)** | `.java` -> ECJ -> `.class` -> `URLClassLoader` | 利用 `System.getProperty("java.class.path")` 获取依赖。 |
| **Android** | `.java` -> ECJ -> `.class` -> **D8** -> `.dex` -> `DexClassLoader` | D8 用于将 Class 转为 Dex 以便 Android 运行时加载 (非混淆用途)。 |

> **【修正注记】**：原报表称 D8 用于混淆，实际代码 `AndroidScriptCompiler.java` 中 D8 用于生成 `classes.dex` 并通过 `InMemoryDexClassLoader` (Android O+) 或文件加载，这是 Android 动态加载 Java 代码的必要步骤，而非为了混淆。

### 3.5 UI 系统模块

**核心类文件**：`core/.../ui/`

*   **智能输入控件**: `SmartInput<T>` 系列 (`SmartNumInput`, `SmartColorInput` 等) 支持数据绑定 (`bind(Supplier)`) 和命令模式 (`onCommand`)。
*   **代码编辑器**: `BioCodeEditor` 基于 `VisTextArea`，支持行号、高亮、自动缩进、右键菜单。
*   **富文本**: `RichText` 支持标签：
    *   颜色: `[color=red]`, `[#RRGGBB]`
    *   尺寸: `[size=32]`
    *   图片: `[img=path|WxH]` 或 `[img=path#region]` (支持 TexturePacker 图集引用)
    *   事件: `[event=xxx]`

---

## 四、文档系统 (Docsify 集成)

**文件**：`engine_docs/`

*   **动态侧边栏**: `index.html` 中包含自定义 JS (`SidebarBuilder`, `ChangelogService`)，能够：
    1.  解析 Markdown 生成目录树。
    2.  **动态注入更新日志**：读取 `changelog.json` 并嫁接到侧边栏。
    3.  **深度链接**：支持 `index.html#/changelog/README?target=v1.10.12` 定位到具体版本。
*   **更新日志生成**: `changelog-generator.gradle` 任务会自动读取 Git Tag 和 Commit Log 生成 JSON 数据。

---

## 五、依赖库清单 (修正版)

基于 `ProjectGradle.txt`:

| 库名 | 版本 | 用途 |
| :--- | :--- | :--- |
| **libGDX** | **1.12.1** | 游戏框架核心 |
| **VisUI** | **1.5.3** | 编辑器 UI 控件库 |
| **ECJ** | 4.6.1 (Android) / 3.33.0 (Desktop) | Eclipse Java 编译器 (用于运行时编译) |
| **R8/D8** | 8.2.33 | Android Dex 转换工具 |
| **Desugar** | 2.0.4 | Android Java 8+ 语法脱糖支持 |

---

## 六、生命周期与工作流 (修正版)

基于 `GameWorld.java` 和 `SceneSystem.java`:

1.  **Awake Phase**: `addComponent` -> `Component.awake()` (立即执行)。
2.  **Start Phase**: `GameWorld.update` -> `SceneSystem.executeStartTask()` -> `Component.start()` (仅 Play 模式第一帧)。
3.  **Fixed Update**: 物理循环 `accumulator >= FIXED_DELTA_TIME (1/60s)`。
4.  **Update**: 逻辑循环，编辑器模式下仅 `@ExecuteInEditMode` 组件执行。
5.  **Render**: `WorldRenderSystem` 渲染。
6.  **Destroy**: `SceneSystem.executeDestroyTask()` -> `Component.destroyImmediate()` (帧末统一清理)。

---

## 七、技术亮点总结

1.  **真正的双端编辑器**：Android 端 `AndroidGdxLauncher` 实现了完整的虚拟键盘、触屏手势适配和 D8 运行时编译，使得在手机上编写 Java 逻辑成为可能。
2.  **高性能 ECS 查询**：`ComponentManager` 使用 `BitSet` 签名匹配实体，查询复杂度为 O(1)，且实现了增量缓存更新。
3.  **Neon 混合动画**：`NeonAnimatorComponent` 支持骨骼属性（浮点）与 Sprite 属性（对象）的混合时间轴，且支持 `CrossFade` 动作融合。
4.  **无感输入映射**：编辑器中的 `ViewWidget` 能够将鼠标/触摸事件逆向投影回 FBO 内部的世界坐标，使得“画中画”游戏体验与全屏运行无异。
5.  **自动化文档流**：从 Git Commit 到 `changelog.json` 再到 Docsify 侧边栏的完全自动化闭环。

---

*报表生成时间：2026-01-22*
*数据来源：基于 ProjectTree.txt, ProjectGradle.txt, ProjectCode.txt, ProjectDocsCode.txt 的深度代码审计*