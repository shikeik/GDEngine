# 环境准备与安装

GDEngine 是一个基于 LibGDX 的轻量级、跨平台 2D 游戏引擎。它最大的特色是支持在 **Android 平板** 和 **PC** 上进行完全一致的开发体验，支持 Java 脚本热重载。

## 系统要求 (实证)

*   **JDK:** 必须安装 **JDK 17** 或更高版本 (根据 `gradle.properties` 配置)。
*   **运行时:**
    *   **PC:** Windows/macOS/Linux (运行 exe)。
    *   **Android:** Android 8.0 (API 26) 及以上 (因使用了 `java.nio.file` 及 D8 动态编译特性)。
*   **IDE (可选但推荐):** IntelliJ IDEA 或 Android Studio。

## 获取引擎

<!-- 动态下载组件 -->
<div id="download-widget" class="down-widget">
    <div class="loading-text">正在获取版本列表...</div>
</div>

*   **GitHub Releases:** [直达下载页](https://github.com/shikeik/GDEngine/releases)

<!-- 样式 -->
<style>
.down-widget {
    border: 1px solid #09D2B8;
    background: #f0fdfa;
    padding: 15px;
    border-radius: 6px;
    margin-bottom: 20px;
}
.ver-control {
    margin-bottom: 12px;
    font-size: 15px;
    font-weight: bold;
    color: #333;
}
.ver-select {
    padding: 4px 8px;
    border: 1px solid #ccc;
    border-radius: 4px;
    margin-left: 10px;
}
.btn-grid {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
}
.dl-btn {
    display: inline-flex;
    align-items: center;
    padding: 8px 16px;
    border-radius: 4px;
    text-decoration: none !important;
    font-size: 14px;
    font-weight: 600;
    transition: opacity 0.2s;
    color: white !important;
}
.dl-btn:hover { opacity: 0.85; }
.btn-android { background: #3DDC84; }
.btn-pc { background: #E76F00; }
.btn-exe { background: #0078D7; }
</style>

<!-- 逻辑脚本 -->
<script>
(function() {
    const REPO = "shikeik/GDEngine";
    // 文件下载走 gcore CDN (极速)
    const CDN_BASE = "https://gcore.jsdelivr.net/gh/" + REPO + "@";
    // 定义反代基准地址 (用于 APK/JAR 下载，避开 CDN 限制)
    const PROXY_BASE = "https://gh-proxy.com/https://github.com/";
    
    function renderWidget(versions) {
        const container = document.getElementById('download-widget');
        if(!container) return;

        let options = '';
        versions.forEach(v => { options += `<option value="${v}">${v}</option>`; });

        const html = `
            <div class="ver-control">
                当前版本: <select id="ver-selector" class="ver-select">${options}</select>
            </div>
            <div class="btn-grid">
                <a id="link-apk" class="dl-btn btn-android" target="_blank">🤖 Android APK</a>
                <a id="link-jar" class="dl-btn btn-pc" target="_blank">☕ Desktop Jar</a>
                <a id="link-exe" class="dl-btn btn-exe" target="_blank">🪟 Windows Exe</a>
            </div>
        `;
        container.innerHTML = html;

        const selector = document.getElementById('ver-selector');
        selector.onchange = () => updateLinks(selector.value);
        if(versions.length > 0) updateLinks(versions[0]);
    }

    function updateLinks(tag) {
        // Tag: v1.10.12.21 -> Clean: 1.10.12.21
        const cleanVer = tag.replace(/^v/, '');
        
        // 假设自动化构建会将产物放入仓库的 dist/ 目录 (支持 CDN 直接读取)
        // 格式: https://gcore.jsdelivr.net/gh/user/repo@tag/dist/filename
        //const basePath = `${CDN_BASE}${tag}/dist/`;
        
        // 构造反代链接
        // 格式: https://gh-proxy.com/https://github.com/{user}/{repo}/releases/download/{tag}/{filename}
        const basePath = `${PROXY_BASE}${REPO}/releases/download/${tag}/`;
        
        document.getElementById('link-apk').href = basePath + `GDEngine_V${cleanVer}.apk`;
        document.getElementById('link-jar').href = basePath + `GDEngine_V${cleanVer}.jar`;
        document.getElementById('link-exe').href = basePath + `GDEngine_V${cleanVer}.exe`;
    }

    fetch('changelog/changelog.json')
        .then(r => r.json())
        .then(data => {
            const versions = [];
            if(data.groups) {
                data.groups.forEach(g => {
                    if(g.patches) {
                        g.patches.forEach(p => {
                            if(p.tag && p.tag !== "HEAD" && !p.isSnapshot) versions.push(p.tag);
                        });
                    }
                });
            }
            if(versions.length === 0) versions.push("v1.0.0");
            renderWidget(versions);
        })
        .catch(e => {
            document.getElementById('download-widget').style.display = 'none';
        });
})();
</script>

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
