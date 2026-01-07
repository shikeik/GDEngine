package com.goldsprite.screens;

import com.goldsprite.gdengine.screens.GScreen;
import java.util.Map;

import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.gdengine.screens.GDEngineSelectionScreen;
import com.goldsprite.solofight.screens.GameContentSelectionScreen;

// [修改] 继承 BaseSelectionScreen
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("场景列表\n", null);
		map.put(">>>引擎开发<<<", GDEngineSelectionScreen.class);
		map.put("", null);
		// [新增] 架构验证入口
		//map.put("架构验证: Internal Demo Game", InternalGameTestScreen.class);
		//map.put("临时测试", TempTestScreen.class);
		map.put("游戏内容(旧版)", GameContentSelectionScreen.class);
	}
}
