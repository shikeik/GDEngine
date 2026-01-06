package com.mygame;

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
public class Main implements IGameScriptEntry {

	//开始时执行
	@Override public void onStart(GameWorld world) {
		Debug.logT("Script", "HelloGameTest脚本项目 onStart().");

		// 创建一个旋转Cube x y size color animSpeed
		createPlayer(0, 0, 100, Color.RED, 0.3f, 0f);
		//再叠一个
		createPlayer(0, 0, 100, Color.RED, 0.3f, 45f);
	}

	private GObject createPlayer(final float x, final float y, final float size, final Color c, final float animSpeed, final float startAngle) {
		GObject player = new GObject("ScriptPlayer");
		player.transform.setPosition(x, y);
		player.transform.setScale(1f);

		player.addComponent(new Component(){
				private NeonBatch neonBatch;

				@Override public void onAwake() {
					neonBatch = new NeonBatch();
				}
				@Override public void update(float delta) {
					float angle = 360f * GameWorld.getTotalTime() * animSpeed;
					neonBatch.setProjectionMatrix(GameWorld.worldCamera.combined);
					neonBatch.begin();
					neonBatch.drawRect(x - size/2f, y - size/2f, size, size, startAngle+angle, 10, c, false);
					neonBatch.end();
				}
			});

		return player;
	}

	// 每帧更新
	@Override public void onUpdate(float delta) {
	}
}
