# GDEngine 项目概览报表

## 一、项目基本信息

**项目名称**：GDEngine  
**项目类型**：基于 LibGDX 的跨平台 2D 游戏引擎  
**当前版本**：v1.10.12.20（2026-01-20）  
**开发状态**：活跃开发中  
**项目路径**：`/storage/emulated/0/AppProjects/new_dev/tmp/GDEngine`

### 核心定位

GDEngine 是一个现代化 2D 游戏引擎，专注于**跨平台热重载开发**与**原生级性能**。支持 Windows 和 Android 双端直接运行编辑器，实现代码修改后无需重启即可实时生效。

---

## 二、项目目录结构分析

### 2.1 顶层目录结构

| 目录/文件 | 功能说明 | 关键内容 |
|-----------|----------|----------|
| `android/` | Android 平台实现 | 启动器、编译器、浏览器、权限管理 |
| `core/` | 引擎核心代码 | ECS 系统、动画、渲染、编辑器 |
| `lwjgl3/` | PC 桌面端实现 | LWJGL3 启动器、编译器、浏览器 |
| `assets/` | 引擎内置资源 | 图标、字体、音效、UI 皮肤、示例素材 |
| `tests/` | 单元测试代码 | 覆盖 ECS、动画、骨骼、FSM 等核心模块 |
| `examples/` | 示例代码 | 各种功能展示屏幕 |
| `docs/` | 开发文档 | 策划文档、历史版本、设计文档 |
| `GDEngine/` | 用户工作区 | 用户项目、模板、引擎文档 |
| `outputs/` | 构建输出 | 打包的 JAR 文件 |

### 2.2 用户工作区结构（GDEngine/）

```
GDEngine/
├── UserProjects/                    # 用户创建的项目
│   ├── MyGame/                      # 示例项目1
│   │   ├── assets/
│   │   ├── libs/                    # 依赖库 (gdx, vis-ui)
│   │   ├── src/main/java/           # 用户源码
│   │   └── project.json             # 项目配置
│   ├── MyGame1/                     # 示例项目2
│   └── MyGame2/                     # 示例项目3（含自定义组件）
├── LocalTemplates/                  # 本地模板
│   └── BigDemo/                     # 大型演示模板
├── engine_docs/                     # 引擎文档（Docsify 格式）
└── engine_config.json               # 引擎全局配置
```

### 2.3 核心代码结构（core/src/main/java/）

```
core/src/main/java/com/goldsprite/gdengine/
├── BuildConfig.java                 # 构建配置
├── PlatformImpl.java                # 平台抽象实现
├── assets/                          # 资源管理模块
│   ├── ColorTextureUtils.java       # 颜色纹理工具
│   ├── CustomAtlasLoader.java       # 图集加载器
│   ├── FontUtils.java               # 字体工具
│   └── VisUIHelper.java             # VisUI 界面助手
├── audio/                           # 音频模块
│   └── SynthAudio.java              # 合成音频系统
├── core/                            # 核心框架
│   ├── Gd.java                      # 全局入口与工具集
│   ├── ComponentRegistry.java       # 组件注册表
│   ├── annotations/                 # 自定义注解
│   ├── command/                     # 命令系统
│   ├── config/                      # 配置管理
│   ├── input/                       # 输入系统
│   ├── platform/                    # 平台文件操作
│   ├── project/                     # 项目管理
│   ├── scripting/                   # 脚本系统
│   ├── utils/                       # 工具类
│   └── web/                         # Web 文档服务
├── ecs/                             # ECS 架构核心
│   ├── ComponentManager.java        # 组件管理器
│   ├── EcsObject.java               # ECS 对象基类
│   ├── GameWorld.java               # 游戏世界（ECS 管理器）
│   ├── SystemType.java              # 系统类型枚举
│   ├── component/                   # 内置组件
│   ├── entity/                      # 实体类
│   ├── fsm/                         # 有限状态机
│   ├── skeleton/                    # 骨骼动画系统
│   └── system/                      # 系统实现
├── input/                           # 输入事件
├── log/                             # 日志系统
├── neonbatch/                       # 2D 渲染批处理
├── screens/                         # 屏幕系统
│   ├── GScreen.java                 # 屏幕基类
│   ├── ScreenManager.java           # 屏幕管理器
│   ├── basics/                      # 基础屏幕
│   ├── ecs/                         # ECS 相关屏幕
│   │   ├── GameRunnerScreen.java    # 游戏运行屏幕
│   │   ├── editor/                  # 编辑器模块
│   │   └── hub/                     # Hub 项目管理界面
│   └── ui/                          # UI 组件
└── utils/                           # 工具类
```

---

## 三、核心模块详细分析

### 3.1 ECS 架构模块

**核心类文件**：`core/src/main/java/com/goldsprite/gdengine/ecs/`

#### 3.1.1 实体与组件

| 类名 | 功能 | 代码行数 |
|------|------|----------|
| `GObject` | 实体对象，场景基本单元 | ~250 行 |
| `Component` | 组件基类，支持生命周期回调 | ~180 行 |
| `TransformComponent` | 变换组件（位置/旋转/缩放） | ~350 行 |
| `SpriteComponent` | 精灵渲染组件 | ~200 行 |
| `SkeletonComponent` | 骨骼组件 | ~250 行 |
| `NeonAnimatorComponent` | 动画控制器组件 | ~300 行 |
| `FsmComponent` | 有限状态机组件 | ~220 行 |
| `RenderComponent` | 渲染组件基类 | ~150 行 |

**关键特性**：

- 每个 `GObject` 自动携带 `TransformComponent`，不可移除
- 支持父子层级嵌套，子物体变换相对于父物体
- 组件添加时自动注入 `gobject` 和 `transform` 引用
- 支持多态组件查询：`getComponent(Class)` 支持查找子类

#### 3.1.2 系统模块

| 系统类 | 类型 | 功能 |
|--------|------|------|
| `BaseSystem` | 抽象基类 | 系统基类，自动注册到 GameWorld |
| `SceneSystem` | UPDATE | 驱动所有 GObject 的生命周期和更新 |
| `WorldRenderSystem` | RENDER | 收集 RenderComponent，按 Layer 排序绘制 |
| `SkeletonSystem` | UPDATE | 骨骼矩阵更新系统 |
| `RenderLayerManager` | 配置 | 渲染层级管理 |

**系统注册机制**：

- 通过 `@GameSystemInfo` 注解声明类型和关注组件
- 支持位运算组合系统类型（UPDATE | FIXED_UPDATE | RENDER）
- 系统构造函数自动注册到 GameWorld

### 3.2 Neon 骨骼动画模块

**核心类文件**：`core/src/main/java/com/goldsprite/gdengine/ecs/skeleton/`

#### 模块结构

```
skeleton/
├── NeonBone.java                    # 骨骼节点（支持矩阵变换）
├── NeonSkeleton.java                # 骨架容器
├── NeonSlot.java                    # 插槽（承载显示内容）
├── BoneSkin.java                    # 皮肤基类
├── NeonGeometrySkin.java            # 几何皮肤
├── animation/
│   ├── NeonAnimation.java           # 动画定义
│   ├── NeonTimeline.java            # 时间轴
│   ├── NeonKeyframe.java            # 关键帧
│   ├── NeonCurve.java               # 曲线类型（线性/平滑/阶梯）
│   └── NeonProperty.java            # 可动画属性枚举
└── data/
    ├── NeonDataModels.java          # 数据模型
    └── NeonJsonUtils.java           # JSON 序列化
```

**核心特性**：

- 支持 4 种曲线插值：LINEAR、SMOOTH、STEPPED、CUBIC
- 支持 6 种动画属性：X、Y、ROTATION、SCALE_X、SCALE_Y、SPRITE
- 骨骼层级矩阵自动计算（父子相对变换）
- 支持动画混合（CrossFade）
- 支持泛型关键帧（支持 String 等对象类型）

### 3.3 编辑器模块

**核心类文件**：`core/src/main/java/com/goldsprite/gdengine/screens/ecs/editor/`

#### MVC 架构

| 角色 | 类名 | 功能 |
|------|------|------|
| Controller | `EditorController` | 编辑器主控制器 |
| View | `EditorGameScreen` | 游戏视图屏幕 |
| View | `ViewWidget` | 视图组件 |
| Presenter | `ScenePresenter` | 场景展示器 |
| Presenter | `HierarchyPresenter` | 层级结构展示器 |
| Presenter | `InspectorPresenter` | 属性检查器展示器 |
| Presenter | `ProjectPresenter` | 项目展示器 |
| Presenter | `GamePresenter` | 游戏运行展示器 |

#### 编辑器面板

| 面板类 | 功能 |
|--------|------|
| `HierarchyPanel` | 场景层级树状视图 |
| `InspectorPanel` | 组件属性编辑面板 |
| `ScenePanel` | 场景编辑视图 |
| `CodePanel` | 代码编辑器 |
| `ConsolePanel` | 日志控制台 |
| `GamePanel` | 游戏运行视图 |
| `ProjectPanel` | 项目文件浏览器 |

#### 属性绘制器（Drawer）

| 绘制器 | 类型 |
|--------|------|
| `PrimitiveDrawer` | 基本类型（int/float/boolean） |
| `StringDrawer` | 字符串 |
| `ColorDrawer` | 颜色选择器 |
| `EnumDrawer` | 枚举下拉框 |
| `Vector2Drawer` | 二维向量 |
| `DefaultObjectDrawer` | 默认对象绘制 |
| `GObjectInspectorDrawer` | GObject 专用绘制 |

### 3.4 Hub 项目管理模块

**核心类文件**：`core/src/main/java/com/goldsprite/gdengine/screens/ecs/hub/`

| 类名 | 功能 |
|------|------|
| `GDEngineHubScreen` | Hub 主屏幕 |
| `HubPresenter` | Hub 业务逻辑 |
| `HubViewImpl` | Hub 视图实现 |
| `SettingsWindow` | 设置窗口 |
| `SetupDialog` | 初始化对话框 |
| `OnlineTemplateDialog` | 在线模板对话框 |

**核心功能**：

- 引擎根目录管理（自动检测/手动设置）
- 项目创建/打开/删除
- 本地和在线模板浏览
- 引擎版本检查与更新

### 3.5 脚本编译模块

#### 编译器接口

| 平台 | 编译器类 | 位置 |
|------|----------|------|
| 桌面端 | `DesktopScriptCompiler` | lwjgl3/src/... |
| Android | `AndroidScriptCompiler` | android/src/... |
| 接口 | `IScriptCompiler` | core/src/... |

#### Android 编译器特性

**文件**：`android/src/main/java/com/goldsprite/gdengine/android/AndroidScriptCompiler.java`

```
核心流程：
1. ECJ 编译 (.java -> .class)
   - JDK 1.8 目标
   - 支持增量编译

2. D8 混淆 (.class -> .dex)
   - minApiLevel: 19
   - 支持内存加载 (API 26+)

3. 智能缓存
   - 检测 APK 版本变化
   - 按需刷新依赖库
   - 缓存编译产物

4. 脚本加载
   - DexClassLoader / InMemoryDexClassLoader
   - 设置为全局脚本类加载器
```

### 3.6 UI 系统模块

**核心类文件**：`core/src/main/java/com/goldsprite/gdengine/ui/`

#### 输入组件

| 组件类 | 功能 |
|--------|------|
| `SmartInput<T>` | 智能输入基类 |
| `SmartTextInput` | 文本输入框 |
| `SmartNumInput` | 数字输入框 |
| `SmartBooleanInput` | 布尔复选框 |
| `SmartColorInput` | 颜色选择器 |
| `SmartSelectInput` | 下拉选择框 |

#### 高级组件

| 组件类 | 功能 |
|--------|------|
| `BioCodeEditor` | 代码编辑器（基于 Java 编辑） |
| `FileTreeWidget` | 文件树组件 |
| `FreePanViewer` | 自由平移视图 |
| `GSplitPane` | 分割面板 |
| `IDEConsole` | IDE 风格控制台 |
| `ToastUI` | Toast 提示 |
| `RichText` | 富文本显示组件 |

#### 富文本系统

**文件**：`core/src/main/java/com/goldsprite/gdengine/ui/widget/richtext/`

```
RichTextParser.java        # 解析器
RichText.java              # 富文本组件
RichElement.java           # 富文本元素
RichStyle.java             # 样式定义
RichTextEvent.java         # 事件处理

支持的标签：
- [color=red]...[/color]  # 颜色
- [#00FF00]...[/color]    # Hex 颜色
- [size=50]...[/size]     # 字体大小
- [img=test.png|32x32]    # 图片
```

### 3.7 平台抽象模块

#### Android 平台支持

**文件**：`android/src/main/java/com/goldsprite/gdengine/android/`

| 文件 | 功能 |
|------|------|
| `AndroidGdxLauncher.java` | Android 应用入口 |
| `AndroidScriptCompiler.java` | 脚本编译器 |
| `AndroidWebBrowser.java` | 内嵌浏览器 |
| `PermissionUtils.java` | 运行时权限管理 |
| `ScreenUtils.java` | 屏幕工具 |
| `UncaughtExceptionActivity.java` | 异常捕获 |

**键盘覆盖层特性**：

- 虚拟键盘 UI（5 行布局）
- 拖拽移动按钮位置
- 自动适应横竖屏
- 沉浸式模式支持

#### LWJGL3 桌面支持

**文件**：`lwjgl3/src/main/java/com/goldsprite/gdengine/lwjgl3/`

| 文件 | 功能 |
|------|------|
| `Lwjgl3Launcher.java` | 桌面应用入口 |
| `DesktopScriptCompiler.java` | 桌面编译器 |
| `DesktopWebBrowser.java` | 桌面浏览器 |
| `StartupHelper.java` | 启动帮助 |

### 3.8 测试模块

**文件位置**：`tests/src/test/java/com/goldsprite/gdengine/tests/`

#### 测试套件

| 测试类 | 测试内容 | 测试方法数 |
|--------|----------|------------|
| `EcsUnitTestSuite.java` | ECS 生命周期、层级、销毁 | 2 |
| `FsmUnitTestSuite.java` | 有限状态机（优先级、打断、霸体） | 3 |
| `SkeletonUnitTestSuite.java` | 骨骼层级矩阵、绘制顺序 | 2 |
| `SystemRegistrationTest.java` | 系统分类、位运算逻辑 | 2 |
| `SystemInterestTest.java` | 多态组件筛选 | 1 |
| `TransformMatrixTest.java` | TRS 变换、层级变换 | 4 |
| `AnimatorUnitTest.java` | 动画驱动骨骼 | 1 |
| `AnimatorMixTest.java` | 动画混合（CrossFade） | 1 |
| `AnimationDataTest.java` | 插值算法（线性/阶梯/平滑） | 3 |
| `GenericAnimationTest.java` | 泛型动画（对象关键帧） | 2 |
| `RichTextParserTest.java` | 富文本解析 | 5 |

**测试框架**：JUnit 4 + LibGDX GdxTestRunner

---

## 四、引擎文档结构

### 4.1 文档目录（engine_docs/）

```
engine_docs/
├── README.md                        # 文档首页
├── _sidebar.md                      # 侧边栏导航
├── index.html                       # Docsify 入口页
├── changelog/
│   ├── README.md                    # 更新日志页
│   ├── changelog.js                 # 更新日志逻辑
│   └── changelog.json               # 更新日志数据
├── javadoc/                         # API 文档
└── manual/                          # 用户手册
    ├── core_concepts/               # 核心概念
    │   ├── ecs.md                   # ECS 架构
    │   ├── lifecycle.md             # 生命周期
    │   └── resources.md             # 资源管理
    ├── getting_started/             # 快速入门
    │   ├── install.md               # 安装指南
    │   ├── workflow.md              # 工作流
    │   └── hello_world.md           # Hello World
    ├── scripting/                   # 脚本开发
    │   ├── hot_reload.md            # 热重载
    │   └── java_basics.md           # Java 基础
    └── systems/                     # 功能模块
        ├── neon_animation.md        # 骨骼动画
        └── ui_system.md             # UI 系统
```

### 4.2 更新日志系统

**文件**：`engine_docs/changelog/changelog.json`

```
版本分组：
- In Development      # 开发中版本
- 1.10.12             # 当前稳定版系列
- 1.10.11
- 1.10.10
- ... (历史版本追溯到 v0.1.0)

更新类型标签：
- feat: 新功能
- fix: Bug 修复
- perf: 性能优化
- docs: 文档更新
- chore: 维护任务
- refactor: 重构
- test: 测试相关
- legacy: 历史记录
```

### 4.3 文档特色功能

**文件**：`engine_docs/index.html`

```
Docsify 定制功能：
1. 自定义侧边栏（支持折叠文件夹）
2. 更新日志动态注入（JSON -> Sidebar Tree）
3. 深度链接支持（?target=tagId）
4. 版本状态徽章（DEV / PREVIEW / CURRENT / HISTORY）
5. Javadoc 直连白名单
6. Unity 风格视觉主题（青色主题色 #09D2B8）
```

---

## 五、用户项目结构

### 5.1 示例项目

| 项目名 | 路径 | 特点 |
|--------|------|------|
| `MyGame` | `GDEngine/UserProjects/MyGame/` | 基础模板，无自定义组件 |
| `MyGame1` | `GDEngine/UserProjects/MyGame1/` | 基础模板，资源较少 |
| `MyGame2` | `GDEngine/UserProjects/MyGame2/` | 含自定义组件 `MyComp`，使用场景文件 |

### 5.2 项目标准结构

```
ProjectName/
├── assets/                          # 资源目录
│   └── gd_icon.png                  # 图标示例
├── libs/                            # 依赖库（自动注入）
│   ├── gdx-1.12.1.jar
│   ├── gdx-freetype-1.12.1.jar
│   ├── gdx-tools-1.12.1.jar
│   ├── vis-ui-1.5.3.jar
│   └── gdengine.jar                 # 引擎核心
├── scenes/                          # 场景文件（可选）
│   └── main.scene
├── src/main/java/                   # Java 源码
│   └── com/
│       └── mygame/
│           └── Main.java            # 入口类（必须实现 IGameScriptEntry）
├── project.json                     # 项目配置
└── project.index                    # 类索引（编译后生成）
```

### 5.3 项目配置示例

**文件**：`project.json`

```json
{
    "name": "MyGame",
    "packageName": "com.mygame",
    "mainClass": "Main",
    "engineVersion": "1.10.12",
    "template": "HelloGame"
}
```

---

## 六、依赖库清单

### 6.1 引擎核心依赖

| 库名 | 版本 | 用途 |
|------|------|------|
| libGDX | 1.12.1 | 2D 游戏框架基础 |
| gdx-freetype | 1.12.1 | FreeType 字体渲染 |
| gdx-tools | 1.12.1 | 工具类（TexturePacker 等） |
| vis-ui | 1.5.3 | UI 控件库 |
| gdengine.jar | 内置 | 引擎核心代码 |

### 6.2 Android 平台依赖

| 依赖 | 用途 |
|------|------|
| Android SDK | Android 应用开发 |
| D8/R8 | Java -> Dex 编译 |
| Eclipse ECJ | Java 编译 |

---

## 七、生命周期与工作流

### 7.1 组件生命周期

```
awake        -> 组件添加时立即执行
onAwake      -> 可重写的初始化方法
start        -> Play 模式第一帧执行
onStart      -> 所有组件初始化完毕后执行
update       -> 每帧调用（与帧率同步）
fixedUpdate  -> 固定时间步长（默认 60Hz）
render       -> 渲染循环
destroy      -> 物体销毁时触发
```

### 7.2 编辑器工作流

```
1. Hub 启动
   └── 选择/创建项目

2. 编辑器模式 (EDIT)
   └── 场景编辑
   └── 属性修改（@ExecuteInEditMode 组件会执行 Update）

3. 构建 (Build)
   └── 编译 Java 源码
   └── 生成 project.index

4. 运行 (Play)
   └── 热重载脚本
   └── 完整生命周期执行

5. 停止 (Stop)
   └── 返回编辑模式
   └── 保留场景状态
```

---

## 八、版本演进历史

### 8.1 版本号规则

**格式**：`主版本.次版本.修订号.构建号`

示例：`1.10.12.20`
- 1.10.12：功能版本
- 20：当日构建序号

### 8.2 近期主要更新（2026-01）

| 版本 | 日期 | 主要变更 |
|------|------|----------|
| 1.10.12.20 | 2026-01-20 | 文档/模板更新 |
| 1.10.12.19 | 2026-01-20 | 云端下载功能收官 |
| 1.10.12.18 | 2026-01-20 | 缓存问题解决方案确定 |
| 1.10.12.17 | 2026-01-20 | Gradle CDN 缓存刷新按钮 |

### 8.3 里程碑版本

| 版本 | 里程碑 |
|------|--------|
| v0.1.0 | 接入 libGDX 引擎 |
| v0.5.0 | 输入系统与操作 UI |
| v0.6.0 | 骨骼动画系统 Neon |
| v1.0.0 | 首个正式版本 |
| v1.10.0 | MVP 架构重构（Hub + Editor） |
| v1.10.12 | 模板与文档系统完善 |

---

## 九、关键设计模式

### 9.1 架构模式

| 模式 | 应用场景 |
|------|----------|
| **ECS** | 实体-组件-系统，Data-Oriented Design |
| **MVC/MVP** | 编辑器各面板（Presenter 分离） |
| **观察者模式** | 事件系统、生命周期回调 |
| **策略模式** | 动画曲线（NeonCurve） |
| **状态模式** | FsmComponent 有限状态机 |
| **工厂模式** | 组件创建、系统注册 |
| **单例模式** | GameWorld、Gd、SpriteBatch |

### 9.2 平台抽象

```
PlatformImpl (抽象接口)
├── DesktopFileHandle (桌面实现)
└── AndroidFileHandle (Android 实现)

IScriptCompiler (脚本编译器接口)
├── DesktopScriptCompiler
└── AndroidScriptCompiler

IWebBrowser (浏览器接口)
├── DesktopWebBrowser
└── AndroidWebBrowser
```

---

## 十、技术亮点总结

### 10.1 跨平台能力

| 平台 | 运行时 | 脚本编译 |
|------|--------|----------|
| Windows | LWJGL3 + 内置 JRE | ECJ -> Class |
| Android | Dalvik/ART | ECJ -> D8 -> Dex |
| 理论支持 | iOS (RoboVM) | 需额外适配 |

### 10.2 性能优化

- **ECS 组件缓存**：`ComponentManager` O(1) 复杂度查询
- **批量渲染**：`NeonBatch` 减少 DrawCall
- **骨骼矩阵预计算**：`SkeletonSystem` 动画后统一更新
- **增量编译**：仅编译修改的 Java 文件
- **智能缓存**：检测 APK 版本变化按需刷新

### 10.3 开发者体验

- **热重载**：修改代码无需重启
- **实时预览**：Editor 内即时看到修改效果
- **可视化调试**：Gizmo 变换工具、Console 日志
- **完整测试**：11 个测试套件覆盖核心模块
- **详细文档**：Docsify 文档系统 + JavaDoc API 文档

---

## 十一、文件统计

| 统计项 | 数量 |
|--------|------|
| Java 源文件（核心） | ~200+ |
| Java 测试文件 | 11 个测试类 |
| 文档文件（Markdown） | 15+ 篇 |
| UI 资源文件 | 50+ 个 |
| 用户项目模板 | 3 个 |
| 单元测试方法 | 30+ 个 |

---

*报表生成时间：2026-01-21*  
*数据来源：ProjectTree.txt、ProjectCode.txt、engine_docs_tree.txt、engine_docs_code.txt*
