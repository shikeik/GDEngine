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
public class FinalEditorTest extends GScreen {
    private EditorLogic logic;

    @Override
    public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

    @Override
    public void create() {
        logic = new EditorLogic();
        logic.create();
    }

    @Override
    public void render(float delta) {
        logic.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        logic.resize(width, height);
    }

    @Override
    public void dispose() {
        logic.dispose();
    }
}

// ==========================================
// 1. 游戏核心
// ==========================================
class GameWorld {
    public Texture playerTex, bgTex;
    public float playerX = 0, playerY = 0;
    private ShapeRenderer debugRenderer;

    public void init() {
        playerTex = createSolidTexture(32, 32, Color.CORAL);
        bgTex = createSolidTexture(512, 512, Color.TEAL);
        debugRenderer = new ShapeRenderer();
    }

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

    public void render(Batch batch) {
        batch.draw(bgTex, -256, -256); 
        batch.draw(playerTex, playerX, playerY);
    }

    public void renderDebug(Camera camera) {
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(Color.RED);
        debugRenderer.line(-1000, 0, 1000, 0); 
        debugRenderer.setColor(Color.GREEN);
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
// 2. FBO 视图控件 (分离了生产与消费)
// ==========================================
class FboViewWidget extends Widget {
    public interface Renderer {
        void render(Batch batch, Camera camera);
    }

    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private SpriteBatch internalBatch;
    private Viewport internalViewport;
    private Renderer renderer;
    private Texture bgSolidTexture; 
    private final int fboW, fboH;

    public FboViewWidget(int virtualW, int virtualH) {
        this.fboW = virtualW;
        this.fboH = virtualH;

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboW, fboH, false);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);

        internalBatch = new SpriteBatch();
        internalViewport = new FitViewport(fboW, fboH);
        ((OrthographicCamera)internalViewport.getCamera()).position.set(0, 0, 0);

        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        bgSolidTexture = new Texture(p);
        p.dispose();
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public Camera getInternalCamera() {
        return internalViewport.getCamera();
    }

    public void setViewportType(boolean isExtend) {
        Camera old = internalViewport.getCamera();
        if (isExtend) {
            internalViewport = new ExtendViewport(fboW, fboH, old);
        } else {
            internalViewport = new FitViewport(fboW, fboH, old);
        }
        internalViewport.update(fboW, fboH, false); 
    }

    // 【新方法】：专门用于生成这一帧的图像
    // 这个方法必须在 Stage.draw() 之前调用，处于纯净的 GL 环境中
    // 【关键修正】：这是 FBO 绘制的入口
    public void renderFrame() {
        if (renderer == null) return;

        // ============================================================
        // CRITICAL FIX 1: 强行关闭剪裁测试
        // 现象原因：上一帧 UI 绘制留下的 "剪裁框" 还在生效。
        // 如果不关掉，FBO 的 glClear 只能清除那个小框框里的颜色，
        // 导致 FBO 其他区域全是上一帧的残影（涂鸦/拖尾）。
        // ============================================================
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // ============================================================
        // CRITICAL FIX 2: 绑定 FBO 并设置视口
        // ============================================================
        fbo.begin();

        // 显式设置 FBO 的视口 (虽然 fbo.begin() 通常会自动做，但为了保险)
        // 必须是 FBO 的物理尺寸
        Gdx.gl.glViewport(0, 0, fboW, fboH);

        // ============================================================
        // CRITICAL FIX 3: 彻底清屏
        // 既然剪裁关了，视口对了，这一步就能把整个 FBO 刷干净
        // ============================================================
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f); 
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 开始绘制游戏内容
        internalBatch.setProjectionMatrix(internalViewport.getCamera().combined);
        internalBatch.begin();
        renderer.render(internalBatch, internalViewport.getCamera());
        internalBatch.end();

        // 解绑
        fbo.end();

        // ============================================================
        // 善后：恢复剪裁状态（可选，但推荐）
        // 虽然 Stage 会自己管理，但为了不破坏后续逻辑，可以不用管，
        // 因为 Stage.draw 开始时会自动处理 ScissorStack。
        // 但这里最重要的是：一定要在 begin 之前 Disable 掉它。
        // ============================================================
    }

    // 【Draw方法】：现在只负责贴图，不做任何复杂的 GL 操作
    @Override
    public void draw(Batch uiBatch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();

        // 1. 画背景框
        uiBatch.setColor(0.2f, 0.2f, 0.2f, 1); 
        uiBatch.draw(bgSolidTexture, x, y, w, h);
        uiBatch.setColor(Color.WHITE);

        // 2. 计算 Letterbox
        float scale = Math.min(w / fboW, h / fboH); 
        float drawW = fboW * scale;
        float drawH = fboH * scale;
        float drawX = x + (w - drawW) / 2;
        float drawY = y + (h - drawH) / 2;

        // 3. 贴上刚才 renderFrame 生成的图
        uiBatch.draw(fboRegion, drawX, drawY, drawW, drawH);
    }

    public void dispose() {
        fbo.dispose();
        internalBatch.dispose();
        bgSolidTexture.dispose();
    }
}

// ==========================================
// 3. 编辑器逻辑
// ==========================================
class EditorLogic {
    Stage stage;
    GameWorld gameWorld;
    FboViewWidget gameView, sceneView;
    Touchpad joystick;

    public void create() {
        VisUI.load();
        gameWorld = new GameWorld();
        gameWorld.init();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createGameWindow();
        createSceneWindow();
    }

    private void createGameWindow() {
        VisWindow win = new VisWindow("Game View (720p)");
        win.setResizable(true);
        win.setSize(500, 350);
        win.setPosition(50, 50);

        gameView = new FboViewWidget(1280, 720);
        gameView.setRenderer((batch, cam) -> {
            cam.position.set(gameWorld.playerX, gameWorld.playerY, 0);
            cam.update();
            gameWorld.render(batch);
        });

        Touchpad.TouchpadStyle style = VisUI.getSkin().get(Touchpad.TouchpadStyle.class);
        joystick = new Touchpad(10, style);

        VisSelectBox<String> box = new VisSelectBox<>();
        box.setItems("Fit", "Extend");
        box.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					gameView.setViewportType(box.getSelected().equals("Extend"));
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
        stack.add(gameView);
        stack.add(uiTable);

        win.add(stack).grow();
        stage.addActor(win);
    }

    private void createSceneWindow() {
        VisWindow win = new VisWindow("Scene View");
        win.setResizable(true);
        win.setSize(400, 300);
        win.setPosition(600, 50);

        sceneView = new FboViewWidget(1280, 720);
        sceneView.setViewportType(true);

        sceneView.setRenderer((batch, cam) -> {
            gameWorld.render(batch);
            batch.end();
            gameWorld.renderDebug(cam);
            batch.begin();
        });

        sceneView.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					float dx = getDeltaX();
					float dy = getDeltaY();
					sceneView.getInternalCamera().translate(-dx * 2, -dy * 2, 0);
					sceneView.getInternalCamera().update();
				}
			});

        win.add(sceneView).grow();
        stage.addActor(win);
    }

    public void render(float delta) {
        // 1. 更新逻辑
        float speed = 200 * delta;
        gameWorld.update(joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

        // 2. 【先】渲染 FBO (Off-screen)
        // 此时我们加上了 glDisable(SCISSOR)，所以这里是干净的
        gameView.renderFrame();
        sceneView.renderFrame();

        // 3. 【后】渲染屏幕 UI
        // 这一步必须恢复屏幕视口，否则 UI 会画错位置
        HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw(); 
    }

    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    public void dispose() {
        VisUI.dispose();
        gameWorld.dispose();
        gameView.dispose();
        sceneView.dispose();
        stage.dispose();
    }
}
