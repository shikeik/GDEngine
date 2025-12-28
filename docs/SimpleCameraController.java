package com.goldsprite.biowar.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class SimpleCameraController implements InputProcessor {

	private final OrthographicCamera camera;
	private final GestureDetector gestureDetector;
	private final InputAdapter mouseInputAdapter;

	// 输入锁定开关
	private boolean inputEnabled = true;

	public static float minZoom = 0.001f;
	public static float maxZoom = 100.0f;
	public static float scrollFactor = 0.1f;

	public SimpleCameraController(OrthographicCamera camera) {
		this.camera = camera;
		GestureDetector.GestureListener listener = new CameraGestureListener();
		// 移动端手势
		this.gestureDetector = new GestureDetector(20, 0.4f, 1.1f, 0.15f, listener);
		// PC端鼠标
		this.mouseInputAdapter = new CameraMouseListener();
	}

	public void setInputEnabled(boolean enabled) {
		this.inputEnabled = enabled;
	}

	public void update(float dt) {
		if (inputEnabled) {
			// [修复] 移动速度基于视口宽度，实现自适应
			// 基础速度：1秒移动 1 个屏幕宽度
			float baseSpeed = camera.viewportWidth; 
			float speed = baseSpeed * camera.zoom * dt;

			if(Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) camera.translate(-speed, 0);
			if(Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.translate(speed, 0);
			if(Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) camera.translate(0, speed);
			if(Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) camera.translate(0, -speed);
		}
		camera.update();
	}

	// --- 代理 InputProcessor ---
	@Override public boolean touchDown(int x, int y, int pointer, int button) {
		if(!inputEnabled) return false;
		if (mouseInputAdapter.touchDown(x, y, pointer, button)) return true;
		return gestureDetector.touchDown(x, y, pointer, button);
	}
	@Override public boolean touchUp(int x, int y, int pointer, int button) {
		mouseInputAdapter.touchUp(x, y, pointer, button);
		return gestureDetector.touchUp(x, y, pointer, button);
	}
	@Override public boolean touchDragged(int x, int y, int pointer) {
		if(!inputEnabled) return false;
		if (mouseInputAdapter.touchDragged(x, y, pointer)) return true;
		return gestureDetector.touchDragged(x, y, pointer);
	}
	@Override public boolean scrolled(float amountX, float amountY) {
		return mouseInputAdapter.scrolled(amountX, amountY);
	}

	@Override public boolean keyDown(int keycode) { return mouseInputAdapter.keyDown(keycode); }
	@Override public boolean keyUp(int keycode) { return mouseInputAdapter.keyUp(keycode); }
	@Override public boolean keyTyped(char character) { return mouseInputAdapter.keyTyped(character); }
	@Override public boolean mouseMoved(int screenX, int screenY) { return mouseInputAdapter.mouseMoved(screenX, screenY); }
	@Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return gestureDetector.touchCancelled(screenX, screenY, pointer, button); }

	// --- 手势逻辑 (Mobile) ---
	private class CameraGestureListener extends GestureDetector.GestureAdapter {
		private float initialScale = 1f;
		@Override
		public boolean pan(float x, float y, float deltaX, float deltaY) {
			if(!inputEnabled) return false;
			if (Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Desktop) {
				// [修复] 计算屏幕像素到世界单位的比例
				float unitsPerPixel = camera.viewportWidth / Gdx.graphics.getWidth();
				camera.translate(-deltaX * unitsPerPixel * camera.zoom, deltaY * unitsPerPixel * camera.zoom);
				return true;
			}
			return false;
		}
		@Override
		public boolean zoom(float initialDistance, float distance) {
			float ratio = initialDistance / distance;
			float newZoom = MathUtils.clamp(initialScale * ratio, minZoom, maxZoom);
			camera.zoom = newZoom;
			return true;
		}
		@Override
		public boolean touchDown(float x, float y, int pointer, int button) {
			initialScale = camera.zoom;
			return false;
		}
	}

	// --- 鼠标逻辑 (PC) ---
	private class CameraMouseListener extends InputAdapter {
		private int lastX, lastY;
		private boolean isPanning = false;

		@Override
		public boolean touchDown(int x, int y, int pointer, int button) {
			if (button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
				isPanning = true;
				lastX = x;
				lastY = y;
				return true;
			}
			return false;
		}

		@Override
		public boolean touchUp(int x, int y, int pointer, int button) {
			if (button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
				isPanning = false;
				return true;
			}
			return false;
		}

		@Override
		public boolean touchDragged(int x, int y, int pointer) {
			if (isPanning) {
				float dx = x - lastX;
				float dy = y - lastY;
				// [修复] 计算屏幕像素到世界单位的比例
				float unitsPerPixel = camera.viewportWidth / Gdx.graphics.getWidth();
				camera.translate(-dx * unitsPerPixel * camera.zoom, dy * unitsPerPixel * camera.zoom);
				lastX = x;
				lastY = y;
				return true;
			}
			return false;
		}

		@Override
		public boolean scrolled(float amountX, float amountY) {
			Vector3 before = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
			float targetZoom = camera.zoom + amountY * scrollFactor * camera.zoom;
			camera.zoom = MathUtils.clamp(targetZoom, minZoom, maxZoom);
			camera.update();
			Vector3 after = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
			camera.translate(before.x - after.x, before.y - after.y);
			return true;
		}
	}
}
