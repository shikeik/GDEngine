package com.goldsprite.gdengine.screens.ecs.editor.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class EditorGizmoSystem {
	public enum Mode { NONE, MOVE, ROTATE, SCALE }
	public Mode mode = Mode.MOVE;

	private final EditorSceneManager sceneManager;

	// 配置
	public static float HANDLE_SIZE = 15f;
	public static float AXIS_LEN = 80f;
	public static float LINE_WIDTH = 2f;

	public EditorGizmoSystem(EditorSceneManager sceneManager) {
		this.sceneManager = sceneManager;
	}

	public void render(NeonBatch batch, float zoom) {
		GObject t = sceneManager.getSelection();
		if (t == null) return;

		// 直接读取真实 Transform
		TransformComponent trans = t.transform;
		float x = trans.worldPosition.x;
		float y = trans.worldPosition.y;
		float rot = trans.worldRotation;

		float s = zoom; // 缩放系数

		// 1. 中心点
		batch.drawCircle(x, y, 6f * s, 0, Color.YELLOW, 16, true);

		float rad = rot * MathUtils.degreesToRadians;
		float cos = MathUtils.cos(rad);
		float sin = MathUtils.sin(rad);
		float len = AXIS_LEN * s;

		// 2. 根据模式绘制
		if (mode == Mode.MOVE) {
			// X轴 (红)
			float xx = x + cos * len;
			float xy = y + sin * len;
			batch.drawLine(x, y, xx, xy, LINE_WIDTH * s, Color.RED);
			// 箭头简略画个方块代替先
			batch.drawRect(xx - 5*s, xy - 5*s, 10*s, 10*s, rot, 0, Color.RED, true);

			// Y轴 (绿)
			float yx = x - sin * len;
			float yy = y + cos * len;
			batch.drawLine(x, y, yx, yy, LINE_WIDTH * s, Color.GREEN);
			batch.drawRect(yx - 5*s, yy - 5*s, 10*s, 10*s, rot, 0, Color.GREEN, true);
		}
		else if (mode == Mode.ROTATE) {
			batch.drawCircle(x, y, len, 2f*s, Color.CYAN, 64, false);
			// 这里可以加一个指示当前角度的线
			float hx = x + cos * len;
			float hy = y + sin * len;
			batch.drawLine(x, y, hx, hy, 1f*s, Color.YELLOW);
			batch.drawCircle(hx, hy, 8f*s, 0, Color.YELLOW, 16, true);
		}
		else if (mode == Mode.SCALE) {
			// 简单画个方块轴
			float xx = x + cos * len;
			float xy = y + sin * len;
			batch.drawLine(x, y, xx, xy, LINE_WIDTH * s, Color.RED);
			batch.drawRect(xx - 6*s, xy - 6*s, 12*s, 12*s, rot, 0, Color.RED, true);

			float yx = x - sin * len;
			float yy = y + cos * len;
			batch.drawLine(x, y, yx, yy, LINE_WIDTH * s, Color.GREEN);
			batch.drawRect(yx - 6*s, yy - 6*s, 12*s, 12*s, rot, 0, Color.GREEN, true);
		}
	}
}
