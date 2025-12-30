package com.goldsprite.solofight.screens.refactor;

import com.goldsprite.gameframeworks.screens.IGScreen;
import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.refactor.tests.EcsBasicTestScreen;
import java.util.Map;
import com.goldsprite.solofight.screens.refactor.tests.EcsLifecycleTestScreen;
import com.goldsprite.solofight.screens.refactor.tests.EcsFsmTestScreen;

public class RefactorSelectionScreen extends BaseSelectionScreen {

	@Override
	public String getIntroduction() {int k16;
		return "架构重构验证实验室\n(Refactoring Lab)";
	}

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("1. ECS 基础循环验证", EcsBasicTestScreen.class);
		map.put("2. 生命周期测试 (Add/Remove/Destroy)", EcsLifecycleTestScreen.class);
		map.put("3. 状态机 (FSM) 优先级测试", EcsFsmTestScreen.class);
	}
}
