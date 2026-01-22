package com.goldsprite.gdengine.core.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * 通用静态文件服务器
 * <p>
 * 可在引擎内调用，也可通过 main() 单独启动。
 * 纯 Java IO 实现，不依赖 LibGDX 上下文。
 * </p>
 */
public class DocServer extends NanoHTTPD {

	private static final int DEFAULT_PORT = 8899;
	private static DocServer instance;
	
	private final File rootDir;

	public DocServer(int port, File rootDir) {
		super(port);
		this.rootDir = rootDir;
	}

	/**
	 * [Engine API] 引擎内启动 (单例模式)
	 * @param rootPath 文档根目录的绝对路径
	 */
	public static void startServer(String rootPath) {
		stopServer(); // 先尝试停止旧的
		try {
			File root = new File(rootPath);
			if (!root.exists()) {
				System.err.println("❌ [DocServer] Root path does not exist: " + rootPath);
				return;
			}
			
			instance = new DocServer(DEFAULT_PORT, root);
			instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			System.out.println("✅ [DocServer] Started at http://localhost:" + DEFAULT_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * [Engine API] 停止服务
	 */
	public static void stopServer() {
		if (instance != null) {
			instance.stop();
			instance = null;
			System.out.println("🛑 [DocServer] Stopped.");
		}
	}
	
	public static String getIndexUrl() {
		return "http://localhost:" + DEFAULT_PORT + "/index.html";
	}

	/**
	 * [CLI API] 命令行独立运行入口
	 * args[0]: (可选) 根目录路径，默认为 "./docs/engine_docs"
	 */
	public static void main(String[] args) {
		String path = args.length > 0 ? args[0] : "docs/engine_docs";
		File root = new File(path);
		
		// 尝试修正路径 (如果在项目根目录运行)
		if (!root.exists()) {
			// 尝试找上一级 (如果在 core/ 运行)
			File upOne = new File("../docs/engine_docs");
			if (upOne.exists()) root = upOne;
		}

		System.out.println(">>> Starting DocServer Standalone...");
		System.out.println(">>> Root: " + root.getAbsolutePath());

		try {
			DocServer server = new DocServer(DEFAULT_PORT, root);
			server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			
			String url = "http://localhost:" + DEFAULT_PORT;
			System.out.println("\n✅ Server is running at: " + url);
			System.out.println("Press Enter to stop...\n");

			// 尝试自动打开浏览器
			try {
				//java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
			} catch (Exception ignored) {}

			System.in.read(); // 阻塞等待回车
			server.stop();
			System.out.println("Server stopped.");
			
		} catch (Exception e) {
			System.err.println("Startup failed: " + e.getMessage());
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		String uri = session.getUri();
		
		// 1. 安全检查
		if (uri.contains("..")) {
			return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden");
		}

		// 2. 默认页处理
		if (uri.equals("/") || uri.isEmpty()) {
			uri = "/index.html";
		}

		// 3. 文件定位
		File file = new File(rootDir, uri);

		// 如果是目录，尝试找 index.html
		if (file.exists() && file.isDirectory()) {
			file = new File(file, "index.html");
		}

		if (!file.exists()) {
			return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found: " + uri);
		}

		// 4. 返回文件流
		try {
			String mime = getMimeTypeForFile(uri);
			FileInputStream fis = new FileInputStream(file);
			return newChunkedResponse(Response.Status.OK, mime, fis);
		} catch (IOException e) {
			return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error: " + e.getMessage());
		}
	}

	public static String getMimeTypeForFile(String uri) {
		uri = uri.toLowerCase();
		if (uri.endsWith(".html") || uri.endsWith(".htm")) return "text/html";
		if (uri.endsWith(".css")) return "text/css";
		if (uri.endsWith(".js")) return "application/javascript";
		if (uri.endsWith(".json")) return "application/json";
		if (uri.endsWith(".png")) return "image/png";
		if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
		if (uri.endsWith(".gif")) return "image/gif";
		if (uri.endsWith(".md")) return "text/markdown";
		if (uri.endsWith(".xml")) return "text/xml";
		if (uri.endsWith(".mp3") || uri.endsWith(".wav")) return "audio/mpeg";
		return "application/octet-stream";
	}
}
