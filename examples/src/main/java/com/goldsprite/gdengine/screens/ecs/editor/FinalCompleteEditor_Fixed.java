package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLVersion;
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
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisWindow;

public class FinalCompleteEditor_Fixed extends GScreen {
	private RealGame realGame;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		realGame = new RealGame();
		realGame.create();
	}

	@Override
	public void render(float delta) {
		realGame.render();
	}

	@Override
	public void resize(int width, int height) {
		realGame.resize(width, height);
	}

	@Override
	public void dispose() {
		realGame.dispose();
	}
}

// ==========================================
// 1. 业务逻辑层 (Model)
// ==========================================
class GameWorld {
	public static final float WORLD_WIDTH = 960; // 对应 Gd 的默认配置
	public static final float WORLD_HEIGHT = 540;

	public Texture playerTex, bgTex;
	public float playerX = 0, playerY = 0;
	public float targetX = 0, targetY = 0;
	private ShapeRenderer debugRenderer;
	// 【修改】新增 Batch，游戏自己管理绘制
	private SpriteBatch batch;

	public static Texture createSolidTexture(int w, int h, Color c) {
		Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
		p.setColor(c);
		p.fill();
		Texture t = new Texture(p);
		p.dispose();
		return t;
	}

	public void init() {
		playerTex = tryLoadTexture("role.png", 32, 32, Color.CORAL);
		bgTex = tryLoadTexture("back.png", 512, 512, Color.TEAL);
		debugRenderer = new ShapeRenderer();
		batch = new SpriteBatch();
	}

	public void update(float delta, float moveX, float moveY) {
		playerX += moveX;
		playerY += moveY;
	}

	public void render() { // 参数不再需要传入 external batch
		// 1. 告诉 Gd 我要开始画了，请应用视口 (处理黑边)
		Gd.view.apply();

		// 2. 获取当前生效的相机 (可能是游戏相机，也可能是编辑器相机)
		Camera cam = Gd.view.getCamera();

		// 3. 开始绘制
		batch.setProjectionMatrix(cam.combined);
		batch.begin();

		batch.draw(bgTex, -1000, -1000, 2000, 2000);
		batch.draw(playerTex, playerX - playerTex.getWidth()/2f, playerY - playerTex.getHeight()/2f);

		batch.end();
	}

	// Debug 同理
	public void renderDebug() {
		Camera cam = Gd.view.getCamera();
		debugRenderer.setProjectionMatrix(cam.combined);
		debugRenderer.begin(ShapeRenderer.ShapeType.Line);

		// 黄色框：世界边界
		debugRenderer.setColor(Color.YELLOW);
		debugRenderer.rect(-WORLD_WIDTH / 2, -WORLD_HEIGHT / 2, WORLD_WIDTH, WORLD_HEIGHT);

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
		// 【修改点】将 Gdx.files 替换为 Gd.files
		// 虽然实机模式下它俩一样，但这是为了统一规范
		try { return new Texture(Gd.files.internal(path)); }
		catch (Exception e) { return createSolidTexture(w, h, c); }
	}

	public void dispose() {
		if (playerTex != null) playerTex.dispose();
		if (bgTex != null) bgTex.dispose();
		debugRenderer.dispose();
		batch.dispose();
	}
}

// ==========================================
// 2. 渲染核心层 (Producer)
// ==========================================
// 替换整个 ViewTarget 类

class ViewTarget {
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

// ==========================================
// 3. UI 展示层 (Consumer) - 深度修复版
// ==========================================
class ViewWidget extends Widget {
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
		bgTexture = GameWorld.createSolidTexture(1, 1, new Color(0.15f, 0.15f, 0.15f, 1));
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

	/**
	 * [修改后] 原有的 screenToWorld 现在复用上面的逻辑
	 */
	public Vector2 screenToWorld(float screenX, float screenY) {
		// 1. 先拿到 FBO 像素坐标
		Vector2 fboPos = mapScreenToFbo(screenX, screenY);
		float fboPixelX = fboPos.x;
		float fboPixelY = fboPos.y;

		// 2. FBO 内部 Viewport 映射 (NDC转换)
		Viewport vp = Gd.view.getGameViewport();
		// 必须拿到 FBO 内部视口的实际偏移和大小
		float vpX = vp.getScreenX();
		float vpY = vp.getScreenY();
		float vpW = vp.getScreenWidth();
		float vpH = vp.getScreenHeight();

		// 转换为 NDC (-1 ~ 1)
		float ndcX = (fboPixelX - vpX) / vpW * 2.0f - 1.0f;
		float ndcY = (fboPixelY - vpY) / vpH * 2.0f - 1.0f;

		// 3. 投影到世界坐标
		OrthographicCamera cam = (OrthographicCamera) Gd.view.getCamera();
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

// ==========================================
// 4. 控制器层 (Controller)
// ==========================================
class EditorController {
	Stage stage;
	GameWorld gameWorld;
	ViewTarget gameTarget, sceneTarget;
	ViewWidget gameWidget, sceneWidget;
	Touchpad joystick;

	// 【修改】场景编辑器专用的上帝相机 (不再放在 ViewTarget 里)
	private OrthographicCamera sceneCamera;

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		gameWorld = new GameWorld();
		// 【修改点 1】 Gd 初始化前不调用 gameWorld.init()
		// Gd.init() 会创建 ViewportManager，GameWorld.init() 会用到它

		// --- FBO 和 Stage 设置 ---
		gameTarget = new ViewTarget(1280, 720); // FBO 模拟手机屏幕分辨率
		sceneTarget = new ViewTarget(1280, 720); // FBO 模拟手机屏幕分辨率

		float scl = 1.2f; // UI 缩放因子
		// Stage 使用 ExtendViewport，将 UI 缩放到 960x540 逻辑分辨率
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		// --- 创建 UI 窗口 ---
		createGameWindow();
		createSceneWindow();

		// --- 初始化代理 ---
		// Gd.init 需要 widget 和 target 来设置编辑器模式下的代理
		Gd.init(Gd.Mode.EDITOR, gameWidget, gameTarget);

		// 【修改点 2】GameWorld 在 Gd 初始化后初始化
		// GameWorld 现在会从 Gd 获取相机和视口
		gameWorld.init();

		// 输入处理链：Stage (UI) -> Gd (代理游戏输入)
		Gdx.input.setInputProcessor(stage);
	}

	private void createGameWindow() {
		VisWindow win = new VisWindow("Game View");
		win.setResizable(true);
		win.setSize(500, 350);
		win.setPosition(50, 50);

		gameWidget = new ViewWidget(gameTarget);

		// --- Game View 输入处理 ---
		gameWidget.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				// 1. 输入注入给 Gd (通知游戏有输入)
				((EditorGameInput) Gd.input).setTouched(true, pointer);

				// 2. 转发给游戏的 InputProcessor (如果游戏有的话)
				if (Gd.input.getInputProcessor() != null) {
					// 必须用全局屏幕坐标去转 FBO 坐标
					Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
					Gd.input.getInputProcessor().touchDown((int) fboPos.x, (int) fboPos.y, pointer, button);
				}

				// 3. 编辑器逻辑：点击设置目标点 (验证坐标映射)
				//    使用 gameWidget.screenToWorld 获取游戏世界坐标
				Vector2 worldPos = gameWidget.screenToWorld(Gdx.input.getX(), Gdx.input.getY());
				gameWorld.targetX = worldPos.x;
				gameWorld.targetY = worldPos.y;
				Gdx.app.log("Editor", "GameView Clicked World: " + worldPos);

				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				((EditorGameInput) Gd.input).setTouched(false, pointer);
				if (Gd.input.getInputProcessor() != null) {
					Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
					Gd.input.getInputProcessor().touchUp((int) fboPos.x, (int) fboPos.y, pointer, button);
				}
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				// 拖拽更新目标点
				Vector2 worldPos = gameWidget.screenToWorld(Gdx.input.getX(), Gdx.input.getY());
				gameWorld.targetX = worldPos.x;
				gameWorld.targetY = worldPos.y;

				if (Gd.input.getInputProcessor() != null) {
					Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
					Gd.input.getInputProcessor().touchDragged((int) fboPos.x, (int) fboPos.y, pointer);
				}
			}
		});

		// --- 摇杆设置 ---
		Texture bg = GameWorld.createSolidTexture(100, 100, Color.DARK_GRAY);
		Texture knob = GameWorld.createSolidTexture(30, 30, Color.LIGHT_GRAY);
		Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
		style.background = new TextureRegionDrawable(new TextureRegion(bg));
		style.knob = new TextureRegionDrawable(new TextureRegion(knob));
		joystick = new Touchpad(10, style);

		// --- 视口模式选择器 ---
		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String mode = box.getSelected();
				if (mode.equals("FIT")) {
					// 【修改】不再直接设置 target 的 viewportMode，而是更新 Gd 的配置
					Gd.view.setConfig(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, Gd.ViewportManager.Type.FIT);
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
				} else if (mode.equals("STRETCH")) {
					Gd.view.setConfig(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, Gd.ViewportManager.Type.STRETCH);
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.STRETCH);
				} else if (mode.equals("EXTEND")) {
					Gd.view.setConfig(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, Gd.ViewportManager.Type.EXTEND);
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);
				}
			}
		});

		// --- UI 布局 ---
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
		sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER); // Scene 通常铺满

		// --- Scene View 输入处理 (拖拽缩放) ---
		sceneWidget.addListener(new DragListener() {
			@Override
			public void drag(InputEvent event, float x, float y, int pointer) {
				// 【修改】使用 sceneCamera
				float dx = getDeltaX(); float dy = getDeltaY();
				float zoom = sceneCamera.zoom;
				sceneCamera.translate(-dx * 2 * zoom, -dy * 2 * zoom, 0);
				sceneCamera.update();
			}

			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				// 【修改】使用 sceneCamera
				sceneCamera.zoom += amountY * 0.1f;
				if (sceneCamera.zoom < 0.1f) sceneCamera.zoom = 0.1f;
				if (sceneCamera.zoom > 5f) sceneCamera.zoom = 5f;
				sceneCamera.update();
				return true;
			}
		});

		win.add(sceneWidget).grow();
		stage.addActor(win);
	}

	public void render(float delta) {
		// --- 更新游戏逻辑 ---
		float speed = 200 * delta;
		// 摇杆输入被 Gd 代理，最终驱动 gameWorld
		gameWorld.update(delta, joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

		// --- 渲染 Game View (玩家视角) ---
		// 1. 告诉 Gd 使用游戏相机
		Gd.view.useGameCamera();
		// 2. 告诉 Gd 屏幕大小，让它更新游戏视口 (Fit/Extend...)
		Gd.view.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		// 3. GameTarget 渲染游戏逻辑
		gameTarget.renderToFbo(() -> {
			// GameWorld 内部会自己用 Gd.view.getCamera() 和 Gd.view.apply()
			gameWorld.render();
			gameWorld.renderDebug(); // GameWorld 内部会自己设置 ProjectionMatrix
		});

		// --- 渲染 Scene View (上帝视角) ---
		sceneTarget.renderToFbo(() -> {
			// 1. 告诉 Gd 使用编辑器相机 (劫持)
			Gd.view.useEditorCamera(sceneCamera);
			// 2. Scene View 不需要模拟游戏视口，直接铺满 FBO
			//    (GameWorld.render() 内部的 Gd.view.apply() 会被跳过)

			// 3. 渲染 GameWorld 内容 (使用 sceneCamera)
			gameWorld.render();
			gameWorld.renderDebug();
		});

		// --- 绘制编辑器 UI ---
		// 确保 GL 状态正确 (防止游戏逻辑乱改)
		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0, 0, 0, 1); // 清除 Stage 背景
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		// 【重要】屏幕尺寸变化时，也要通知 Gd.view 更新游戏视口
		Gd.view.update(width, height);
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

// ==========================================
// 6. 实机运行入口 (The Acid Test)
// 用于验证游戏逻辑是否彻底解耦，不依赖编辑器也能跑
// ==========================================
class RealGame extends ApplicationAdapter {
	// 【修改点 1】引入 Viewport
	Viewport viewport;
	SpriteBatch batch;
	OrthographicCamera camera;
	GameWorld gameWorld;

	// 模拟手机屏幕的逻辑分辨率 (通常由 FitViewport 或 ExtendViewport 管理)
	// 这里为了演示简单，直接用 960x540
	final float VIEW_WIDTH = 960;
	final float VIEW_HEIGHT = 540;

	@Override
	public void create() {
		// 初始化 Release 模式
		Gd.init(Gd.Mode.RELEASE, null, null);

		gameWorld = new GameWorld();
		gameWorld.init();
	}

	@Override
	public void resize(int width, int height) {
		// 【关键】实机屏幕变化时，通知 Gd 更新视口
		Gd.view.update(width, height);
	}

	@Override
	public void render() {
		float delta = Gd.graphics.getDeltaTime();

		// --- 输入处理 (实机逻辑) ---
		// 在编辑器里我们用 UI 摇杆，实机里我们用键盘模拟 (或者手机屏幕触摸)
		float speed = 200 * delta;
		float moveX = 0, moveY = 0;

		// 验证 Gd.input 是否透传成功
		if (Gd.input.isKeyPressed(Input.Keys.A)) moveX -= speed;
		if (Gd.input.isKeyPressed(Input.Keys.D)) moveX += speed;
		if (Gd.input.isKeyPressed(Input.Keys.W)) moveY += speed;
		if (Gd.input.isKeyPressed(Input.Keys.S)) moveY -= speed;

		// --- 坐标点击测试 ---
		if (Gd.input.isTouched()) {
			// 【修改点 4】使用 Viewport 来转换坐标
			// 它会自动处理黑边偏移，比单纯的 camera.unproject 更稳
			Vector2 touchPos = new Vector2(Gd.input.getX(), Gd.input.getY());
			viewport.unproject(touchPos); // 屏幕 -> 世界

			gameWorld.targetX = touchPos.x;
			gameWorld.targetY = touchPos.y;
		}

		// 更新逻辑
		gameWorld.update(delta, moveX, moveY);

		// 更新相机跟随
		camera.position.set(gameWorld.playerX, gameWorld.playerY, 0);
		camera.update();

		// 清屏 (实机全屏清)
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 应用视口 (虽然 resize 里 apply 过，但为了防止其他 FBO 干扰，每帧 apply 也可以)
		viewport.apply();

		// 渲染 (RealGame 不需要 useGameCamera，因为默认就是)
		gameWorld.render();
		gameWorld.renderDebug();
	}

	@Override
	public void dispose() {
		batch.dispose();
		gameWorld.dispose();
	}
}


// ==========================================
// 5. 全局代理层 (Global Delegate) - LibGDX 1.12.1 适配版
// ==========================================
class Gd {
	// 其他模块透传
	public static final Files files = Gdx.files;
	public static final Application app = Gdx.app;
	// 音频模块通常也需要透传
	public static final Audio audio = Gdx.audio;
	public static Input input;       // 替换 Gdx.input
	public static Graphics graphics; // 替换 Gdx.graphics

	// 【新增】视口与相机管理器
	public static ViewportManager view;

	public static void init(Mode mode, ViewWidget widget, ViewTarget target) {
		if (mode == Mode.RELEASE) {
			input = Gdx.input;
			graphics = Gdx.graphics;
		} else {
			input = new EditorGameInput(widget);
			graphics = new EditorGameGraphics(target);
		}
		// 初始化视口管理器 (默认配置 960x540, FIT)
		view = new ViewportManager(960, 540);
	}

	// ---------------------------------------------------------
	// 视口管理器：配置驱动 + 相机劫持核心
	// ---------------------------------------------------------
	public static class ViewportManager {
		// 配置数据
		private int logicWidth, logicHeight;

		// 游戏标准视口 (Game View 用)
		private Viewport gameViewport;
		private Camera gameCamera;

		// 当前激活的相机 (可能是游戏相机，也可能是编辑器上帝相机)
		private Camera activeCamera;

		// 状态标记：是否被编辑器劫持
		private boolean isHijacked = false;

		public ViewportManager(int w, int h) {
			this.logicWidth = w;
			this.logicHeight = h;
			this.gameCamera = new OrthographicCamera();
			// 默认策略：FitViewport (可扩展为支持配置切换 Stretch/Extend)
			this.gameViewport = new FitViewport(logicWidth, logicHeight, gameCamera);
			this.gameCamera.position.set(0, 0, 0); // 居中

			// 默认状态
			useGameCamera();
		}

		/**
		 * 实机/编辑器 响应窗口大小变化
		 */
		public void update(int screenW, int screenH) {
			// 更新游戏视口的物理尺寸计算
			gameViewport.update(screenW, screenH, true);
		}

		/**
		 * 【模式 A】使用游戏原生相机 (Game View / RealGame)
		 */
		public void useGameCamera() {
			this.activeCamera = gameCamera;
			this.isHijacked = false;
		}

		/**
		 * 【模式 B】劫持：使用编辑器的上帝相机 (Scene View)
		 */
		public void useEditorCamera(Camera sceneCamera) {
			this.activeCamera = sceneCamera;
			this.isHijacked = true;
		}

		/**
		 * 获取当前应该使用的相机
		 */
		public Camera getCamera() {
			return activeCamera;
		}

		/**
		 * 应用视口 (设置 glViewport)
		 * 只有在非劫持模式下，才应用 Fit/BlackBars 逻辑。
		 * 如果被劫持(SceneView)，通常希望铺满窗口，由 ViewTarget 的 clear 逻辑处理即可。
		 */
		public void apply() {
			if (!isHijacked) {
				gameViewport.apply();
			}
		}

		// 供游戏逻辑获取原始视口 (如果需要做射线检测等)
		public Viewport getGameViewport() { return gameViewport; }
	}

	public enum Mode {RELEASE, EDITOR}
}

/**
 * 编辑器模式下的 Input 代理
 * 负责将 全局屏幕坐标 修正为 FBO 像素坐标
 */
class EditorGameInput implements Input {
	private final ViewWidget widget;
	private InputProcessor processor;
	private boolean isTouched = false;

	public EditorGameInput(ViewWidget widget) { this.widget = widget; }
	public void setTouched(boolean touched, int pointer) { this.isTouched = touched; }
	// 【修复 1】接口要求返回 int，必须强制转换
	@Override public int getX() { return getX(0); }
	@Override public int getX(int pointer) {
		// mapScreenToFbo 返回的是 float，转为 int 以符合接口定义
		return (int) widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer)).x;
	}
	@Override public int getY() { return getY(0); }
	@Override public int getY(int pointer) { return (int) widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer)).y; }
	@Override public boolean isTouched() { return isTouched || Gdx.input.isTouched(); }
	@Override public boolean isTouched(int pointer) { return Gdx.input.isTouched(pointer); }
	@Override public boolean justTouched() { return Gdx.input.justTouched(); }
	@Override public InputProcessor getInputProcessor() { return processor; }
	@Override public void setInputProcessor(InputProcessor processor) { this.processor = processor; }
	@Override public boolean isPeripheralAvailable(Peripheral peripheral) { return false; }
	// --- 透传 Gdx.input ---
	@Override public int getDeltaX() { return Gdx.input.getDeltaX(); }
	@Override public int getDeltaX(int pointer) { return Gdx.input.getDeltaX(pointer); }
	@Override public int getDeltaY() { return Gdx.input.getDeltaY(); }
	@Override public int getDeltaY(int pointer) { return Gdx.input.getDeltaY(pointer); }
	@Override public boolean isButtonPressed(int button) { return Gdx.input.isButtonPressed(button); }
	@Override public boolean isButtonJustPressed(int button) { return Gdx.input.isButtonJustPressed(button); }
	@Override public boolean isKeyPressed(int key) { return Gdx.input.isKeyPressed(key); }
	@Override public boolean isKeyJustPressed(int key) { return Gdx.input.isKeyJustPressed(key); }
	// --- 文本输入相关 ---
	@Override public void getTextInput(TextInputListener listener, String title, String text, String hint) { Gdx.input.getTextInput(listener, title, text, hint); }
	// 【修复 2】实现 1.12.1 新增的接口方法
	@Override
	public void getTextInput(TextInputListener listener, String title, String text, String hint, OnscreenKeyboardType type) { Gdx.input.getTextInput(listener, title, text, hint, type); }
	@Override public void setOnscreenKeyboardVisible(boolean visible) { Gdx.input.setOnscreenKeyboardVisible(visible); }
	@Override public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) { Gdx.input.setOnscreenKeyboardVisible(visible, type); }
	// --- 传感器与杂项 ---
	@Override public void vibrate(int milliseconds) { }
	@Override public void vibrate(int milliseconds, boolean fallback) { }
	@Override public void vibrate(int milliseconds, int amplitude, boolean fallback) { }
	@Override public void vibrate(VibrationType vibrationType) { }
	@Override public float getAzimuth() { return 0; }
	@Override public float getPitch() { return 0; }
	@Override public float getRoll() { return 0; }
	@Override public void getRotationMatrix(float[] matrix) {}
	@Override public long getCurrentEventTime() { return Gdx.input.getCurrentEventTime(); }
	@Override public boolean isCatchBackKey() { return false; }
	@Override public void setCatchBackKey(boolean catchBack) { }
	@Override public boolean isCatchMenuKey() { return false; }
	@Override public void setCatchMenuKey(boolean catchMenu) { }
	@Override public void setCatchKey(int keycode, boolean catchKey) { }
	@Override public boolean isCatchKey(int keycode) { return false; }
	@Override public float getAccelerometerX() { return 0; }
	@Override public float getAccelerometerY() { return 0; }
	@Override public float getAccelerometerZ() { return 0; }
	@Override public float getGyroscopeX() { return 0; }
	@Override public float getGyroscopeY() { return 0; }
	@Override public float getGyroscopeZ() { return 0; }
	@Override public int getMaxPointers() { return Gdx.input.getMaxPointers(); }
	@Override public int getRotation() { return 0; }
	@Override public Orientation getNativeOrientation() { return Orientation.Landscape; }
	@Override public boolean isCursorCatched() { return false; }
	@Override public void setCursorCatched(boolean catched) { }
	@Override public void setCursorPosition(int x, int y) { }
	@Override public float getPressure() { return 0; }
	@Override public float getPressure(int pointer) { return 0; }
}

/**
 * 编辑器模式下的 Graphics 代理
 * 负责欺骗游戏：屏幕只有 FBO 那么大
 */
class EditorGameGraphics implements Graphics {
	private final ViewTarget target;

	public EditorGameGraphics(ViewTarget target) { this.target = target; }
	@Override public int getWidth() { return target.getFboWidth(); }
	@Override public int getHeight() { return target.getFboHeight(); }
	@Override public int getBackBufferWidth() { return target.getFboWidth(); }
	@Override public int getBackBufferHeight() { return target.getFboHeight(); }
	@Override public float getBackBufferScale() { return 0; }
	@Override public float getDeltaTime() { return Gdx.graphics.getDeltaTime(); }
	@Override public float getRawDeltaTime() { return Gdx.graphics.getRawDeltaTime(); }
	@Override public int getFramesPerSecond() { return Gdx.graphics.getFramesPerSecond(); }
	@Override public GraphicsType getType() { return null; }
	// --- GL 版本支持 (1.12.1 新增) ---
	@Override public boolean isGL30Available() { return Gdx.graphics.isGL30Available(); }
	// 【修复 3】补充 GL31 支持
	@Override public boolean isGL31Available() { return Gdx.graphics.isGL31Available(); }
	@Override public boolean isGL32Available() { return false; }
	@Override public GL20 getGL20() { return Gdx.graphics.getGL20(); }
	@Override public void setGL20(GL20 gl20) { Gdx.graphics.setGL20(gl20); }
	@Override public GL30 getGL30() { return Gdx.graphics.getGL30(); }
	@Override public void setGL30(GL30 gl30) { Gdx.graphics.setGL30(gl30); }
	@Override public GL31 getGL31() { return Gdx.graphics.getGL31(); }
	@Override public void setGL31(GL31 gl31) { Gdx.graphics.setGL31(gl31); }
	@Override public GL32 getGL32() { return null; }
	@Override public void setGL32(GL32 gl32) {  }
	@Override public long getFrameId() { return Gdx.graphics.getFrameId(); }
	@Override public float getPpiX() { return Gdx.graphics.getPpiX(); }
	@Override public float getPpiY() { return Gdx.graphics.getPpiY(); }
	@Override public float getPpcX() { return Gdx.graphics.getPpcX(); }
	@Override public float getPpcY() { return Gdx.graphics.getPpcY(); }
	@Override public float getDensity() { return Gdx.graphics.getDensity(); }
	@Override public boolean supportsDisplayModeChange() { return false; }
	@Override public Monitor getPrimaryMonitor() { return Gdx.graphics.getPrimaryMonitor(); }
	@Override public Monitor getMonitor() { return Gdx.graphics.getMonitor(); }
	@Override public Monitor[] getMonitors() { return Gdx.graphics.getMonitors(); }
	@Override public DisplayMode[] getDisplayModes() { return Gdx.graphics.getDisplayModes(); }
	@Override public DisplayMode[] getDisplayModes(Monitor monitor) { return Gdx.graphics.getDisplayModes(monitor); }
	@Override public DisplayMode getDisplayMode() { return Gdx.graphics.getDisplayMode(); }
	@Override public DisplayMode getDisplayMode(Monitor monitor) { return Gdx.graphics.getDisplayMode(monitor); }
	@Override public boolean setFullscreenMode(DisplayMode displayMode) { return false; }
	@Override public boolean setWindowedMode(int width, int height) { return false; }
	@Override public void setTitle(String title) { }
	@Override public void setUndecorated(boolean undecorated) { }
	@Override public void setResizable(boolean resizable) { }
	@Override public void setVSync(boolean vsync) { }
	@Override public void setForegroundFPS(int fps) { }
	@Override public BufferFormat getBufferFormat() { return Gdx.graphics.getBufferFormat(); }
	@Override public boolean supportsExtension(String extension) { return Gdx.graphics.supportsExtension(extension); }
	@Override public boolean isContinuousRendering() { return Gdx.graphics.isContinuousRendering(); }
	@Override public void setContinuousRendering(boolean isContinuous) { Gdx.graphics.setContinuousRendering(isContinuous); }
	@Override public void requestRendering() { Gdx.graphics.requestRendering(); }
	@Override public boolean isFullscreen() { return false; }
	@Override public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) { return Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot); }
	// 【修复 4】SystemCursor 引用问题
	@Override public void setSystemCursor(Cursor.SystemCursor systemCursor) { }
	@Override public void setCursor(Cursor cursor) { }
	@Override public GLVersion getGLVersion() { return Gdx.graphics.getGLVersion(); }
	// 【修复 5】刘海屏适配方法 (1.12.x 新增)
	@Override public int getSafeInsetLeft() { return 0; }
	@Override public int getSafeInsetTop() { return 0; }
	@Override public int getSafeInsetBottom() { return 0; }
	@Override public int getSafeInsetRight() { return 0; }
}
