package com.goldsprite.solofight.screens.ecs;

import com.goldsprite.gameframeworks.screens.IGScreen;
import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import java.util.Map;
import com.goldsprite.solofight.screens.ecs.tests.EcsVisualTestScreen;
import com.goldsprite.solofight.screens.ecs.tests.skeleton.SkeletonVisualScreen;
import com.goldsprite.solofight.screens.ecs.tests.SpriteVisualScreen;
import com.goldsprite.solofight.screens.ecs.tests.JsonLiveEditScreen;

public class RefactorSelectionScreen extends BaseSelectionScreen {

	@Override
	public String getIntroduction() {int k16;
		return "Ecs测试列表\n(Refactoring Lab)";
	}

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("可视化测试 (太阳系)", EcsVisualTestScreen.class);
		map.put("骨骼动画集成测试 (NeonSkeleton)", SkeletonVisualScreen.class);
		map.put("帧动画测试 (Enma01)", SpriteVisualScreen.class);
		map.put("JSON 实时编辑 (Live Editor)", JsonLiveEditScreen.class); // 新增
	}
}
