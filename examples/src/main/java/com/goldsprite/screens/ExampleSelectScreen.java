package com.goldsprite.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.gdengine.screens.ecs.hub.GDEngineEditorScreen;
import com.goldsprite.gdengine.screens.ecs.hub.GDEngineHubScreen;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameScreen;

import java.util.Map;

/**
 * GDEngine 开发主入口
 * 策略：只展示核心工具，旧有的业务逻辑归档至二级菜单
 */
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		// --- 核心开发工具 ---
		map.put(">>> GDEngine Hub (项目管理) <<<", GDEngineHubScreen.class);
		map.put(">>> Editor Core (场景编辑器) <<<", EditorGameScreen.class); // 暂时还需要 // 注释掉, 已被 GDEngineHubScreen 取代, 无项目上下文启动没有意义
		map.put("代码编辑器场景(已弃置)", GDEngineEditorScreen.class);

		map.put("", null); // 分隔线

		// --- 引擎功能测试 ---
		map.put("测试", GDEngineSelectionScreen.class);
	}
}
