package com.goldsprite.solofight.screens.refactor;

import com.goldsprite.gameframeworks.screens.IGScreen;
import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.refactor.tests.EcsLifecycleTestScreen;
import java.util.Map;

public class RefactorSelectionScreen extends BaseSelectionScreen {

	@Override
	public String getIntroduction() {int k16;
		return "Ecs测试列表\n(Refactoring Lab)";
	}

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("生命周期测试", EcsLifecycleTestScreen.class);
		map.put("增删改查测试", null);
	}
}
