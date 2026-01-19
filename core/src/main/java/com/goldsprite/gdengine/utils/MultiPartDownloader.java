package com.goldsprite.gdengine.utils;

import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.log.Debug;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 分卷下载器 (The "Evil Cult" Downloader)
 * 专为绕过 GitHub/CDN 单文件大小限制设计。
 */
public class MultiPartDownloader {

    // 数据模型：对应 manifest.json
    public static class Manifest {
        public String name;       // 最终文件名
        public long totalSize;    // 总大小
        public String version;    // 版本校验
        public String updatedAt;  // [新增] 补上这个缺失的字段！
        public ArrayList<Part> parts;
    }

    public static class Part {
        public int index;         // 顺序索引 (0, 1, 2...)
        public String file;       // 分卷文件名 (e.g., "docs.zip.001")
        public String md5;        // 校验码 (可选)
        public long size;         // 分卷大小
    }
	
	public interface ManifestCallback {
        void onSuccess(Manifest manifest);
        void onError(String err);
    }

    public interface ProgressCallback {
        void onProgress(int percent, String msg);
    }
	
	/**
     * 仅获取云端清单信息 (用于版本检查)
     */
    public static void fetchManifest(String url, ManifestCallback callback) {
        new Thread(() -> {
            try {
                String jsonStr = fetchString(url);
                Json json = new Json();
                json.setIgnoreUnknownFields(true);
                Manifest m = json.fromJson(Manifest.class, jsonStr);

                // 切回主线程
                com.badlogic.gdx.Gdx.app.postRunnable(() -> callback.onSuccess(m));
            } catch (Exception e) {
                com.badlogic.gdx.Gdx.app.postRunnable(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    /**
     * 启动下载任务
     * @param manifestUrl 远端 manifest.json 的直链
     * @param saveDir 本地保存/解压目录
     */
    public static void download(String manifestUrl, String saveDir, ProgressCallback callback, Runnable onFinish) {
        new Thread(() -> {
            File workDir = new File(saveDir, "download_cache");
            if (!workDir.exists()) workDir.mkdirs();

            try {
                // 1. 获取清单
                callback.onProgress(0, "正在获取清单...");
                String jsonStr = fetchString(manifestUrl);
                Debug.logT("Downloader", "Manifest: " + jsonStr);
                
                // [修复] 开启容错模式，防止未来加字段导致崩溃
                Json json = new Json();
                json.setIgnoreUnknownFields(true); 
                Manifest manifest = json.fromJson(Manifest.class, jsonStr);
				
                // 2. 并发下载分卷
                int totalParts = manifest.parts.size();
                ExecutorService executor = Executors.newFixedThreadPool(4); // 4线程并发
                AtomicInteger downloadedCount = new AtomicInteger(0);
                AtomicInteger globalError = new AtomicInteger(0);

                // 提取 Base URL (移除 manifest.json，保留目录路径)
                String baseUrl = manifestUrl.substring(0, manifestUrl.lastIndexOf('/') + 1);

                for (Part part : manifest.parts) {
                    executor.submit(() -> {
                        if (globalError.get() != 0) return; // 熔断

                        String partUrl = baseUrl + part.file;
                        File partFile = new File(workDir, part.file);

                        try {
                            // 断点续传检查 (简单版：只看大小)
                            if (partFile.exists() && partFile.length() == part.size) {
                                Debug.logT("Downloader", "Skip existing part: " + part.index);
                            } else {
                                Debug.logT("Downloader", "Downloading part: " + part.index);
                                downloadFile(partUrl, partFile);
                            }
                            
                            int current = downloadedCount.incrementAndGet();
                            int percent = (int)((float)current / totalParts * 50); // 下载占 50% 进度
                            callback.onProgress(percent, "下载分卷: " + current + "/" + totalParts);

                        } catch (Exception e) {
                            e.printStackTrace();
                            globalError.set(1);
                            callback.onProgress(-1, "分卷下载失败: " + part.file);
                        }
                    });
                }

                executor.shutdown();
                // 等待所有任务完成 (最长 10 分钟)
                if (!executor.awaitTermination(10, TimeUnit.MINUTES) || globalError.get() != 0) {
                    throw new IOException("下载超时或出错");
                }

                // 3. 合并分卷
                callback.onProgress(50, "正在合并分卷...");
                File finalZip = new File(workDir, manifest.name);
                mergeParts(workDir, manifest.parts, finalZip);

                // 4. 解压
                callback.onProgress(75, "正在解压资源...");
                unzip(finalZip, new File(saveDir));
				
				// 5. 清理 (修复: 删除临时下载目录)
				try {
					deleteDir(workDir); // 需要一个辅助方法，或者简单的递归删除
				} catch (Exception e) {
					Debug.logT("Downloader", "Cache cleanup failed: " + e.getMessage());
				}

				callback.onProgress(100, "完成");
                if (onFinish != null) onFinish.run();

            } catch (Exception e) {
                e.printStackTrace();
                callback.onProgress(-1, "错误: " + e.getMessage());
            }
        }).start();
    }

    // --- Helpers ---

    private static String fetchString(String urlStr) throws IOException {
        Debug.logT("Downloader", "Fetching Manifest: " + urlStr); // [新增] 打印 URL 方便调试
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // [修改] 增加超时时间到 15秒 (原5秒)
        conn.setConnectTimeout(15000); 
        conn.setReadTimeout(15000); // [新增] 读取也设超时

        conn.setRequestMethod("GET");

        // [新增] 检查 HTTP 状态码
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new IOException("Server returned " + status);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static void downloadFile(String urlStr, File target) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // 强转一下设置超时
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        try (InputStream in = conn.getInputStream(); // 使用 conn.getInputStream() 而不是 url.openStream()
		FileOutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }

    private static void mergeParts(File workDir, List<Part> parts, File dest) throws IOException {
        // 确保按 index 排序
        Collections.sort(parts, (a, b) -> Integer.compare(a.index, b.index));

        try (FileOutputStream fos = new FileOutputStream(dest);
             BufferedOutputStream out = new BufferedOutputStream(fos)) {
            
            byte[] buf = new byte[8192];
            for (Part part : parts) {
                File partFile = new File(workDir, part.file);
                if (!partFile.exists()) throw new IOException("Missing part: " + part.file);

                try (FileInputStream fis = new FileInputStream(partFile)) {
                    int len;
                    while ((len = fis.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                // 合并完一个删一个，节省空间 (可选)
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
            if (files != null) {
                for (File f : files) deleteDir(f);
            }
        }
        file.delete();
    }
}
