# 资源管理(未完全审查)

## 资源路径
所有资源必须放置在项目的 `assets/` 文件夹下。代码中使用**相对路径**引用。

例如：文件位于 `MyGame/assets/sprites/hero.png`，代码中写作 `"sprites/hero.png"`。

## 核心工具类

### 1. ScriptResourceTracker
这是脚本加载资源的推荐方式。它会自动追踪你加载的资源，并在游戏停止 (`GameRunnerScreen.dispose`) 时自动释放内存，防止内存泄漏。

```java
// 加载纹理
Texture tex = ScriptResourceTracker.loadTexture("images/player.png");

// 加载区域 (如果是单图)
TextureRegion reg = ScriptResourceTracker.loadRegion("images/player.png");
```

### 2. CustomAtlasLoader
用于加载 自定义格式图集 (`.atlas`)。支持特殊的 `#` 语法来直接获取子区域。

```java
// 语法: "图集路径.atlas#子区域名"
TextureRegion sword = CustomAtlasLoader.inst().getRegion("atlas/weapons.atlas", "sword_iron");
```

### 3. AssetUtils
用于文件索引查询。由于 Jar 包内无法列出文件列表，引擎在构建时会生成 `assets.txt` 索引。
`AssetUtils.listNames("sprites/")` 可以返回该目录下的所有文件名。
