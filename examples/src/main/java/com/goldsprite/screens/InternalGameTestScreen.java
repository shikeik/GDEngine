package com.goldsprite.screens;

import com.goldsprite.gdengine.screens.ecs.GameRunnerScreen;
import com.goldsprite.solofight.scripts.InternalDemoGame;

/**
 * 包装器屏幕：专门用于在菜单中启动 InternalDemoGame
 */
public class InternalGameTestScreen extends GameRunnerScreen {
	public InternalGameTestScreen() {
		super(new InternalDemoGame());
	}
}
