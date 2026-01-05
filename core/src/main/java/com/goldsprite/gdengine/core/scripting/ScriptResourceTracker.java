package com.goldsprite.gdengine.core.scripting;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本资源追踪器
 * 职责：
 * 1. 代理资源加载 (自动去 projectAssetsRoot 找)
 * 2. 记录所有加载的资源 (Disposable)
 * 3. 提供一键清理功能 (在 GameRunner 退出时调用)
 */
public class ScriptResourceTracker {

	// 暂存所有由脚本请求加载的资源
	private static final List<Disposable> trackedResources = new ArrayList<>();

	/**
	 * 安全加载纹理
	 * @param path 相对于项目 assets 目录的路径 (如 "player.png")
	 * @return 加载好的 Texture，如果失败返回 null
	 */
	public static Texture loadTexture(String path) {
		FileHandle file = GameWorld.getAsset(path);
		if (!file.exists()) {
			Debug.logT("Asset", "❌ 找不到资源: %s (在 %s)", path, file.path());
			return null;
		}

		try {
			Texture tex = new Texture(file);
			trackedResources.add(tex); // 记账
			return tex;
		} catch (Exception e) {
			Debug.logT("Asset", "加载失败: %s -> %s", path, e.getMessage());
			return null;
		}
	}

	/**
	 * 安全加载并包装为 Region
	 */
	public static TextureRegion loadRegion(String path) {
		Texture tex = loadTexture(path);
		return tex != null ? new TextureRegion(tex) : null;
	}

	/**
	 * 销毁所有追踪的资源 (重置游戏或退出时调用)
	 */
	public static void disposeAll() {
		if (trackedResources.isEmpty()) return;

		Debug.logT("Asset", "正在清理脚本资源: %d 个...", trackedResources.size());
		for (Disposable d : trackedResources) {
			try { d.dispose(); } catch (Exception ignored) {}
		}
		trackedResources.clear();
	}
}
