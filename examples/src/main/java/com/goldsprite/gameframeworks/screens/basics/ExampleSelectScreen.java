package com.goldsprite.gameframeworks.screens.basics;

import com.goldsprite.gameframeworks.screens.IGScreen;
import java.util.Map;

import com.goldsprite.solofight.screens.game.GameScreen;
import com.goldsprite.solofight.screens.menus.TestSelectionScreen;
import com.goldsprite.solofight.screens.refactor.RefactorSelectionScreen;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.solofight.screens.tests.TempTestScreen;

// [修改] 继承 BaseSelectionScreen
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("场景列表(创建时间逆序)\n", null);

		// [新增] 重构实验室入口 (置顶)
        map.put(">>> 架构重构实验室 <<<", RefactorSelectionScreen.class);
		
		// [修改] 关联测试子菜单
		map.put("测试场景", TestSelectionScreen.class);
		map.put("!!! 临时测试 (Temp) !!!", TempTestScreen.class);

		map.put("展示场景", null);

		map.put("编辑器场景", null);

		map.put("游戏内容场景", null);
		map.put("主游戏", GameScreen.class);

		// 之后可以在这里加新的子菜单入口
	}
}
