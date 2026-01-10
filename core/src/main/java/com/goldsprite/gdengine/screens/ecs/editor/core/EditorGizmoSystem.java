package com.goldsprite.gdengine.screens.ecs.editor.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class EditorGizmoSystem {
	public enum Mode { SELECT, MOVE, ROTATE, SCALE }
	public Mode mode = Mode.MOVE;

	private final EditorSceneManager sceneManager;

	// 视觉配置
	public static float HANDLE_SIZE = 15f;
	public static float AXIS_LEN = 80f;
	public static float OUTLINE_WIDTH = 1.5f; // 描边宽度

	// 缓存
	private static final float[] tmpPoly = new float[8];

	public EditorGizmoSystem(EditorSceneManager sceneManager) {
		this.sceneManager = sceneManager;
	}

	public void render(NeonBatch batch, float zoom) {
		GObject t = sceneManager.getSelection();
		if (t == null) return;

		TransformComponent trans = t.transform;
		float x = trans.worldPosition.x;
		float y = trans.worldPosition.y;
		float rot = trans.worldRotation;

		// Gizmo 大小随相机缩放，保持屏幕像素大小一致
		float s = zoom * 1.4f;

		// 1. 中心点 (双色圆)
		drawDualCircle(batch, s, x, y, 5f * s, Color.YELLOW, true);

		float rad = rot * MathUtils.degreesToRadians;
		float cos = MathUtils.cos(rad);
		float sin = MathUtils.sin(rad);
		float centerDist = AXIS_LEN * s;

		// 2. 根据模式绘制
		if (mode == Mode.MOVE) {
			float arrowSize = 14f * s;
			float halfArrow = arrowSize * 0.6f;
			float lineLen = centerDist - halfArrow;

			// X轴 (红)
			float endXx = x + cos * lineLen;
			float endXy = y + sin * lineLen;
			drawDualLine(batch, s, x, y, endXx, endXy, 2f * s, Color.RED);
			drawArrowHead(batch, s, endXx, endXy, rot, arrowSize, Color.RED);

			// Y轴 (绿)
			float endYx = x - sin * lineLen;
			float endYy = y + cos * lineLen;
			drawDualLine(batch, s, x, y, endYx, endYy, 2f * s, Color.GREEN);
			drawArrowHead(batch, s, endYx, endYy, rot + 90f, arrowSize, Color.GREEN);
		}
		else if (mode == Mode.ROTATE) {
			float hx = x + cos * centerDist;
			float hy = y + sin * centerDist;
			// 连线
			drawDualLine(batch, s, x, y, hx, hy, 1.5f * s, Color.YELLOW);
			// 大圆环轨迹 (Visual Guide)
			batch.drawCircle(x, y, centerDist, 1.5f * s, new Color(1, 1, 0, 0.3f), 64, false);
			// 手柄
			drawDualCircle(batch, s, hx, hy, HANDLE_SIZE/2 * s, Color.YELLOW, true);
		}
		else if (mode == Mode.SCALE) {
			float boxSize = 10f * s;

			// X轴
			float endXx = x + cos * centerDist;
			float endXy = y + sin * centerDist;
			drawDualLine(batch, s, x, y, endXx, endXy, 1.5f * s, Color.RED);
			drawDualRect(batch, s, endXx, endXy, boxSize, boxSize, rot, Color.RED);

			// Y轴
			float endYx = x - sin * centerDist;
			float endYy = y + cos * centerDist;
			drawDualLine(batch, s, x, y, endYx, endYy, 1.5f * s, Color.GREEN);
			drawDualRect(batch, s, endYx, endYy, boxSize, boxSize, rot, Color.GREEN);
		}
	}

	// --- 绘图辅助 (White Outline + Colored Core) ---

	private void drawDualLine(NeonBatch batch, float s, float x1, float y1, float x2, float y2, float w, Color c) {
		// 白底
		batch.drawLine(x1, y1, x2, y2, w + OUTLINE_WIDTH*s*2, Color.WHITE);
		// 色芯
		batch.drawLine(x1, y1, x2, y2, w, c);
	}

	private void drawDualCircle(NeonBatch batch, float s, float x, float y, float r, Color c, boolean fill) {
		if(fill) {
			batch.drawCircle(x, y, r + OUTLINE_WIDTH*s, 0, Color.WHITE, 16, true);
			batch.drawCircle(x, y, r, 0, c, 16, true);
		} else {
			// 空心圆目前不支持双色描边，直接画单色
			batch.drawCircle(x, y, r, 2f*s, c, 32, false);
		}
	}

	private void drawDualRect(NeonBatch batch, float s, float cx, float cy, float w, float h, float rot, Color c) {
		float ow = OUTLINE_WIDTH * s;
		float outW = w + ow * 2;
		float outH = h + ow * 2;
		batch.drawRect(cx - outW/2f, cy - outH/2f, outW, outH, rot, 0, Color.WHITE, true);
		batch.drawRect(cx - w/2f, cy - h/2f, w, h, rot, 0, c, true);
	}

	private void drawArrowHead(NeonBatch batch, float s, float bx, float by, float angleDeg, float size, Color c) {
		float rad = angleDeg * MathUtils.degreesToRadians;
		float cos = MathUtils.cos(rad);
		float sin = MathUtils.sin(rad);

		float tipX = bx + cos * size;
		float tipY = by + sin * size;
		float halfW = size * 0.5f;

		float p1x = bx - sin * halfW;
		float p1y = by + cos * halfW;
		float p2x = bx + sin * halfW;
		float p2y = by - cos * halfW;

		tmpPoly[0] = tipX; tmpPoly[1] = tipY;
		tmpPoly[2] = p1x;  tmpPoly[3] = p1y;
		tmpPoly[4] = p2x;  tmpPoly[5] = p2y;

		// 模拟描边：先画大的白色三角形，再画小的彩色三角形
		float ow = OUTLINE_WIDTH * s * 2f;
		batch.drawPolygon(tmpPoly, 3, ow, Color.WHITE, false); // 描边层
		batch.drawPolygon(tmpPoly, 3, 0, c, true); // 填充层
	}
}
