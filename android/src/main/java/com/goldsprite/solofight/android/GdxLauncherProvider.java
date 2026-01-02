package com.goldsprite.solofight.android;

import com.badlogic.gdx.ApplicationListener;
import com.goldsprite.GdxLauncher;

public class GdxLauncherProvider {
	public static Class<? extends ApplicationListener> gdxLauncher =
	GdxLauncher.class;

	public static ApplicationListener launcherGame() {
		try {
			return gdxLauncher.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
