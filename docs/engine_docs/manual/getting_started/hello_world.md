# Hello World

让我们编写第一个游戏脚本。

## 1. 创建脚本
在您的项目源码目录下（例如 `src/main/java/com/mygame/`），创建一个类 `Main.java`。

**必须实现接口：** `com.goldsprite.gdengine.core.scripting.IGameScriptEntry`

## 2. 代码示例

```java
package com.mygame;

import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.log.Debug;

public class Main implements IGameScriptEntry {

    @Override
    public void onStart(GameWorld world) {
        Debug.log("Hello, GDEngine!");

        // 创建一个实体
        GObject player = new GObject("Player");
        
        // 添加图片组件
        SpriteComponent sprite = player.addComponent(SpriteComponent.class);
        sprite.setPath("gd_icon.png"); // assets/gd_icon.png
        sprite.width = 100;
        sprite.height = 100;
        
        // 设置位置
        player.transform.setPosition(0, 0);
    }

    @Override
    public void onUpdate(float delta) {
        // 每帧逻辑 (可选)
    }
}
```

## 3. 运行
在 Hub 中点击该项目，进入 **Editor**。
点击顶部工具栏的 `Run Game`，引擎将编译您的代码并启动游戏场景。
