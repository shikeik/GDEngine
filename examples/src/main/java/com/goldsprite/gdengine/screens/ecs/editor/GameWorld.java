package com.goldsprite.gdengine.screens.ecs.editor;

// ==========================================
// 1. 业务逻辑层 (Model)
// ==========================================
// ==========================================
// GameWorld 的关键修改 (请确保 GameWorld 类已按我们之前的讨论更新)
// 1. 移除了私有的 Camera, Viewport
// 2. GameWorld.init() 不再创建 Camera/Viewport
// 3. GameWorld.render() 使用 Gd.view.getCamera() 和 Gd.view.apply()
// 4. Gd.input/Gd.graphics 被正确使用
// ==========================================
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GameWorld {
	public static final float WORLD_WIDTH = 960; // 配合 Gd.view 的默认逻辑分辨率
	public static final float WORLD_HEIGHT = 540;

	public Texture playerTex, bgTex;
	public float playerX = 0, playerY = 0;
	public float targetX = 0, targetY = 0;
	private ShapeRenderer debugRenderer;

	// 【修改】移除私有 Camera 和 Viewport
	// private Viewport viewport;
	// private Camera camera;

	private SpriteBatch batch; // 游戏自己管理 Batch

	public void init() {
		playerTex = tryLoadTexture("role.png", 32, 32, Color.CORAL);
		bgTex = tryLoadTexture("back.png", 512, 512, Color.TEAL);
		debugRenderer = new ShapeRenderer();

		// 【修改】不再自己 new Camera/Viewport
		// camera = new OrthographicCamera();
		// viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
		// camera.position.set(0, 0, 0);

		batch = new SpriteBatch(); // 游戏自己管理 Batch
	}

	public void update(float delta, float moveX, float moveY) {
		playerX += moveX;
		playerY += moveY;
	}

	// 【修改】render 方法签名，不再需要外部传入 Batch
	// 而是自己获取 Gd.view 的相机和应用视口
	public void render() {
		// 1. 应用视口 (Gd.view 会根据是否被劫持来决定是否加黑边)
		Gd.view.apply();

		// 2. 获取当前相机 (可能是玩家相机，也可能是编辑器相机)
		Camera cam = Gd.view.getCamera();
		batch.setProjectionMatrix(cam.combined);

		batch.begin();
		// 画大背景
		batch.draw(bgTex, -1000, -1000, 2000, 2000);
		batch.draw(playerTex, playerX - playerTex.getWidth() / 2f, playerY - playerTex.getHeight() / 2f);
		batch.end();
	}

	// Debug 渲染也需要获取相机
	public void renderDebug() {
		// 【修改】获取 Gd.view 的相机
		Camera cam = Gd.view.getCamera();
		debugRenderer.setProjectionMatrix(cam.combined);

		debugRenderer.begin(ShapeRenderer.ShapeType.Line);

		debugRenderer.setColor(Color.YELLOW);
		// 使用 GameWorld 的分辨率来画边界
		debugRenderer.rect(-WORLD_WIDTH / 2, -WORLD_HEIGHT / 2, WORLD_WIDTH, WORLD_HEIGHT);

		debugRenderer.setColor(Color.RED);
		debugRenderer.line(-1000, 0, 1000, 0);
		debugRenderer.setColor(Color.GREEN);
		debugRenderer.line(0, -1000, 0, 1000);

		debugRenderer.setColor(Color.CYAN);
		debugRenderer.line(targetX - 20, targetY, targetX + 20, targetY);
		debugRenderer.line(targetX, targetY - 20, targetX, targetY + 20);

		debugRenderer.end();
	}

	private Texture tryLoadTexture(String path, int w, int h, Color c) {
		try {
			// 【修改】使用 Gd.files
			return new Texture(Gd.files.internal(path));
		} catch (Exception e) {
			return createSolidTexture(w, h, c);
		}
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
		batch.dispose(); // 释放自己的 Batch
	}
}
