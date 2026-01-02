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
// 0. 入口 (Entry Point)
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
// 1. 游戏核心 (Game World) - 纯逻辑与绘制
// ==========================================
class GameWorld {
    public Texture playerTex, bgTex;
    public float playerX = 0, playerY = 0;

    // 简单的辅助线绘制工具
    private ShapeRenderer debugRenderer;

    public void init() {
        // 模拟资源 (白色方块代替图片，防止你没有素材报错)
        playerTex = createSolidTexture(32, 32, Color.CORAL);
        bgTex = createSolidTexture(512, 512, Color.TEAL); // 地板
        debugRenderer = new ShapeRenderer();
    }

    // 生成纯色纹理的辅助方法
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

    // 纯粹的绘制，不知道视口，不知道FBO
    public void render(Batch batch) {
        // 画背景 (在 0,0 处)
        batch.draw(bgTex, -256, -256); 
        // 画玩家
        batch.draw(playerTex, playerX, playerY);
    }

    // 画辅助线 (世界原点)
    public void renderDebug(Camera camera) {
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(Color.RED);
        debugRenderer.line(-1000, 0, 1000, 0); // X轴
        debugRenderer.setColor(Color.GREEN);
        debugRenderer.line(0, -1000, 0, 1000); // Y轴
        debugRenderer.end();
    }

    public void dispose() {
        playerTex.dispose();
        bgTex.dispose();
        debugRenderer.dispose();
    }
}

// ==========================================
// 2. FBO 视图控件 (The Clean Widget)
// ==========================================
class FboViewWidget extends Widget {
    // 内部渲染的回调接口
    public interface Renderer {
        void render(Batch batch, Camera camera);
    }

    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private SpriteBatch internalBatch;
    private Viewport internalViewport; // 内部相机的视口

    private Renderer renderer;
    private Texture bgSolidTexture; // 用来“刷墙”的纹理

    private final int fboW, fboH;

    public FboViewWidget(int virtualW, int virtualH) {
        this.fboW = virtualW;
        this.fboH = virtualH;

        // 1. 初始化 FBO
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboW, fboH, false);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true); // 翻转Y轴

        // 2. 内部工具
        internalBatch = new SpriteBatch();
        // 默认 internalViewport
        internalViewport = new FitViewport(fboW, fboH);
        ((OrthographicCamera)internalViewport.getCamera()).position.set(0, 0, 0); // 默认看世界原点

        // 3. 创建一个纯白色 1x1 纹理，用于画背景底色
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
        internalViewport.update(fboW, fboH, false); // 保持相机位置不变
    }

    @Override
    public void draw(Batch uiBatch, float parentAlpha) {
        // === 步骤 A: 渲染 FBO (虚拟机内部) ===
        if (renderer != null) {
            uiBatch.end(); // 暂停 UI 绘制

            // 【核心修复开始】==========================================
            // 检查当前是否开启了剪裁（VisWindow 肯定开了）
            boolean wasScissorEnabled = Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST);
			wasScissorEnabled = true;
			
            // 必须暂时关闭剪裁！
            // 否则 FBO 里的 glClear 和 draw 都会被限制在“屏幕上窗口的那个矩形里”
            // 这就是导致“涂鸦”和“画面残缺”的元凶
            if (wasScissorEnabled) Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            // ========================================================

            fbo.begin();

            // FBO 内部清屏 (现在没有剪裁限制了，这里会真正清除整个 FBO)
            Gdx.gl.glViewport(0, 0, fboW, fboH);
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f); 
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            internalBatch.setProjectionMatrix(internalViewport.getCamera().combined);
            internalBatch.begin();
            renderer.render(internalBatch, internalViewport.getCamera());
            internalBatch.end();

            fbo.end();

            // 【核心修复结束】==========================================
            // 恢复现场：如果之前开了剪裁，现在要开回来，否则 VisWindow 的边框会被画坏
            if (wasScissorEnabled) Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);

            // 恢复 UI 视口
            HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            uiBatch.begin(); // 恢复 UI 绘制
        }

        // === 步骤 B: 绘制 Widget (贴到墙上) ===

        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();

        // 画背景底色 (这一步依然保留，作为双重保险，且美观)
        uiBatch.setColor(0.2f, 0.2f, 0.2f, 1); 
        uiBatch.draw(bgSolidTexture, x, y, w, h);
        uiBatch.setColor(Color.WHITE);

        // 计算 Letterbox
        float scale = Math.min(w / fboW, h / fboH);
        float drawW = fboW * scale;
        float drawH = fboH * scale;
        float drawX = x + (w - drawW) / 2;
        float drawY = y + (h - drawH) / 2;

        // 画 FBO 画面
        uiBatch.draw(fboRegion, drawX, drawY, drawW, drawH);
    }

    public void dispose() {
        fbo.dispose();
        internalBatch.dispose();
        bgSolidTexture.dispose();
    }
}

// ==========================================
// 3. 编辑器逻辑组装 (Editor Logic)
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

        // 编辑器 UI
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

        // 1. 创建 FBO Widget (1280x720)
        gameView = new FboViewWidget(1280, 720);

        // 2. 注入渲染逻辑
        gameView.setRenderer((batch, cam) -> {
            // Game 视图：相机跟随玩家
            cam.position.set(gameWorld.playerX, gameWorld.playerY, 0);
            cam.update();
            gameWorld.render(batch);
        });

        // 3. UI 布局
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

        // 第一行：右上角的下拉框
        // .expandX() 让这一格在水平方向占满，.right() 让内容靠右
        uiTable.add(box).expandX().top().right().width(80).pad(5);

        // 换行
        uiTable.row();

        // 第二行：中间占位符
        // 添加一个空的 Actor，让它 expand() fill() 撑开中间的空间，把上面的顶上去，下面的挤下来
        uiTable.add().expand().fill();

        // 换行
        uiTable.row();

        // 第三行：左下角的摇杆
        // 修正点：先 add(joystick)，再设置它的对齐方式
        uiTable.add(joystick).bottom().left().pad(10);
			
        Stack stack = new Stack();
        stack.add(gameView);
        stack.add(uiTable);

        win.add(stack).grow(); // 填满窗口
        stage.addActor(win);
    }

    private void createSceneWindow() {
        VisWindow win = new VisWindow("Scene View");
        win.setResizable(true);
        win.setSize(400, 300);
        win.setPosition(600, 50);

        // Scene 视图
        sceneView = new FboViewWidget(1280, 720);
        sceneView.setViewportType(true); // 默认 Extend 看更多

        // 【关键】Scene 视图的渲染
        sceneView.setRenderer((batch, cam) -> {
            // 这里相机不跟随，只负责画
            gameWorld.render(batch);
            // 额外画辅助线，证明这是 Scene 视图
            batch.end();
            gameWorld.renderDebug(cam);
            batch.begin();
        });

        // 拖拽控制
        sceneView.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					float dx = getDeltaX();
					float dy = getDeltaY();
					// 简单的反向移动相机
					// *2 是为了让拖动感觉快一点
					sceneView.getInternalCamera().translate(-dx * 2, -dy * 2, 0);
					sceneView.getInternalCamera().update();
				}
			});

        win.add(sceneView).grow();
        stage.addActor(win);
    }

    public void render(float delta) {
        // 简单的输入更新
        float speed = 200 * delta;
        gameWorld.update(joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

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
