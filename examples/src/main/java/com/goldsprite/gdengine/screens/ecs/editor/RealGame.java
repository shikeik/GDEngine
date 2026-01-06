package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.core.Gd;

// ==========================================
// 6. 实机运行入口 (The Acid Test)
// ==========================================
public class RealGame extends ApplicationAdapter {
    GameWorld gameWorld;

    @Override
    public void create() {
		// 1. 【修改点】初始化 RELEASE 模式 (传入原生 Gdx 实现)
		Gd.init(Gd.Mode.RELEASE, Gdx.input, Gdx.graphics, null);

		// 2. 初始化游戏
		gameWorld = new GameWorld();
		gameWorld.init();

        System.out.println("RealGame Started in RELEASE Mode");
    }

    @Override
    public void resize(int width, int height) {
        // 实机屏幕变化时，直接更新游戏视口
        gameWorld.getViewport().update(width, height, true);
    }

    @Override
    public void render() {
        float delta = Gd.graphics.getDeltaTime();

        // 简单的键盘控制验证
        float speed = 200 * delta;
        float moveX = 0, moveY = 0;
        if (Gd.input.isKeyPressed(Input.Keys.A)) moveX -= speed;
        if (Gd.input.isKeyPressed(Input.Keys.D)) moveX += speed;
        if (Gd.input.isKeyPressed(Input.Keys.W)) moveY += speed;
        if (Gd.input.isKeyPressed(Input.Keys.S)) moveY -= speed;

        // 点击测试
        if (Gd.input.isTouched()) {
            Vector2 touchPos = new Vector2(Gd.input.getX(), Gd.input.getY());
            gameWorld.getViewport().unproject(touchPos);
            gameWorld.targetX = touchPos.x;
            gameWorld.targetY = touchPos.y;
        }

        // 逻辑更新
        gameWorld.update(delta, moveX, moveY);

        // 渲染
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 应用视口
        gameWorld.getViewport().apply();
        // 传入游戏相机进行渲染
        gameWorld.render(gameWorld.getGameCamera());
        gameWorld.renderDebug(gameWorld.getGameCamera());
    }

    @Override
    public void dispose() {
        gameWorld.dispose();
    }
}
