# GDEngine 开发者中心

**GDEngine** 是一个基于 LibGDX 的现代化 2D 游戏引擎，专注于**跨平台热重载开发**与**原生级性能**。

### 核心特性
*   **双端开发：** 支持 Windows (PC) 与 Android (手机) 直接运行编辑器。
*   **ECS 架构：** 基于实体-组件-系统的高性能架构。
*   **Neon 动画：** 内置高性能骨骼动画系统。
*   **热重载：** 修改 Java 代码后，无需重启即可在编辑器内实时生效。

### 快速导航
*   [🚀 快速开始](manual/getting_started/install.md)
*   [🧩 核心概念](manual/core_concepts/ecs.md)
*   [💀 骨骼动画](manual/systems/neon_animation.md)
*   [📚 API 参考](javadoc/index.html)
*   [📅 更新日志](changelog/README.md)

## 网络测试

这是一个 19MB 的音频文件，用于测试 Cloudflare CDN 的下载速度。

<!-- 方式 B：强制下载 (推荐，HTML 写法) -->
<a href="SFX_Ambience_Forest_Day_Loop.wav" download="Test_Audio_19MB.wav">
    <button style="padding:10px; background:#09D2B8; color:white; border:none; border-radius:4px; cursor:pointer;">
        📥 强制下载测试 (19MB)
    </button>
</a>

<!-- 方式 C：直接嵌入播放器 (如果你想在线听) -->
<audio controls src="SFX_Ambience_Forest_Day_Loop.wav"></audio>
