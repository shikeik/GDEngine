package com.goldsprite.gdengine.ecs.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;

import java.util.List;

@GameSystemInfo(interestComponents = {SpriteComponent.class, TransformComponent.class})
public class SpriteSystem extends BaseSystem {

	private final SpriteBatch batch;
	private OrthographicCamera camera;

	public SpriteSystem(SpriteBatch batch, OrthographicCamera camera) {
		this.batch = batch;
		this.camera = camera;
	}

	public void setCamera(OrthographicCamera camera) {
		this.camera = camera;
	}

	@Override
	public void update(float delta) {
		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		List<GObject> entities = getInterestEntities();
		for (GObject entity : entities) {
			drawEntity(entity);
		}

		batch.end();
	}

	private void drawEntity(GObject entity) {
		SpriteComponent sprite = entity.getComponent(SpriteComponent.class);
		TransformComponent transform = entity.transform;

		if (sprite == null || !sprite.isEnable() || sprite.region == null) return;

		TextureRegion region = sprite.region;

		// [核心修复] 使用世界坐标属性
		// 1. 获取基础世界属性
		float x = transform.worldPosition.x;
		float y = transform.worldPosition.y;
		float rotation = transform.worldRotation;
		float s = transform.worldScale;

		// 2. 处理偏移 (Offset)
		// Offset 是基于物体局部坐标系的，因此需要跟随物体旋转和缩放
		float ox = sprite.offsetX * s;
		float oy = sprite.offsetY * s;

		if (rotation != 0) {
			float cos = MathUtils.cosDeg(rotation);
			float sin = MathUtils.sinDeg(rotation);
			// 2D 旋转公式
			float newOx = ox * cos - oy * sin;
			float newOy = ox * sin + oy * cos;
			ox = newOx;
			oy = newOy;
		}
		// 将旋转缩放后的偏移应用到世界坐标
		x += ox;
		y += oy;

		// 3. 处理尺寸与缩放
		float w = sprite.width;
		float h = sprite.height;

		// Flip 逻辑作用于最终的世界缩放
		float scaleX = s * (sprite.flipX ? -1 : 1);
		float scaleY = s * (sprite.flipY ? -1 : 1);

		// 4. 颜色混合
		Color oldColor = batch.getColor();
		batch.setColor(sprite.color);

		// 5. 绘制
		// SpriteBatch 的 draw 参数：
		// x, y: 绘制矩形的左下角 (未旋转前)
		// originX, originY: 旋转中心 (相对于 x,y)
		// 我们的 x,y 是物体的中心点，所以绘制起始点要减去宽高的一半

		float drawX = x - w / 2f;
		float drawY = y - h / 2f;
		float originX = w / 2f;
		float originY = h / 2f;

		batch.draw(
			region,
			drawX, drawY,
			originX, originY,
			w, h,
			scaleX, scaleY,
			rotation
		);

		// 恢复颜色
		batch.setColor(oldColor);
	}
}
