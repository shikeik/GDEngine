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
import org.lwjgl.opengl.DisplayMode;

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
		Viewport vp = target.viewport;
		// 必须拿到 FBO 内部视口的实际偏移和大小
		float vpX = vp.getScreenX();
		float vpY = vp.getScreenY();
		float vpW = vp.getScreenWidth();
		float vpH = vp.getScreenHeight();

		// 转换为 NDC (-1 ~ 1)
		float ndcX = (fboPixelX - vpX) / vpW * 2.0f - 1.0f;
		float ndcY = (fboPixelY - vpY) / vpH * 2.0f - 1.0f;

		// 3. 投影到世界坐标
		OrthographicCamera cam = (OrthographicCamera) target.camera;
		float halfWorldW = (vp.getWorldWidth() * cam.zoom) / 2f;
		float halfWorldH = (vp.getWorldHeight() * cam.zoom) / 2f;

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
		// 【修改点 1】注意：GameWorld.init() 里如果用了 Gdx.graphics，可能会报错，
		// 所以建议把 init 放在 Gd 初始化之后，或者确保 init 里只加载资源

		// 默认配置
		gameTarget = new ViewTarget(1280, 720);
		gameTarget.setViewportMode(ViewTarget.ViewportMode.FIT);

		sceneTarget = new ViewTarget(1280, 720);
		sceneTarget.setViewportMode(ViewTarget.ViewportMode.EXTEND);

		float scl = 1f;
		stage = new Stage(new ExtendViewport(960*scl, 540*scl));

		createGameWindow();  // 这里面创建了 gameWidget
		createSceneWindow();

		// 【修改点 2】初始化全局代理 (Editor模式)
		Gd.init(Gd.Mode.EDITOR, gameWidget, gameTarget);

		// 【修改点 3】现在可以安全初始化游戏逻辑了
		gameWorld.init();

		// 输入处理链：Stage (UI) -> Gd (游戏逻辑注入)
		Gdx.input.setInputProcessor(stage);
	}

	private void createGameWindow() {
		VisWindow win = new VisWindow("Game View");
		win.setResizable(true);
		win.setSize(500, 350);
		win.setPosition(50, 50);

		// ... 前面的代码不变 ...
		gameWidget = new ViewWidget(gameTarget);

		// 【新增】输入注入：将 UI 触摸事件透传给游戏
		gameWidget.addListener(new InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					// 将 Scene2D 的事件注入到 Gd 代理中
					// event.getStageX() 是屏幕坐标吗？不，是 Stage 坐标。
					// 这里的 x, y 是 Widget 本地坐标。
					// 为了准确，我们直接用 Gdx.input.getX() 获取原始数据给 Gd 处理，
					// 或者利用 Gd.input.injectInput 方法。

					// 简单粗暴方案：直接通知 Gd "被摸了"，坐标由 Gd.input.getX() 自动计算
					((EditorGameInput)Gd.input).setTouched(true, pointer);

					// 触发游戏的 InputProcessor
					if(Gd.input.getInputProcessor() != null) {
						// 计算出 FBO 坐标
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchDown((int)fboPos.x, (int)fboPos.y, pointer, button);
					}
					return true;
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					((EditorGameInput)Gd.input).setTouched(false, pointer);
					if(Gd.input.getInputProcessor() != null) {
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchUp((int)fboPos.x, (int)fboPos.y, pointer, button);
					}
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					if(Gd.input.getInputProcessor() != null) {
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchDragged((int)fboPos.x, (int)fboPos.y, pointer);
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


// ==========================================
// 5. 全局代理层 (Global Delegate) - LibGDX 1.12.1 适配版
// ==========================================
class Gd {
	public enum Mode { RELEASE, EDITOR }

	public static Input input;       // 替换 Gdx.input
	public static Graphics graphics; // 替换 Gdx.graphics

	// 其他模块透传
	public static final Files files = Gdx.files;
	public static final Application app = Gdx.app;
	// 音频模块通常也需要透传
	public static final Audio audio = Gdx.audio;

	public static void init(Mode mode, ViewWidget widget, ViewTarget target) {
		if (mode == Mode.RELEASE) {
			input = Gdx.input;       // 实机直接用原生
			graphics = Gdx.graphics; // 实机直接用原生
		} else {
			input = new EditorGameInput(widget);
			graphics = new EditorGameGraphics(target);
		}
	}
}

/**
 * 编辑器模式下的 Input 代理
 * 负责将 全局屏幕坐标 修正为 FBO 像素坐标
 */
class EditorGameInput implements Input {
	private ViewWidget widget;
	private InputProcessor processor;
	private boolean isTouched = false;

	public EditorGameInput(ViewWidget widget) { this.widget = widget; }

	public void setTouched(boolean touched, int pointer) { this.isTouched = touched; }

	// 【修复 1】接口要求返回 int，必须强制转换
	@Override
	public int getX() { return getX(0); }

	@Override
	public int getX(int pointer) {
		// mapScreenToFbo 返回的是 float，转为 int 以符合接口定义
		return (int) widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer)).x;
	}

	@Override
	public int getY() { return getY(0); }

	@Override
	public int getY(int pointer) {
		return (int) widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer)).y;
	}

	@Override
	public boolean isTouched() { return isTouched || Gdx.input.isTouched(); }

	@Override
	public boolean isTouched(int pointer) { return Gdx.input.isTouched(pointer); }

	@Override
	public boolean justTouched() { return Gdx.input.justTouched(); }

	@Override
	public void setInputProcessor(InputProcessor processor) { this.processor = processor; }

	@Override
	public InputProcessor getInputProcessor() { return processor; }

	@Override
	public boolean isPeripheralAvailable(Peripheral peripheral) {
		return false;
	}

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
	@Override public void getTextInput(TextInputListener listener, String title, String text, String hint) {
		Gdx.input.getTextInput(listener, title, text, hint);
	}

	// 【修复 2】实现 1.12.1 新增的接口方法
	@Override
	public void getTextInput(TextInputListener listener, String title, String text, String hint, OnscreenKeyboardType type) {
		Gdx.input.getTextInput(listener, title, text, hint, type);
	}

	@Override public void setOnscreenKeyboardVisible(boolean visible) { Gdx.input.setOnscreenKeyboardVisible(visible); }
	@Override public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) { Gdx.input.setOnscreenKeyboardVisible(visible, type); }

	// --- 传感器与杂项 ---
	@Override public void vibrate(int milliseconds) { }

	@Override
	public void vibrate(int milliseconds, boolean fallback) {
	}

	@Override
	public void vibrate(int milliseconds, int amplitude, boolean fallback) {
	}

	@Override
	public void vibrate(VibrationType vibrationType) {
	}

	@Override public float getAzimuth() { return 0; }
	@Override public float getPitch() { return 0; }
	@Override public float getRoll() { return 0; }
	@Override public void getRotationMatrix(float[] matrix) { }
	@Override public long getCurrentEventTime() { return Gdx.input.getCurrentEventTime(); }
	@Override public void setCatchBackKey(boolean catchBack) { }
	@Override public boolean isCatchBackKey() { return false; }
	@Override public void setCatchMenuKey(boolean catchMenu) { }
	@Override public boolean isCatchMenuKey() { return false; }
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
	@Override public void setCursorCatched(boolean catched) { }
	@Override public boolean isCursorCatched() { return false; }
	@Override public void setCursorPosition(int x, int y) { }
	@Override public float getPressure() { return 0; }
	@Override public float getPressure(int pointer) { return 0; }
}

/**
 * 编辑器模式下的 Graphics 代理
 * 负责欺骗游戏：屏幕只有 FBO 那么大
 */
class EditorGameGraphics implements Graphics {
	private ViewTarget target;
	public EditorGameGraphics(ViewTarget target) { this.target = target; }

	@Override public int getWidth() { return target.getFboWidth(); }
	@Override public int getHeight() { return target.getFboHeight(); }
	@Override public int getBackBufferWidth() { return target.getFboWidth(); }
	@Override public int getBackBufferHeight() { return target.getFboHeight(); }

	@Override
	public float getBackBufferScale() {
		return 0;
	}

	@Override public float getDeltaTime() { return Gdx.graphics.getDeltaTime(); }
	@Override public float getRawDeltaTime() { return Gdx.graphics.getRawDeltaTime(); }
	@Override public int getFramesPerSecond() { return Gdx.graphics.getFramesPerSecond(); }

	@Override
	public GraphicsType getType() {
		return null;
	}

	// --- GL 版本支持 (1.12.1 新增) ---
	@Override public boolean isGL30Available() { return Gdx.graphics.isGL30Available(); }

	// 【修复 3】补充 GL31 支持
	@Override public boolean isGL31Available() { return Gdx.graphics.isGL31Available(); }

	@Override
	public boolean isGL32Available() {
		return false;
	}

	@Override public GL20 getGL20() { return Gdx.graphics.getGL20(); }
	@Override public GL30 getGL30() { return Gdx.graphics.getGL30(); }
	@Override public GL31 getGL31() { return Gdx.graphics.getGL31(); }

	@Override
	public GL32 getGL32() {
		return null;
	}

	@Override public void setGL20(GL20 gl20) { Gdx.graphics.setGL20(gl20); }
	@Override public void setGL30(GL30 gl30) { Gdx.graphics.setGL30(gl30); }
	@Override public void setGL31(GL31 gl31) { Gdx.graphics.setGL31(gl31); }

	@Override
	public void setGL32(GL32 gl32) {

	}

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
	@Override public void setContinuousRendering(boolean isContinuous) { Gdx.graphics.setContinuousRendering(isContinuous); }
	@Override public boolean isContinuousRendering() { return Gdx.graphics.isContinuousRendering(); }
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
