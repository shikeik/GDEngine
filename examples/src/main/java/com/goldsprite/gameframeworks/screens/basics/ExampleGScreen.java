package com.goldsprite.gameframeworks.screens.basics;

import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.solofight.core.Debug;
import com.badlogic.gdx.Gdx;

public abstract class ExampleGScreen extends GScreen {
	// 1. 定义基准尺寸 (540p)
	protected static final float VIEWPORT_SCALE = 1.2f; // 保持原本的缩放系数
	protected static final float BASE_SHORT = 540f;
	protected static final float BASE_LONG = 960f;

	public String getIntroduction() { return ""; }

	// 2. 强制子类指定方向
	public abstract ScreenManager.Orientation getOrientation();

	// 3. 智能初始化视口 (接管 GScreen 的 initViewport)
	@Override
	protected void initViewport() {
		float w, h, sw, sh;
		int screenW, screenH;
		screenW = Gdx.graphics.getWidth();
		screenH = Gdx.graphics.getHeight();
		int sLong = Math.max(screenW, screenH);
		int sShort = Math.min(screenW, screenH);
		if (getOrientation() == ScreenManager.Orientation.Landscape) {
			w = BASE_LONG;
			h = BASE_SHORT;
			screenW = sLong;
			screenH = sShort;
		} else {
			w = BASE_SHORT;
			h = BASE_LONG;
			screenW = sShort;
			screenH = sLong;
		}

		// 自动应用缩放系数
		uiViewport = new ExtendViewport(w * VIEWPORT_SCALE, h * VIEWPORT_SCALE);
		//Debug.log("1ui视口宽高: %s", getViewSize());
		
		uiViewport.update(screenW, screenH, true);
		//Debug.log("2ui视口宽高: %s", getViewSize());int k5;
		
	}

	// 4. 自动处理转屏逻辑
	@Override
	public void show() {
		super.show(); // GScreen.show 处理输入和 resize
		getScreenManager().setOrientation(getOrientation());

		// [核心改动] 将介绍文本注入到 DebugUI，而不是自己画
		Debug.setIntros(getIntroduction());
	}
}
