package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

// ==========================================
// 2. 渲染核心层 (Producer)
// ==========================================
public class ViewTarget {
	public FrameBuffer fbo;
	public TextureRegion fboRegion;
	// Batch 移除，不再由 Target 管理渲染流程，它只提供环境
	// public SpriteBatch batch;

	private final int fboW;
	private final int fboH;

	public ViewTarget(int w, int h) {
		this.fboW = w;
		this.fboH = h;

		fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
		fboRegion = new TextureRegion(fbo.getColorBufferTexture());
		fboRegion.flip(false, true);
	}

	public int getFboWidth() { return fboW; }
	public int getFboHeight() { return fboH; }

	/**
	 * 纯粹的 FBO 环境准备
	 * 只负责 bind, clear, unbind
	 */
	public void renderToFbo(Runnable renderLogic) {
		fbo.begin();

		// 1. 默认铺满清屏
		Gdx.gl.glViewport(0, 0, fboW, fboH);
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 2. 执行渲染逻辑 (GameWorld 内部会自己处理 batch 和 matrix)
		renderLogic.run();

		fbo.end();
	}

	public void dispose() {
		fbo.dispose();
	}
}





