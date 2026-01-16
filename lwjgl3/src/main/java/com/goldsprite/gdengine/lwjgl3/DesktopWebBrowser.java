package com.goldsprite.gdengine.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.goldsprite.gdengine.core.web.IWebBrowser;

public class DesktopWebBrowser implements IWebBrowser {
	@Override
	public void openUrl(String url, String title) {
		// Desktop 端暂时调用系统浏览器
		// 优点：性能好，兼容性好，无需额外巨大的依赖库
		Gdx.net.openURI(url);
	}

	@Override
	public void close() {
		// 系统浏览器无法由程序关闭
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}
}
