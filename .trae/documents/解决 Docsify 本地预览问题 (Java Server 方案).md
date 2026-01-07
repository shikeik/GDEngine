# 解决 Docsify 本地预览问题的方案

您遇到的“一直转圈”问题是因为现代浏览器的 **CORS (跨域资源共享) 安全策略**。Docsify 需要通过 JavaScript 动态加载 Markdown 文件，而浏览器默认禁止从 `file://` 协议（即直接双击文件）发起这种请求。

为了实现“双击即用”且不依赖 Python/Node.js 等额外工具，利用本项目**必然存在 Java 环境**的特点，我为您设计了一个零依赖的解决方案。

## 执行计划

### 1. 创建微型文档服务器 (Java 实现)
我们将利用 JDK 自带的 `com.sun.net.httpserver` 包编写一个仅 50 行代码的微型 HTTP 服务器。
- **位置**: `tools/SimpleDocServer.java`
- **特点**: 无需任何第三方库，只要有 Java 就能跑。

### 2. 创建一键启动脚本
在项目根目录创建一个批处理文件，负责编译服务器代码、启动服务并自动打开浏览器。
- **位置**: `view_docs.bat`
- **功能**:
    1. 编译 `tools/SimpleDocServer.java`。
    2. 启动服务器，指向 `docs/manual` 目录。
    3. 调用系统命令打开 `http://localhost:8899`。

### 3. 验证
您只需双击 `view_docs.bat`，即可在浏览器中完美浏览文档，无需任何额外配置。

这个方案完美契合 Java 游戏项目的环境，既保留了 Docsify 的强大功能，又解决了本地浏览的限制。
