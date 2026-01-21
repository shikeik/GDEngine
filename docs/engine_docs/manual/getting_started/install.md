# 环境准备与安装

GDEngine 是一个基于 LibGDX 的轻量级、跨平台 2D 游戏引擎。它最大的特色是支持在 **Android 平板** 和 **PC** 上进行完全一致的开发体验，支持 Java 脚本热重载。

## 系统要求 (实证)

*   **JDK:** 必须安装 **JDK 17** 或更高版本 (根据 `gradle.properties` 配置)。
*   **运行时:**
    *   **PC:** Windows/macOS/Linux (运行 exe)。
    *   **Android:** Android 8.0 (API 26) 及以上 (因使用了 `java.nio.file` 及 D8 动态编译特性)。
*   **IDE (可选但推荐):** IntelliJ IDEA 或 Android Studio。

## 获取引擎
*   **GitHub Releases:** [下载当前版本Releases](https://github.com/shikeik/GDEngine/releases)
*   **高速镜像:** [点击下载 (国内加速)](https://gdengine.pages.dev/download/mirror)

### 1. Android 用户 (推荐平板)
下载并安装最新的 **APK**。
*   引擎内置了 ECJ (Eclipse Compiler for Java) 和 D8 转换器，**无需** 连接电脑即可在手机/平板上编译 Java 代码。
*   可配合 **外接键盘** 以获得最佳编码体验。

### 2. Windows 用户
下载 **EXE** 发行版。
*   **特点:** 内置 JRE 环境，**无需手动安装 Java**，双击直接运行。

---

## 开发环境 (可选)
如果您需要编写复杂的 Java 脚本逻辑，建议配合 IDE 使用：
*   **Android:** 使用 **AIDE+** 或 **MT管理器** 修改项目内的 `.java` 文件。
*   **PC:** 推荐使用 **IntelliJ IDEA** 或 **VS Code** 打开项目文件夹。

## 源码构建
如果您希望参与引擎开发：
1. 克隆仓库: `git clone https://github.com/shikeik/GDEngine.git`
2. 打开项目根目录。
3. 运行 Gradle 任务: `gradlew build`。

---

## 常见问题

**Q: Android 上启动项目报错 "Permission Denied"？**
A: 引擎在 Android 上需要**所有文件访问权限** (`MANAGE_EXTERNAL_STORAGE`) 来读写项目文件。首次启动时请务必在弹出的权限请求中点击"允许"。