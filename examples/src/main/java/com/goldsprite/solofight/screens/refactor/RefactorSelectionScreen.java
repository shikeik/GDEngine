package com.goldsprite.solofight.screens.refactor;

import com.goldsprite.gameframeworks.screens.IGScreen;
import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.refactor.tests.EcsBasicTestScreen;
import java.util.Map;
import com.goldsprite.solofight.screens.refactor.tests.EcsLifecycleTestScreen;

public class RefactorSelectionScreen extends BaseSelectionScreen {

	@Override
	public String getIntroduction() {
		return "架构重构验证实验室\n(Refactoring Lab)";
	}

	@Override
	protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
		map.put("1. ECS 基础循环验证", EcsBasicTestScreen.class);
		map.put("2. 生命周期测试 (Add/Remove/Destroy)", EcsLifecycleTestScreen.class);
		map.put("3. 父子层级变换测试", null); // 待实现
		map.put("4. 物理碰撞系统测试", null); // 待实现
		map.put("5. 状态机 (FSM) 测试", null); // 待实现
	}
}
