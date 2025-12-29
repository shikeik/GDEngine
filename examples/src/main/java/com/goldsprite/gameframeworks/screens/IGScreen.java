package com.goldsprite.gameframeworks.screens;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.viewport.Viewport;

public interface IGScreen extends Screen {
	ScreenManager getScreenManager();

	void setScreenManager(ScreenManager screenManager);

	boolean isInitialized();

	void initialize();

	InputMultiplexer getImp();

	void setImp(InputMultiplexer imp);
	
	Viewport getUIViewport();
}
