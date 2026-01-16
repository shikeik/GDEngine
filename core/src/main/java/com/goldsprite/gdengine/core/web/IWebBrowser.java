package com.goldsprite.gdengine.core.web;

/**
 * 跨平台 Web 浏览器接口
 * <p>
 * 职责：统一不同平台的网页浏览行为。
 * </p>
 */
public interface IWebBrowser {
	/**
	 * 打开指定 URL
	 * @param url 目标地址 (http://... 或 http://localhost:port/...)
	 * @param title 窗口标题 (仅 Android/内嵌模式有效)
	 */
	void openUrl(String url, String title);

	/**
	 * 关闭浏览器 (仅内嵌模式有效)
	 */
	void close();

	/**
	 * 是否支持内嵌浏览
	 * @return true=内嵌(Android), false=外部跳出(Desktop)
	 */
	boolean isEmbedded();
}
