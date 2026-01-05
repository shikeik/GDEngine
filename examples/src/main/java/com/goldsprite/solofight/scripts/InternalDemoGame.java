package com.goldsprite.solofight.scripts;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 内置测试游戏逻辑
 * 用来验证 GameRunnerScreen 是否能正确加载 IGameScriptEntry
 */
public class InternalDemoGame implements IGameScriptEntry {

	@Override
	public void onStart(GameWorld world) {
		Debug.logT("Script", ">>> InternalDemoGame Start! <<<");
		Debug.logT("Script", "正在创建测试场景...");

		// 创建一个旋转Cube
		createPlayer(0, 0, 100, Color.WHITE, 0.3f);

		// 创建一个"影子" (测试多物体)
		createPlayer(300, 0, 150, Color.BLACK, 0.5f).transform.setScale(0.8f);
	}

	private GObject createPlayer(float x, float y, float size, Color c, float animSpeed) {
		GObject player = new GObject("ScriptPlayer");
		player.transform.setPosition(x, y);
		player.transform.setScale(1f);int k;

		player.addComponent(new Component(){
			private NeonBatch neonBatch;

			@Override public void onAwake() {
				neonBatch = new NeonBatch();
			}
			@Override public void update(float delta) {
				float angle = 360f * GameWorld.getTotalTime() * animSpeed;
				neonBatch.setProjectionMatrix(GameWorld.worldCamera.combined);
				neonBatch.begin();
				neonBatch.drawRect(x - size/2f, y - size/2f, size, size, angle, 10, c, false);
				neonBatch.end();
			}
		});

		return player;
	}

	@Override
	public void onUpdate(float delta) {
		// 简单的全局逻辑测试
		Debug.info("Script Update...%s", GameWorld.getTotalTime());
	}
}
