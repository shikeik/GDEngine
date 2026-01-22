package com.goldsprite.gdengine.ecs.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class NeonGeometrySkin implements BoneSkin {

	public enum Shape { BOX, CIRCLE }

	public Shape shape = Shape.BOX;
	public float width = 10f;
	public boolean filled = true;
	public float strokeWidth = 2f;

	public NeonGeometrySkin(Shape shape, float width, boolean filled) {
		this.shape = shape;
		this.width = width;
		this.filled = filled;
	}

	@Override
	public void draw(NeonBatch batch, Affine2 t, float length, Color color) {
		// 1. 反解矩阵
		float rotation = (float) Math.atan2(t.m10, t.m00) * 57.2957795f;
		float sx = (float) Math.sqrt(t.m00 * t.m00 + t.m10 * t.m10);
		float sy = (float) Math.sqrt(t.m01 * t.m01 + t.m11 * t.m11);

		// 2. 计算视觉尺寸 和 中心偏移
		if (shape == Shape.BOX) {
			// [整形关键]
			// 逻辑长度: 0 -> length
			// 视觉范围: -width/2 -> length (向后延伸半个宽度，填补关节缝隙)
			// 这样关节处就是一个以 (0,0) 为中心的圆头效果(如果两段都是这样)

			float extension = width / 2f; // 向后延伸量
			float visualLen = length + extension; // 总视觉长度

			// 视觉中心在局部坐标系的位置
			// Start: -extension, End: length
			// Center: (-extension + length) / 2
			float localCx = (length - extension) / 2f;
			float localCy = 0f;

			// 变换到世界坐标
			float worldCx = localCx * t.m00 + localCy * t.m01 + t.m02;
			float worldCy = localCx * t.m10 + localCy * t.m11 + t.m12;

			float finalW = visualLen * sx;
			float finalH = width * sy;

			// 绘制 (drawRect 接受中心点)
			// 注意：我们这里不需要再手动减半宽半高了，因为 worldCx 就是几何中心
			// 等等，NeonBatch.drawRect 的 x,y 到底是左下角还是中心？
			// 回看之前的修复：batch.drawRect(worldCx - finalW/2f...)
			// 说明 batch.drawRect 的参数是【左下角】。

			batch.drawRect(
				worldCx - finalW / 2f,
				worldCy - finalH / 2f,
				finalW, finalH,
				rotation, strokeWidth, color, filled
			);

		} else if (shape == Shape.CIRCLE) {
			batch.drawCircle(t.m02, t.m12, width * sx, strokeWidth, color, 16, filled);
		}
	}
}
