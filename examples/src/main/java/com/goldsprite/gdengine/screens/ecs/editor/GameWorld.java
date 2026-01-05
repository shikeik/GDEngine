package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// ==========================================
// 1. 业务逻辑层 (Model) - 配置驱动 & 依赖注入版
// ==========================================
public class GameWorld {
	// 不再写死静态常量，而是从 Gd.config 读取
	// public static final float WORLD_WIDTH = 960;

	public Texture playerTex, bgTex;
	public float playerX = 0, playerY = 0;
	public float targetX = 0, targetY = 0;
	private ShapeRenderer debugRenderer;
	private SpriteBatch batch;

	// 【核心】游戏自己的相机和视口
	private Camera gameCamera;
	private Viewport viewport;

	public void init() {
		playerTex = tryLoadTexture("role.png", 32, 32, Color.CORAL);
		bgTex = tryLoadTexture("back.png", 512, 512, Color.TEAL);
		debugRenderer = new ShapeRenderer();
		batch = new SpriteBatch();

		// 初始化时，根据 Gd 配置创建视口
		reloadConfig();
	}

	/**
	 * 【新增】热重载配置
	 * 当编辑器修改分辨率或视口类型时调用
	 */
	public void reloadConfig() {
		Gd.Config conf = Gd.config;

		// 如果相机还没创建，先创建
		if (gameCamera == null) {
			gameCamera = new OrthographicCamera();
			gameCamera.position.set(0, 0, 0);
		}

		// 根据配置工厂化视口
		switch (conf.viewportType) {
			case FIT:
				viewport = new FitViewport(conf.logicWidth, conf.logicHeight, gameCamera);
				break;
			case STRETCH:
				viewport = new StretchViewport(conf.logicWidth, conf.logicHeight, gameCamera);
				break;
			case EXTEND:
				viewport = new ExtendViewport(conf.logicWidth, conf.logicHeight, gameCamera);
				break;
		}

		// 立即应用一次更新 (使用当前屏幕/FBO大小)
		// 注意：Gd.graphics.getWidth() 会根据实机/编辑器模式返回正确的值
		viewport.update(Gd.graphics.getWidth(), Gd.graphics.getHeight(), true);
	}

	public void update(float delta, float moveX, float moveY) {
		playerX += moveX;
		playerY += moveY;

		// 【核心】逻辑更新永远只驱动“游戏相机”
		// 无论渲染时用哪个相机看，游戏逻辑认为主角就在屏幕中间
		gameCamera.position.set(playerX, playerY, 0);
		gameCamera.update();
	}

	/**
	 * 【核心】依赖注入式渲染
	 * @param targetCamera 使用哪个相机来观察世界
	 */
	public void render(Camera targetCamera) {
		// 设置矩阵
		batch.setProjectionMatrix(targetCamera.combined);

		batch.begin();
		// 画背景
		batch.draw(bgTex, -1000, -1000, 2000, 2000);
		// 画角色
		batch.draw(playerTex, playerX - playerTex.getWidth() / 2f, playerY - playerTex.getHeight() / 2f);
		batch.end();
	}

	public void renderDebug(Camera targetCamera) {
		debugRenderer.setProjectionMatrix(targetCamera.combined);
		debugRenderer.begin(ShapeRenderer.ShapeType.Line);

		// 画世界边界 (读取配置的大小)
		float w = Gd.config.logicWidth;
		float h = Gd.config.logicHeight;

		debugRenderer.setColor(Color.YELLOW);
		debugRenderer.rect(-w / 2, -h / 2, w, h);

		debugRenderer.setColor(Color.RED);
		debugRenderer.line(-1000, 0, 1000, 0);
		debugRenderer.setColor(Color.GREEN);
		debugRenderer.line(0, -1000, 0, 1000);

		debugRenderer.setColor(Color.CYAN);
		debugRenderer.line(targetX - 20, targetY, targetX + 20, targetY);
		debugRenderer.line(targetX, targetY - 20, targetX, targetY + 20);

		debugRenderer.end();
	}

	// 暴露给外部使用 (比如 RealGame resize 时需要 update)
	public Viewport getViewport() { return viewport; }
	public Camera getGameCamera() { return gameCamera; }

	private Texture tryLoadTexture(String path, int w, int h, Color c) {
		try { return new Texture(Gd.files.internal(path)); }
		catch (Exception e) { return createSolidTexture(w, h, c); }
	}

	public static Texture createSolidTexture(int w, int h, Color c) {
		Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
		p.setColor(c);
		p.fill();
		Texture t = new Texture(p);
		p.dispose();
		return t;
	}

	public void dispose() {
		if (playerTex != null) playerTex.dispose();
		if (bgTex != null) bgTex.dispose();
		debugRenderer.dispose();
		batch.dispose();
	}
}
