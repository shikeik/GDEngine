package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.GLVersion;

// ==========================================
// 5. 全局代理层 (Global Delegate) - 配置驱动版
// ==========================================
public class Gd {
	// 基础模块透传
	public static final Files files = Gdx.files;
	public static final Application app = Gdx.app;
	public static final Audio audio = Gdx.audio;

	// 核心代理
	public static Input input;
	public static Graphics graphics;

	// 【新增】项目配置中心 (单例数据)
	public static final Config config = new Config();

	public static void init(Mode mode, ViewWidget widget, ViewTarget target) {
		if (mode == Mode.RELEASE) {
			input = Gdx.input;
			graphics = Gdx.graphics;
		} else {
			input = new EditorGameInput(widget);
			graphics = new EditorGameGraphics(target);
		}
	}

	public enum Mode { RELEASE, EDITOR }

	// ==========================================
	// 配置数据定义
	// ==========================================
	public static class Config {
		// 逻辑设计分辨率 (默认 960x540)
		public float logicWidth = 960;
		public float logicHeight = 540;

		// 视口适配策略
		public ViewportType viewportType = ViewportType.FIT;
	}

	public enum ViewportType { FIT, EXTEND, STRETCH }
}

/**
 * 编辑器模式下的 Input 代理
 * 负责将 全局屏幕坐标 修正为 FBO 像素坐标
 */
class EditorGameInput implements Input {
	private final ViewWidget widget;
	private InputProcessor processor;
	private boolean isTouched = false;

	public EditorGameInput(ViewWidget widget) { this.widget = widget; }
	public void setTouched(boolean touched, int pointer) { this.isTouched = touched; }
	// 【修复 1】接口要求返回 int，必须强制转换
	@Override public int getX() { return getX(0); }
	@Override public int getX(int pointer) {
		// mapScreenToFbo 返回的是 float，转为 int 以符合接口定义
		return (int) widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer)).x;
	}
	@Override public int getY() { return getY(0); }
	@Override public int getY(int pointer) { return (int) widget.mapScreenToFbo(Gdx.input.getX(pointer), Gdx.input.getY(pointer)).y; }
	@Override public boolean isTouched() { return isTouched || Gdx.input.isTouched(); }
	@Override public boolean isTouched(int pointer) { return Gdx.input.isTouched(pointer); }
	@Override public boolean justTouched() { return Gdx.input.justTouched(); }
	@Override public InputProcessor getInputProcessor() { return processor; }
	@Override public void setInputProcessor(InputProcessor processor) { this.processor = processor; }
	@Override public boolean isPeripheralAvailable(Peripheral peripheral) { return false; }
	// --- 透传 Gdx.input ---
	@Override public int getDeltaX() { return Gdx.input.getDeltaX(); }
	@Override public int getDeltaX(int pointer) { return Gdx.input.getDeltaX(pointer); }
	@Override public int getDeltaY() { return Gdx.input.getDeltaY(); }
	@Override public int getDeltaY(int pointer) { return Gdx.input.getDeltaY(pointer); }
	@Override public boolean isButtonPressed(int button) { return Gdx.input.isButtonPressed(button); }
	@Override public boolean isButtonJustPressed(int button) { return Gdx.input.isButtonJustPressed(button); }
	@Override public boolean isKeyPressed(int key) { return Gdx.input.isKeyPressed(key); }
	@Override public boolean isKeyJustPressed(int key) { return Gdx.input.isKeyJustPressed(key); }
	// --- 文本输入相关 ---
	@Override public void getTextInput(TextInputListener listener, String title, String text, String hint) { Gdx.input.getTextInput(listener, title, text, hint); }
	// 【修复 2】实现 1.12.1 新增的接口方法
	@Override
	public void getTextInput(TextInputListener listener, String title, String text, String hint, OnscreenKeyboardType type) { Gdx.input.getTextInput(listener, title, text, hint, type); }
	@Override public void setOnscreenKeyboardVisible(boolean visible) { Gdx.input.setOnscreenKeyboardVisible(visible); }
	@Override public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) { Gdx.input.setOnscreenKeyboardVisible(visible, type); }
	// --- 传感器与杂项 ---
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
	@Override public int getRotation() { return 0; }
	@Override public Orientation getNativeOrientation() { return Orientation.Landscape; }
	@Override public boolean isCursorCatched() { return false; }
	@Override public void setCursorCatched(boolean catched) { }
	@Override public void setCursorPosition(int x, int y) { }
	@Override public float getPressure() { return 0; }
	@Override public float getPressure(int pointer) { return 0; }
}

/**
 * 编辑器模式下的 Graphics 代理
 * 负责欺骗游戏：屏幕只有 FBO 那么大
 */
class EditorGameGraphics implements Graphics {
	private final ViewTarget target;

	public EditorGameGraphics(ViewTarget target) { this.target = target; }
	@Override public int getWidth() { return target.getFboWidth(); }
	@Override public int getHeight() { return target.getFboHeight(); }
	@Override public int getBackBufferWidth() { return target.getFboWidth(); }
	@Override public int getBackBufferHeight() { return target.getFboHeight(); }
	@Override public float getBackBufferScale() { return 0; }
	@Override public float getDeltaTime() { return Gdx.graphics.getDeltaTime(); }
	@Override public float getRawDeltaTime() { return Gdx.graphics.getRawDeltaTime(); }
	@Override public int getFramesPerSecond() { return Gdx.graphics.getFramesPerSecond(); }
	@Override public GraphicsType getType() { return null; }
	// --- GL 版本支持 (1.12.1 新增) ---
	@Override public boolean isGL30Available() { return Gdx.graphics.isGL30Available(); }
	// 【修复 3】补充 GL31 支持
	@Override public boolean isGL31Available() { return Gdx.graphics.isGL31Available(); }
	@Override public boolean isGL32Available() { return false; }
	@Override public GL20 getGL20() { return Gdx.graphics.getGL20(); }
	@Override public void setGL20(GL20 gl20) { Gdx.graphics.setGL20(gl20); }
	@Override public GL30 getGL30() { return Gdx.graphics.getGL30(); }
	@Override public void setGL30(GL30 gl30) { Gdx.graphics.setGL30(gl30); }
	@Override public GL31 getGL31() { return Gdx.graphics.getGL31(); }
	@Override public void setGL31(GL31 gl31) { Gdx.graphics.setGL31(gl31); }
	@Override public GL32 getGL32() { return null; }
	@Override public void setGL32(GL32 gl32) {  }
	@Override public long getFrameId() { return Gdx.graphics.getFrameId(); }
	@Override public float getPpiX() { return Gdx.graphics.getPpiX(); }
	@Override public float getPpiY() { return Gdx.graphics.getPpiY(); }
	@Override public float getPpcX() { return Gdx.graphics.getPpcX(); }
	@Override public float getPpcY() { return Gdx.graphics.getPpcY(); }
	@Override public float getDensity() { return Gdx.graphics.getDensity(); }
	@Override public boolean supportsDisplayModeChange() { return false; }
	@Override public Monitor getPrimaryMonitor() { return Gdx.graphics.getPrimaryMonitor(); }
	@Override public Monitor getMonitor() { return Gdx.graphics.getMonitor(); }
	@Override public Monitor[] getMonitors() { return Gdx.graphics.getMonitors(); }
	@Override public DisplayMode[] getDisplayModes() { return Gdx.graphics.getDisplayModes(); }
	@Override public DisplayMode[] getDisplayModes(Monitor monitor) { return Gdx.graphics.getDisplayModes(monitor); }
	@Override public DisplayMode getDisplayMode() { return Gdx.graphics.getDisplayMode(); }
	@Override public DisplayMode getDisplayMode(Monitor monitor) { return Gdx.graphics.getDisplayMode(monitor); }
	@Override public boolean setFullscreenMode(DisplayMode displayMode) { return false; }
	@Override public boolean setWindowedMode(int width, int height) { return false; }
	@Override public void setTitle(String title) { }
	@Override public void setUndecorated(boolean undecorated) { }
	@Override public void setResizable(boolean resizable) { }
	@Override public void setVSync(boolean vsync) { }
	@Override public void setForegroundFPS(int fps) { }
	@Override public BufferFormat getBufferFormat() { return Gdx.graphics.getBufferFormat(); }
	@Override public boolean supportsExtension(String extension) { return Gdx.graphics.supportsExtension(extension); }
	@Override public boolean isContinuousRendering() { return Gdx.graphics.isContinuousRendering(); }
	@Override public void setContinuousRendering(boolean isContinuous) { Gdx.graphics.setContinuousRendering(isContinuous); }
	@Override public void requestRendering() { Gdx.graphics.requestRendering(); }
	@Override public boolean isFullscreen() { return false; }
	@Override public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) { return Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot); }
	// 【修复 4】SystemCursor 引用问题
	@Override public void setSystemCursor(Cursor.SystemCursor systemCursor) { }
	@Override public void setCursor(Cursor cursor) { }
	@Override public GLVersion getGLVersion() { return Gdx.graphics.getGLVersion(); }
	// 【修复 5】刘海屏适配方法 (1.12.x 新增)
	@Override public int getSafeInsetLeft() { return 0; }
	@Override public int getSafeInsetTop() { return 0; }
	@Override public int getSafeInsetBottom() { return 0; }
	@Override public int getSafeInsetRight() { return 0; }
}
