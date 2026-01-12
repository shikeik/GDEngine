package com.goldsprite.solofight.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.main.GameScreen;
import com.goldsprite.solofight.screens.tests.*;

import java.util.Map;

public class GameContentSelectionScreen extends BaseSelectionScreen {
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("测试场景", null);
		map.put("Transform Scaling Test", TransformTestScreen.class);
		map.put("H5 渐变血条测试", HealthBarDemoScreen.class);
		map.put("Floating Text (飘字) 测试", TextTestScreen.class);
		map.put("Neon Glow (辉光) 测试(原BioWar)", BloomDebugScreen.class);
		map.put("Synth Audio (合成音效) 测试", AudioTestScreen.class);
		map.put("SmartCamera (智能相机) 测试", CameraTestScreen.class);
		map.put("输入系统综合测试", InputTestScreen.class);
		map.put("连招测试", CombatScreen.class);

		map.put("新测试", null);
		map.put("RichText (富文本) 测试", RichTextTestScreen.class);
		map.put("RichText Layout (布局) 测试", RichTextLayoutTestScreen.class);
		map.put("flatIcon设计器开发", IconEditorDemo.class);

		map.put("游戏内容场景", null);
		map.put("主游戏", GameScreen.class);
	}
}
