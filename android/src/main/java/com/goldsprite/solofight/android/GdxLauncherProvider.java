package com.goldsprite.solofight.android;

import com.badlogic.gdx.ApplicationListener;

public class GdxLauncherProvider {
	public static Class<? extends ApplicationListener> gdxLauncher = 
	com.goldsprite.solofight.GdxLauncher.class;

	public static ApplicationListener launcherGame() {
		try {
			return gdxLauncher.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
