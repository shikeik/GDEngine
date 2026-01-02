package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
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
// 1. 业务逻辑层 (Model) - 还原了真实素材加载
// ==========================================
class GameWorld {
	public Texture playerTex, bgTex;
	public float playerX = 0, playerY = 0;

	// 用于演示“点击移动”的目标点
	public float targetX = 0, targetY = 0;

	private ShapeRenderer debugRenderer;

	public void init() {
		// 【还原内容】：这里直接加载你的真实图片
		// 请确保 android/assets 目录下有这两个文件，否则会崩
		// 如果想回退到色块模式，把这两行注释掉，打开下面的 tryLoadTexture
		playerTex = new Texture(Gdx.files.internal("role.png"));
		bgTex = new Texture(Gdx.files.internal("back.png")); // 注意：原本你写的是 back.png

		debugRenderer = new ShapeRenderer();
	}

	public void update(float delta, float moveX, float moveY) {
		playerX += moveX;
		playerY += moveY;
	}

	public void render(Batch batch) {
		// 画背景 (平铺或居中，这里演示居中)
		batch.draw(bgTex, -bgTex.getWidth()/2f, -bgTex.getHeight()/2f);
		// 画玩家
		batch.draw(playerTex, playerX - playerTex.getWidth()/2f, playerY - playerTex.getHeight()/2f);
	}

	public void renderDebug(Camera camera) {
		debugRenderer.setProjectionMatrix(camera.combined);
		debugRenderer.begin(ShapeRenderer.ShapeType.Line);

		// 坐标轴
		debugRenderer.setColor(Color.RED);
		debugRenderer.line(-1000, 0, 1000, 0);
		debugRenderer.setColor(Color.GREEN);
		debugRenderer.line(0, -1000, 0, 1000);

		// 【第二步演示】：画出鼠标点击的目标点（十字）
		debugRenderer.setColor(Color.CYAN);
		debugRenderer.line(targetX - 10, targetY, targetX + 10, targetY);
		debugRenderer.line(targetX, targetY - 10, targetX, targetY + 10);

		debugRenderer.end();
	}

	public void dispose() {
		if(playerTex != null) playerTex.dispose();
		if(bgTex != null) bgTex.dispose();
		if(debugRenderer != null) debugRenderer.dispose();
	}
}

// ==========================================
// 2. 渲染核心层 (Producer) - 增加了坐标逆转换
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

	// 【第二步核心】：将 FBO 内部的像素坐标转换为游戏世界坐标
	public Vector2 unproject(float fboX, float fboY) {
		// viewport.unproject 需要一个 Vector3
		// 注意：FBO 的坐标系 Y 是向上的，不需要翻转，直接传给 viewport
		Vector3 vec = new Vector3(fboX, fboY, 0);
		viewport.unproject(vec);
		return new Vector2(vec.x, vec.y);
	}

	public void dispose() {
		fbo.dispose();
		batch.dispose();
	}
}

// ==========================================
// 3. UI 展示层 (Consumer) - 增加了点击事件映射
// ==========================================
class ViewWidget extends Widget {
	private ViewTarget target;
	private Texture bgTexture;

	// 保存绘制参数，供 Input 使用
	private float drawX, drawY, drawW, drawH;

	public ViewWidget(ViewTarget target) {
		this.target = target;
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(0.1f, 0.1f, 0.1f, 1);
		p.fill();
		bgTexture = new Texture(p);
		p.dispose();
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		float x = getX(); float y = getY();
		float w = getWidth(); float h = getHeight();

		// 1. 画底框
		batch.draw(bgTexture, x, y, w, h);

		// 2. 计算比例 (Letterboxing)
		float widgetRatio = w / h;
		float fboRatio = (float)target.viewport.getWorldWidth() / target.viewport.getWorldHeight();

		if (widgetRatio > fboRatio) {
			drawH = h; drawW = drawH * fboRatio;
		} else {
			drawW = w; drawH = drawW / fboRatio;
		}

		drawX = x + (w - drawW) / 2;
		drawY = y + (h - drawH) / 2;

		// 3. 贴 FBO
		batch.draw(target.fboRegion, drawX, drawY, drawW, drawH);
	}

	// 【第二步核心】：坐标转换工具
	// 将 Widget 上的触摸点 (localX, localY) 转换为 游戏世界坐标
	// 【修正版】坐标转换工具
	// 【终极修正版】坐标转换工具
	public Vector2 screenToWorld(float localX, float localY) {
		// 1. 判断点击是否在画面内 (排除 Letterbox 黑边)
		float contentX = drawX - getX();
		float contentY = drawY - getY();

		if (localX < contentX || localX > contentX + drawW) return null;
		if (localY < contentY || localY > contentY + drawH) return null;

		// 2. 转为 FBO 内部的像素坐标 (相对于左下角)
		float offsetX = localX - contentX;
		float offsetY = localY - contentY;

		// 缩放比例 (FBO像素 / UI像素)
		float scaleX = target.viewport.getScreenWidth() / drawW;
		float scaleY = target.viewport.getScreenHeight() / drawH;

		// FBO 内部坐标 (Bottom-Up)
		float fboX = offsetX * scaleX;
		float fboY = offsetY * scaleY;

		// 3. 【手动计算世界坐标】 (Bypass Gdx.graphics.getHeight bug)
		// 既然我们知道 FBO 是 1280x720，且相机是正交相机，我们可以直接算

		OrthographicCamera cam = (OrthographicCamera) target.camera;

		// 计算相对于 FBO 中心的偏移量
		float halfW = target.viewport.getScreenWidth() / 2f;
		float halfH = target.viewport.getScreenHeight() / 2f;

		float centeredX = fboX - halfW;
		float centeredY = fboY - halfH;

		// 应用相机的 Zoom 和 Position
		float worldX = cam.position.x + centeredX * cam.zoom;
		float worldY = cam.position.y + centeredY * cam.zoom;

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

		// 【第二步验证】：点击 Game 视图，设置目标点
		gameWidget.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// 调用我们在 Widget 里写的转换方法
				Vector2 worldPos = gameWidget.screenToWorld(x, y);

				if (worldPos != null) {
					Gdx.app.log("Input", "Click World: " + worldPos);
					// 设置到游戏世界里，验证是否准确
					gameWorld.targetX = worldPos.x;
					gameWorld.targetY = worldPos.y;

					// (可选) 瞬移玩家到点击位置，方便测试
					// gameWorld.playerX = worldPos.x;
					// gameWorld.playerY = worldPos.y;
				}
			}
		});

		Touchpad.TouchpadStyle style = VisUI.getSkin().get(Touchpad.TouchpadStyle.class);
		joystick = new Touchpad(10, style);

		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("Fit", "Extend");
		box.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				gameTarget.setViewportType(box.getSelected().equals("Extend"));
			}
		});

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

		// Render Pass (FBO)
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

		gameTarget.renderToFbo(() -> {
			gameTarget.camera.position.set(gameWorld.playerX, gameWorld.playerY, 0);
			gameTarget.camera.update();
			gameWorld.render(gameTarget.batch);

			// 在 Game 视图也画一下点击点，验证坐标
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

		// UI Pass
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
