# EditorGameScreen 升级执行案 (Upgrade Execution Plan) - V3

## 1. 目标 (Goal)
将 `IconEditorDemo` 的 **场景树 (Hierarchy)** 和 **属性面板 (Inspector)** 完整集成到 `EditorGameScreen` 中。
(核心编辑器交互功能如 Gizmo、选择、撤销重做已在之前阶段完成并验证)。

## 2. 现状分析 (Current Status Analysis)

*   **已完成 (Completed)**:
    *   **核心系统**: `CommandManager`, `SceneManager`, `GizmoSystem` 已集成。
    *   **基础渲染**: Gizmo 和选择高亮已实现。
    *   **输入交互**: 
        *   选择逻辑 (`EditorInput.touchDown` 委托) 已修复。
        *   Gizmo 操作 (MRS) 和 撤销/重做 (< >) 已工作。
        *   相机拖拽与 Gizmo 操作的冲突已通过 `isEditorHandling` 标志解决。
    *   **数据适配**: `GObjectAdapter` 已就绪。

*   **待完成 (To Do)**:
    *   **UI 布局重构**: 当前 UI 仅包含两个 ViewWidget (Game/Scene) 和简单的 Toolbar。需要重新划分区域以容纳 Hierarchy 和 Inspector。
    *   **Hierarchy Tree**: `updateSceneHierarchy()` 方法目前为空，导致左侧没有场景树。
    *   **Inspector Panel**: `updatePropertyPanel()` 方法目前为空，导致右侧没有属性面板。

## 3. 执行步骤 (Step-by-Step Implementation Plan)

### 第一阶段：UI 布局重构 (Phase 1: UI Layout Refactoring)
*   **目标**: 创建一个 "BorderLayout" 风格的布局。
*   **实现**:
    *   修改 `EditorController.createSceneWindow` (或 `create` 方法中的整体布局)。
    *   使用 `VisTable` 或 `MultiSplitPane` 将屏幕划分为：
        *   **Left**: Hierarchy Tree (宽 ~250px)
        *   **Center**: Scene View & Game View (Tab 切换或上下分屏)
        *   **Right**: Inspector (宽 ~300px)
    *   为 Hierarchy 和 Inspector 预留 `VisTable` 容器 (`hierarchyTable`, `inspectorTable`)。

### 第二阶段：场景树集成 (Phase 2: Hierarchy Integration)
*   **目标**: 实现 `updateSceneHierarchy()`。
*   **实现**:
    *   在 `hierarchyTable` 中创建并维护一个 `VisTree`。
    *   **数据源**: 遍历 `sceneManager.getRoot()`。
    *   **节点构建**: 为每个 `EditorTarget` 创建 `UiNode` (复用 `IconEditorDemo` 的 `UiNode` 类)。
    *   **交互**: 
        *   点击树节点 -> 调用 `sceneManager.selectNode()`。
        *   监听 `EditorListener.onStructureChanged` -> 重建树。
    *   **同步**: 当在 3D 视图中点击物体时，树节点应自动高亮 (expand & select)。

### 第三阶段：属性面板集成 (Phase 3: Inspector Integration)
*   **目标**: 实现 `updatePropertyPanel()`。
*   **实现**:
    *   在 `EditorController` 中持有 `Inspector` 实例 (需确保 `Inspector` 类可访问且无 `IconEditorDemo` 强依赖)。
    *   **数据源**: `sceneManager.getSelection()`。
    *   **构建**: 当 `onSelectionChanged` 触发时，调用 `inspector.build(inspectorTable, selection)`。
    *   **双向绑定**: 确保 Inspector 修改属性时触发 `Command`，并刷新 Scene View。

### 第四阶段：最终验证 (Phase 4: Final Verification)
*   **验证清单**:
    1.  **布局**: 左右侧面板显示正常，中间是游戏/场景视图。
    2.  **树操作**: 点击树节点，场景中物体被选中并显示 Gizmo。
    3.  **属性操作**: 修改 Inspector 数值，物体实时变化；撤销操作，Inspector 数值回滚。
    4.  **同步**: 在场景中点击物体，树节点自动选中。

---
**确认**: 计划已更新。请批准执行。
