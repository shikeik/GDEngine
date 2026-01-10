# 自动化执行策划案 (Execution Plan)

## 1. 启动与流程 (Startup & Flow)
- [x] **启动屏逻辑**: 修改 `GdxLauncher`，将默认启动屏设为 `ExampleSelectScreen`，但在初始化完成后立即跳转至 `SoloEditorScreen`。
- [x] **返回功能**: 在 `SoloEditorScreen` 中确保有路径返回 `ExampleSelectScreen` (或通过重启/Back键)。

## 2. 屏幕与布局 (Screen & Layout)
- [x] **横屏强制**: 在 `SoloEditorScreen` 中覆盖 `getOrientation` 返回 `Landscape`。
- [x] **布局修复**: 确保 `SceneViewPanel` 和 `GameViewPanel` 在横屏下正确填充，修复被拉伸的问题。

## 3. 渲染修复 (Rendering Fixes)
- [x] **Scene View**:
    - 修复鼠标中键/右键拖动时的 "跟随" 逻辑 (Screen to World delta 转换)。
    - 修复画面拉伸 (Aspect Ratio in Camera)。
- [x] **Game View**:
    - 修复 "完全没有东西" 的问题 (确保 `GameWorld.worldCamera` 被正确更新和使用)。
    - 确保 `ScissorStack` 不会错误裁剪。

## 4. UI 还原 (UI Restoration)
- [x] **MRS Toolbar**:
    - 在顶部工具栏增加 Select, Move, Rotate, Scale 按钮。
    - 绑定快捷键 (Q, W, E, R)。
- [x] **指令历史**: 确保 `HistoryPanel` 功能正常 (CommandManager 绑定)。
- [x] **停靠系统**: 检查 `DockableWindowManager` 的交互逻辑，确保窗口嵌套分割 (Nested Split) 功能可用。

## 5. 自动化流水线 (Automation Pipeline)
创建一个自动化脚本 `EditorAutomation`，在 `SoloEditorScreen` 启动后自动执行以下操作并输出 Log：

1.  **初始化检查**:
    - 验证 `HierarchyPanel`, `InspectorPanel`, `SceneViewPanel`, `GameViewPanel` 是否存在。
    - 验证初始实体 (2个角色 + tempBack 背景) 是否加载。

2.  **GObject 操作模拟**:
    - **Create**: 创建一个新的 GObject "AutoTest_Obj"。
    - **Update**: 修改其 Transform (位置/旋转)。
    - **Component**: 添加 `SpriteComponent`，并在 Inspector 中验证同步。
    - **Delete**: 删除该对象。

3.  **MRS 工具栏模拟**:
    - 点击 Toolbar 上的 Move/Rotate/Scale 按钮，验证 `GizmoSystem` 状态切换。

4.  **移动模拟 (WASD)**:
    - 聚焦 `GameView`，发送 WASD 按键事件，验证角色位置变化。
    - 聚焦 `SceneView`，发送 WASD (摄像机漫游)，验证摄像机位置变化。

5.  **输出反馈**:
    - 每一步操作成功后输出 `[AUTOMATION] SUCCESS: ...`
    - 失败则输出 `[AUTOMATION] FAIL: ...`
