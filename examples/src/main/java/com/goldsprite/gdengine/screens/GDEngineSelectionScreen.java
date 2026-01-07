package com.goldsprite.gdengine.screens;

import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.gdengine.screens.ecs.EcsVisualTestScreen;
import com.goldsprite.gdengine.screens.ecs.JsonLiveEditScreen;
import com.goldsprite.gdengine.screens.ecs.SpriteVisualScreen;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameScreen;
import com.goldsprite.gdengine.screens.ecs.hub.GDEngineHubScreen;
import com.goldsprite.gdengine.screens.ecs.hub.SetupDialog;
import com.goldsprite.gdengine.screens.ecs.skeleton.SkeletonVisualScreen;
import java.util.Map;

public class GDEngineSelectionScreen extends BaseSelectionScreen {int k5;
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put(">>>GDEngine Hub<<<", GDEngineHubScreen.class);
		map.put("", null);
		map.put("编辑器开发", null);
		map.put("引擎 编辑器", EditorGameScreen.class);
		
		map.put("功能验证", null);
		map.put("Ecs 可视化测试 (太阳系)", EcsVisualTestScreen.class);
		map.put("Ecs 骨骼动画集成测试 (NeonSkeleton)", SkeletonVisualScreen.class);
		map.put("Ecs 帧动画测试 (Enma01)", SpriteVisualScreen.class);
		map.put("Ecs 骨骼动画 JSON 实时编辑 (Live Editor)", JsonLiveEditScreen.class);
	}
}
