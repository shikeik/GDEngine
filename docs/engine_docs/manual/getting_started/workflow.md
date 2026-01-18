# 引擎工作流 (Hub)

GDEngine 使用 **Hub** 作为项目管理中心，类似于 Unity Hub。

## 启动 Hub
运行引擎 Jar 包（或在 IDE 中运行 `Lwjgl3Launcher`），首先进入的就是 **GDEngine Hub**。

![Hub Screenshot](../_images/hub_preview.png)

## 核心功能

### 1. 项目管理
*   **New Project:** 基于标准模板创建新项目。引擎会自动处理包名重构和依赖注入。
*   **Open Project:** 打开现有的 GDEngine 项目。
*   **Settings:** 配置引擎根目录（默认为 User Home 下的 GDEngine）。

### 2. 开发辅助
*   **Console:** 底部的控制台会实时显示引擎日志。
*   **Docs:** 点击 "在线文档" 可直接跳转到本网站查看 API 和更新日志。

## 项目结构
一个标准的 GDEngine 项目包含：
*   `project.json`: 项目元数据（入口类、引擎版本）。
*   `src/main/java/`: 您的 Java 源代码。
*   `assets/`: 图片、音频等资源文件。
*   `libs/`: 引擎自动注入的依赖库（`gdengine.jar` 等）。
