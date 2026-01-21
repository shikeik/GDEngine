# Hello World

让我们编写第一个游戏脚本。GDEngine 的脚本本质上就是标准的 Java 类。

## 1. 入口脚本 (Main.java)

每个项目都需要一个入口类，它必须实现 `IGameScriptEntry` 接口。

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

		// 1. 创建实体
		GObject player = new GObject("Player");
		
		// 2. 设置变换 (Transform)
		player.transform.setPosition(0, 0); // 中心点
		player.transform.setScale(2.0f);	// 放大2倍

		// 3. 添加组件 (挂载一张图片)
		//SpriteComponent sprite = player.addComponent(SpriteComponent.class);
		// 4. 设置资源 (路径相对于 assets/)
		// 确保 assets/gd_icon.png 存在
		//sprite.setPath("gd_icon.png"); 
		// 快捷方式: 这将自动为精灵组件设置精灵图(内部自动从项目assets/查找, 失败则降级为引擎内置assets/中查找)并自动设置width/height属性
		SpriteComponent sprite = player.addComponent(new SpriteComponent("gd_icon.png"));
		
		// 当然也可以手动设置大小
		sprite.width = 100;
		sprite.height = 100;
	}
	
	@Override
	public void onUpdate(float delta) {
		// 全局每帧逻辑 (可选)
		// 通常建议将逻辑写在 Component 或 System 中，而不是这里
	}
}
```

## 2. 运行流程

### 2.1 编写代码

1. 方式一：IDE，推荐IDEA，安卓AIDE+或AndroidIDE也可(但引擎作者使用AIDE+)
	1. 进入gd项目根，打开IDE，等待项目导入完成
	2. 打开Main.java入口类, 粘贴上方代码然后保存
2. 方式二：引擎内工作流
	1. 在 Hub 中点击项目进入 **Editor**, new project选择空项目模板创建项目
		- 此时场景是空的（或者包含默认模板内容）。
	2. 单击后弹窗确认或直接双击快速进入项目
	3. project面板展开src, 看到Main.java, 双击打开, 这会切换到正中Code面板
	4. 长按代码编辑器出现长按菜单，全选粘贴代码并保存
		- 或快捷键ctrl+A全选, ctrl+V粘贴, ctrl+S保存
	5. 点preview回到场景视图

### 2.2 编译运行

1. 点击顶部工具栏的 **Build** 来编译项目。
	*   *底层发生了什么：* 编译器调用 ECJ 将 `.java` 编译为 `.class` (Android上转为 `.dex`)，并更新 `project.index`。
2. 然后点击顶部工具栏的 **Run Editor** (绿色按钮)。
3. 引擎将以编辑器模式开始运行。
4. 您应该能在控制台看到 橙色的 用户Hello日志，场景中出现图标。