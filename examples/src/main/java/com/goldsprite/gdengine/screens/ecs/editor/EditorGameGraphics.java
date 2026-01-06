package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.GLVersion;

/**
 * 编辑器模式下的 Graphics 代理
 * 职责：欺骗游戏逻辑，使其认为屏幕尺寸等于 FBO 的尺寸。
 */
public class EditorGameGraphics implements Graphics {
	private final ViewTarget target;

	public EditorGameGraphics(ViewTarget target) {
		this.target = target;
	}

	// --- 核心拦截逻辑：分辨率欺骗 ---

	@Override public int getWidth() { return target.getFboWidth(); }
	@Override public int getHeight() { return target.getFboHeight(); }
	@Override public int getBackBufferWidth() { return target.getFboWidth(); }
	@Override public int getBackBufferHeight() { return target.getFboHeight(); }

	// --- 其他 Graphics 接口透传或屏蔽 ---

	@Override public float getBackBufferScale() { return 0; }
	@Override public float getDeltaTime() { return Gdx.graphics.getDeltaTime(); }
	@Override public float getRawDeltaTime() { return Gdx.graphics.getRawDeltaTime(); }
	@Override public int getFramesPerSecond() { return Gdx.graphics.getFramesPerSecond(); }
	@Override public GraphicsType getType() { return null; }
	@Override public GLVersion getGLVersion() { return Gdx.graphics.getGLVersion(); }
	@Override public long getFrameId() { return Gdx.graphics.getFrameId(); }
	@Override public float getPpiX() { return Gdx.graphics.getPpiX(); }
	@Override public float getPpiY() { return Gdx.graphics.getPpiY(); }
	@Override public float getPpcX() { return Gdx.graphics.getPpcX(); }
	@Override public float getPpcY() { return Gdx.graphics.getPpcY(); }
	@Override public float getDensity() { return Gdx.graphics.getDensity(); }

	@Override public boolean isGL30Available() { return Gdx.graphics.isGL30Available(); }
	@Override public boolean isGL31Available() { return Gdx.graphics.isGL31Available(); }
	@Override public boolean isGL32Available() { return false; }
	@Override public GL20 getGL20() { return Gdx.graphics.getGL20(); }
	@Override public void setGL20(GL20 gl20) { Gdx.graphics.setGL20(gl20); }
	@Override public GL30 getGL30() { return Gdx.graphics.getGL30(); }
	@Override public void setGL30(GL30 gl30) { Gdx.graphics.setGL30(gl30); }
	@Override public GL31 getGL31() { return Gdx.graphics.getGL31(); }
	@Override public void setGL31(GL31 gl31) { Gdx.graphics.setGL31(gl31); }
	@Override public GL32 getGL32() { return null; }
	@Override public void setGL32(GL32 gl32) { }

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
	@Override public void setSystemCursor(Cursor.SystemCursor systemCursor) { }
	@Override public void setCursor(Cursor cursor) { }

	@Override public int getSafeInsetLeft() { return 0; }
	@Override public int getSafeInsetTop() { return 0; }
	@Override public int getSafeInsetBottom() { return 0; }
	@Override public int getSafeInsetRight() { return 0; }
}
