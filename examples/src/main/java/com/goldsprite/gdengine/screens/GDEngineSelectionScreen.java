package com.goldsprite.gdengine.screens;

import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.gdengine.screens.ecs.EcsVisualTestScreen;
import com.goldsprite.gdengine.screens.ecs.JsonLiveEditScreen;
import com.goldsprite.gdengine.screens.ecs.SpriteVisualScreen;
import com.goldsprite.gdengine.screens.ecs.editor.SceneGameEditorWrapper2;
import com.goldsprite.gdengine.screens.ecs.skeleton.SkeletonVisualScreen;

import java.util.Map;
import com.goldsprite.gdengine.screens.ecs.editor.SceneViewDemoScreen;
import com.goldsprite.gdengine.screens.ecs.editor.SceneGameEditorWrapper;
import com.goldsprite.gdengine.screens.ecs.editor.SceneGameEditorWrapperFBO;
import com.goldsprite.gdengine.screens.ecs.editor.FinalEditorTest;

public class GDEngineSelectionScreen extends BaseSelectionScreen {int k11;
	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		map.put("功能验证", null);
		map.put("Ecs 可视化测试 (太阳系)", EcsVisualTestScreen.class);
		map.put("Ecs 骨骼动画集成测试 (NeonSkeleton)", SkeletonVisualScreen.class);
		map.put("Ecs 帧动画测试 (Enma01)", SpriteVisualScreen.class);
		map.put("Ecs 骨骼动画 JSON 实时编辑 (Live Editor)", JsonLiveEditScreen.class); // 新增

		map.put("编辑器开发", null);
		map.put("裁剪世界视图开发", SceneViewDemoScreen.class);
		map.put("ai编辑器视图", SceneGameEditorWrapper.class);
		map.put("ai编辑器视图2", SceneGameEditorWrapper2.class);
		map.put("ai编辑器视图3FBO!", SceneGameEditorWrapperFBO.class);
		map.put("ai编辑器视图4FBO2!", FinalEditorTest.class);
	}
}
