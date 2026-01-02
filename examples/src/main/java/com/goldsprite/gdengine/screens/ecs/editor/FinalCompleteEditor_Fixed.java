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

public class FinalCompleteEditor_Fixed extends GScreen {
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
		// 画大背景
		batch.draw(bgTex, -1000, -1000, 2000, 2000);
		batch.draw(playerTex, playerX - playerTex.getWidth()/2f, playerY - playerTex.getHeight()/2f);
	}

	public void renderDebug(Camera camera) {
		debugRenderer.setProjectionMatrix(camera.combined);
		debugRenderer.begin(ShapeRenderer.ShapeType.Line);

		// 黄色框：世界边界
		debugRenderer.setColor(Color.YELLOW);
		debugRenderer.rect(-WORLD_WIDTH/2, -WORLD_HEIGHT/2, WORLD_WIDTH, WORLD_HEIGHT);

		// 坐标轴
		debugRenderer.setColor(Color.RED);
		debugRenderer.line(-1000, 0, 1000, 0);
		debugRenderer.setColor(Color.GREEN);
		debugRenderer.line(0, -1000, 0, 1000);

		// 目标点
		debugRenderer.setColor(Color.CYAN);
		debugRenderer.line(targetX - 20, targetY, targetX + 20, targetY);
		debugRenderer.line(targetX, targetY - 20, targetX, targetY + 20);

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
		// FBO 纹理翻转
		fboRegion.flip(false, true);

		batch = new SpriteBatch();
		camera = new OrthographicCamera();

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
// 3. UI 展示层 (Consumer) - 重点修复区域
// ==========================================
class ViewWidget extends Widget {
	// 对应显示策略
	public enum DisplayMode { FIT, STRETCH, COVER }

	private ViewTarget target;
	private Texture bgTexture;
	private DisplayMode displayMode = DisplayMode.FIT;

	// 记录绘制参数
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

		// 1. 画背景
		batch.draw(bgTexture, x, y, w, h);

		// 2. 【修复显示逻辑】：处理拉伸和铺满
		float widgetRatio = w / h;
		// FBO 的比例固定是 1280/720
		float fboRatio = (float)target.viewport.getScreenWidth() / target.viewport.getScreenHeight();

		if (displayMode == DisplayMode.STRETCH) {
			// STRETCH: 强行填满，无视比例
			drawX = x; drawY = y;
			drawW = w; drawH = h;
		}
		else if (displayMode == DisplayMode.COVER) {
			// COVER (模拟Extend): 保持比例，填满窗口 (会有裁切)
			if (widgetRatio > fboRatio) {
				// 窗口更宽，匹配宽度，高度溢出
				drawW = w;
				drawH = drawW / fboRatio;
			} else {
				// 窗口更高，匹配高度，宽度溢出
				drawH = h;
				drawW = drawH * fboRatio;
			}
			// 居中
			drawX = x + (w - drawW) / 2;
			drawY = y + (h - drawH) / 2;
		}
		else {
			// FIT: 保持比例，全部显示 (有黑边)
			if (widgetRatio > fboRatio) {
				drawH = h; drawW = drawH * fboRatio;
			} else {
				drawW = w; drawH = drawW / fboRatio;
			}
			drawX = x + (w - drawW) / 2;
			drawY = y + (h - drawH) / 2;
		}

		// 3. 贴 FBO
		batch.draw(target.fboRegion, drawX, drawY, drawW, drawH);
	}

	// 【终极坐标转换】：纯数学硬算，修复Y轴反转，支持所有缩放
	public Vector2 screenToWorld(float localX, float localY) {
		// 1. 算出相对于绘制区域(drawX, drawY)的偏移比例
		// 也就是：点击点在图片上的百分比位置 (0.0 ~ 1.0)
		float percentX = (localX - (drawX - getX())) / drawW;
		float percentY = (localY - (drawY - getY())) / drawH;

		// 3. 计算 FBO 内部的像素坐标
		// FBO 总大小
		float fboTotalW = target.viewport.getScreenWidth();
		float fboTotalH = target.viewport.getScreenHeight();

		// 像素位置
		float fboPixelX = percentX * fboTotalW;
		float fboPixelY = percentY * fboTotalH;

		// 4. 处理 FBO 内部 Viewport 的黑边 (Fit模式下)
		// FitViewport 在 FBO 内部也有 screenX/Y/W/H
		Viewport vp = target.viewport;
		float vpX = vp.getScreenX();
		float vpY = vp.getScreenY();
		float vpW = vp.getScreenWidth();
		float vpH = vp.getScreenHeight();

		// 归一化设备坐标 (NDC) -1 ~ 1
		float ndcX = (fboPixelX - vpX) / vpW * 2.0f - 1.0f;
		float ndcY = (fboPixelY - vpY) / vpH * 2.0f - 1.0f;

		// 6. 映射到世界坐标
		OrthographicCamera cam = (OrthographicCamera) target.camera;
		float worldW = vp.getWorldWidth() * cam.zoom;
		float worldH = vp.getWorldHeight() * cam.zoom;

		float worldX = cam.position.x + ndcX * (worldW / 2f);
		float worldY = cam.position.y + ndcY * (worldH / 2f);

		return new Vector2(worldX, worldY);
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

		// 默认配置
		gameTarget = new ViewTarget(1280, 720);
		gameTarget.setViewportMode(ViewTarget.ViewportMode.FIT);

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
				if (mode.equals("FIT")) {
					gameTarget.setViewportMode(ViewTarget.ViewportMode.FIT);
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
				} else if (mode.equals("STRETCH")) {
					gameTarget.setViewportMode(ViewTarget.ViewportMode.STRETCH);
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.STRETCH);
				} else if (mode.equals("EXTEND")) {
					gameTarget.setViewportMode(ViewTarget.ViewportMode.EXTEND);
					// 编辑器里的 Extend 效果通常是铺满窗口 (Cover)
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);
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
		// Scene 默认也用 Cover 模式看全景
		sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);

		sceneWidget.addListener(new DragListener() {
			@Override
			public void drag(InputEvent event, float x, float y, int pointer) {
				float dx = getDeltaX(); float dy = getDeltaY();
				float zoom = ((OrthographicCamera)sceneTarget.camera).zoom;
				// 反向移动相机
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
