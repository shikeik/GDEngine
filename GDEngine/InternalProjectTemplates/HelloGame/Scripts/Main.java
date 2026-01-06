package ${PACKAGE_NAME};

import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;
import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;

/**
 * 自动生成的游戏入口脚本
 */
public class Main implements IGameScriptEntry {

	@Override
	public void onStart(GameWorld world) {
		Debug.logT("Script", "游戏启动! 欢迎来到 ${PROJECT_NAME}!");

		// 创建一个示例方块
		createCube(world, 0, 0);
	}

	private void createCube(GameWorld world, float x, float y) {
		GObject obj = new GObject("Cube");
		obj.transform.setPosition(x, y);

		// 动态添加一个简单的渲染组件
		obj.addComponent(new Component() {
			private NeonBatch batch;

			@Override
			public void onAwake() {
				batch = new NeonBatch();
			}

			@Override
			public void update(float delta) {
				// 简单的旋转动画
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
		// 全局逻辑写在这里
	}
}
