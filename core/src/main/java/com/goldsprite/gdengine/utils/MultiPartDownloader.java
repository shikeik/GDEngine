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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 分卷下载器 (封装版)
 * 自动处理 CDN 缓存穿透与分卷合并。
 */
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
        public String file;
        public String md5;
        public long size;
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
     * 仅获取清单 (自动绕过缓存)
     */
    public static void fetchManifest(String url, ManifestCallback callback) {
        new Thread(() -> {
            try {
                // [封装] 自动添加随机时间戳，强制获取最新清单
                String noCacheUrl = appendParam(url, "t", String.valueOf(System.currentTimeMillis()));

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
     * 执行完整下载流程 (自动绕过缓存 + 自动版本对齐)
     */
    public static void download(String manifestUrl, String saveDir, ProgressCallback callback, Runnable onFinish) {
        new Thread(() -> {
            File workDir = new File(saveDir, "download_cache");
            if (!workDir.exists()) workDir.mkdirs();

            try {
                // 1. 获取清单 (强制刷新)
                callback.onProgress(0, "正在获取清单...");
                String noCacheManifestUrl = appendParam(manifestUrl, "t", String.valueOf(System.currentTimeMillis()));
                String jsonStr = fetchString(noCacheManifestUrl);

                Debug.logT("Downloader", "Manifest fetched: " + jsonStr.substring(0, Math.min(50, jsonStr.length())) + "...");

                Json json = new Json();
                json.setIgnoreUnknownFields(true);
                Manifest manifest = json.fromJson(Manifest.class, jsonStr);

                // 2. 准备分卷 URL 参数 (使用清单里的 updatedAt 作为版本号)
                // 这样确保了下载的分卷版本与清单版本严格一致
                String versionParam = "0";
                try {
                    if (manifest.updatedAt != null) {
                        versionParam = URLEncoder.encode(manifest.updatedAt, StandardCharsets.UTF_8.name());
                    }
                } catch (Exception ignored) {}
                final String finalVersionParam = versionParam;

                // 3. 并发下载
                int totalParts = manifest.parts.size();
                ExecutorService executor = Executors.newFixedThreadPool(4);
                AtomicInteger downloadedCount = new AtomicInteger(0);
                AtomicInteger globalError = new AtomicInteger(0);

                String baseUrl = manifestUrl.substring(0, manifestUrl.lastIndexOf('/') + 1);

                for (Part part : manifest.parts) {
                    executor.submit(() -> {
                        if (globalError.get() != 0) return;

                        // [封装] 自动拼接版本参数
                        String partUrl = baseUrl + part.file;
                        partUrl = appendParam(partUrl, "v", finalVersionParam);

                        File partFile = new File(workDir, part.file);

                        try {
                            // 简单断点续传: 如果大小一致且不用强制刷新，则跳过
                            // 此时 partUrl 已经带了版本号，如果是新版本，URL 变了，理论上 CDN 会回源
                            // 但本地文件可能重名。为了保险，我们检查大小。
                            // 更保险的是检查 MD5，但比较耗时。这里信赖大小+版本号URL。
                            if (partFile.exists() && partFile.length() == part.size) {
                                // Skip
                            } else {
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

                // 4. 合并
                callback.onProgress(50, "正在合并分卷...");
                File finalZip = new File(workDir, manifest.name);
                mergeParts(workDir, manifest.parts, finalZip);

                // 5. 解压
                callback.onProgress(75, "正在解压资源...");
                unzip(finalZip, new File(saveDir));

                // 6. 清理
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
    // Internal Helpers
    // ==========================================

    /** URL 参数拼接工具 */
    private static String appendParam(String url, String key, String val) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + "=" + val;
    }

    private static String fetchString(String urlStr) throws IOException {
        Debug.logT("Downloader", "GET: " + urlStr);
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
        Collections.sort(parts, (a, b) -> Integer.compare(a.index, b.index));
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
