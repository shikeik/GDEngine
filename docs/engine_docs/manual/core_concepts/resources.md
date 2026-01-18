# 资源管理

GDEngine 提供了跨平台的资源加载机制。

## 路径规则
所有资源必须放在项目的 `assets/` 目录下。代码中引用时使用**相对路径**。

*   `assets/images/player.png` -> 代码写 `"images/player.png"`

## 动态加载
脚本中可以使用工具类加载资源：

```java
// 加载图片
Texture tex = ScriptResourceTracker.loadTexture("images/player.png");

// 加载图集区域 (TexturePacker)
TextureRegion reg = CustomAtlasLoader.inst().getRegion("atlas/game.atlas", "sword_icon");
