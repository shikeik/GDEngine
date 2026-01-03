package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;

// ==========================================
// 6. 实机运行入口 (The Acid Test)
// 用于验证游戏逻辑是否彻底解耦，不依赖编辑器也能跑
// ==========================================
public class RealGame extends ApplicationAdapter {
    // 【修改】移除本地的 Batch, Camera, Viewport
    // SpriteBatch batch; -> 移除了，GameWorld 自己管理
    // Viewport viewport; -> 移除了，Gd.view 管理
    // Camera camera;     -> 移除了，Gd.view 管理

    GameWorld gameWorld;

    @Override
    public void create() {
        // 1. 初始化代理 (RELEASE模式)
        // 这会自动创建 Gd.view (ViewportManager) 和默认的 GameCamera
        Gd.init(Gd.Mode.RELEASE, null, null);

        // 2. 初始化游戏
        gameWorld = new GameWorld();
        gameWorld.init();

        System.out.println("RealGame Started in RELEASE Mode");
    }

    @Override
    public void resize(int width, int height) {
        // 【关键】通知 Gd 更新视口
        Gd.view.update(width, height);
    }

    @Override
    public void render() {
        float delta = Gd.graphics.getDeltaTime();

        // --- 输入处理 ---
        float speed = 200 * delta;
        float moveX = 0, moveY = 0;

        if (Gd.input.isKeyPressed(Input.Keys.A)) moveX -= speed;
        if (Gd.input.isKeyPressed(Input.Keys.D)) moveX += speed;
        if (Gd.input.isKeyPressed(Input.Keys.W)) moveY += speed;
        if (Gd.input.isKeyPressed(Input.Keys.S)) moveY -= speed;

        // --- 坐标点击测试 ---
        if (Gd.input.isTouched()) {
            // 【修改】使用 Gd.view 获取视口进行坐标转换
            Vector2 touchPos = new Vector2(Gd.input.getX(), Gd.input.getY());
            Gd.view.getGameViewport().unproject(touchPos);

            gameWorld.targetX = touchPos.x;
            gameWorld.targetY = touchPos.y;
        }

        // 更新游戏逻辑
        gameWorld.update(delta, moveX, moveY);

        // --- 相机跟随逻辑 ---
        // 【核心修复】不要用 null 的 local camera，而是找 Gd 要
        Camera cam = Gd.view.getCamera();
        cam.position.set(gameWorld.playerX, gameWorld.playerY, 0);
        cam.update();

        // --- 渲染 ---
        // 实机全屏清屏 (黑边颜色)
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // GameWorld 内部会调用 Gd.view.apply() 和 batch.begin()
        gameWorld.render();
        gameWorld.renderDebug();
    }

    @Override
    public void dispose() {
        gameWorld.dispose();
    }
}
