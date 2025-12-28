package com.goldsprite.solofight.core;

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
	public float maxZoom = 100.0f;

	// [v3.5] 地图约束开关 (默认关闭)
	public boolean mapConstraint = false;

	private Rectangle mapBounds = new Rectangle(-1000, -1000, 2000, 2000);

	// --- 状态 ---
	private final OrthographicCamera camera;
	private final Vector3 position = new Vector3();
	private float zoom = 1.0f;

	private float trauma = 0f;
	private float shakePower = 30f;
	private float shakeDecay = 1.5f;

	// --- 调试数据 ---
	private final Rectangle rawBounds = new Rectangle();    // 红: 原始极值
	private final Rectangle aspectBounds = new Rectangle(); // 蓝: Raw + Aspect (无Padding)
	private final Rectangle finalBounds = new Rectangle();  // 黄: Final Camera View

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

		// 1. 计算 Raw Bounds (绝对包围盒)
		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

		for (Vector2 t : targets) {
			if (t.x < minX) minX = t.x; if (t.x > maxX) maxX = t.x;
			if (t.y < minY) minY = t.y; if (t.y > maxY) maxY = t.y;
		}
		if (maxX == minX) { minX -= 10; maxX += 10; }
		if (maxY == minY) { minY -= 10; maxY += 10; }
		rawBounds.set(minX, minY, maxX - minX, maxY - minY);

		// 获取屏幕比例
		float viewportW = camera.viewportWidth;
		float viewportH = camera.viewportHeight;
		float screenAspect = viewportW / viewportH;

		// 2. 计算 Aspect Bounds (蓝框: Raw 适配屏幕比例, 无Padding)
		// [v3.5] 修正需求：Raw紧贴但符合比例
		calculateAspectRect(rawBounds, aspectBounds, screenAspect, 0); // padding = 0
		if (mapConstraint) applyMapConstraint(aspectBounds); // 蓝框也要受约束

		// 3. 计算 Final Camera Target (黄框逻辑)
		// 3.1 基础计算: Raw + Padding -> Aspect
		Rectangle targetRect = new Rectangle(); // 临时变量
		calculateAspectRect(rawBounds, targetRect, screenAspect, padding);

		// 3.2 计算理想 Zoom
		float targetZoom = targetRect.width / viewportW; // 宽/宽 或 高/高 是一样的，因为已经修正过比例
		targetZoom = MathUtils.clamp(targetZoom, minZoom, maxZoom);

		// 3.3 根据 Zoom 反推视口大小 (因为 Zoom 限制可能导致视野比 TargetRect 更小或更大)
		float viewW = viewportW * targetZoom;
		float viewH = viewportH * targetZoom;

		// 3.4 确定中心点
		float centerX = targetRect.x + targetRect.width / 2;
		float centerY = targetRect.y + targetRect.height / 2;

		// 3.5 约束中心点 (Clamping)
		// [v3.5] 修正逻辑：确保 viewW/viewH 矩形在 mapBounds 内
		if (mapConstraint) {
			Vector2 clampedCenter = clampCenter(centerX, centerY, viewW, viewH);
			centerX = clampedCenter.x;
			centerY = clampedCenter.y;
		}

		// 4. 平滑 (Smoothing)
		if (smoothEnabled) {
			float alpha = Math.min(1.0f, delta * smoothSpeed);
			this.position.x += (centerX - this.position.x) * alpha;
			this.position.y += (centerY - this.position.y) * alpha;
			this.zoom += (targetZoom - this.zoom) * alpha;
		} else {
			this.position.x = centerX; this.position.y = centerY; this.zoom = targetZoom;
		}

		// 5. 震动 (Shake)
		float shakeX = 0, shakeY = 0;
		if (trauma > 0) {
			float shake = trauma * trauma;
			shakeX = (MathUtils.random() * 2 - 1) * shakePower * shake;
			shakeY = (MathUtils.random() * 2 - 1) * shakePower * shake;
			trauma -= delta * shakeDecay;
			if (trauma < 0) trauma = 0;
		}

		// 6. 输出 Final Bounds (用于调试显示)
		float finalW = viewportW * this.zoom;
		float finalH = viewportH * this.zoom;
		finalBounds.set(
			this.position.x - finalW / 2 + shakeX,
			this.position.y - finalH / 2 + shakeY,
			finalW, finalH
		);
		// 注意：FinalBounds 包含了震动偏移，这可能导致稍微超出地图，这是符合预期的(震动不应被硬切)
	}

	// 辅助：将 src 矩形扩展为符合 aspect 的 dst 矩形，并增加 pad
	private void calculateAspectRect(Rectangle src, Rectangle dst, float aspect, float pad) {
		float targetW = src.width + pad * 2;
		float targetH = src.height + pad * 2;
		float targetAspect = targetW / targetH;

		if (targetAspect > aspect) targetH = targetW / aspect;
		else targetW = targetH * aspect;

		float cx = src.x + src.width / 2;
		float cy = src.y + src.height / 2;
		dst.set(cx - targetW / 2, cy - targetH / 2, targetW, targetH);
	}

	// 辅助：直接约束矩形在地图内 (用于 Blue Box)
	private void applyMapConstraint(Rectangle rect) {
		// 如果矩形比地图还大，强制居中
		if (rect.width > mapBounds.width) {
			rect.x = mapBounds.x + mapBounds.width/2 - rect.width/2;
		} else {
			if (rect.x < mapBounds.x) rect.x = mapBounds.x;
			if (rect.x + rect.width > mapBounds.x + mapBounds.width) rect.x = mapBounds.x + mapBounds.width - rect.width;
		}

		if (rect.height > mapBounds.height) {
			rect.y = mapBounds.y + mapBounds.height/2 - rect.height/2;
		} else {
			if (rect.y < mapBounds.y) rect.y = mapBounds.y;
			if (rect.y + rect.height > mapBounds.y + mapBounds.height) rect.y = mapBounds.y + mapBounds.height - rect.height;
		}
	}

	// 辅助：计算受约束的中心点 (用于 Camera)
	private Vector2 tmpVec = new Vector2();
	private Vector2 clampCenter(float cx, float cy, float vw, float vh) {
		float minX = mapBounds.x + vw / 2;
		float maxX = mapBounds.x + mapBounds.width - vw / 2;
		float minY = mapBounds.y + vh / 2;
		float maxY = mapBounds.y + mapBounds.height - vh / 2;

		if (minX > maxX) cx = mapBounds.x + mapBounds.width / 2;
		else cx = MathUtils.clamp(cx, minX, maxX);

		if (minY > maxY) cy = mapBounds.y + mapBounds.height / 2;
		else cy = MathUtils.clamp(cy, minY, maxY);

		return tmpVec.set(cx, cy);
	}

	public void apply() {
		camera.position.x = finalBounds.x + finalBounds.width / 2;
		camera.position.y = finalBounds.y + finalBounds.height / 2;
		camera.zoom = this.zoom;
		camera.update();
	}

	public void drawDebug(ShapeRenderer renderer, SpriteBatch batch, BitmapFont font, boolean drawFill) {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		renderer.setProjectionMatrix(camera.combined);

		// 1. 填充层
		if (drawFill) {
			renderer.begin(ShapeRenderer.ShapeType.Filled);
			renderer.setColor(0f, 1f, 1f, 0.1f); // Blue
			renderer.rect(aspectBounds.x, aspectBounds.y, aspectBounds.width, aspectBounds.height);
			renderer.setColor(1f, 1f, 0f, 0.15f); // Yellow
			renderer.rect(finalBounds.x, finalBounds.y, finalBounds.width, finalBounds.height);
			renderer.end();
		}

		// 2. 线框层
		renderer.begin(ShapeRenderer.ShapeType.Line);
		// Map (Orange)
		renderer.setColor(1f, 0.6f, 0f, 1f);
		renderer.rect(mapBounds.x, mapBounds.y, mapBounds.width, mapBounds.height);
		// Raw (Magenta)
		renderer.setColor(1f, 0f, 1f, 1f);
		renderer.rect(rawBounds.x, rawBounds.y, rawBounds.width, rawBounds.height);
		// Target Aspect (Cyan) - [v3.5] Raw + Aspect
		renderer.setColor(0f, 1f, 1f, 0.8f);
		renderer.rect(aspectBounds.x, aspectBounds.y, aspectBounds.width, aspectBounds.height);
		// Final (Yellow)
		renderer.setColor(1f, 1f, 0f, 1f);
		renderer.rect(finalBounds.x, finalBounds.y, finalBounds.width, finalBounds.height);
		renderer.end();

		Gdx.gl.glDisable(GL20.GL_BLEND);

		// 3. 标签
		if (batch != null && font != null) {
			batch.setProjectionMatrix(camera.combined);
			batch.begin();
			float fontScale = 1.5f * camera.zoom;
			font.getData().setScale(fontScale);

			drawLabel(batch, font, "MAP", mapBounds, Color.ORANGE, fontScale);
			drawLabel(batch, font, "RAW", rawBounds, Color.MAGENTA, fontScale);
			drawLabel(batch, font, "RAW+ASPECT", aspectBounds, Color.CYAN, fontScale);
			drawLabel(batch, font, "FINAL CAM", finalBounds, Color.YELLOW, fontScale);

			batch.end();
		}
	}

	private void drawLabel(SpriteBatch batch, BitmapFont font, String text, Rectangle r, Color color, float scale) {
		font.setColor(color);
		font.draw(batch, text, r.x + r.width - 10 * scale, r.y + r.height + 20 * scale);
	}

	public OrthographicCamera getCamera() { return camera; }
}
