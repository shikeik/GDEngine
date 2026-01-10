# IconEditorDemo 技术文档与集成指南

## 1. IconEditorDemo 技术文档

### 1.1 概述 (Overview)
`IconEditorDemo` 是一个基于 LibGDX 和 VisUI 构建的矢量图标设计器原型。它不仅是一个独立的工具演示，更包含了一套完整的编辑器核心逻辑（Gizmo、Command、SceneGraph），旨在被复用于游戏场景编辑器 (`EditorGameScreen`) 中。

### 1.2 核心架构 (Core Architecture)
项目采用类似 MVC 的分层架构：

*   **SceneManager (Model)**: 
    *   管理场景图（Scene Graph），维护根节点 `Root` 和当前选中项 `Selection`。
    *   负责节点的增删改查结构操作。
    *   提供观察者模式 (`EditorListener`) 通知外部结构或选择的变化。
*   **GizmoSystem (View/Controller)**: 
    *   提供可视化编辑手柄（移动、旋转、缩放）。
    *   负责渲染 Gizmo 图形（箭头、圆环）。
    *   处理鼠标拾取（Raycasting）和变换逻辑，将用户拖拽转化为 `EditorTarget` 的属性变更。
*   **CommandManager (Controller)**: 
    *   实现撤销/重做（Undo/Redo）机制。
    *   所有对模型的修改（变换、属性修改、结构变更）都封装为 `ICommand` 接口。
*   **Inspector (View)**: 
    *   属性面板，反射或绑定 `EditorTarget` 的属性供用户修改。
    *   支持双向绑定：UI 修改 -> Command -> Model 更新 -> UI 刷新。

### 1.3 数据抽象 (Data Abstraction)
为了让编辑器逻辑（Gizmo 等）能同时操作“图标节点”和“游戏实体”，系统定义了 `EditorTarget` 接口。任何实现了此接口的对象都可以被 GizmoSystem 操作。

*   **EditorTarget 接口**: 定义了 `getX`, `setX`, `getRotation`, `getChildren` 等基础方法。
*   **BaseNode**: `IconEditorDemo` 专用的数据模型（用于画图标），实现了 `EditorTarget`。
*   **GObjectAdapter**: (集成用) 用于适配 ECS `GObject` 的包装器，将 `GObject` 伪装成 `EditorTarget`。

### 1.4 使用教程 (User Guide)

#### 1.4.1 基础操作
*   **创建节点**: 在 Hierarchy 面板点击 `+` 按钮，选择形状（Circle, Rect, Group）。
*   **选择物体**: 点击 Hierarchy 树中的节点，或直接在场景视图中点击物体。
*   **变换操作**:
    *   **移动 (Move)**: 按 `M` 键或点击工具栏移动图标。拖动红色/绿色箭头沿轴移动，或拖动中心方块自由移动。
    *   **旋转 (Rotate)**: 按 `R` 键或点击工具栏旋转图标。拖动蓝色圆环进行旋转。
    *   **缩放 (Scale)**: 按 `S` 键或点击工具栏缩放图标。拖动方块手柄进行缩放。
*   **层级管理**: 在 Hierarchy 中拖拽节点，可以改变父子关系（Reparenting）。

#### 1.4.2 属性编辑
*   选中物体后，右侧 **Inspector** 面板会显示其属性。
*   **常规属性**: 修改 X, Y, Scale, Rotation, Name。
*   **图形属性**: 修改 Color (R, G, B, A)，Width, Height, Radius 等。
*   **撤销/重做**: 所有的编辑操作均可撤销。点击工具栏 `<` `>` 按钮或使用快捷键（需绑定）。

#### 1.4.3 项目管理
*   **保存**: 编辑器自动保存或手动点击保存（视具体实现而定，当前 Demo 使用 JSON 序列化）。
*   **加载**: 在左侧 File Tree 中双击 `.json` 文件加载项目。

---

## 2. EditorGameScreen 集成指南

本指南详细说明如何将 `IconEditorDemo` 的编辑能力移植到 `EditorGameScreen`，实现对游戏世界中实体（Entity/GObject）的可视化编辑。

### 2.1 集成目标
利用 `GizmoSystem`, `SceneManager`, `CommandManager` 来操作 ECS 架构中的 `GObject`，复用已有的拖拽、变换和属性编辑功能。

### 2.2 详细步骤

#### 步骤 1：核心组件注入 (Core Injection)
在 `EditorController` 中初始化编辑器核心系统。这些系统将独立于游戏逻辑运行，但在渲染和输入上与游戏循环交织。

```java
// EditorController.java
public class EditorController {
    // ... 原有变量 ...
    
    // 编辑器核心组件
    private CommandManager commandManager;
    private SceneManager sceneManager;
    private GizmoSystem gizmoSystem;
    
    // 输入处理器
    private SceneViewInputProcessor editorInput; 

    public void create() {
        // 1. 初始化核心
        commandManager = new CommandManager();
        sceneManager = new SceneManager(commandManager);
        gizmoSystem = new GizmoSystem(sceneManager, commandManager);
        
        // 2. 初始化输入 (需解耦，见步骤 3)
        // 注意：这里需要传入 Scene 摄像机和 ViewWidget 用于坐标转换
        editorInput = new SceneViewInputProcessor(sceneManager, gizmoSystem, commandManager, sceneCamera, viewWidget);
        
        // 3. 注册到 InputMultiplexer
        // 确保 editorInput 在 gameInput 之前，以便拦截编辑器操作
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(editorInput); 
        // ...
    }
}
```

#### 步骤 2：数据适配 (Data Adaptation)
确保 `GObjectAdapter` 完整实现 `EditorTarget` 接口。

*   **Wrapper Cache**: 必须维护 `GObject` <-> `GObjectAdapter` 的映射（`GObjectWrapperCache`），确保同一个 `GObject` 总是返回同一个 `Adapter` 实例，否则 `==` 判断会失效，导致 Gizmo 无法识别选中项。
*   **同步机制**: `Adapter` 的 Setter 方法应直接修改 `GObject` 的 Component（如 `TransformComponent`），并处理必要的脏标记（Dirty Flag）。

#### 步骤 3：输入系统解耦 (Input Decoupling)
原 `IconEditorDemo` 中的 `EditorInput` 类强依赖于 `IconEditorDemo` 实例（获取 Camera 等）。需要重构或新建 `SceneViewInputProcessor`。

**SceneViewInputProcessor 职责**:
1.  **坐标转换 (Unproject)**: 将屏幕触点（Screen Coords）转换为 FBO 内的世界坐标（World Coords）。
    *   使用 `viewWidget.localToStageCoordinates` 和 `stageToScreen` 获取 Viewport 偏移。
    *   使用 `sceneCamera.unproject` 将 Viewport 坐标转为世界坐标。
2.  **事件分发**:
    *   **Gizmo 操作**: 优先调用 `gizmoSystem.handleInput()`。如果 Gizmo 捕获了事件（如鼠标悬停在箭头上），则消耗该事件。
    *   **选择操作**: 如果 Gizmo 未捕获，进行射线检测（Raycast），找到鼠标下的 `GObject`。
    *   **转换**: 找到 `GObject` 后，通过 `GObjectWrapperCache` 获取 Adapter，调用 `sceneManager.selectNode(adapter)`。

#### 步骤 4：渲染集成 (Rendering Integration)
在 `EditorController.render()` 的场景渲染阶段（Scene FBO 绘制块内）注入 Gizmo 渲染。

```java
// EditorController.java -> render()
sceneTarget.renderToFbo(() -> {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    
    // 1. 渲染游戏世界 (Sprite, Map, Particles...)
    gameRenderSystem.update(delta);
    
    // 2. 渲染 Gizmo (画在最上层，不被游戏物体遮挡)
    neonBatch.setProjectionMatrix(sceneCamera.combined);
    neonBatch.begin();
    // 传入 zoom 以保持 Gizmo 在不同缩放级别下大小一致
    gizmoSystem.render(neonBatch, sceneCamera.zoom); 
    neonBatch.end();
    
    // 3. 渲染调试线框 (可选)
    if (debugMode) drawDebugRects();
});
```

#### 步骤 5：UI 面板复用 (UI Reuse)
1.  **Inspector 集成**:
    *   在 `EditorGameScreen` 的 UI 布局中放置 `Inspector` 面板。
    *   初始化 `Inspector` 时传入 `EditorController` 的 `sceneManager`。
    *   当 `sceneManager` 选中项变化时，Inspector 会自动刷新。
    *   *注意*: 需要为 `GObjectAdapter` 编写特定的 Inspector 生成逻辑，或者让 Adapter 实现通用的属性反射接口。

2.  **Hierarchy 集成**:
    *   创建 `EntityTree` 面板，监听 `GameWorld` 的实体列表变化。
    *   当点击树节点时，调用 `sceneManager.selectNode(adapter)` 同步选中状态。

### 2.3 注意事项
*   **坐标系一致性**: 确保 Gizmo 的渲染坐标系与游戏世界坐标系一致（Y-up vs Y-down，单位比例等）。
*   **Input 穿透**: 确保 UI 点击不会误触场景中的物体。VisUI 通常会拦截 Input，但自定义的 `SceneViewInputProcessor` 需要小心处理。
*   **性能**: `GObjectAdapter` 应该是轻量级的，不要在每一帧都创建新的 Adapter。

