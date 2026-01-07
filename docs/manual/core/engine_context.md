# 引擎上下文 (Engine Context)

`com.goldsprite.gdengine.core.Gd` 是整个 GDEngine 的中枢神经。

## 1. 为什么需要 Gd 类？

虽然 LibGDX 提供了全局的 `Gdx` 类来访问系统资源，但在构建复杂的编辑器引擎（Editor-Engine）架构时，原生的 `Gdx` 往往不够用：

1.  **环境隔离**: 在编辑器模式下，输入（Input）可能来自编辑器 UI 而不是游戏窗口。`Gd.input` 允许我们在运行时动态切换输入源。
2.  **扩展性**: 我们需要挂载像 `ScriptCompiler` (脚本编译器) 这样 LibGDX 不具备的模块。
3.  **统一配置**: `Gd.config` 提供了游戏逻辑分辨率等核心配置的集中管理。

## 2. 核心模块概览

| 模块 | 字段 | 说明 |
| :--- | :--- | :--- |
| **基础代理** | `Gd.app`, `Gd.audio` | 直接指向 LibGDX 的实现，保持 API 习惯一致。 |
| **动态代理** | `Gd.input`, `Gd.graphics` | **关键**: 在编辑器中，这些会被替换为编辑器特有的实现。 |
| **脚本系统** | `Gd.compiler` | 负责运行时编译 Java 脚本。 |
| **配置中心** | `Gd.config` | 存储逻辑分辨率、视口适配策略等。 |

## 3. 运行模式 (Running Modes)

引擎有两种运行模式，通过 `Gd.mode` 获取：

*   **RELEASE**: 实机运行模式。此时 `Gd` 行为与 `Gdx` 基本一致。
*   **EDITOR**: 编辑器预览模式。此时输入可能被拦截，渲染可能被重定向到 FrameBuffer。

```java
// 示例：根据运行模式决定是否全屏
if (Gd.mode == Gd.Mode.RELEASE) {
    Gd.graphics.setFullscreenMode(Gd.graphics.getDisplayMode());
}
```

## 4. 初始化流程

`Gd.init()` 通常由 `AndroidGdxLauncher` 或 `Lwjgl3Launcher` 在游戏启动的最早阶段调用。

> **注意**: 普通开发者无需手动调用 `init()`，除非你在编写自定义的启动器。

## 5. 常见问题 (FAQ)

*   **Q: 为什么我修改了 `Gdx.input` 的 Processor 没生效？**
    *   A: 请检查你是否使用了 `Gd.input`。在编辑器模式下，直接操作 `Gdx.input` 可能会被编辑器层覆盖。建议始终使用 `Gd.input`。

