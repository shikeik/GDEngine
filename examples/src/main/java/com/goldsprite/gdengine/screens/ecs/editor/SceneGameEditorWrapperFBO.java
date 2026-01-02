package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
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
// 入口包装类
// ==========================================
public class SceneGameEditorWrapperFBO extends GScreen {
    private EditorLogic editor;

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

    @Override
    public void create() {
        editor = new EditorLogic();
        editor.create();
    }

    @Override
    public void render(float delta) {
        editor.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        editor.resize(width, height);
    }

    @Override
    public void dispose() {
        editor.dispose();
    }
}

// ==========================================
// 1. 输入抽象层 (Input Bridge) - 解耦的第一步
// ==========================================
interface IGameInput {
    // 这是一个抽象的输入源，可以是真实的键盘，也可以是编辑器的虚拟信号
    float getMoveX();
    float getMoveY();
    boolean isActionPressed();
    // 还可以扩展 getPointerX(), getPointerY() 等，用于解决坐标映射问题
}

// ==========================================
// 2. 游戏核心 (Game Core) - 纯净的业务逻辑
// ==========================================
class GameWorld {
    // 游戏内部的“标准”分辨率，不管外部窗口多大，游戏只认这个
    public static final float VIRTUAL_WIDTH = 1280;
    public static final float VIRTUAL_HEIGHT = 720;

    private Texture playerTex, bgTex;
    private float playerX = 0, playerY = 0;
    private IGameInput inputSource; // 核心持有输入源，而不是直接读 Gdx.input

    public void init() {
        // 模拟资源加载
        playerTex = new Texture(Gdx.files.internal("role.png"));
        bgTex = new Texture(Gdx.files.internal("back.png"));
    }

    public void setInputSource(IGameInput input) {
        this.inputSource = input;
    }

    public void update(float delta) {
        if (inputSource != null) {
            float speed = 300 * delta;
            playerX += inputSource.getMoveX() * speed;
            playerY += inputSource.getMoveY() * speed;
        }
    }

    // 渲染方法：纯粹的绘制，不知道 FBO 的存在
    public void render(Batch batch) {
        batch.draw(bgTex, -500, -500, 1000, 1000);
        batch.draw(playerTex, playerX, playerY);
    }

    public float getPlayerX() { return playerX; }
    public float getPlayerY() { return playerY; }

    public void dispose() {
        playerTex.dispose();
        bgTex.dispose();
    }
}

// ==========================================
// 3. 核心黑科技：FBO 渲染容器 (虚拟机显示器)
// ==========================================
class FboRenderTarget extends Widget {
    // 渲染回调接口
    public interface WorldRenderer {
        void render(Batch batch, Camera camera);
    }

    private FrameBuffer fbo;
    private TextureRegion fboRegion; // 用于翻转纹理坐标
    private WorldRenderer renderer;
    private Viewport internalViewport; // FBO 内部的视口逻辑 (比如 Fit/Extend)
    private SpriteBatch internalBatch; // 专门用于画 FBO 内部的 Batch，与 UI 隔离

    // 虚拟分辨率
    private final int fboWidth, fboHeight;

    public FboRenderTarget(int virtualWidth, int virtualHeight) {
        this.fboWidth = virtualWidth;
        this.fboHeight = virtualHeight;

        // 1. 创建 FrameBuffer (RGB888, 不带深度，如果做3D需要带深度)
        // 这就是我们的“显存画布”
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, false);

        // FBO 的纹理在内存里是上下颠倒的，需要包装成 Region 并翻转
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);

        // 2. 内部渲染工具
        internalBatch = new SpriteBatch();
        // 默认内部使用 Fit 模式，保证游戏逻辑分辨率一致
        internalViewport = new FitViewport(virtualWidth, virtualHeight);
        ((OrthographicCamera)internalViewport.getCamera()).setToOrtho(false, virtualWidth, virtualHeight);
    }

    public void setRenderer(WorldRenderer renderer) {
        this.renderer = renderer;
    }

    // 切换内部视口策略 (Extend, Fit, Stretch)
    public void setViewportType(ViewportType type) {
        Camera oldCam = internalViewport.getCamera();
        switch (type) {
            case FIT: internalViewport = new FitViewport(fboWidth, fboHeight, oldCam); break;
            case EXTEND: internalViewport = new ExtendViewport(fboWidth, fboHeight, oldCam); break;
            case STRETCH: internalViewport = new FixedStretchViewport(fboWidth, fboHeight, oldCam); break; // 自定义Stretch
        }
        // 切换后需要 update 一次，设为 FBO 的物理大小
        internalViewport.update(fboWidth, fboHeight, true);
    }

    // 为了方便演示 Stretch 模式写的一个简单 Viewport
    public static class FixedStretchViewport extends FitViewport {
        public FixedStretchViewport(float w, float h, Camera c) { super(w, h, c); }
        // 这里的逻辑通常直接用 StretchViewport 即可，为了保持代码整洁直接复用逻辑
    }

    public enum ViewportType { FIT, EXTEND, STRETCH }

    public Camera getInternalCamera() {
        return internalViewport.getCamera();
    }

    @Override
    public void draw(Batch uiBatch, float parentAlpha) {
        // 不需要 validate，因为 FBO 大小是固定的
        if (renderer == null) return;

        // ==============================================
        // 阶段 A: 渲染游戏世界到 FBO (Off-screen Rendering)
        // ==============================================

        // 1. 暂停外部 UI 绘制
        uiBatch.end();

        // 2. 绑定 FBO，开始“作画”
        fbo.begin();

        // 3. 【关键】：设置 OpenGL 视口为 FBO 的大小
        // 无论外部窗口多小，这里永远是 1280x720 (或者你设定的虚拟分辨率)
        Gdx.gl.glViewport(0, 0, fboWidth, fboHeight);

        // 4. 清理 FBO 画布
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 5. 调用游戏渲染逻辑
        internalBatch.setProjectionMatrix(internalViewport.getCamera().combined);
        internalBatch.begin();
        renderer.render(internalBatch, internalViewport.getCamera());
        internalBatch.end();

        // 6. 解绑 FBO，恢复到屏幕
        fbo.end();

        // 7. 恢复外部视口 (这一步通过 HdpiUtils 自动找回全屏视口)
        HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // ==============================================
        // 阶段 B: 将 FBO 当作一张图画到 UI 上
        // ==============================================

        uiBatch.begin();

        // 计算按比例缩放 (Letterboxing)：让 FBO 保持比例显示在 Widget 区域内
        // 这就是实机画面在编辑器里的“投影”
        float widgetW = getWidth();
        float widgetH = getHeight();

        // 简单的 Fit 算法，让 FBO 纹理居中显示，保持比例
        // 如果想要 Stretch 填满，直接用 widgetW, widgetH 即可
        // 这里演示简单的填满 (可能会拉伸显示，但不影响内部逻辑)
        uiBatch.draw(fboRegion, getX(), getY(), widgetW, widgetH);
    }

    public void dispose() {
        fbo.dispose();
        internalBatch.dispose();
    }
}

// ==========================================
// 4. 编辑器逻辑组装 (Editor Logic)
// ==========================================
class EditorLogic {
    private Stage stage;
    private GameWorld gameWorld;
    private FboRenderTarget gameViewTarget;
    private FboRenderTarget sceneViewTarget;

    // UI 组件
    private Touchpad joystick;

    public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

        // 编辑器 UI 使用 ScreenViewport (保证按钮清晰)，或者 ExtendViewport
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 1. 初始化纯净的游戏核心
        gameWorld = new GameWorld();
        gameWorld.init();

        // 2. 组装输入源 (连接 Joystick -> GameWorld)
        // 这里的匿名类就是 "Input Bridge" 的具体实现
        gameWorld.setInputSource(new IGameInput() {
				@Override
				public float getMoveX() {
					return joystick != null ? joystick.getKnobPercentX() : 0;
				}
				@Override
				public float getMoveY() {
					return joystick != null ? joystick.getKnobPercentY() : 0;
				}
				@Override
				public boolean isActionPressed() {
					return false; // 示例
				}
			});

        // 3. 创建窗口
        createGameViewWindow();
        createSceneViewWindow();
    }

    private void createGameViewWindow() {
        VisWindow window = new VisWindow("Game View (FBO 720p)");
        window.setResizable(true);
        window.setSize(500, 300);
        window.setPosition(50, 50);

        // A. 创建 FBO 渲染目标 (固定 1280x720 内部分辨率)
        gameViewTarget = new FboRenderTarget(1280, 720);
        gameViewTarget.setViewportType(FboRenderTarget.ViewportType.FIT); // 内部保持比例

        // B. 注入渲染逻辑
        gameViewTarget.setRenderer((batch, camera) -> {
            // Game 视图：相机跟随
            camera.position.set(gameWorld.getPlayerX(), gameWorld.getPlayerY(), 0);
            camera.update();
            gameWorld.render(batch);
        });

        // C. UI 覆盖层
        Table uiLayer = new Table();
        uiLayer.setFillParent(true);

        // 摇杆
        Touchpad.TouchpadStyle style = VisUI.getSkin().get(Touchpad.TouchpadStyle.class);
        joystick = new Touchpad(10, style);

        // 视口切换
        VisSelectBox<String> selector = new VisSelectBox<>();
        selector.setItems("Fit", "Extend");
        selector.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if(selector.getSelected().equals("Fit")) 
						gameViewTarget.setViewportType(FboRenderTarget.ViewportType.FIT);
					else 
						gameViewTarget.setViewportType(FboRenderTarget.ViewportType.EXTEND);
				}
			});

        uiLayer.top().right().add(selector).width(100).pad(5);
        uiLayer.row();
        uiLayer.add().expand().fill();
        uiLayer.row();
        uiLayer.left().bottom().add(joystick).size(100).pad(20);

        Stack stack = new Stack();
        stack.add(gameViewTarget);
        stack.add(uiLayer);

        window.add(stack).grow();
        stage.addActor(window);
    }

    private void createSceneViewWindow() {
        VisWindow window = new VisWindow("Scene View (FBO Independent)");
        window.setResizable(true);
        window.setSize(400, 300);
        window.setPosition(600, 50);

        // Scene 视图也可以有自己的分辨率，或者跟 Game 一样
        sceneViewTarget = new FboRenderTarget(1280, 720);
        sceneViewTarget.setViewportType(FboRenderTarget.ViewportType.EXTEND); // 编辑器看全景

        sceneViewTarget.setRenderer((batch, camera) -> {
            // Scene 视图：相机位置由拖拽控制，不跟随
            gameWorld.render(batch);
            // 可以在这里画网格线 DebugLines
        });

        // Scene 相机控制 (拖拽)
        // 注意：这里的拖拽修改的是 sceneViewTarget 内部的相机
        sceneViewTarget.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					float dx = getDeltaX();
					float dy = getDeltaY();
					// 简单的反向移动
					// 注意：这里需要根据 FBO 分辨率和 Widget 大小的比例来缩放 dx/dy 才能达到完美手感
					// 暂时简单处理
					float ratio = 1280f / sceneViewTarget.getWidth(); 
					sceneViewTarget.getInternalCamera().translate(-dx * ratio, -dy * ratio, 0);
					sceneViewTarget.getInternalCamera().update();
				}
			});

        window.add(sceneViewTarget).grow();
        stage.addActor(window);
    }

    public void render(float delta) {
        gameWorld.update(delta);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
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
        if(gameViewTarget != null) gameViewTarget.dispose();
        if(sceneViewTarget != null) sceneViewTarget.dispose();
        stage.dispose();
    }
}
