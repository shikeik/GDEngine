package com.goldsprite.gameframeworks.screens.basics;

import com.goldsprite.gameframeworks.screens.IGScreen;
import java.util.Map;
import com.goldsprite.solofight.screens.menus.TestSelectionScreen;

// [修改] 继承 BaseSelectionScreen
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	public String getIntroduction() {
		return "示例选择";
	}

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("场景列表(创建时间逆序)\n", null);
		
		// [修改] 关联测试子菜单
		map.put("测试场景", TestSelectionScreen.class);

		map.put("展示场景", null);

		map.put("编辑器场景", null);

		map.put("游戏内容场景", null);
		
		// 之后可以在这里加新的子菜单入口
	}
}
