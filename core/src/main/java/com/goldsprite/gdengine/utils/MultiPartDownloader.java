package com.goldsprite.gdengine.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.log.Debug;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MultiPartDownloader {

	public static class Manifest {
		public String name;
		public long totalSize;
		public String version;
		public String updatedAt;
		public ArrayList<Part> parts;
	}

	public static class Part {
		public int index;
		public String file; // 现在通常是相对路径 (例如 "docs.xxx.p0.zip")
		public String md5;
		public long size;
	}

	public interface ShaCallback {
		void onSuccess(String sha);
		void onError(String err);
	}

	public interface ProgressCallback {
		void onProgress(int percent, String msg);
	}

	public interface ManifestCallback {
		void onSuccess(Manifest manifest);
		void onError(String err);
	}

	// ==========================================
	// Public API
	// ==========================================

	/**
	 * [Step 1] 获取最新 Commit SHA (官方 API + Header 优化)
	 */
	public static void fetchLatestSha(ShaCallback callback) {
		new Thread(() -> {
			try {
				// 1. 建立连接 (保持使用 git/refs 接口，因为它最快)
				URL url = new URL(com.goldsprite.gdengine.core.config.CloudConstants.API_LATEST_SHA);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(5000); // 5秒超时足够了
				conn.setReadTimeout(5000);
				conn.setRequestMethod("GET");

				// [修改] 移除无效的 Accept 头，它对 refs 接口没用
				conn.setRequestProperty("Cache-Control", "no-cache");

				int status = conn.getResponseCode();
				if (status != 200) throw new IOException("API HTTP " + status);

				// 2. 读取 JSON 字符串
				StringBuilder sb = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
				}
				String jsonResponse = sb.toString();

				// 3. [核心] 简单粗暴提取 SHA
				// JSON 结构是: { ... "object": { "sha": "xxxxxxxx...", ... } ... }
				// 我们直接找 "sha": "..." 这种模式
				// 这种 regex 查找在几百字节的字符串里是纳秒级的，完全不影响性能

				String sha = null;
				// 正则匹配 40位 Hex 字符串
				Pattern pattern = Pattern.compile("\"sha\"\\s*:\\s*\"([a-f0-9]{40})\"");
				Matcher matcher = pattern.matcher(jsonResponse);

				if (matcher.find()) {
					sha = matcher.group(1);
				}

				if (sha == null) throw new IOException("SHA not found in response");

				final String finalSha = sha;
				Debug.logT("Downloader", "Got SHA: " + finalSha);

				Gdx.app.postRunnable(() -> callback.onSuccess(finalSha));

			} catch (Exception e) {
				Gdx.app.postRunnable(() -> callback.onError(e.getMessage()));
			}
		}).start();
	}

	public static void fetchManifest(String url, ManifestCallback callback) {
		new Thread(() -> {
			try {
				// 强制添加时间戳，绕过反代或浏览器缓存
				String noCacheUrl = appendParam(url, "t", "" + System.currentTimeMillis());

				String jsonStr = fetchString(noCacheUrl);
				Json json = new Json();
				json.setIgnoreUnknownFields(true);
				Manifest m = json.fromJson(Manifest.class, jsonStr);

				Gdx.app.postRunnable(() -> callback.onSuccess(m));
			} catch (Exception e) {
				Gdx.app.postRunnable(() -> callback.onError(e.getMessage()));
			}
		}).start();
	}

	/**
	 * 执行完整下载流程
	 * @param manifestUrl 清单地址 (建议走 Proxy)
	 * @param assetBaseUrl 资源基准地址 (建议走 CDN，如果为 null 则默认使用 manifestUrl 的父目录)
	 * @param saveDir 保存目录
	 */
	public static void download(String manifestUrl, String assetBaseUrl, String saveDir, ProgressCallback callback, Runnable onFinish) {
		new Thread(() -> {
			File workDir = new File(saveDir, "download_cache");
			if (!workDir.exists()) workDir.mkdirs();

			try {
				// 1. 获取清单 (强制刷新)
				callback.onProgress(0, "正在获取清单...");
				String noCacheManifestUrl = appendParam(manifestUrl, "t", String.valueOf(System.currentTimeMillis()));
				String jsonStr = fetchString(noCacheManifestUrl);

				Debug.logT("Downloader", "Manifest fetched from: " + noCacheManifestUrl);

				Json json = new Json();
				json.setIgnoreUnknownFields(true);
				Manifest manifest = json.fromJson(Manifest.class, jsonStr);

				// 2. 准备版本号参数 (用于分卷 URL 的双重保险)
				String versionParam = "0";
				try {
					if (manifest.updatedAt != null) {
						versionParam = URLEncoder.encode(manifest.updatedAt, StandardCharsets.UTF_8.name());
					}
				} catch (Exception ignored) {}
				final String finalVersionParam = versionParam;

				// 3. 确定资源基准 URL (修复 final 问题)
				String tempBaseUrl; // 使用临时变量处理逻辑
				if (assetBaseUrl != null && !assetBaseUrl.isEmpty()) {
					tempBaseUrl = assetBaseUrl;
					// 确保以 / 结尾
					if (!tempBaseUrl.endsWith("/")) tempBaseUrl += "/";
				} else {
					// 回退逻辑：使用 manifestUrl 的目录
					tempBaseUrl = manifestUrl.substring(0, manifestUrl.lastIndexOf('/') + 1);
				}
				Debug.logT("Downloader", "Asset Base URL: " + tempBaseUrl);

				// 【关键修复】定义一个不可变的 final 变量供下方的 Lambda 使用
				final String finalBaseUrl = tempBaseUrl;

				// 4. 并发下载
				int totalParts = manifest.parts.size();
				ExecutorService executor = Executors.newFixedThreadPool(4);
				AtomicInteger downloadedCount = new AtomicInteger(0);
				AtomicInteger globalError = new AtomicInteger(0);

				for (Part part : manifest.parts) {
					executor.submit(() -> {
						if (globalError.get() != 0) return;

						// [核心逻辑] 拼接 URL
						String partUrl;
						if (part.file.startsWith("http")) {
							partUrl = part.file; // 绝对路径直接用
						} else {
							partUrl = finalBaseUrl + part.file; // 相对路径拼接 Base
						}

						// 附加版本参数
						partUrl = appendParam(partUrl, "v", finalVersionParam);

						File partFile = new File(workDir, part.file);

						try {
							// 简单校验：如果文件存在且大小一致，跳过
							if (partFile.exists() && partFile.length() == part.size) {
								// Skip
							} else {
								Debug.logT("Downloader", "Downloading part: " + partUrl);
								downloadFile(partUrl, partFile);
							}

							int current = downloadedCount.incrementAndGet();
							int percent = (int)((float)current / totalParts * 50);
							callback.onProgress(percent, "下载分卷: " + current + "/" + totalParts);

						} catch (Exception e) {
							e.printStackTrace();
							globalError.set(1);
							callback.onProgress(-1, "分卷下载失败: " + part.file);
						}
					});
				}

				executor.shutdown();
				if (!executor.awaitTermination(10, TimeUnit.MINUTES) || globalError.get() != 0) {
					throw new IOException("下载过程中断");
				}

				// 5. 合并
				callback.onProgress(50, "正在合并分卷...");
				File finalZip = new File(workDir, manifest.name);
				mergeParts(workDir, manifest.parts, finalZip);

				// 6. 解压
				callback.onProgress(75, "正在解压资源...");
				unzip(finalZip, new File(saveDir));

				// 7. 清理
				deleteDir(workDir);

				callback.onProgress(100, "完成");
				if (onFinish != null) onFinish.run();

			} catch (Exception e) {
				e.printStackTrace();
				callback.onProgress(-1, "错误: " + e.getMessage());
			}
		}).start();
	}

	// ==========================================
	// Internal Helpers (保持不变)
	// ==========================================

	private static String appendParam(String url, String key, String val) {
		String separator = url.contains("?") ? "&" : "?";
		return url + separator + key + "=" + val;
	}

	private static String fetchString(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(15000);
		conn.setReadTimeout(15000);
		conn.setRequestMethod("GET");

		int status = conn.getResponseCode();
		if (status != 200) throw new IOException("HTTP " + status);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) sb.append(line);
			return sb.toString();
		}
	}

	private static void downloadFile(String urlStr, File target) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(15000);
		conn.setReadTimeout(15000);

		try (InputStream in = conn.getInputStream();
			 FileOutputStream out = new FileOutputStream(target)) {
			byte[] buf = new byte[4096];
			int n;
			while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
		}
	}

	private static void mergeParts(File workDir, List<Part> parts, File dest) throws IOException {
		parts.sort((a, b) -> Integer.compare(a.index, b.index));
		try (FileOutputStream fos = new FileOutputStream(dest);
			 BufferedOutputStream out = new BufferedOutputStream(fos)) {
			byte[] buf = new byte[8192];
			for (Part part : parts) {
				File partFile = new File(workDir, part.file);
				if (!partFile.exists()) throw new IOException("Missing part: " + part.file);
				try (FileInputStream fis = new FileInputStream(partFile)) {
					int len;
					while ((len = fis.read(buf)) > 0) out.write(buf, 0, len);
				}
				partFile.delete();
			}
		}
	}

	private static void unzip(File zipFile, File targetDir) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			byte[] buffer = new byte[4096];
			while ((entry = zis.getNextEntry()) != null) {
				File newFile = new File(targetDir, entry.getName());
				if (entry.isDirectory()) {
					newFile.mkdirs();
				} else {
					new File(newFile.getParent()).mkdirs();
					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						int len;
						while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
					}
				}
			}
		}
	}

	private static void deleteDir(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) for (File f : files) deleteDir(f);
		}
		file.delete();
	}
}
