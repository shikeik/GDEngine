# 环境准备与安装

GDEngine 是一个基于 LibGDX 的跨平台 2D 游戏引擎，支持 Java 脚本热重载。

## 系统要求

*   **JDK:** 必须安装 **JDK 17** 或更高版本。
*   **操作系统:** Windows 10/11, macOS, 或 Linux。
*   **IDE (可选但推荐):** IntelliJ IDEA 或 Android Studio。

## 获取引擎

### 1. Android 用户 (推荐平板)
下载并安装 **APK** 文件。
*   **GitHub Releases:** [下载最新 APK](https://github.com/shikeik/GDEngine/releases)
*   **高速镜像:** [点击下载 (国内加速)](https://gdengine.pages.dev/download/mirror) *(示例链接，请替换为实际地址)*

### 2. Windows 用户
下载 **EXE** 发行版。
*   **特点:** 内置 JRE 环境，**无需手动安装 Java**，解压即用。
*   **下载:** [GDEngine_Win64.exe](https://github.com/shikeik/GDEngine/releases)

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
