package com.goldsprite.solofight.refactor.ecs.component;

import com.badlogic.gdx.math.Vector2;

/**
 * 变换组件：位置、缩放、朝向
 * 替换了原有的 GMath Vector2 为 LibGDX Vector2
 */
public class TransformComponent extends Component {
	public Vector2 position = new Vector2(); // 原点位置
	public Vector2 scale = new Vector2(1, 1);
	public int faceDir = 1; // 1: Right, -1: Left (简化自 Vector2Int)

	public TransformComponent() {}

	public void setPosition(float x, float y) {
		this.position.set(x, y);
	}

	@Override
	public void drawGizmos() {
		// 预留：将来在这里调用 Gizmos.drawCircle(position, 5, Color.YELLOW);
	}
}
