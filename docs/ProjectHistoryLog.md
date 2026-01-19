# GDEngine 更新日志

## [Overview] 总览
- 暂无

## [Plan] 未来规划路线图
### `v1.10.9.x` 资源与预制体 (Assets & Prefabs)
- [ ] **资源浏览器**: 实现 Project Window，扫描 assets 目录显示缩略图
- [ ] **拖拽交互**: 支持从资源窗口拖拽图片/文件到 Inspector 或 Scene
- [ ] **预制体系统**: 实现 `.prefab` 文件的序列化、保存与运行时实例化

### `v1.10.8.x` 物理系统集成 (Physics Integration)
- [ ] **物理引擎**: 集成 Box2D 物理世界
- [ ] **物理组件**: 实现 `RigidBodyComponent` (刚体) 和 `ColliderComponent` (碰撞体)
- [ ] **物理系统**: 实现 `PhysicsSystem`，在 FixedUpdate 中驱动物理模拟
- [ ] **可视化**: 在编辑器 Scene 视图中绘制物理碰撞线框 (Gizmos)

---

## [Current] 当前开发版本
### `1.10.11.x` 架构重构与模式分离 (Architecture & Modes)
**核心架构 (Core Architecture)**
- [x] **[New] 运行模式**: 引入 `GameWorld.Mode` (EDIT/PLAY/PAUSE)，彻底分离编辑器态与运行态逻辑
- [x] **[New] 编辑态执行**: 新增 `@ExecuteInEditMode` 注解，允许特定组件在编辑器模式下运行
- [x] **[New] 项目服务**: 引入 `ProjectService` (Model层)，统一管理项目的创建、模板实例化与文件操作
- [x] **[Refactor] 控制器**: 实现 `EditorController`，协调编辑器各模块 (MVP) 的初始化与交互

**构建与工程 (Build & Engineering)**
- [x] **[Refactor] 任务管理**: 将引擎构建任务移至 `gradle/engine-tasks.gradle`，实现模块化管理
- [x] **[New] 自动索引**: 实现 `generateEngineIndex` 构建任务，自动生成组件全类名索引 `engine.index`
- [x] **[Adj] 模板引擎**: 重构项目创建流程，支持源码包名重构 (Refactoring) 与元数据 (`project.json`) 注入

**编辑器功能 (Editor Features)**
- [x] **[Fix] Inspector交互**: 修复修改 GameObject 名称时焦点丢失的问题 (移除结构变更事件)
- [x] **[New] 数据双向绑定**: 实现 `SmartInput` 被动轮询机制，支持运行时数据实时反向同步到 UI
- [x] **[Opt] 性能优化**: Inspector 面板引入 30FPS 节流阀 (Throttling)，大幅降低高频刷新开销
- [x] **[New] 日志解析**: 实现 `LogParser`，支持 Markdown 风格的更新日志解析与富文本高亮
- [x] **[Opt] 渲染查询**: `WorldRenderSystem` 新增 `queryRenderables` 接口，解耦渲染循环与点击检测
- [x] **[Adj] 调试日志**: `Debug` 新增 `logErr` 红色高亮接口，优化日志分类标记

**UI 与组件 (UI & Components)**
- [x] **[Adj] 样式优化**: 调整 `VisUI` 的 SplitPane 手柄大小 (桌面/移动端适配) 与 CheckBox 尺寸
- [x] **[Adj] 屏幕适配**: `BaseSelectionScreen` 调整为横屏模式 (Landscape) 并优化视口缩放
- [x] **[Opt] 变换组件**: `TransformComponent` 移除运行时字符串拼接，关键矩阵字段对 Inspector 隐藏 (`@Hide`)

### `1.10.7.x` 发布冲刺与体验优化 (Release Prep & UX)
**构建与流程 (Build & Workflow)**
- [x] **[New] 资源同步**: 统一 Gradle 资源同步任务 (Sync Task) 与日志输出，确保构建产物完整性
- [ ] **[Adj] 流程标准化**: 规范化项目的创建、导出与更新流程
- [x] **[Adj] 测试流程梳理修复**: 无上下文时测试内容逻辑修正
- [x] **[Adj] 任务优化**: 将耗时的 Javadoc 生成任务从构建中分离，改为手动发布
- [x] **[Fix] 编译缓存**: 修复编译缓存未清理导致组件清单重复的问题

**编辑器核心 (Editor Core)**
- [x] **[Fix] 场景初始化**: 修复从主页进场景没有渲染系统问题
- [x] **[Fix] 场景刷新**: 修复从主页跳转场景编辑器时数据不刷新问题
- [x] **[Adj] 性能优化**: Hierarchy 树改为 30帧/秒 限制刷新，降低空闲开销
- [ ] **[Refactor] 代码解耦**: 清理核心大类，提升代码独立性 (Cleanup)
- [x] **[Fix] 输入映射**: 修复输入映射错误，补回游戏核心代理
- [x] **[Fix] 相机初始化**: 修复 GameCam 尺寸初始化为 0 的 Bug
- [x] **[Refactor] 组件接口**: RenderComponent.contains 改为默认实现，降低子类实现成本

**交互手感 (Interaction)**
- [x] **[Fix] 滚动冲突**: 拖拽数值/手柄按下时，暂时禁用父级滚动视图 (ScrollFocus)
- [x] **[Adj] 滚动体验**: 禁用 ScrollPane 的横向滚动与超限回弹 (Overscroll)
- [x] **[New] 统一菜单**: 统一右键(PC)与长按(Mobile)唤出上下文菜单的逻辑
- [x] **[Fix] 焦点丢失**: 修复 GameObject 名称输入时焦点意外丢失的问题
- [x] **[Fix] 输入隔离**: 游戏视图输入增加视口范围检测，防止误触

**UI 视觉 (Visual Polish)**
- [x] **[Adj] 面板背景**: 给组件标题栏(Header)和属性面板(Body)增加差异化背景色
- [x] **[Adj] 属性条目**: 给每个属性行增加略深色背景，提升可读性
- [x] **[Fix] 样式修复**: 修复 CheckBox 样式透明度问题
- [x] **[Adj] 输入控件**: Float/String 输入框限制最大宽度并右对齐 (统一长度标准)
- [x] **[Fix] 文本布局**: `toString` 绘制支持 `\n` 自动计算行高
- [ ] **[New] 适配配置**: 增加分辨率配置与安全边距 (Safe Margin) 配置
- [x] **[New] 视觉反馈**: 使用 NeonStage 绘制高亮边框，增强选中反馈

---

## [History] 历史版本归档

## `1.10.x` 对标 Unity Editor 核心架构
### `1.10.6.x` 渲染架构重构 (Rendering Overhaul)
- [x] **统一渲染管线**: 引入 `WorldRenderSystem`，替代了散乱的 Sprite/Skeleton 系统
- [x] **系统架构分离**: GameWorld 明确分离 `Update` (逻辑) 与 `Render` (渲染) 循环
- [x] **层级管理**: 引入 `RenderLayerManager`，支持自定义 Sorting Layer 和 Depth
- [x] **渲染组件基类**: 抽象 `RenderComponent`，统一所有可渲染对象的接口 (`render`, `contains`)
- [x] **精准选中**: 编辑器点击检测改为基于渲染层级的倒序检测，实现“所见即所得”

### `1.10.5.x` 编辑器交互修复 (Interaction Fixes)
- [x] **Gizmo 优化**: 修复缩放手柄手感，增加中心等比缩放，完善按下时的视觉反馈
- [x] **相机控制**: 修复右键拖拽不跟手问题，实现 FBO 坐标系下的 1:1 漫游
- [x] **输入兼容**: 解决 PC 滚轮缩放与 Android 双指缩放失效的问题

### `1.10.4.x` 引擎化转型
- [x] 正式转为引擎开发项目结构
- [x] 清理所有旧代码 (SoloEditor, IconEditor, 业务逻辑)

### `1.10.3.x` 变换组件重构
- [x] **矩阵变换**: `TransformComponent` 全面升级为 `Affine2` 矩阵计算
- [x] **层级系统**: 完善父子级变换矩阵的级联更新逻辑

### `1.10.1.x` - `1.10.2.x` 组件系统增强
- [x] **Inspector 升级**: 实现反射式属性面板
- [x] **组件添加**: 实现扫描式 Add Component 弹窗与搜索功能
- [x] **自定义组件**: 解决用户脚本组件无法在编辑器显示的问题

### `1.10.0.x` 基础建设
- [x] 实现类加载器的热更新机制
- [x] 修复快捷键映射系统的 Bug

---

## `1.9.x` 图标编辑器 (已归档)
### `1.9.9.x` EcsEditor 适配
- [x] 完成通用编辑器对 ECS 系统的初步适配

### `1.9.8.x` 操作优化
- [x] 移动操作支持单轴锁定
- [x] 增加对齐功能 (网格对齐/数值对齐)

### `1.9.7.x` 项目管理
- [x] 实现本地项目文件的保存与读取 (`.json`)
- [x] 内部类独立化，优化代码结构

### `1.9.4.x` - `1.9.6.x` 体验修复
- [x] 增加 Undo/Redo (撤销重做) 系统
- [x] 优化 Picking (点击选中) 逻辑
- [x] 禁用编辑器相机 WASD，改为纯鼠标操作

### `1.9.0.x` - `1.9.3.x` 早期开发
- [x] 实现基础 Hierarchy 和 Inspector 面板
- [x] 实现 Gizmo 视觉反馈 (选中高亮)
- [x] 解决重名物体自动重命名问题

---

## `1.8.x` 早期探索 (Hub & Templates)
### `1.8.14.x` 版本控制
- [x] 增加引擎版本信息，支持检测并更新用户项目的 libs

### `1.8.12.x` 模板系统
- [x] 实现基于模板创建新项目
- [x] 规范化 `InternalTemplates` 与 `UserProjects` 目录结构

### `1.8.11.x` Hub 交互
- [x] Hub 界面增加右键菜单 (删除项目)
- [x] 支持双击打开项目

### `1.8.10.x` 配置系统
- [x] 增加 `GDEngineConfig`，支持配置项目根目录 (跨平台同步支持)
