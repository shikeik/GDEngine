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
	
	// 添加到 ViewTarget 类中
	public int getFboWidth() { return fboW; }
	public int getFboHeight() { return fboH; }

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

		// 【第1步】先清空整个 FBO（画出黑边背景）
		Gdx.gl.glViewport(0, 0, fboW, fboH);
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f); // 这里的颜色就是 FBO 内部黑边的颜色
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 【第2步 - 核心修复】应用 Viewport 的实际渲染区域
		// 不要用 (0,0,fboW,fboH)，要用 viewport 计算出来的区域！
		// 这样 FIT 模式下，画面才会保持比例，左右留出黑边，而不是被拉伸填满
		viewport.apply(); 

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		renderLogic.run();
		batch.end();

		fbo.end();
	}
	
	public void resize(int w, int h) {
		// 这一步至关重要，如果 FBO 内部视口没更新，黑边数据(vpX, vpY)就是错的
		viewport.update(w, h, true); // true 表示居中相机
	}

	public void dispose() {
		fbo.dispose();
		batch.dispose();
	}
}

// ==========================================
// 3. UI 展示层 (Consumer) - 深度修复版
// ==========================================
class ViewWidget extends Widget {
    public enum DisplayMode { FIT, STRETCH, COVER }

    private ViewTarget target;
    private Texture bgTexture; // 用于显示Widget自身的底色(调试用)
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
        batch.draw(target.fboRegion, 
                   widgetX + drawnImageX, 
                   widgetY + drawnImageY, 
                   drawnImageW, 
                   drawnImageH);
    }

    /**
     * 将 UI 触摸点 (Local Coordinates) 转换为 游戏世界坐标 (World Coordinates)
     * 对应步骤：UI点击 -> 相对图片位置 -> FBO像素位置 -> NDC坐标 -> 世界坐标
     */
    public Vector2 screenToWorld(float localX, float localY) {
        // localX/Y 是 Scene2D 传进来的，原点是 Widget 左下角 (0,0)
        // 已经自动处理了 Window 的 Padding，不需要我们操心

        // [Step 1] 计算点击点在“FBO图片区域”内的归一化坐标 (0.0 ~ 1.0)
        // 我们需要减去 Widget 层的黑边偏移 (drawnImageX/Y)
        float percentX = (localX - drawnImageX) / drawnImageW;
        float percentY = (localY - drawnImageY) / drawnImageH;

        // [Step 2] 还原到 FBO 的物理像素位置
        // 如果点在正中心 (0.5)，且 FBO 宽 1280，那就是第 640 个像素
        float fboPixelX = percentX * target.getFboWidth();
        float fboPixelY = percentY * target.getFboHeight();

        // [Step 3] FBO 内部 Viewport 映射 (处理第二层黑边)
        // 获取 Viewport 在 FBO 内部实际占用的矩形区域
        Viewport vp = target.viewport;
        float vpX = vp.getScreenX();      // FBO 内部黑边的宽度
        float vpY = vp.getScreenY();      // FBO 内部黑边的高度
        float vpW = vp.getScreenWidth();  // 游戏画面在 FBO 里的实际宽度
        float vpH = vp.getScreenHeight(); // 游戏画面在 FBO 里的实际高度

        // 计算 NDC (归一化设备坐标: -1 ~ 1)
        // 公式含义：(当前像素 - 起始像素) / 总宽度 -> 得到 0~1 -> 映射到 -1~1
        float ndcX = (fboPixelX - vpX) / vpW * 2.0f - 1.0f;
        float ndcY = (fboPixelY - vpY) / vpH * 2.0f - 1.0f;

        // [Step 4] 投影到世界坐标 (利用 Camera 的能力)
        OrthographicCamera cam = (OrthographicCamera) target.camera;

        // 既然我们有了 NDC，就可以利用 Camera 的 Zoom 和 Position 算出世界坐标
        // 世界宽度的一半 = (视口世界宽 * 缩放) / 2
        float halfWorldW = (vp.getWorldWidth() * cam.zoom) / 2f;
        float halfWorldH = (vp.getWorldHeight() * cam.zoom) / 2f;

        // 最终坐标 = 相机位置 + (偏离中心的比例 * 世界的一半宽)
        float worldX = cam.position.x + ndcX * halfWorldW;
        float worldY = cam.position.y + ndcY * halfWorldH;

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

		float scl = 1f;
		stage = new Stage(new ExtendViewport(960*scl, 540*scl));
		//stage = new Stage(new ScreenViewport());
		
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
