package com.goldsprite.gdengine.utils;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.BuildConfig;

public class ThreadedDownload {

    // 进度回调接口
    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int percentage, String message);
    }

    // 下载任务类
    public static class DownloadTask implements Runnable {
        private final String downloadUrl;
        private final String saveDirectory;
        private final ProgressListener listener;
        private final Runnable onFinish;   // 新增：完成后回调

        public DownloadTask(String downloadUrl,
                            String saveDirectory,
                            ProgressListener listener,
                            Runnable onFinish) {
            this.downloadUrl = downloadUrl;
            this.saveDirectory = saveDirectory;
            this.listener = listener;
            this.onFinish = onFinish;
        }

        @Override
        public void run() {
            Path tempZipFile = null;
            try {
                // 1. 获取文件大小
                long fileSize = getFileSize(downloadUrl);
                if (fileSize <= 0) {
                    notifyError("无法获取文件大小");
                    return;
                }

                // 2. 准备保存路径
                tempZipFile = Paths.get(saveDirectory, "download_temp.zip");
                Files.createDirectories(tempZipFile.getParent());

                // 3. 执行下载
                long downloaded = 0;
                URL url = new URL(downloadUrl);
                try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
				FileOutputStream fos = new FileOutputStream(tempZipFile.toFile())) {

                    ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
                    while (rbc.read(buffer) != -1) {
                        buffer.flip();
                        if (buffer.hasRemaining()) {
                            fos.getChannel().write(buffer);
                        }
                        downloaded += buffer.position();
                        buffer.clear();

                        int progress = (int) ((downloaded * 100) / fileSize);
                        notifyProgress(progress, "下载中: " + progress + "%");
                    }
                }

                // 4. 下载完成，开始解压
                notifyProgress(100, "下载完成，正在解压...");
                extractZip(tempZipFile.toFile(), new File(saveDirectory));

                // 5. 清理临时文件
                Files.deleteIfExists(tempZipFile);
                notifyProgress(100, "任务完成！");

                // ===== 触发回调 =====
                //Debug.logT("ZipDownLoader", "触发回调");
                if (onFinish != null) {
                    onFinish.run();
                }

            } catch (Exception e) {
                try {
                    if (tempZipFile != null) Files.deleteIfExists(tempZipFile);
                } catch (IOException ignored) {}
                notifyError("错误: " + e.getMessage());
            }
        }

        private long getFileSize(String urlString) throws IOException {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            return conn.getContentLengthLong();
        }

        private void extractZip(File zipFile, File outputFolder) throws IOException {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    int firstSlashIndex = entryName.indexOf('/');
                    if (firstSlashIndex != -1) {
                        entryName = entryName.substring(firstSlashIndex + 1);
                    }
                    if (entryName.isEmpty()) continue;

                    File entryFile = new File(outputFolder, entryName);
                    if (entry.isDirectory()) {
                        entryFile.mkdirs();
                        continue;
                    }

                    File parent = entryFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    zis.closeEntry();
                }
            }
        }

        private void notifyProgress(int percentage, String msg) {
            if (listener != null) {
                listener.onProgress(percentage, msg);
            }
        }

        private void notifyError(String err) {
            if (listener != null) {
                listener.onProgress(-1, err);
            }
        }
    }

    // 对外入口：支持传入“完成后回调”
    public static void download(Runnable onFinish) {
        String DOWNLOAD_URL = "https://github.com/shikeik/GDEngine/releases/download/"+BuildConfig.DEV_VERSION+"/engine_docs.zip";
        String SAVE_PATH = "/storage/emulated/0/AppProjects/new_dev/GDEngine/GDEngine/engine_docs/";

        ProgressListener listener = (percentage, message) -> {
            if (percentage == -1) {
                System.err.println("\n" + message);int k;
            } else {
                Debug.logT("ZipDownLoader ", String.format("\r[%-50s] %d%% %s\n",
                           "=".repeat(Math.max(0, percentage / 2)) + ">",
                           percentage, message));
            }
        };

        Thread thread = new Thread(new DownloadTask(DOWNLOAD_URL, SAVE_PATH, listener, onFinish));
        thread.start();
        Debug.logT("ZipDownLoader", "下载任务已启动...");
    }

    // 主方法：示例调用
    public static void main(String[] args) {
        download(() -> System.out.println("=== 下载+解压全部完成，回调触发 ==="));
    }
}

