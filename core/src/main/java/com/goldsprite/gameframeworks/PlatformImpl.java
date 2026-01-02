package com.goldsprite.gameframeworks;

import com.badlogic.gdx.Gdx;
import com.goldsprite.gameframeworks.screens.ScreenManager;

import java.util.function.Consumer;

public class PlatformImpl {
	public static Consumer<Boolean> showSoftInputKeyBoard;
	public static Consumer<Boolean> fullScreenEvent;

	public static ScreenManager.Orientation defaultOrientaion = ScreenManager.Orientation.Portrait;

	public static int getTouchCount() {
		int count = 0;
		// 检查前10个指针（通常足够）
		for (int pointer = 0; pointer < 10; pointer++) {
			if (Gdx.input.isTouched(pointer)) {
				count++;
			}
		}
		return count;
	}
}
