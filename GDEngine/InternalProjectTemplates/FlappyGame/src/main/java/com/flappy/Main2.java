package com.flappy;

import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;

public class Main2 implements IGameScriptEntry {
	@Override
	public void onStart(GameWorld world) {
		Debug.logT("Script", "Flappy Game Started!");
		// 启动游戏逻辑组件
		new Game(world);
	}

	@Override
	public void onUpdate(float delta) {
	}
}
