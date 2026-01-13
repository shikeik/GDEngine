package com.hellogame;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.log.Debug;

/**
 * 测试游戏逻辑 </br>
 * 用来验证 GameRunnerScreen 是否能正确加载 IGameScriptEntry </br>
 * GDEngine的HelloWorld
 */
public class Main implements IGameScriptEntry {

	// 初始化时执行
	@Override public void onStart(GameWorld world) {
		Debug.logT("Script", "HelloGameTest onStart().");

		// Cube x y size color animSpeed
		createPlayer(0, 0, 100, Color.RED, 0.3f, 0f);
		//
		createPlayer(0, 0, 100, Color.RED, 0.3f, 45f);
	}

	private GObject createPlayer(final float x, final float y, final float size, final Color c, final float animSpeed, final float startAngle) {
		GObject player = new GObject("ScriptPlayer");
		player.transform.setPosition(x, y);
		player.transform.setScale(1f);

		player.addComponent(new RotorComponent(x, y, size, c, animSpeed, startAngle));

		return player;
	}

	// 每帧执行
	@Override public void onUpdate(float delta) {
	}

}
