package com.hellogame;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.RenderComponent;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.core.annotations.*;

class RotorComponent extends RenderComponent {
	public float animSpeed;
	@Header("Offset")
	public float x; // 现在作为 Offset X
	public float y; // 现在作为 Offset Y

	public float size;
	public float startAngle;
	public final Color c;

	@Tooltip("当前旋转角度")
	@ReadOnly
	public float curAngle;

	public RotorComponent(float x, float y, float size, Color c, float animSpeed, float startAngle) {
		this.animSpeed = animSpeed;
		this.x = x; // 偏移量
		this.y = y; // 偏移量
		this.size = size;
		this.startAngle = startAngle;
		this.c = c;
		this.sortingLayer = "Entity";
	}

	public RotorComponent() {
		this(0, 0, 100, Color.RED, 0.3f, 0f);
	}

	@Override
	public void update(float delta) {
		// 纯逻辑
	}

	@Override
	public void render(NeonBatch batch, Camera camera) {
		if (transform == null) return;

		float angle = 360f * GameWorld.getTotalTime() * animSpeed;
		curAngle = angle;

		// 计算最终绘制坐标: 世界坐标 + 偏移量
		float drawX = transform.worldPosition.x + x;
		float drawY = transform.worldPosition.y + y;

		batch.drawRect(drawX - size / 2f, drawY - size / 2f, size, size, startAngle + angle, 10, c, false);
	}

	@Override
	public boolean contains(float wx, float wy) {
		if (transform == null) return false;

		// 计算点击检测区域中心
		float centerX = transform.worldPosition.x + x;
		float centerY = transform.worldPosition.y + y;

		return wx >= centerX - size/2 && wx <= centerX + size/2 &&
			wy >= centerY - size/2 && wy <= centerY + size/2;
	}
}
