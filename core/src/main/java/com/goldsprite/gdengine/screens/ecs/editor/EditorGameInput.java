package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

/**
 * 编辑器模式下的 Input 代理
 * 职责：将全局屏幕坐标修正为 FBO 像素坐标，实现“画中画”输入支持。
 */
public class EditorGameInput implements Input {
	private final ViewWidget widget;
	private InputProcessor processor;
	private boolean isTouched = false;

	public EditorGameInput(ViewWidget widget) {
		this.widget = widget;
	}

	public void setTouched(boolean touched, int pointer) {
		this.isTouched = touched;
	}

	/**
	 * 判断当前输入是否在视口区域内 (核心过滤逻辑)
	 */
	private boolean shouldProcessInput() {
		return widget.isInViewport(Gdx.input.getX(), Gdx.input.getY());
	}

	// --- 核心拦截逻辑：坐标映射 ---

	@Override
	public int getX() {
		return getX(0);
	}

	@Override
	public int getX(int pointer) {
		// mapScreenToFbo 返回的是 float，转为 int 以符合接口定义
		Vector2 fboPos = widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer));
		return (int) fboPos.x;
	}

	@Override
	public int getY() {
		return getY(0);
	}

	@Override
	public int getY(int pointer) {
		Vector2 fboPos = widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer));
		return (int) fboPos.y;
	}

	// --- 其他 Input 接口透传 ---

	@Override public boolean isTouched() { return shouldProcessInput() && (isTouched || Gdx.input.isTouched()); }
	@Override public boolean isTouched(int pointer) { return shouldProcessInput() && Gdx.input.isTouched(pointer); }
	@Override public boolean justTouched() { return shouldProcessInput() && Gdx.input.justTouched(); }
	@Override public InputProcessor getInputProcessor() { return processor; }
	@Override public void setInputProcessor(InputProcessor processor) { this.processor = processor; }
	@Override public boolean isPeripheralAvailable(Peripheral peripheral) { return false; }
	@Override public int getRotation() { return 0; }
	@Override public Orientation getNativeOrientation() { return Orientation.Landscape; }
	@Override public void setCursorCatched(boolean catched) { }
	@Override public boolean isCursorCatched() { return false; }
	@Override public void setCursorPosition(int x, int y) { }

	@Override public int getDeltaX() { return Gdx.input.getDeltaX(); }
	@Override public int getDeltaX(int pointer) { return Gdx.input.getDeltaX(pointer); }
	@Override public int getDeltaY() { return Gdx.input.getDeltaY(); }
	@Override public int getDeltaY(int pointer) { return Gdx.input.getDeltaY(pointer); }

	@Override public boolean isButtonPressed(int button) { return shouldProcessInput() && Gdx.input.isButtonPressed(button); }
	@Override public boolean isButtonJustPressed(int button) { return shouldProcessInput() && Gdx.input.isButtonJustPressed(button); }
	@Override public boolean isKeyPressed(int key) { return shouldProcessInput() && Gdx.input.isKeyPressed(key); }
	@Override public boolean isKeyJustPressed(int key) { return shouldProcessInput() && Gdx.input.isKeyJustPressed(key); }

	@Override public void getTextInput(TextInputListener listener, String title, String text, String hint) { Gdx.input.getTextInput(listener, title, text, hint); }
	@Override public void getTextInput(TextInputListener listener, String title, String text, String hint, OnscreenKeyboardType type) { Gdx.input.getTextInput(listener, title, text, hint, type); }
	@Override public void setOnscreenKeyboardVisible(boolean visible) { Gdx.input.setOnscreenKeyboardVisible(visible); }
	@Override public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) { Gdx.input.setOnscreenKeyboardVisible(visible, type); }

	@Override public void vibrate(int milliseconds) { }
	@Override public void vibrate(int milliseconds, boolean fallback) { }
	@Override public void vibrate(int milliseconds, int amplitude, boolean fallback) { }
	@Override public void vibrate(VibrationType vibrationType) { }
	@Override public float getAzimuth() { return 0; }
	@Override public float getPitch() { return 0; }
	@Override public float getRoll() { return 0; }
	@Override public void getRotationMatrix(float[] matrix) {}
	@Override public long getCurrentEventTime() { return Gdx.input.getCurrentEventTime(); }
	@Override public boolean isCatchBackKey() { return false; }
	@Override public void setCatchBackKey(boolean catchBack) { }
	@Override public boolean isCatchMenuKey() { return false; }
	@Override public void setCatchMenuKey(boolean catchMenu) { }
	@Override public void setCatchKey(int keycode, boolean catchKey) { }
	@Override public boolean isCatchKey(int keycode) { return false; }
	@Override public float getAccelerometerX() { return 0; }
	@Override public float getAccelerometerY() { return 0; }
	@Override public float getAccelerometerZ() { return 0; }
	@Override public float getGyroscopeX() { return 0; }
	@Override public float getGyroscopeY() { return 0; }
	@Override public float getGyroscopeZ() { return 0; }
	@Override public int getMaxPointers() { return Gdx.input.getMaxPointers(); }
	@Override public float getPressure() { return 0; }
	@Override public float getPressure(int pointer) { return 0; }
}
