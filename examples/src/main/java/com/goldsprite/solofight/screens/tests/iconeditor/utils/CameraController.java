package com.goldsprite.solofight.screens.tests.iconeditor.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

public class CameraController extends InputAdapter {
    private final OrthographicCamera cam;
    private int lastX, lastY;
    private boolean enabled = true;

    public CameraController(OrthographicCamera cam) { this.cam = cam; }
    public void setInputEnabled(boolean v) { enabled = v; }

    public void update(float dt) {
        if(!enabled) return;
        // [Fix] 禁用 WASD 移动
        // float speed = 500 * dt * cam.zoom;
        // if(Gdx.input.isKeyPressed(Input.Keys.A)) cam.translate(-speed, 0);
        // if(Gdx.input.isKeyPressed(Input.Keys.D)) cam.translate(speed, 0);
        // if(Gdx.input.isKeyPressed(Input.Keys.W)) cam.translate(0, speed);
        // if(Gdx.input.isKeyPressed(Input.Keys.S)) cam.translate(0, -speed);
        cam.update();
    }

    @Override public boolean scrolled(float amountX, float amountY) {
        if(!enabled) return false;
        cam.zoom += amountY * 0.1f;
        cam.zoom = MathUtils.clamp(cam.zoom, 0.1f, 10f);
        return true;
    }

    @Override public boolean touchDown(int x, int y, int pointer, int button) {
        if(!enabled) return false;
        if(button == Input.Buttons.RIGHT) { lastX = x; lastY = y; return true; }
        return false;
    }

    @Override public boolean touchDragged(int x, int y, int pointer) {
        if(!enabled) return false;
        if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            float z = cam.zoom;
            cam.translate(-(x - lastX)*z, (y - lastY)*z);
            lastX = x; lastY = y;
            return true;
        }
        return false;
    }
}
