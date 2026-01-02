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
// 1. 游戏核心业务 (纯逻辑)
// ==========================================
class GameWorld {
    public Texture playerTex, bgTex;
    public float playerX = 0, playerY = 0;
    private ShapeRenderer debugRenderer;

    public void init() {
        // 模拟资源
        playerTex = createSolidTexture(32, 32, Color.CORAL);
        bgTex = createSolidTexture(512, 512, Color.TEAL);
        debugRenderer = new ShapeRenderer();
    }

    // 简单的纯色纹理生成
    private Texture createSolidTexture(int w, int h, Color c) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    public void update(float dx, float dy) {
        playerX += dx;
        playerY += dy;
    }

    // 纯粹的绘制
    public void render(Batch batch) {
        batch.draw(bgTex, -256, -256); 
        batch.draw(playerTex, playerX, playerY);
    }

    // 画辅助线
    public void renderDebug(Camera camera) {
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(Color.YELLOW);
        debugRenderer.line(-1000, 0, 1000, 0); 
        debugRenderer.line(0, -1000, 0, 1000); 
        debugRenderer.end();
    }

    public void dispose() {
        playerTex.dispose();
        bgTex.dispose();
        debugRenderer.dispose();
    }
}

// ==========================================
// 2. 渲染目标 (ViewTarget) - 负责生产画面
// 它不是 Widget，它只负责把画面画到 FBO 里
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
        fboRegion.flip(false, true); // FBO 纹理是倒的，需要翻转

        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(w, h, camera); // 默认 Fit
        camera.position.set(0, 0, 0);
    }

    public void setViewportType(boolean extend) {
        if (extend) viewport = new ExtendViewport(width, height, camera);
        else viewport = new FitViewport(width, height, camera);
        viewport.update(width, height, false);
    }

    // 这里是 "核弹" 的核心：在纯净环境下渲染
    public void renderToFbo(Runnable renderLogic) {
        fbo.begin();

        // 1. 设置干净的 GL 状态
        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f); // FBO 背景色
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 2. 执行渲染逻辑
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        renderLogic.run(); // 回调外部传入的绘制代码
        batch.end();

        fbo.end();
    }

    public void dispose() {
        fbo.dispose();
        batch.dispose();
    }
}

// ==========================================
// 3. 显示控件 (ViewWidget) - 负责展示画面
// 它现在仅仅是一个简单的图片展示器，加上了 Letterbox 计算
// ==========================================
class ViewWidget extends Widget {
    private ViewTarget target;
    private Texture bgTexture; // 深灰色背景

    public ViewWidget(ViewTarget target) {
        this.target = target;

        // 生成纯色背景纹理
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(0.2f, 0.2f, 0.2f, 1);
        p.fill();
        bgTexture = new Texture(p);
        p.dispose();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // 1. 画深灰色背景 (填满 Widget 区域)
        batch.draw(bgTexture, getX(), getY(), getWidth(), getHeight());

        // 2. 计算 Letterbox (保持比例居中)
        float widgetRatio = getWidth() / getHeight();
        float fboRatio = (float)target.viewport.getWorldWidth() / target.viewport.getWorldHeight();

        float drawW, drawH;
        if (widgetRatio > fboRatio) {
            drawH = getHeight();
            drawW = drawH * fboRatio;
        } else {
            drawW = getWidth();
            drawH = drawW / fboRatio;
        }

        float drawX = getX() + (getWidth() - drawW) / 2;
        float drawY = getY() + (getHeight() - drawH) / 2;

        // 3. 贴图 (贴刚才 Target 生成好的 FBO)
        batch.draw(target.fboRegion, drawX, drawY, drawW, drawH);
    }

    public void dispose() {
        bgTexture.dispose();
    }
}

// ==========================================
// 4. 主程序组装
// ==========================================
public class FinalProduction extends GScreen {
    Stage stage;
    GameWorld gameWorld;

    ViewTarget gameTarget, sceneTarget;
    ViewWidget gameWidget, sceneWidget;

    Touchpad joystick;

    @Override
    public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

    @Override
    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();

        gameWorld = new GameWorld();
        gameWorld.init();

        // 1. 创建渲染目标 (Producers)
        gameTarget = new ViewTarget(1280, 720);
        sceneTarget = new ViewTarget(1280, 720);
        sceneTarget.setViewportType(true); // Scene 默认 Extend

        // 2. 创建 UI (Consumers)
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

        // UI 布局
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

        // 拖拽控制
        sceneWidget.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					sceneTarget.camera.translate(-getDeltaX() * 2, -getDeltaY() * 2, 0);
					sceneTarget.camera.update();
				}
			});

        win.add(sceneWidget).grow();
        stage.addActor(win);
    }

    @Override
    public void render(float delta) {
        // 1. 逻辑更新
        float speed = 200 * delta;
        gameWorld.update(joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

        // ============================================================
        // 2. 渲染 FBO (Render Pass) - 这里就是 "核弹" 逻辑的应用
        // 彻底禁掉 Scissor，保证 FBO 渲染纯净
        // ============================================================
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // Game 视图渲染逻辑
        gameTarget.renderToFbo(() -> {
            gameTarget.camera.position.set(gameWorld.playerX, gameWorld.playerY, 0);
            gameTarget.camera.update();
            gameWorld.render(gameTarget.batch);
        });

        // Scene 视图渲染逻辑
        sceneTarget.renderToFbo(() -> {
            gameWorld.render(sceneTarget.batch);
            // 可以在这里画 Debug 线
            sceneTarget.batch.end();
            gameWorld.renderDebug(sceneTarget.camera);
            sceneTarget.batch.begin();
        });

        // ============================================================
        // 3. 渲染 UI (UI Pass)
        // ============================================================
        // 恢复屏幕视口
        HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        VisUI.dispose();
        gameWorld.dispose();
        gameTarget.dispose();
        sceneTarget.dispose();
        gameWidget.dispose();
        stage.dispose();
    }
}
