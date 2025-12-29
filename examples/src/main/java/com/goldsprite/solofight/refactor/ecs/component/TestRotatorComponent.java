package com.goldsprite.solofight.refactor.ecs.component;

import com.goldsprite.solofight.core.DebugUI;

/**
 * 测试组件：控制实体左右移动，用于验证 Update 循环
 */
public class TestRotatorComponent extends Component {
	private float speed = 100f;
	private float range = 400f;
	private float startX;

	@Override
	public void awake() {
		super.awake();
		// 记录初始位置
		startX = getTransform().position.x;
		DebugUI.log("[%s] TestRotator Awake!", getGObject().getName());
	}

	@Override
	public void update(float delta) {
		super.update(delta);

		// 移动逻辑
		float currentX = getTransform().position.x;
		currentX += speed * delta;

		// 简单的来回移动
		if (Math.abs(currentX - startX) > range) {
			speed = -speed;
		}

		getTransform().setPosition(currentX, getTransform().position.y);

		// 输出调试信息 (验证 Log 是否刷屏)
		// DebugUI.log("Entity: %s, X: %.2f", getGObject().getName(), currentX);
	}
}
