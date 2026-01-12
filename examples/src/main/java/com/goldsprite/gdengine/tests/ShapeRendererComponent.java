package com.goldsprite.gdengine.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 测试专用：简单的形状渲染组件
 * 用于验证 Transform 的世界坐标计算是否正确
 */
public class ShapeRendererComponent extends Component {

	public enum ShapeType { BOX, CIRCLE, TRIANGLE }

	public ShapeType type = ShapeType.BOX;
	public Color color = new Color(Color.WHITE); // 独立副本
	public float size = 50f;

	// 简单的自转速度 (测试 Update 驱动)
	public float rotateSpeed = 0f;

	public ShapeRendererComponent set(ShapeType type, Color color, float size) {
		this.type = type;
		this.color.set(color);
		this.size = size;
		return this;
	}

	@Override
	public void update(float delta) {
		if (rotateSpeed != 0) {
			transform.rotation += rotateSpeed * delta;
		}
	}

	/**
	 * 核心验证点：
	 * 读取 transform.worldPosition 和 transform.worldRotation 来绘图。
	 */
	public void draw(NeonBatch batch) {
		float x = transform.worldPosition.x;
		float y = transform.worldPosition.y;
		float rot = transform.worldRotation;

		// 简单取 X 轴缩放作为整体大小 (暂不支持非等比缩放的可视化)
		float sx = transform.worldScale.x;
		float sy = transform.worldScale.y;

		// 如果我们没存 worldScale，且有父级，这里其实拿不到父级的缩放叠加。
		// 但对于验证"位移跟随"和"旋转跟随"已经足够了。
		// 若要完美验证缩放传递，NeonBatch 需要支持直接传入 Affine2 矩阵。
		// 这里我们暂时只画位置和旋转。

		float finalSizeX = size * sx;
		float finalSizeY = size * sy;

		switch (type) {
			case BOX:
				// 绘制以(x,y)为中心的矩形
				batch.drawRect(x - finalSizeX/2, y - finalSizeY/2, finalSizeX, finalSizeY, rot, 2f, color, false);
				break;
			case CIRCLE:
				batch.drawCircle(x, y, finalSizeX/2, 2f, color, 16, false);
				break;
			case TRIANGLE:
				batch.drawRegularPolygon(x, y, finalSizeX/2, 3, rot, 2f, color, false);
				break;
		}

		// 画一根黄色指示线，方便看清楚旋转角度
		float len = finalSizeX * 0.6f;
		float cos = MathUtils.cosDeg(rot);
		float sin = MathUtils.sinDeg(rot);
		batch.drawLine(x, y, x + cos * len, y + sin * len, 2f, Color.YELLOW);
	}
}
