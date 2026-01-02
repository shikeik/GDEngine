package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisWindow;

public class FinalCompleteEditor_Step2 extends GScreen {
	private EditorController controller;

	@Override
	public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

	@Override
	public void create() {
		controller = new EditorController();
		controller.create();
	}

	@Override
	public void render(float delta) {
		controller.render(delta);
	}

	@Override
	public void resize(int width, int height) {
		controller.resize(width, height);
	}

	@Override
	public void dispose() {
		controller.dispose();
	}
}

// ==========================================
// 1. 业务逻辑层 (Model)
// ==========================================
class GameWorld {
	// 逻辑世界大小
	public static final float WORLD_WIDTH = 480;
	public static final float WORLD_HEIGHT = 320;

	public Texture playerTex, bgTex;
	public float playerX = 0, playerY = 0;
	public float targetX = 0, targetY = 0;
	private ShapeRenderer debugRenderer;

	public void init() {
		playerTex = tryLoadTexture("role.png", 32, 32, Color.CORAL);
		bgTex = tryLoadTexture("back.png", 512, 512, Color.TEAL);
		debugRenderer = new ShapeRenderer();
	}

	public void update(float delta, float moveX, float moveY) {
		playerX += moveX;
		playerY += moveY;
	}

	public void render(Batch batch) {
		// 背景画大一点
		batch.draw(bgTex, -1000, -1000, 2000, 2000);
		batch.draw(playerTex, playerX - playerTex.getWidth()/2f, playerY - playerTex.getHeight()/2f);
	}

	public void renderDebug(Camera camera) {
		debugRenderer.setProjectionMatrix(camera.combined);
		debugRenderer.begin(ShapeRenderer.ShapeType.Line);

		// 黄色框：设计分辨率边界 (480x320)
		debugRenderer.setColor(Color.YELLOW);
		debugRenderer.rect(-WORLD_WIDTH/2, -WORLD_HEIGHT/2, WORLD_WIDTH, WORLD_HEIGHT);

		// 坐标轴
		debugRenderer.setColor(Color.RED);
		debugRenderer.line(-1000, 0, 1000, 0);
		debugRenderer.setColor(Color.GREEN);
		debugRenderer.line(0, -1000, 0, 1000);

		// 鼠标点击目标点
		debugRenderer.setColor(Color.CYAN);
		debugRenderer.line(targetX - 10, targetY, targetX + 10, targetY);
		debugRenderer.line(targetX, targetY - 10, targetX, targetY + 10);

		debugRenderer.end();
	}

	private Texture tryLoadTexture(String path, int w, int h, Color c) {
		try { return new Texture(Gdx.files.internal(path)); }
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
		if(playerTex != null) playerTex.dispose();
		if(bgTex != null) bgTex.dispose();
		debugRenderer.dispose();
	}
}

// ==========================================
// 2. 渲染核心层 (Producer)
// ==========================================
class ViewTarget {
	// 支持的视口模式
	public enum ViewportMode { FIT, EXTEND, STRETCH }

	public FrameBuffer fbo;
	public TextureRegion fboRegion;
	public SpriteBatch batch;
	public Viewport viewport;
	public Camera camera;

	private int fboW, fboH;

	public ViewTarget(int w, int h) {
		this.fboW = w;
		this.fboH = h;

		fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
		fboRegion = new TextureRegion(fbo.getColorBufferTexture());
		fboRegion.flip(false, true);

		batch = new SpriteBatch();
		camera = new OrthographicCamera();

		// 默认 Fit
		setViewportMode(ViewportMode.FIT);
		camera.position.set(0, 0, 0);
	}

	public void setViewportMode(ViewportMode mode) {
		Camera oldCam = (viewport != null) ? viewport.getCamera() : camera;
		switch (mode) {
			case FIT: viewport = new FitViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, oldCam); break;
			case STRETCH: viewport = new StretchViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, oldCam); break;
			case EXTEND: viewport = new ExtendViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, oldCam); break;
		}
		// 更新视口，告诉它 FBO 的物理大小
		viewport.update(fboW, fboH, false);
	}

	public void renderToFbo(Runnable renderLogic) {
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		fbo.begin();
		Gdx.gl.glViewport(0, 0, fboW, fboH);

		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		renderLogic.run();
		batch.end();

		fbo.end();
	}

	public void dispose() {
		fbo.dispose();
		batch.dispose();
	}
}

// ==========================================
// 3. UI 展示层 (Consumer)
// ==========================================
class ViewWidget extends Widget {
	// 对应 UI 上的显示模式：
	// FIT: Letterbox
	// STRETCH: 强行拉伸填满
	// EXTEND: 模拟填充 (Crop)
	public enum DisplayMode { FIT, STRETCH, EXTEND }

	private ViewTarget target;
	private Texture bgTexture;
	private DisplayMode displayMode = DisplayMode.FIT;

	// 记录画面参数供 Input 使用
	private float drawX, drawY, drawW, drawH;

	public ViewWidget(ViewTarget target) {
		this.target = target;
		bgTexture = GameWorld.createSolidTexture(1, 1, new Color(0.3f, 0.0f, 0.1f, 1));
	}

	public void setDisplayMode(DisplayMode mode) {
		this.displayMode = mode;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		float x = getX(); float y = getY();
		float w = getWidth(); float h = getHeight();

		// 1. 画 Widget 背景
		batch.draw(bgTexture, x, y, w, h);

		// 2. 根据模式计算绘制区域
		if (displayMode == DisplayMode.STRETCH) {
			// 拉伸模式：直接填满
			drawX = x; drawY = y;
			drawW = w; drawH = h;
		} else if (displayMode == DisplayMode.FIT) {
			// Fit 模式：Letterbox
			calculateFit(x, y, w, h);
		} else if (displayMode == DisplayMode.EXTEND) {
			// Extend 模式 (这里简单处理为 Fit，如果想做 ZoomIn 效果需要改这里)
			// 通常编辑器里的 Extend 意味着 "看到更多"，这取决于 ViewTarget 的 ExtendViewport，
			// 而不是 Widget 把 FBO 放大。所以 Widget 还是保持 Fit 展示最合理。
			calculateFit(x, y, w, h);
		}

		// 3. 贴 FBO
		batch.draw(target.fboRegion, drawX, drawY, drawW, drawH);
	}

	private void calculateFit(float x, float y, float w, float h) {
		float widgetRatio = w / h;
		float fboRatio = (float)target.viewport.getScreenWidth() / target.viewport.getScreenHeight();

		if (widgetRatio > fboRatio) {
			drawH = h; drawW = drawH * fboRatio;
		} else {
			drawW = w; drawH = drawW / fboRatio;
		}
		drawX = x + (w - drawW) / 2;
		drawY = y + (h - drawH) / 2;
	}

	// 【终极坐标转换】：Widget点击 -> 游戏世界
	// 不依赖 viewport.unproject，纯数学硬算，解决所有缩放/拉伸问题
	public Vector2 screenToWorld(float localX, float localY) {
		// A. 第一层：UI -> FBO 像素
		float fboX, fboY;

		if (displayMode == DisplayMode.STRETCH) {
			// 拉伸模式下，直接按比例映射
			fboX = localX * (target.viewport.getScreenWidth() / getWidth());
			fboY = localY * (target.viewport.getScreenHeight() / getHeight());
		} else {
			// Fit 模式下，要排除黑边
			float contentX = drawX - getX();
			float contentY = drawY - getY();

			// 点在黑边上
			if (localX < contentX || localX > contentX + drawW) return null;
			if (localY < contentY || localY > contentY + drawH) return null;

			float scaleX = target.viewport.getScreenWidth() / drawW;
			float scaleY = target.viewport.getScreenHeight() / drawH;

			fboX = (localX - contentX) * scaleX;
			fboY = (localY - contentY) * scaleY;
		}

		// B. 第二层：FBO 像素 -> 游戏世界 (手动 Unproject)
		// 这一步必须考虑 ViewTarget 内部 Viewport 的黑边和缩放
		Viewport vp = target.viewport;

		// 1. 归一化设备坐标 (NDC): -1 到 1
		// 注意：FBO 内部 Viewport 有它自己的 screenX/Y/W/H (用于 FBO 内部的 Fit/Letterbox)
		float viewportX = vp.getScreenX();
		float viewportY = vp.getScreenY();
		float viewportW = vp.getScreenWidth();
		float viewportH = vp.getScreenHeight();

		// FBO 像素坐标 y 是 Bottom-Up 的，LibGDX Viewport 也是 Bottom-Up 的 (在 update(w,h,false) 模式下)
		// 所以这里直接用 fboY 即可，不需要翻转，因为我们没有用 Gdx.input

		// 手动计算 World 坐标
		// 公式：World = CameraPos + (Pixel - ViewportCenter) * Zoom

		// 但 FitViewport 内部可能有黑边，用 unproject 最稳，但不能用 Gdx.graphics.getHeight
		// 我们用 Viewport 提供的纯数学 unproject
		Vector3 vec = new Vector3(fboX, fboY, 0);
		vp.unproject(vec);

		return new Vector2(vec.x, vec.y);
	}

	public void dispose() {
		bgTexture.dispose();
	}
}

// ==========================================
// 4. 控制器层 (Controller)
// ==========================================
class EditorController {
	Stage stage;
	GameWorld gameWorld;
	ViewTarget gameTarget, sceneTarget;
	ViewWidget gameWidget, sceneWidget;
	Touchpad joystick;

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		gameWorld = new GameWorld();
		gameWorld.init();

		// 默认分辨率 FBO
		gameTarget = new ViewTarget(1280, 720);
		sceneTarget = new ViewTarget(1280, 720);
		sceneTarget.setViewportMode(ViewTarget.ViewportMode.EXTEND);

		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		createGameWindow();
		createSceneWindow();
	}

	private void createGameWindow() {
		VisWindow win = new VisWindow("Game View");
		win.setResizable(true);
		win.setSize(500, 350);
		win.setPosition(50, 50);

		gameWidget = new ViewWidget(gameTarget);

		gameWidget.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Vector2 worldPos = gameWidget.screenToWorld(x, y);
				if (worldPos != null) {
					gameWorld.targetX = worldPos.x;
					gameWorld.targetY = worldPos.y;
				}
			}
		});

		Texture bg = GameWorld.createSolidTexture(100, 100, Color.DARK_GRAY);
		Texture knob = GameWorld.createSolidTexture(30, 30, Color.LIGHT_GRAY);
		Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
		style.background = new TextureRegionDrawable(new TextureRegion(bg));
		style.knob = new TextureRegionDrawable(new TextureRegion(knob));
		joystick = new Touchpad(10, style);

		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String mode = box.getSelected();
				// 两个层级都要切换，保证逻辑和显示一致
				if (mode.equals("FIT")) {
					gameTarget.setViewportMode(ViewTarget.ViewportMode.FIT);
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
				} else if (mode.equals("STRETCH")) {
					gameTarget.setViewportMode(ViewTarget.ViewportMode.STRETCH);
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.STRETCH);
				} else if (mode.equals("EXTEND")) {
					gameTarget.setViewportMode(ViewTarget.ViewportMode.EXTEND);
					// Extend 逻辑下，Widget 依然保持 Fit 显示（看到更多 FBO 内容），而不是拉伸 Widget
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
				}
			}
		});
		// 强制初始化
		gameTarget.setViewportMode(ViewTarget.ViewportMode.FIT);
		gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);

		Table uiTable = new Table();
		uiTable.setFillParent(true);
		uiTable.add(box).expandX().top().right().width(80).pad(5);
		uiTable.row();
		uiTable.add().expand().fill();
		uiTable.row();
		uiTable.add(joystick).bottom().left().pad(10);

		Stack stack = new Stack();
		stack.add(gameWidget);
		stack.add(uiTable);

		win.add(stack).grow();
		stage.addActor(win);
	}

	private void createSceneWindow() {
		VisWindow win = new VisWindow("Scene View");
		win.setResizable(true);
		win.setSize(400, 300);
		win.setPosition(600, 50);

		sceneWidget = new ViewWidget(sceneTarget);
		sceneWidget.addListener(new DragListener() {
			@Override
			public void drag(InputEvent event, float x, float y, int pointer) {
				float dx = getDeltaX(); float dy = getDeltaY();
				float zoom = ((OrthographicCamera)sceneTarget.camera).zoom;
				sceneTarget.camera.translate(-dx * 2 * zoom, -dy * 2 * zoom, 0);
				sceneTarget.camera.update();
			}
			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				OrthographicCamera cam = (OrthographicCamera)sceneTarget.camera;
				cam.zoom += amountY * 0.1f;
				if(cam.zoom < 0.1f) cam.zoom = 0.1f;
				if(cam.zoom > 5f) cam.zoom = 5f;
				cam.update();
				return true;
			}
		});
		win.add(sceneWidget).grow();
		stage.addActor(win);
	}

	public void render(float delta) {
		float speed = 200 * delta;
		gameWorld.update(delta, joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

		gameTarget.renderToFbo(() -> {
			gameTarget.camera.position.set(gameWorld.playerX, gameWorld.playerY, 0);
			gameTarget.camera.update();
			gameWorld.render(gameTarget.batch);
			gameTarget.batch.end();
			gameWorld.renderDebug(gameTarget.camera);
			gameTarget.batch.begin();
		});

		sceneTarget.renderToFbo(() -> {
			gameWorld.render(sceneTarget.batch);
			sceneTarget.batch.end();
			gameWorld.renderDebug(sceneTarget.camera);
			sceneTarget.batch.begin();
		});

		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	public void dispose() {
		VisUI.dispose();
		gameWorld.dispose();
		gameTarget.dispose();
		sceneTarget.dispose();
		gameWidget.dispose();
		stage.dispose();
	}
}
