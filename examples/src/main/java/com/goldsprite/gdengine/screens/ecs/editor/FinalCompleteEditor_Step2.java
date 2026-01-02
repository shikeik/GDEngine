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
	public Texture playerTex, bgTex;
	public float playerX = 0, playerY = 0;
	public float targetX = 0, targetY = 0;
	private ShapeRenderer debugRenderer;

	public void init() {
		// 如果没有图片，会自动生成色块
		playerTex = tryLoadTexture("role.png", 32, 32, Color.CORAL);
		bgTex = tryLoadTexture("back.png", 512, 512, Color.TEAL);
		debugRenderer = new ShapeRenderer();
	}

	public void update(float delta, float moveX, float moveY) {
		playerX += moveX;
		playerY += moveY;
	}

	public void render(Batch batch) {
		// 【修复2】：背景图强行画大一点 (2000x2000)，保证填满屏幕
		// 假设背景图是重复纹理，或者就是一张大图
		batch.draw(bgTex, -1000, -1000, 2000, 2000);

		// 画玩家
		batch.draw(playerTex, playerX - playerTex.getWidth()/2f, playerY - playerTex.getHeight()/2f);
	}

	public void renderDebug(Camera camera) {
		debugRenderer.setProjectionMatrix(camera.combined);
		debugRenderer.begin(ShapeRenderer.ShapeType.Line);

		debugRenderer.setColor(Color.RED);
		debugRenderer.line(-1000, 0, 1000, 0);
		debugRenderer.setColor(Color.GREEN);
		debugRenderer.line(0, -1000, 0, 1000);

		debugRenderer.setColor(Color.CYAN);
		debugRenderer.line(targetX - 10, targetY, targetX + 10, targetY);
		debugRenderer.line(targetX, targetY - 10, targetX, targetY + 10);

		debugRenderer.end();
	}

	private Texture tryLoadTexture(String path, int w, int h, Color fallbackColor) {
		try {
			return new Texture(Gdx.files.internal(path));
		} catch (Exception e) {
			return createSolidTexture(w, h, fallbackColor);
		}
	}

	// 辅助：生成纯色纹理
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
		if(debugRenderer != null) debugRenderer.dispose();
	}
}

// ==========================================
// 2. 渲染核心层 (Producer)
// ==========================================
class ViewTarget {
	public FrameBuffer fbo;
	public TextureRegion fboRegion;
	public SpriteBatch batch;
	public Viewport viewport;
	public Camera camera;

	private int width, height;

	public ViewTarget(int w, int h) {
		this.width = w;
		this.height = h;

		fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
		fboRegion = new TextureRegion(fbo.getColorBufferTexture());
		fboRegion.flip(false, true);

		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		viewport = new FitViewport(w, h, camera);
		camera.position.set(0, 0, 0);
	}

	public void setViewportType(boolean extend) {
		Camera oldCam = viewport.getCamera();
		if (extend) viewport = new ExtendViewport(width, height, oldCam);
		else viewport = new FitViewport(width, height, oldCam);
		viewport.update(width, height, false);
	}

	public void renderToFbo(Runnable renderLogic) {
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		fbo.begin();
		Gdx.gl.glViewport(0, 0, width, height);
		Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		renderLogic.run();
		batch.end();

		fbo.end();
	}

	public Vector2 unproject(float fboX, float fboY) {
		OrthographicCamera cam = (OrthographicCamera) camera;
		float halfW = viewport.getScreenWidth() / 2f;
		float halfH = viewport.getScreenHeight() / 2f;
		float centeredX = fboX - halfW;
		float centeredY = fboY - halfH;
		float worldX = cam.position.x + centeredX * cam.zoom;
		float worldY = cam.position.y + centeredY * cam.zoom;
		return new Vector2(worldX, worldY);
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
	private ViewTarget target;
	private Texture bgTexture;
	private float drawX, drawY, drawW, drawH;

	public ViewWidget(ViewTarget target) {
		this.target = target;
		// 【修复3】：把背景改成深紫红色，方便看清 Widget 的边界
		bgTexture = GameWorld.createSolidTexture(1, 1, new Color(0.3f, 0.0f, 0.1f, 1));
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		float x = getX(); float y = getY();
		float w = getWidth(); float h = getHeight();

		// 1. 画紫红色背景底框 (占位区)
		batch.draw(bgTexture, x, y, w, h);

		// 2. 计算 Letterbox
		float widgetRatio = w / h;
		float fboRatio = (float)target.viewport.getWorldWidth() / target.viewport.getWorldHeight();

		if (widgetRatio > fboRatio) {
			drawH = h; drawW = drawH * fboRatio;
		} else {
			drawW = w; drawH = drawW / fboRatio;
		}

		drawX = x + (w - drawW) / 2;
		drawY = y + (h - drawH) / 2;

		// 3. 贴 FBO (游戏画面)
		batch.draw(target.fboRegion, drawX, drawY, drawW, drawH);
	}

	public Vector2 screenToWorld(float localX, float localY) {
		float contentX = drawX - getX();
		float contentY = drawY - getY();
		if (localX < contentX || localX > contentX + drawW) return null;
		if (localY < contentY || localY > contentY + drawH) return null;

		float offsetX = localX - contentX;
		float offsetY = localY - contentY;
		float scaleX = target.viewport.getScreenWidth() / drawW;
		float scaleY = target.viewport.getScreenHeight() / drawH;
		return target.unproject(offsetX * scaleX, offsetY * scaleY);
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

		gameTarget = new ViewTarget(1280, 720);
		sceneTarget = new ViewTarget(1280, 720);
		sceneTarget.setViewportType(true);

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

		// 点击测试
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

		// 【修复1】：手搓一个摇杆样式，确保一定能显示
		// 1. 底座：深灰色圆
		Texture bg = GameWorld.createSolidTexture(100, 100, Color.DARK_GRAY);
		// 2. 摇杆头：浅灰色圆
		Texture knob = GameWorld.createSolidTexture(30, 30, Color.LIGHT_GRAY);
		Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
		style.background = new TextureRegionDrawable(new TextureRegion(bg));
		style.knob = new TextureRegionDrawable(new TextureRegion(knob));

		joystick = new Touchpad(10, style);

		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("Fit", "Extend");
		box.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				gameTarget.setViewportType(box.getSelected().equals("Extend"));
			}
		});

		// 【修复4】：开局强制刷新一次 Fit 模式，防止黑屏
		gameTarget.setViewportType(false);

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
				float dx = getDeltaX();
				float dy = getDeltaY();
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
