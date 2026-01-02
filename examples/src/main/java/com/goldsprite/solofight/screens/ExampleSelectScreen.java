package com.goldsprite.solofight.screens;

import com.goldsprite.gameframeworks.screens.GScreen;
import java.util.Map;

import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.ecs.EcsVisualTestScreen;
import com.goldsprite.solofight.screens.ecs.JsonLiveEditScreen;
import com.goldsprite.solofight.screens.ecs.SpriteVisualScreen;
import com.goldsprite.solofight.screens.ecs.skeleton.SkeletonVisualScreen;
import com.goldsprite.solofight.screens.game.GameScreen;
import com.goldsprite.solofight.screens.tests.*;

// [修改] 继承 BaseSelectionScreen
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("场景列表\n", null);

		map.put("测试场景", null);
		map.put("临时测试", TempTestScreen.class);
		map.put("H5 渐变血条测试", HealthBarDemoScreen.class);
		map.put("Floating Text (飘字) 测试", TextTestScreen.class);
		map.put("Neon Glow (辉光) 测试(原BioWar)", BloomDebugScreen.class);
		map.put("Synth Audio (合成音效) 测试", AudioTestScreen.class);
		map.put("SmartCamera (智能相机) 测试", CameraTestScreen.class);
		map.put("输入系统综合测试", InputTestScreen.class);
		map.put("连招测试", CombatScreen.class);
		map.put("Ecs 可视化测试 (太阳系)", EcsVisualTestScreen.class);
		map.put("Ecs 骨骼动画集成测试 (NeonSkeleton)", SkeletonVisualScreen.class);
		map.put("Ecs 帧动画测试 (Enma01)", SpriteVisualScreen.class);
		map.put("Ecs 骨骼动画 JSON 实时编辑 (Live Editor)", JsonLiveEditScreen.class); // 新增

		map.put("编辑器场景", null);

		map.put("游戏内容场景", null);
		map.put("主游戏", GameScreen.class);
	}
}
