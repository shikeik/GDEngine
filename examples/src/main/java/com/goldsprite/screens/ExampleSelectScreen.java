package com.goldsprite.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.gdengine.screens.ecs.hub.GDEngineHubScreen;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameScreen;
import com.goldsprite.solofight.screens.GameContentSelectionScreen;
import com.goldsprite.gdengine.screens.GDEngineSelectionScreen;

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
		map.put(">>> Editor Core (场景编辑器) <<<", EditorGameScreen.class);

		map.put("", null); // 分隔线

		// --- 引擎功能测试 ---
		map.put("引擎内部组件测试", GDEngineSelectionScreen.class);

		// --- 历史遗产归档 (SoloFight) ---
		// 所有的 Fighter, Combo, InputTest 等等都在这里面
		map.put("SoloFight 历史归档 (Archive)", GameContentSelectionScreen.class);

		// 这里的 SoloEditorScreen 我们就不放出来了，眼不见心不烦
		// map.put("SoloEditor (Deprecated UI Shell)", com.goldsprite.solofight.screens.editor.SoloEditorScreen.class);
	}
}
