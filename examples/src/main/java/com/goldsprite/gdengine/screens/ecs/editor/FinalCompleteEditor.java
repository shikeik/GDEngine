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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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

// ==========================================
// 0. 入口
// ==========================================
public class FinalCompleteEditor extends GScreen {int k123;
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

    // 简单的辅助线工具
    private ShapeRenderer debugRenderer;

    public void init() {
        // 安全加载：优先尝试加载图片，如果没有则生成色块
        playerTex = tryLoadTexture("role.png", 32, 32, Color.CORAL);
        bgTex = tryLoadTexture("back.png", 512, 512, Color.TEAL);

        debugRenderer = new ShapeRenderer();
    }

    public void update(float delta, float moveX, float moveY) {
        // 简单的移动逻辑
        playerX += moveX;
        playerY += moveY;
    }

    // 纯粹的绘制 (不知道视口，不知道FBO)
    public void render(Batch batch) {
        // 画背景 (假设图很大，或者是重复贴图)
        batch.draw(bgTex, -bgTex.getWidth()/2f, -bgTex.getHeight()/2f); 
        // 画玩家 (居中绘制)
        batch.draw(playerTex, playerX - playerTex.getWidth()/2f, playerY - playerTex.getHeight()/2f);
    }

    // 画辅助线 (世界坐标系)
    public void renderDebug(Camera camera) {
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);

        // 画世界坐标轴
        debugRenderer.setColor(Color.RED);
        debugRenderer.line(-1000, 0, 1000, 0); // X轴
        debugRenderer.setColor(Color.GREEN);
        debugRenderer.line(0, -1000, 0, 1000); // Y轴

        // 画玩家位置框
        debugRenderer.setColor(Color.YELLOW);
        debugRenderer.rect(playerX - 20, playerY - 20, 40, 40);

        debugRenderer.end();
    }

    // --- 资源加载辅助 ---
    private Texture tryLoadTexture(String path, int w, int h, Color fallbackColor) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (Exception e) {
            // 如果没找到文件，生成一个纯色块
            Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
            p.setColor(fallbackColor);
            p.fill();
            Texture t = new Texture(p);
            p.dispose();
            return t;
        }
    }

    public void dispose() {
        if(playerTex != null) playerTex.dispose();
        if(bgTex != null) bgTex.dispose();
        if(debugRenderer != null) debugRenderer.dispose();
    }
}

// ==========================================
// 2. 渲染核心层 (Producer) - 只管生产画面
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
        camera.position.set(0, 0, 0); // 默认看世界中心
    }

    public void setViewportType(boolean extend) {
        Camera oldCam = viewport.getCamera();
        if (extend) viewport = new ExtendViewport(width, height, oldCam);
        else viewport = new FitViewport(width, height, oldCam);
        viewport.update(width, height, false);
    }

    // 核心渲染方法
    public void renderToFbo(Runnable renderLogic) {
        // 1. 强行关闭剪裁，防止UI干扰
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        fbo.begin();

        // 2. 清理画布
        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f); // 编辑器背景灰
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 3. 执行绘制
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
// 3. UI 展示层 (Consumer) - 只管贴图
// ==========================================
class ViewWidget extends Widget {
    private ViewTarget target;
    private Texture bgTexture;

    public ViewWidget(ViewTarget target) {
        this.target = target;
        // 背景底色
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

        float drawW, drawH;
        if (widgetRatio > fboRatio) {
            drawH = h; drawW = drawH * fboRatio;
        } else {
            drawW = w; drawH = drawW / fboRatio;
        }

        float drawX = x + (w - drawW) / 2;
        float drawY = y + (h - drawH) / 2;

        // 3. 贴 FBO
        batch.draw(target.fboRegion, drawX, drawY, drawW, drawH);
    }

    public void dispose() {
        bgTexture.dispose();
    }
}

// ==========================================
// 4. 控制器层 (Controller) - 组装一切
// ==========================================
class EditorController {
    Stage stage;
    GameWorld gameWorld;

    ViewTarget gameTarget, sceneTarget;
    ViewWidget gameWidget, sceneWidget;

    Touchpad joystick;

    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();

        // 1. 初始化世界
        gameWorld = new GameWorld();
        gameWorld.init();

        // 2. 初始化生产端 (FBOs) - 设定内部分辨率为 1280x720
        gameTarget = new ViewTarget(1280, 720);
        sceneTarget = new ViewTarget(1280, 720);
        sceneTarget.setViewportType(true); // Scene 默认 Extend

        // 3. 初始化 UI
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

        // UI 布局：摇杆 + 模式切换
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

        // 拖拽移动 Scene 相机
        sceneWidget.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					float dx = getDeltaX();
					float dy = getDeltaY();
					// 移动速度根据 zoom 调整会更顺手
					float zoom = ((OrthographicCamera)sceneTarget.camera).zoom;
					sceneTarget.camera.translate(-dx * 2 * zoom, -dy * 2 * zoom, 0);
					sceneTarget.camera.update();
				}

				@Override
				public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
					// 简单的滚轮缩放
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
        // 1. 逻辑更新 (Joystick 控制)
        float speed = 200 * delta;
        gameWorld.update(delta, joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

        // ============================================================
        // 2. 离屏渲染 (Render Pass)
        // ============================================================

        // A. 渲染 Game 视图 (相机跟随玩家)
        gameTarget.renderToFbo(() -> {
            gameTarget.camera.position.set(gameWorld.playerX, gameWorld.playerY, 0);
            gameTarget.camera.update();
            gameWorld.render(gameTarget.batch);
        });

        // B. 渲染 Scene 视图 (相机独立，带 Debug 线)
        sceneTarget.renderToFbo(() -> {
            gameWorld.render(sceneTarget.batch);

            // 额外画 Debug 线 (需要用 debugRenderer)
            sceneTarget.batch.end();
            gameWorld.renderDebug(sceneTarget.camera);
            sceneTarget.batch.begin();
        });

        // ============================================================
        // 3. 屏幕渲染 (UI Pass)
        // ============================================================

        // 恢复屏幕视口
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
