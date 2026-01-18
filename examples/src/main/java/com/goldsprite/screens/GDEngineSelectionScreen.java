package com.goldsprite.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.RichTextLayoutTestScreen;
import com.goldsprite.gdengine.screens.VisTabTestScreen2;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.gdengine.screens.ecs.EcsVisualTestScreen;
import com.goldsprite.gdengine.screens.ecs.JsonLiveEditScreen;
import com.goldsprite.gdengine.screens.ecs.SpriteVisualScreen;
import com.goldsprite.gdengine.screens.ecs.skeleton.SkeletonVisualScreen;

import java.util.Map;

public class GDEngineSelectionScreen extends BaseSelectionScreen {
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
//		map.put(">>>GDEngine Hub<<<", GDEngineHubScreen.class);
//		map.put("", null);
//		map.put("编辑器开发", null);
//		map.put("引擎 编辑器", EditorGameScreen.class);

		map.put("功能验证 ECS", null);
		map.put("Ecs 可视化测试 (太阳系)", EcsVisualTestScreen.class);
		map.put("Ecs 骨骼动画集成测试 (NeonSkeleton)", SkeletonVisualScreen.class);
		map.put("Ecs 帧动画测试 (Enma01)", SpriteVisualScreen.class);
		map.put("Ecs 骨骼动画 JSON 实时编辑 (Live Editor)", JsonLiveEditScreen.class);

		map.put("功能验证 其他", null);
		map.put("富文本布局测试", RichTextLayoutTestScreen.class);
		map.put("UI 测试: VisTabbedPane (源码复刻)", VisTabTestScreen2.class);

		map.put("临时观测测试(用完即删)", null);
	}
}
