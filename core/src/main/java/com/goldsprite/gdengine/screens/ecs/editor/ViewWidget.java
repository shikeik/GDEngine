package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.ColorTextureUtils;

// ==========================================
// 3. UI 展示层 (Consumer) - 深度修复版
// ==========================================

public class ViewWidget extends Widget {
	private final ViewTarget target;
	private final Texture bgTexture; // 用于显示Widget自身的底色(调试用)
	private DisplayMode displayMode = DisplayMode.FIT;
	// --- 绘制参数缓存 (用于坐标逆向推导) ---
	// 这些变量描述了：FBO图片到底被画在了Widget里的什么位置、多大尺寸？
	private float drawnImageX, drawnImageY; // 图片绘制的左下角 (相对于 Widget 自身 (0,0))
	private float drawnImageW, drawnImageH; // 图片绘制的实际宽、高 (像素)
	public ViewWidget(ViewTarget target) {
		this.target = target;
		// 深灰色背景，如果能看到它，说明 Widget 这一层有黑边
		bgTexture = ColorTextureUtils.createColorTexture(new Color(0.15f, 0.15f, 0.15f, 1));
	}

	public void setDisplayMode(DisplayMode mode) {
		this.displayMode = mode;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		validate(); // 确保 Scene2D 布局已计算

		// 1. 获取 Widget 在屏幕上的绝对位置和大小
		// getX()/getY() 是相对于父容器的坐标
		float widgetX = getX();
		float widgetY = getY();
		float widgetW = getWidth();
		float widgetH = getHeight();

		// 画 Widget 的底色
		batch.setColor(1, 1, 1, parentAlpha);
		batch.draw(bgTexture, widgetX, widgetY, widgetW, widgetH);

		// 2. 准备计算：FBO 原始比例 vs Widget 比例
		// 【关键】必须用 getFboWidth (物理尺寸)，不能用 viewport.getScreenWidth (逻辑尺寸)
		float fboW = target.getFboWidth();
		float fboH = target.getFboHeight();
		float fboRatio = fboW / fboH;
		float widgetRatio = (widgetH == 0) ? 1 : widgetW / widgetH;

		// 3. 计算图片应该画多大、画哪里 (计算第一层黑边)
		if (displayMode == DisplayMode.STRETCH) {
			drawnImageW = widgetW;
			drawnImageH = widgetH;
			drawnImageX = 0; // 铺满，无偏移
			drawnImageY = 0;
		} else {
			boolean scaleByWidth;
			if (displayMode == DisplayMode.COVER) {
				scaleByWidth = widgetRatio > fboRatio; // 类似 ExtendViewport
			} else {
				scaleByWidth = widgetRatio < fboRatio; // 类似 FitViewport
			}

			if (scaleByWidth) {
				// 宽度对齐，高度自动计算
				drawnImageW = widgetW;
				drawnImageH = widgetW / fboRatio;
			} else {
				// 高度对齐，宽度自动计算
				drawnImageH = widgetH;
				drawnImageW = widgetH * fboRatio;
			}

			// 居中计算：算出相对于 Widget 左下角的偏移量
			drawnImageX = (widgetW - drawnImageW) / 2f;
			drawnImageY = (widgetH - drawnImageH) / 2f;
		}

		// 4. 正式绘制
		// draw 的时候需要绝对坐标，所以加上 widgetX/Y
		batch.draw(target.fboRegion, widgetX + drawnImageX, widgetY + drawnImageY, drawnImageW, drawnImageH);
	}

	/**
	 * [新方法] 将 屏幕物理坐标 (Gdx.input.getX) 转换为 FBO 像素坐标
	 * 供 Gd.input 使用，实现“无感化”输入的基石
	 */
	public Vector2 mapScreenToFbo(float screenX, float screenY) {
		// 1. 利用 Scene2D 自带功能，将 全局屏幕坐标 -> Widget 本地坐标
		Vector2 local = new Vector2(screenX, screenY);
		this.screenToLocalCoordinates(local); // 这一步处理了 Viewport、Camera、Window Padding 等所有 Scene2D 层级的变换

		// 2. Widget 本地坐标 -> FBO 像素坐标
		// (local.x - 图片绘制偏移) / 图片绘制宽 * FBO实际宽
		float percentX = (local.x - drawnImageX) / drawnImageW;
		// 注意：Scene2D Y轴向上，而 local.y 也是向上的，所以直接算即可
		float percentY = (local.y - drawnImageY) / drawnImageH;

		float fboPixelX = percentX * target.getFboWidth();
		float fboPixelY = percentY * target.getFboHeight();

		return local.set(fboPixelX, fboPixelY); // 复用 Vector2 返回
	}

	public boolean isInViewport(int screenX, int screenY) {
		Vector2 local = new Vector2(screenX, screenY);
		this.screenToLocalCoordinates(local);
		
		// 1. Widget 自身边界判定 (必须在 ViewWidget 矩形内)
		if (local.x < 0 || local.x > getWidth() || local.y < 0 || local.y > getHeight()) {
			return false;
		}

		// 2. 绘制内容判定 (必须在 drawnImage 区域内)
		// 注意：在 COVER 模式下，drawnImage 可能比 Widget 大，但我们已经通过第一步限制了只能点 Widget 内部
		// 只有在 FIT 模式下，drawnImage 小于 Widget (有黑边)，这步判定才有实际裁剪意义
		return local.x >= drawnImageX && local.x <= drawnImageX + drawnImageW &&
				local.y >= drawnImageY && local.y <= drawnImageY + drawnImageH;
	}

	/**
	 * [修改后] 原有的 screenToWorld 现在复用上面的逻辑
	 * 增加参数 OrthographicCamera: 直接使用相机进行坐标转换
	 */
	public Vector2 screenToWorld(float screenX, float screenY, OrthographicCamera camera) {
		// 1. 先拿到 FBO 像素坐标
		Vector2 fboPos = mapScreenToFbo(screenX, screenY);
		float fboPixelX = fboPos.x;
		float fboPixelY = fboPos.y;

		// 2. FBO 像素坐标转换为世界坐标
		// 获取FBO的中心点
		float fboCenterX = target.getFboWidth() / 2f;
		float fboCenterY = target.getFboHeight() / 2f;

		// 计算相对于FBO中心的偏移
		float offsetX = fboPixelX - fboCenterX;
		float offsetY = fboPixelY - fboCenterY;

		// 考虑相机缩放
		float worldX = camera.position.x + offsetX * camera.zoom;
		float worldY = camera.position.y + offsetY * camera.zoom;

		return new Vector2(worldX, worldY);
	}

	/**
	 * 保留原有的screenToWorld方法，使用Viewport参数
	 */
	public Vector2 screenToWorld(float screenX, float screenY, Viewport vp) {
		// 1. 先拿到 FBO 像素坐标
		Vector2 fboPos = mapScreenToFbo(screenX, screenY);
		float fboPixelX = fboPos.x;
		float fboPixelY = fboPos.y;

		// 2. FBO 内部 Viewport 映射 (NDC转换)

		// 必须拿到 FBO 内部视口的实际偏移和大小
		float vpX = vp.getScreenX();
		float vpY = vp.getScreenY();
		float vpW = vp.getScreenWidth();
		float vpH = vp.getScreenHeight();

		// 转换为 NDC (-1 ~ 1)
		float ndcX = (fboPixelX - vpX) / vpW * 2.0f - 1.0f;
		float ndcY = (fboPixelY - vpY) / vpH * 2.0f - 1.0f;

		// 3. 投影到世界坐标
		OrthographicCamera cam = (OrthographicCamera) vp.getCamera(); // 直接从 Viewport 拿相机
		float halfWorldW = (vp.getWorldWidth() * cam.zoom) / 2f;
		float halfWorldH = (vp.getWorldHeight() * cam.zoom) / 2f;

		float worldX = cam.position.x + ndcX * halfWorldW;
		float worldY = cam.position.y + ndcY * halfWorldH;

		return new Vector2(worldX, worldY);
	}

	public void dispose() {
		bgTexture.dispose();
	}

	public enum DisplayMode {FIT, STRETCH, COVER}
}
