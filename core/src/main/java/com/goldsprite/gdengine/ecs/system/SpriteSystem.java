package com.goldsprite.gdengine.ecs.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

		// 1. 基础属性
		float x = transform.position.x + sprite.offsetX;
		float y = transform.position.y + sprite.offsetY;
		float w = sprite.width;
		float h = sprite.height;

		// 2. 变换属性
		float rotation = transform.rotation;
		float scaleX = transform.scale * (sprite.flipX ? -1 : 1);
		float scaleY = transform.scale * (sprite.flipY ? -1 : 1);

		// 3. 颜色混合 (Sprite颜色 * 批处理颜色)
		Color oldColor = batch.getColor();
		batch.setColor(sprite.color);

		// 4. 绘制
		// 我们假设 Transform.position 是物体的"脚底中心"或"几何中心"
		// 为了方便旋转，通常以中心为原点
		// batch.draw 的 x,y 是左下角，originX,Y 是旋转中心(相对于左下角)

		// 策略：将图片中心对齐到 Transform.position
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
