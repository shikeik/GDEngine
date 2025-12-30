package com.goldsprite.solofight.refactor.ecs.component;

import com.goldsprite.solofight.core.DebugUI;

/**
 * 测试组件：控制实体左右移动，用于验证 Update 循环
 */
public class TestRotatorComponent extends Component {
	private float speed = 100f;
	private float range = 400f;
	private float startX;
	private boolean initialized = false;

	// [新增] 配置方法
	public TestRotatorComponent setConfig(float speed, float range) {
		this.speed = speed;
		this.range = range;
		return this;
	}

	@Override
	public void awake() {
		super.awake();
		startX = getTransform().position.x;
		initialized = true;
		// DebugUI.log("[%s] Rotator Awake", getGObject().getName());
	}

	@Override
	public void update(float delta) {
		super.update(delta);
		if (!initialized) return;

		float currentX = getTransform().position.x;
		currentX += speed * delta;

		// 修正逻辑：基于 startX 进行摆动
		// 简单的 PingPong 逻辑
		if (currentX > startX + range && speed > 0) speed = -speed;
		if (currentX < startX - range && speed < 0) speed = -speed;

		getTransform().setPosition(currentX, getTransform().position.y);
	}
}
