package com.goldsprite.solofight.screens.menus;

import com.goldsprite.gameframeworks.screens.IGScreen;
import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.tests.HealthBarDemoScreen;

import java.util.Map;

public class TestSelectionScreen extends BaseSelectionScreen {

	@Override
	public String getIntroduction() {
		return "测试场景列表";
	}

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("UI 组件测试", null);
		map.put("H5 渐变血条演示", HealthBarDemoScreen.class);

		map.put("物理/碰撞测试", null);
		// 以后加别的...
	}
}
