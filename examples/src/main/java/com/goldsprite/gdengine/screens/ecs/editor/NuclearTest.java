package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisWindow;

// 直接继承 ApplicationAdapter 或 GScreen，看你的环境
public class NuclearTest extends GScreen {

    Stage stage;
    FrameBuffer fbo;
    TextureRegion fboRegion;
    SpriteBatch fboBatch;
    OrthographicCamera fboCam;
    ShapeRenderer debugShapes;

    // 用于显示的 Image 控件
    Image gameImageActor; 

    // 测试变量
    float time = 0;

    @Override
    public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

    @Override
    public void create() {
        if(!VisUI.isLoaded()) VisUI.load();

        // 1. 建立 FBO (1280x720)
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, false);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true); // 翻转 Y 轴

        fboBatch = new SpriteBatch();
        fboCam = new OrthographicCamera(1280, 720);
        fboCam.position.set(0, 0, 0); // 中心对准 0,0

        debugShapes = new ShapeRenderer();

        // 2. 建立 UI
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createWindow();
    }

    private void createWindow() {
        VisWindow win = new VisWindow("FBO TEST");
        win.setResizable(true);
        win.setSize(600, 400);
        win.setPosition(100, 100);

        // 我们直接用 Image 来显示 FBO 纹理，不搞复杂的 Widget 了
        gameImageActor = new Image(fboRegion);

        // 简单的拖拽逻辑：拖动图片移动相机
        gameImageActor.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					fboCam.translate(-getDeltaX() * 2, -getDeltaY() * 2, 0);
					fboCam.update();
				}
			});

        // 放入 Table 布局
        Table root = new Table();
        // 使用 fit 方式缩放图片
        root.add(gameImageActor).grow(); 

        win.add(root).grow();
        stage.addActor(win);
    }

    @Override
    public void render(float delta) {
        time += delta;

        // ============================================================
        // 步骤 1: 暴力渲染 FBO (在一切开始之前)
        // ============================================================

        // A. 强制关闭剪裁测试 (这是防止涂鸦的关键)
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // B. 绑定 FBO
        fbo.begin();

        // C. 设置全尺寸视口
        Gdx.gl.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());

        // D. 【暴力清屏】红蓝闪烁背景
        // 如果你能看到背景在红蓝之间渐变，说明清屏成功了，绝对不可能有涂鸦
        float r = (float)Math.abs(Math.sin(time));
        float b = (float)Math.abs(Math.cos(time));
        Gdx.gl.glClearColor(r, 0, b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // E. 画内容
        fboBatch.setProjectionMatrix(fboCam.combined);
        fboBatch.begin();

        // 画一个巨大的网格，证明相机在动
        drawDebugGrid(fboBatch);

        fboBatch.end();

        // F. 画中心十字线 (ShapeRenderer)
        debugShapes.setProjectionMatrix(fboCam.combined);
        debugShapes.begin(ShapeRenderer.ShapeType.Line);
        debugShapes.setColor(Color.YELLOW);
        debugShapes.line(-500, 0, 500, 0);
        debugShapes.line(0, -500, 0, 500);
        debugShapes.rect(-50, -50, 100, 100); // 中心方块
        debugShapes.end();

        fbo.end();

        // ============================================================
        // 步骤 2: 渲染 UI
        // ============================================================

        // 恢复屏幕视口 (用 HdpiUtils 适配高分屏)
        HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        ScreenUtils.clear(Color.BLACK); // 清除屏幕背景

        stage.act(delta);
        stage.draw();
    }

    // 画一个简单的网格背景
    private void drawDebugGrid(Batch batch) {
        // 造一个临时的白色纹理
        if (!VisUI.isLoaded()) return; 
        Texture white = VisUI.getSkin().getRegion("white").getTexture();
        batch.setColor(1, 1, 1, 0.3f);

        for (int i = -1000; i <= 1000; i+=100) {
            batch.draw(white, i, -1000, 2, 2000); // 竖线
            batch.draw(white, -1000, i, 2000, 2); // 横线
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        fbo.dispose();
        fboBatch.dispose();
        debugShapes.dispose();
        stage.dispose();
        VisUI.dispose();
    }
}
