# SoloEditor 策划与设计案

## 1. 概述 (Overview)
本案旨在为 **SoloFight** 打造一个全功能的编辑器屏幕 (`SoloEditorScreen`)。该编辑器将集结 **NFightGame** 的 `MapEditor` 布局与功能，以及 **SoloFight** 现有的 `IconEditorDemo` 编辑核心，并完全基于 SoloFight 的底层技术栈（ECS, NeonBatch）实现。

## 2. 核心目标 (Core Objectives)
1.  **复刻布局**: 还原 `MapEditorScreen` 的经典布局（嵌套可停靠窗口）。
2.  **底层替换**: 摒弃 `NFightGame` 的旧底层，全面对接 `SoloFight` 的 `GameWorld` (ECS) 和 `NeonBatch` (渲染)。
3.  **功能融合**: 集成 `IconEditorDemo` 的 Gizmo（变换手柄）、Command（撤销重做）和 SceneManager。
4.  **数据支持**: 实现 `GObject` 的 JSON 序列化与反序列化，支持项目保存与加载。

## 3. 屏幕布局 (Layout Specification)

编辑器采用 **嵌套可停靠窗口系统 (Nested Dockable Window System)**，整体布局如下：

### 3.1 顶部 (Top)
*   **Toolbar (工具栏)**:
    *   包含常用操作：保存 (Save)、加载 (Load)。
    *   编辑模式切换：选择 (Select)、移动 (Move)、旋转 (Rotate)、缩放 (Scale)。
    *   运行控制：播放 (Play)、暂停 (Pause)、步进 (Step)。

### 3.2 左侧 (Left Dock)
*   **左上 (Left Top) - Hierarchy (层级树)**:
    *   显示当前场景 (`GameWorld`) 中的所有 `GObject`。
    *   支持拖拽改变父子关系 (Reparenting)。
    *   与 Scene 视图和 Inspector 联动（点击选中）。
*   **左下 (Left Bottom) - File Tree (文件树)**:
    *   显示项目资源目录 (`assets/`).
    *   支持文件拖拽到场景或属性面板（如拖拽图片创建 Sprite）。
    *   **核心功能**: `GObject` 的 JSON 序列化/反序列化，支持双击打开 Prefab 或 Scene 文件。

### 3.3 右侧 (Right Dock)
*   **Inspector (检查器)**:
    *   显示当前选中 `GObject` 的详细信息。
    *   **组件式编辑**: 自动列出并编辑挂载的 Components (Transform, Sprite, Script 等)。
    *   支持修改属性并实时反馈到 Scene 视图。
*   **History Panel (操作历史面板)**:
    *   **位置**: 依附于 Inspector 的左下方 (或作为 Inspector 的附属面板)。
    *   **功能**: 显示 `CommandManager` 的操作栈。
    *   支持点击历史记录回滚 (Undo/Redo) 到任意状态。

### 3.4 中间 (Center - Sandwiched)
中间区域被左右面板夹在中间，包含主要的视图窗口：

*   **左上 (Center Top-Left) - Scene View (场景视图)**:
    *   **编辑模式**: 也就是 `IconEditorDemo` 的核心视图。
    *   显示网格 (Grid)。
    *   显示 Gizmos (移动/旋转/缩放手柄)。
    *   支持鼠标框选、点击拾取。
    *   摄像机漫游 (Pan/Zoom)。
*   **右上 (Center Top-Right) - Game View (游戏视图)**:
    *   **预览模式**: 渲染最终游戏画面 (`NeonBatch` 渲染)。
    *   显示 UI (`Stage`)。
    *   无 Gizmos 干扰，所见即所得。
*   **下方 (Center Bottom) - Console (控制台)**:
    *   显示系统日志 (Log, Warning, Error)。
    *   支持命令输入 (如有)。

## 4. 技术实现方案 (Technical Implementation)

### 4.1 窗口系统 (Window System)
*   **移植**: 将 `NFightGame` 中的 `com.goldsprite.dockablewindow` 模块移植到 `SoloFight` 的 `gd-editor` 模块中。
*   **集成**: 确保 Dockable 系统能与 `VisUI` 和 `Scene2D` 完美共存。

### 4.2 编辑核心 (Editor Core)
*   **复用 IconEditorDemo**:
    *   **GizmoSystem**: 直接迁移用于 Scene View 的物体操作。
    *   **CommandManager**: 作为全局撤销重做系统的核心。
    *   **SceneManager**: 改造为适配 `GameWorld` 的代理层。

### 4.3 ECS 对接 (ECS Integration)
*   **Hierarchy**: 监听 `GameWorld` 的实体添加/移除事件，实时更新树状图。
*   **Inspector**: 使用反射 (Reflection) 或 `Bean` 机制扫描 `Component` 的字段，生成对应的 UI 控件 (TextField, Checkbox, ColorPicker 等)。

### 4.4 序列化 (Serialization)
*   **Json**: 使用 LibGDX 的 `Json` 库。
*   **GObject**: 实现 `Json.Serializable` 接口或自定义 `JsonSerializer`。
    *   保存：遍历 `GObject` 及其 Components，保存属性。
    *   加载：读取 JSON，实例化 `GObject`，动态添加 Components 并恢复属性。

## 5. 开发计划 (Roadmap)

1.  **基础设施准备**:
    *   移植 `DockableWindow` 代码。
    *   建立 `SoloEditorScreen` 骨架。
2.  **布局搭建**:
    *   实现 Toolbar, Hierarchy, FileTree, Inspector, History Panel, Console 的 UI 容器。
    *   配置 Dockable 布局。
3.  **核心功能接入**:
    *   接入 `IconEditorDemo` 的 Gizmo 和 Command 系统。
    *   实现 Scene View 渲染。
4.  **数据流打通**:
    *   实现 ECS 到 Hierarchy/Inspector 的双向绑定。
    *   实现 JSON 序列化/反序列化。
