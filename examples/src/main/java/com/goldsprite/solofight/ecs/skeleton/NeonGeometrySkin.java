package com.goldsprite.solofight.ecs.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;

/**
 * 默认几何皮肤
 * 支持：矩形、圆形，支持实心填充或描边
 */
public class NeonGeometrySkin implements BoneSkin {

	public enum Shape { BOX, CIRCLE }

	public Shape shape = Shape.BOX;
	public float width = 10f;  // 宽度（垂直于骨骼方向）
	public boolean filled = true; // 是否填充
	public float strokeWidth = 2f; // 如果不填充，描边宽度

	public NeonGeometrySkin(Shape shape, float width, boolean filled) {
		this.shape = shape;
		this.width = width;
		this.filled = filled;
	}

	@Override
	public void draw(NeonBatch batch, Affine2 t, float length, Color color) {
		// 骨骼通常是从 (0,0) 指向 (length, 0)
		// 我们根据这个局部坐标系画形状，然后应用矩阵 t

		// 1. 从矩阵反解旋转和缩放 (假设无剪切)
		float rotation = (float) Math.atan2(t.m10, t.m00) * 57.2957795f; // rad -> deg
		float sx = (float) Math.sqrt(t.m00 * t.m00 + t.m10 * t.m10);
		float sy = (float) Math.sqrt(t.m01 * t.m01 + t.m11 * t.m11);

		// 2. 计算几何中心点的世界坐标
		// 局部中心: (length/2, 0)
		float localCx = length / 2f;
		float localCy = 0f;

		// Apply Matrix: x' = x*m00 + y*m01 + m02
		float worldCx = localCx * t.m00 + localCy * t.m01 + t.m02;
		float worldCy = localCx * t.m10 + localCy * t.m11 + t.m12;

		float finalW = length * sx;
		float finalH = width * sy;

		// 3. 绘制
		if (shape == Shape.BOX) {
			batch.drawRect(worldCx, worldCy, finalW, finalH, rotation, strokeWidth, color, filled);
		} else if (shape == Shape.CIRCLE) {
			// 圆形画在骨骼原点 (t.m02, t.m12) 作为关节
			batch.drawCircle(t.m02, t.m12, width * sx, strokeWidth, color, 16, filled);
		}
	}
}
