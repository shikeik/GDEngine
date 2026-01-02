package com.goldsprite.solofight.screens;

import com.goldsprite.gameframeworks.screens.IGScreen;
import java.util.Map;

import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.game.GameScreen;
import com.goldsprite.solofight.screens.menus.TestSelectionScreen;
import com.goldsprite.solofight.screens.ecs.RefactorSelectionScreen;
import com.goldsprite.solofight.screens.tests.TempTestScreen;
import com.goldsprite.solofight.screens.editor.EditorSelectionScreen;
import com.goldsprite.solofight.screens.editor.tests.SceneEditorScreen;

// [修改] 继承 BaseSelectionScreen
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("场景列表\n", null);

		//map.put("临时测试", TempTestScreen.class);
		
		map.put(">>> 新ECS场景 <<<", RefactorSelectionScreen.class);
		map.put(">>> 编辑器开发 (Tools) <<<", EditorSelectionScreen.class);

		map.put("编辑器", SceneEditorScreen.class);

		map.put("H5轮子场景", TestSelectionScreen.class);

		map.put("游戏内容场景", null);
		map.put("主游戏", GameScreen.class);
	}
}
