package com.goldsprite.gameframeworks.screens.basics;

import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.PlatformImpl;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.log.Debug;
import com.badlogic.gdx.Gdx;

public abstract class ExampleGScreen extends GScreen {

	public String getIntroduction() { return ""; }

	// 4. 自动处理转屏逻辑
	@Override
	public void show() {
		super.show();

		// [核心改动] 将介绍文本注入到 DebugUI，而不是自己画
		Debug.setIntros(getIntroduction());
	}
}
