我将对 `IconEditorDemo.java` 进行重构，将所有内部类提取到独立的包结构中，并进行通用化设计以支持未来的扩展。

### 1. 包结构设计
我将在 `com.goldsprite.solofight.screens.tests` 下创建一个新的子包 `iconeditor`，并按功能模块划分：

*   **`com.goldsprite.solofight.screens.tests.iconeditor`**
    *   `IconEditorDemo.java` (主类保留或更新引用，作为上下文中心)
*   **`com.goldsprite.solofight.screens.tests.iconeditor.model`** (数据模型)
    *   `EditorTarget.java` (接口：抽象编辑器对象，为 ECS/GObject 预留适配接口)
    *   `BaseNode.java` (基础实现)
    *   `GroupNode.java`
    *   `RectShape.java`
    *   `CircleShape.java`
*   **`com.goldsprite.solofight.screens.tests.iconeditor.system`** (核心逻辑)
    *   `SceneManager.java` (场景图管理)
    *   `GizmoSystem.java` (变换手柄系统)
    *   `EditorInput.java` (输入处理器)
    *   `EditorListener.java` (事件接口)
*   **`com.goldsprite.solofight.screens.tests.iconeditor.ui`** (界面组件)
    *   `UiNode.java` (层级树节点，包含拖拽逻辑)
    *   `FileNode.java` (文件树节点)
    *   `Inspector.java` (属性面板)
    *   `InspectorStrategy.java` (属性面板策略接口)
*   **`com.goldsprite.solofight.screens.tests.iconeditor.commands`** (命令模式)
    *   `TransformCommand.java`
    *   `PropertyChangeCommand.java`
    *   `GenericPropertyChangeCommand.java`
    *   `ColorChangeCommand.java`
    *   `AddNodeCommand.java`
    *   `DeleteNodeCommand.java`
    *   `ReparentCommand.java`
*   **`com.goldsprite.solofight.screens.tests.iconeditor.utils`**
    *   `CameraController.java`

### 2. 通用化处理方案
*   **EditorTarget 抽象**：`EditorTarget` 将作为核心接口，解耦具体的数据实现。未来 ECS Entity 可以通过实现此接口（或通过 Adapter 模式）直接接入编辑器，而无需修改 `SceneManager` 或 `GizmoSystem`。
*   **依赖注入**：将 `IconEditorDemo` 中的私有组件（如 `NeonBatch`, `UiStage`, `DragAndDrop`）通过 Getter 方法暴露，并在子系统构造时注入 `IconEditorDemo` 实例作为 Context，解决循环依赖。

### 3. 执行步骤
1.  **创建目录结构**：在文件系统中建立上述包结构。
2.  **提取并修改类文件**：
    *   将内部类逐一写入新文件。
    *   修改类修饰符为 `public`。
    *   添加必要的 `import` 语句。
    *   将对 `IconEditorDemo` 私有字段的访问改为调用 Getter 方法。
3.  **重构主类**：
    *   删除 `IconEditorDemo.java` 中的所有内部类。
    *   添加必要的 Getter 方法 (`getSceneManager`, `getGizmoSystem`, `getDragAndDrop`, `markDirty` 等)。
    *   更新 `import` 以引用新位置的类。
4.  **验证**：确保代码编译通过且功能逻辑保持一致。