# Undo/Redo 系统实现计划

我将分步为您实现 Undo/Redo 系统，包括核心逻辑、UI 面板集成以及对现有编辑器的改造。

## 1. 核心 Command 架构
在 `core` 模块中建立通用的命令模式基础：
- **`ICommand` 接口**: 定义 `execute()`, `undo()`, `getName()` 等方法。
- **`CommandManager` 类**: 
    - 管理 Undo/Redo 栈。
    - 提供 `executeCommand(cmd)` 方法。
    - 提供监听器接口，以便 UI 更新。

## 2. 修改 UI 组件
- **`SmartNumInput` 改造**: 
    - 增加 `onCommand(oldVal, newVal)` 回调机制。
    - 确保在拖拽开始时记录初始值，拖拽结束时触发 Command，同时保留实时预览功能。
- **`CommandHistoryUI`**:
    - 确保其能被正确集成到主界面中。

## 3. 改造编辑器 (`IconEditorDemo`)
### A. 定义具体命令 (作为内部静态类或独立类)
1. **`TransformCommand`**: 记录对象的位置、旋转、缩放变化 (用于 Gizmo 操作)。
2. **`PropertyChangeCommand`**: 通用属性修改 (用于 Inspector)。
3. **`StructureCommand`**: 处理节点的添加、删除。
4. **`HierarchyMoveCommand`**: 处理节点在层级树中的拖拽移动/重排。

### B. 接入命令系统
- **Gizmo 交互 (`EditorInput`)**:
    - `touchDown`: 记录对象初始状态。
    - `touchUp`: 如果状态改变，提交 `TransformCommand`。
- **属性面板 (`Inspector`)**:
    - 使用改造后的 `SmartNumInput`，在数值变更确认后提交 `PropertyChangeCommand`。
- **场景管理 (`SceneManager`)**:
    - 将 `addNode`, `deleteNode`, `moveNode` 等方法的内部逻辑封装进 Command，原方法改为调用 `commandManager.execute(...)`。

### C. UI 集成
- **Toolbar**: 在顶部工具栏增加 Undo/Redo 按钮，绑定 `CommandManager`。
- **History Panel**: 
    - 在界面右侧或底部加入 `CommandHistoryUI`。
    - 添加一个开关按钮 (Toggle) 来控制面板的显示/隐藏。
    - 监听 `CommandManager` 的事件，实时更新历史列表。

## 4. 验证
- 验证 Gizmo 拖拽操作的 Undo/Redo 是否准确还原。
- 验证 Inspector 数值修改的 Undo/Redo。
- 验证节点添加、删除、层级移动的 Undo/Redo。
- 检查历史面板的显示和更新。
