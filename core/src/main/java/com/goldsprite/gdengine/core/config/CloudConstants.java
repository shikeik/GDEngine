package com.goldsprite.gdengine.core.config;

/**
 * 云端资源配置中心
 * 策略：SHA-Anchor (API锚点 + 混合分发)
 */
public class CloudConstants {

	public static final String REPO_OWNER = "shikeik";
	public static final String REPO_NAME = "GDEngine";
	public static final String BRANCH = "main";

	// 1. 锚点接口 (官方 API)
	// 用于获取最新 Commit SHA，Header: Accept: application/vnd.github.sha
	public static final String API_LATEST_SHA = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/git/refs/heads/" + BRANCH;

	// 2. 清单通道 (gh-proxy 反代)
	// 优势：响应快，无冷启动。配合 SHA 路径物理穿透缓存。
	// 模板: https://gh-proxy.com/https://raw.githubusercontent.com/{user}/{repo}/{sha}/{path}
	private static final String MANIFEST_PROXY_TEMPLATE = "https://gh-proxy.com/https://raw.githubusercontent.com/%s/%s/%s/%s";

	// 3. 资源通道 (gcore CDN)
	// 优势：带宽大，全球加速。配合 SHA 版本号物理穿透缓存。
	// 模板: https://gcore.jsdelivr.net/gh/{user}/{repo}@{sha}/{path}
	private static final String ASSET_CDN_TEMPLATE = "https://gcore.jsdelivr.net/gh/%s/%s@%s/%s";

	// --- 业务路径 ---
	public static final String PATH_DOCS_DIST = "dist/";
	public static final String PATH_TEMPLATES_DIST = "dist/templates/";

	/**
	 * 获取清单 URL (走反代)
	 * @param sha Commit SHA
	 * @param relativePath 相对路径 (如 "dist/docs_manifest.json")
	 */
	public static String getManifestUrl(String sha, String relativePath) {
		return String.format(MANIFEST_PROXY_TEMPLATE, REPO_OWNER, REPO_NAME, sha, relativePath);
	}

	/**
	 * 获取资源基准 URL (走 CDN)
	 * @param sha Commit SHA
	 * @param relativePath 相对路径 (如 "dist/")
	 */
	public static String getAssetCdnBaseUrl(String sha, String relativePath) {
		return String.format(ASSET_CDN_TEMPLATE, REPO_OWNER, REPO_NAME, sha, relativePath);
	}
}
