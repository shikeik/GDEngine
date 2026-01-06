package com.goldsprite.gdengine.core.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.log.Debug;

import java.util.HashSet;
import java.util.Set;

public class AssetUtils {
	private static final Set<String> fileIndex = new HashSet<>();
	private static boolean indexLoaded = false;

	/** 加载 assets.txt 索引 */
	public static void loadIndex() {
		if (indexLoaded) return;

		FileHandle indexFile = Gdx.files.internal("assets.txt");
		if (indexFile.exists()) {
			String content = indexFile.readString("UTF-8");
			String[] lines = content.split("\\r?\\n");
			for (String line : lines) {
				if (!line.trim().isEmpty()) {
					// 统一路径分隔符为 /
					fileIndex.add(line.trim().replace("\\", "/"));
				}
			}
			Debug.logT("AssetUtils", "Assets Index Loaded: " + fileIndex.size() + " entries.");
		}
		indexLoaded = true;
	}

	/**
	 * 从索引中查找子文件名称列表
	 * @param basePath 相对路径 (如 "libs")
	 * @return 文件名数组 (如 ["gdengine.jar", ...])
	 */
	public static String[] listNames(String basePath) {
		loadIndex();

		String searchPath = basePath.replace("\\", "/");
		if (searchPath.endsWith("/")) searchPath = searchPath.substring(0, searchPath.length() - 1);

		Array<String> results = new Array<>();
		String prefix = searchPath.isEmpty() ? "" : searchPath + "/";

		for (String path : fileIndex) {
			if (path.startsWith(prefix)) {
				// 获取去掉前缀后的剩余部分
				String suffix = path.substring(prefix.length());
				// 如果剩余部分不包含 /，说明是直接子文件
				if (!suffix.isEmpty() && !suffix.contains("/")) {
					results.add(suffix);
				}
			}
		}

		return results.toArray(String.class);
	}
}
