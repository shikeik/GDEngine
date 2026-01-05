package com.mygame;

import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;
import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;

/**
 * è‡çš¸¸£èæœ
 */
public class Main implements IGameScriptEntry {

	@Override
	public void onStart(GameWorld world) {
		Debug.logT("Script", "æ¸¸æ! æ¬¢è¥å MyGame2!");

		// åˆ»ºä¸ä¸¤ºä¾
		createCube(world, 0, 0);
	}

	private void createCube(GameWorld world, float x, float y) {
		GObject obj = new GObject("Cube");
		obj.transform.setPosition(x, y);

		// åŠ¨æ·»åŠä¸®€•çæ¸²æç»»¶
		obj.addComponent(new Component() {
			private NeonBatch batch;

			@Override
			public void onAwake() {
				batch = new NeonBatch();
			}

			@Override
			public void update(float delta) {
				// ç®•çæ—‹è½¬åŠ¨ç
				float angle = GameWorld.getTotalTime() * 100f;

				batch.setProjectionMatrix(GameWorld.worldCamera.combined);
				batch.begin();
				batch.drawRect(transform.position.x - 25, transform.position.y - 25,
					50, 50, angle, 4, Color.CYAN, false);
				batch.end();
			}
		});
	}

	@Override
	public void onUpdate(float delta) {
		// ¨åé€»è™åœ¨è¿
	}
}
