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
		for (GObject entity : entities) drawEntity(entity);
		batch.end();
	}

	private void drawEntity(GObject entity) {
		SpriteComponent sprite = entity.getComponent(SpriteComponent.class);
		TransformComponent transform = entity.transform;

		if (sprite == null || !sprite.isEnable() || sprite.region == null) return;

		TextureRegion region = sprite.region;

		// [修改] 直接使用 Transform 缓存的世界属性
		float x = transform.worldPosition.x;
		float y = transform.worldPosition.y;
		float rotation = transform.worldRotation;
		float sx = transform.worldScale.x;
		float sy = transform.worldScale.y;

		// Offset 逻辑 (近似)
		float ox = sprite.offsetX * sx;
		float oy = sprite.offsetY * sy;
		if (rotation != 0) {
			float cos = MathUtils.cosDeg(rotation);
			float sin = MathUtils.sinDeg(rotation);
			x += ox * cos - oy * sin;
			y += ox * sin + oy * cos;
		} else {
			x += ox; y += oy;
		}

		// Flip
		if (sprite.flipX) sx = -sx;
		if (sprite.flipY) sy = -sy;

		float w = sprite.width;
		float h = sprite.height;

		Color oldColor = batch.getColor();
		batch.setColor(sprite.color);

		batch.draw(region, x - w / 2f, y - h / 2f, w / 2f, h / 2f, w, h, sx, sy, rotation);

		batch.setColor(oldColor);
	}
}
