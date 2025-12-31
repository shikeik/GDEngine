package com.goldsprite.solofight.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;

public class GestureTrail {
	public Vector2 start = new Vector2();
	public Vector2 end = new Vector2();
	public boolean isTap;
	public float life = 1.0f;

	// 缓存吸附后的向量，用于绘制箭头
	private final Vector2 tipVec = new Vector2();
	private final Vector2 normalVec = new Vector2();

	public GestureTrail(float x1, float y1, float x2, float y2, boolean isTap) {
		this.start.set(x1, y1);
		this.isTap = isTap;

		if (isTap) {
			this.end.set(x1, y1);
		} else {
			// [核心复刻] 视觉吸附逻辑 (Visual Snap)
			float dx = x2 - x1;
			float dy = y2 - y1;
			float len = (float) Math.sqrt(dx * dx + dy * dy);
			if (len < 1) len = 1;

			// 计算原始角度
			float angleRad = MathUtils.atan2(dy, dx);

			// 强制吸附到 45 度倍数 (PI/4)
			float step = MathUtils.PI / 4;
			float snappedAngle = Math.round(angleRad / step) * step;

			// 计算吸附后的终点
			this.end.x = x1 + MathUtils.cos(snappedAngle) * len;
			this.end.y = y1 + MathUtils.sin(snappedAngle) * len;

			// 预计算箭头几何数据 (用于渲染)
			float ndx = this.end.x - this.start.x;
			float ndy = this.end.y - this.start.y;
			this.tipVec.set(ndx / len, ndy / len); // 单位方向向量
			this.normalVec.set(-tipVec.y, tipVec.x); // 法线向量
		}
	}

	public boolean update(float delta) {
		life -= delta * 2.5f; // 约 0.4秒消失
		return life > 0;
	}

	public void draw(NeonBatch batch) {
		Color c = new Color(1, 1, 1, life * 0.8f);

		if (isTap) {
			// 绘制菱形 (Tap)
			float size = 30f;
			float[] verts = new float[] {
				start.x, start.y - size, // Top
				start.x + size, start.y, // Right
				start.x, start.y + size, // Bottom
				start.x - size, start.y  // Left
			};
			batch.drawPolygon(verts, 4, 0, c, true);
		} else {
			// 绘制箭头 (Swipe)
			float wStart = 2f;
			float wEnd = 15f;
			float tipLen = 30f;

			// P1: Start Left
			float x1 = start.x - normalVec.x * wStart;
			float y1 = start.y - normalVec.y * wStart;

			// P2: End Left
			float x2 = end.x - normalVec.x * wEnd;
			float y2 = end.y - normalVec.y * wEnd;

			// P3: Tip
			float x3 = end.x + tipVec.x * tipLen;
			float y3 = end.y + tipVec.y * tipLen;

			// P4: End Right
			float x4 = end.x + normalVec.x * wEnd;
			float y4 = end.y + normalVec.y * wEnd;

			// P5: Start Right
			float x5 = start.x + normalVec.x * wStart;
			float y5 = start.y + normalVec.y * wStart;

			float[] verts = new float[] { x1,y1, x2,y2, x3,y3, x4,y4, x5,y5 };
			batch.drawPolygon(verts, 5, 0, c, true);
		}
	}
}
