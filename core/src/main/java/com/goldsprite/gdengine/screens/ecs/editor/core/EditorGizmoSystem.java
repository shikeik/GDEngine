package com.goldsprite.gdengine.screens.ecs.editor.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
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

	// [新增] 激活的手柄状态 (0:None, 1:X, 2:Y, 3:Center)
	// 由 EditorController 设置
	public int activeScaleHandle = 0;
	public static final int HANDLE_NONE = 0, HANDLE_X = 1, HANDLE_Y = 2, HANDLE_CENTER = 3;

	public EditorGizmoSystem(EditorSceneManager sceneManager) {
		this.sceneManager = sceneManager;
	}

	public void render(NeonBatch batch, float zoom) {
		GObject t = sceneManager.getSelection();
		if (t == null) return;

		// [修改] 直接读缓存
		float x = t.transform.worldPosition.x;
		float y = t.transform.worldPosition.y;
		float rot = t.transform.worldRotation;

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
			float boxSize = 10f * s; // 基础大小

			// [重构] 视觉反馈：按下时放大，否则保持 1:1
			float scaleFactorX = (activeScaleHandle == HANDLE_X) ? 1.5f : 1.0f;
			float scaleFactorY = (activeScaleHandle == HANDLE_Y) ? 1.5f : 1.0f;
			float scaleFactorC = (activeScaleHandle == HANDLE_CENTER) ? 1.5f : 1.0f;

			// 0. 中心等比手柄 (青色)
			// 基础尺寸稍大一点 (1.2倍)，按下再放大
			float centerSize = boxSize * 1.2f * scaleFactorC;
			drawDualRect(batch, s, x, y, centerSize, centerSize, rot, Color.CYAN);

			// X轴 (红色)
			float endXx = x + cos * centerDist;
			float endXy = y + sin * centerDist;
			drawDualLine(batch, s, x, y, endXx, endXy, 1.5f * s, Color.RED);

			float sizeX = boxSize * scaleFactorX;
			drawDualRect(batch, s, endXx, endXy, sizeX, sizeX, rot, Color.RED);

			// Y轴 (绿色)
			float endYx = x - sin * centerDist;
			float endYy = y + cos * centerDist;
			drawDualLine(batch, s, x, y, endYx, endYy, 1.5f * s, Color.GREEN);

			float sizeY = boxSize * scaleFactorY;
			drawDualRect(batch, s, endYx, endYy, sizeY, sizeY, rot, Color.GREEN);
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
