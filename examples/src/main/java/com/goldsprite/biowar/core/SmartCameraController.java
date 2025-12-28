package com.goldsprite.biowar.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

public class SmartCameraController {

	// --- 配置 ---
	public boolean smoothEnabled = true;
	public float smoothSpeed = 5.0f;
	public float padding = 100f;
	public float minZoom = 0.5f;
	public float maxZoom = 2.0f;

	private Rectangle mapBounds = new Rectangle(-1000, -1000, 2000, 2000);

	// --- 状态 ---
	private final OrthographicCamera camera;
	private final Vector3 position = new Vector3();
	private float zoom = 1.0f;

	private float trauma = 0f;
	private float shakePower = 30f;
	private float shakeDecay = 1.5f;

	// --- 调试数据 ---
	private final Rectangle rawBounds = new Rectangle();
	private final Rectangle aspectBounds = new Rectangle();
	private final Rectangle finalBounds = new Rectangle();

	public SmartCameraController(OrthographicCamera camera) {
		this.camera = camera;
		this.position.set(camera.position);
		this.zoom = camera.zoom;
	}

	public void setMapBounds(float x, float y, float w, float h) {
		this.mapBounds.set(x, y, w, h);
	}

	public void addTrauma(float amount) {
		this.trauma = MathUtils.clamp(this.trauma + amount, 0, 1);
	}

	public void update(List<Vector2> targets, float delta) {
		if (targets == null || targets.isEmpty()) return;

		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

		for (Vector2 t : targets) {
			if (t.x < minX) minX = t.x; if (t.x > maxX) maxX = t.x;
			if (t.y < minY) minY = t.y; if (t.y > maxY) maxY = t.y;
		}

		if (maxX == minX) { minX -= 10; maxX += 10; }
		if (maxY == minY) { minY -= 10; maxY += 10; }

		rawBounds.set(minX, minY, maxX - minX, maxY - minY);

		float targetW = rawBounds.width + padding * 2;
		float targetH = rawBounds.height + padding * 2;

		float viewportW = camera.viewportWidth;
		float viewportH = camera.viewportHeight;
		float screenAspect = viewportW / viewportH;
		float targetAspect = targetW / targetH;

		if (targetAspect > screenAspect) targetH = targetW / screenAspect;
		else targetW = targetH * screenAspect;

		float centerX = rawBounds.x + rawBounds.width / 2;
		float centerY = rawBounds.y + rawBounds.height / 2;

		aspectBounds.set(centerX - targetW / 2, centerY - targetH / 2, targetW, targetH);

		float targetZoom = Math.max(targetW / viewportW, targetH / viewportH);
		targetZoom = MathUtils.clamp(targetZoom, minZoom, maxZoom);

		// Clamping
		float viewW = viewportW * targetZoom;
		float viewH = viewportH * targetZoom;

		float minCamX = mapBounds.x + viewW / 2;
		float maxCamX = mapBounds.x + mapBounds.width - viewW / 2;
		float minCamY = mapBounds.y + viewH / 2;
		float maxCamY = mapBounds.y + mapBounds.height - viewH / 2;

		if (minCamX > maxCamX) centerX = mapBounds.x + mapBounds.width / 2;
		else centerX = MathUtils.clamp(centerX, minCamX, maxCamX);

		if (minCamY > maxCamY) centerY = mapBounds.y + mapBounds.height / 2;
		else centerY = MathUtils.clamp(centerY, minCamY, maxCamY);

		if (smoothEnabled) {
			float alpha = Math.min(1.0f, delta * smoothSpeed);
			this.position.x += (centerX - this.position.x) * alpha;
			this.position.y += (centerY - this.position.y) * alpha;
			this.zoom += (targetZoom - this.zoom) * alpha;
		} else {
			this.position.x = centerX; this.position.y = centerY; this.zoom = targetZoom;
		}

		float shakeX = 0, shakeY = 0;
		if (trauma > 0) {
			float shake = trauma * trauma; 
			shakeX = (MathUtils.random() * 2 - 1) * shakePower * shake;
			shakeY = (MathUtils.random() * 2 - 1) * shakePower * shake;
			trauma -= delta * shakeDecay;
			if (trauma < 0) trauma = 0;
		}

		float finalW = viewportW * this.zoom;
		float finalH = viewportH * this.zoom;
		finalBounds.set(this.position.x - finalW / 2 + shakeX, this.position.y - finalH / 2 + shakeY, finalW, finalH);
	}

	public void apply() {
		camera.position.x = finalBounds.x + finalBounds.width / 2;
		camera.position.y = finalBounds.y + finalBounds.height / 2;
		camera.zoom = this.zoom; 
		camera.update();
	}

	/**
	 * 可视化调试 (v3.4: 支持开关填充背景)
	 * @param drawFill 是否绘制半透明底色
	 */
	public void drawDebug(ShapeRenderer renderer, SpriteBatch batch, BitmapFont font, boolean drawFill) {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		renderer.setProjectionMatrix(camera.combined);

		// 1. 填充层 (Filled) - 可开关
		if (drawFill) {
			renderer.begin(ShapeRenderer.ShapeType.Filled);
			// 理想视口 (Target): 亮青色
			renderer.setColor(0f, 1f, 1f, 0.1f); 
			renderer.rect(aspectBounds.x, aspectBounds.y, aspectBounds.width, aspectBounds.height);
			// 最终相机 (Final): 亮黄色
			renderer.setColor(1f, 1f, 0f, 0.15f); 
			renderer.rect(finalBounds.x, finalBounds.y, finalBounds.width, finalBounds.height);
			renderer.end();
		}

		// 2. 线框层 (Line) - 始终绘制，高亮
		renderer.begin(ShapeRenderer.ShapeType.Line);
		// 地图边界: 亮橙色
		renderer.setColor(1f, 0.6f, 0f, 1f);
		renderer.rect(mapBounds.x, mapBounds.y, mapBounds.width, mapBounds.height);
		// 原始包围 (Raw): 亮洋红
		renderer.setColor(1f, 0f, 1f, 1f);
		renderer.rect(rawBounds.x, rawBounds.y, rawBounds.width, rawBounds.height);
		// 理想视口: 亮青
		renderer.setColor(0f, 1f, 1f, 0.8f);
		renderer.rect(aspectBounds.x, aspectBounds.y, aspectBounds.width, aspectBounds.height);
		// 最终相机: 亮黄
		renderer.setColor(1f, 1f, 0f, 1f);
		renderer.rect(finalBounds.x, finalBounds.y, finalBounds.width, finalBounds.height);
		renderer.end();

		Gdx.gl.glDisable(GL20.GL_BLEND);

		// 3. 标签层 (Text)
		if (batch != null && font != null) {
			batch.setProjectionMatrix(camera.combined);
			batch.begin();
			// 字体大小随缩放调整，防止缩小地图时字太小
			float fontScale = 1.5f * camera.zoom;
			font.getData().setScale(fontScale);

			drawLabel(batch, font, "MAP BOUNDS", mapBounds, Color.ORANGE, fontScale);
			drawLabel(batch, font, "RAW", rawBounds, Color.MAGENTA, fontScale);
			drawLabel(batch, font, "TARGET ASPECT", aspectBounds, Color.CYAN, fontScale);
			drawLabel(batch, font, "FINAL CAM", finalBounds, Color.YELLOW, fontScale);

			batch.end();
		}
	}

	private void drawLabel(SpriteBatch batch, BitmapFont font, String text, Rectangle r, Color color, float scale) {
		font.setColor(color);
		// 绘制在矩形右上角，偏移量随 scale 调整
		font.draw(batch, text, r.x + r.width - 10 * scale, r.y + r.height + 20 * scale);
	}

	public OrthographicCamera getCamera() { return camera; }
}
