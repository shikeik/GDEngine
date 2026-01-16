package com.goldsprite.gdengine.core.web;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.goldsprite.gdengine.log.Debug;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;

/**
 * 本地文档服务器 (Micro HTTP Server)
 * 职责：将 Gdx.files 本地文件映射为 HTTP 流，供 WebView 访问。
 */
public class DocServer extends NanoHTTPD {

	private static final int PORT = 8899;
	private static DocServer instance;

	// 文档在文件系统中的相对根路径
	// 对应 Gdx.files.local("docs/engine_docs") 或 internal("docs/engine_docs")
	private static final String DOC_ROOT = "docs/engine_docs";

	private DocServer() {
		super(PORT);
	}

	public static void startServer() {
		if (instance == null) {
			instance = new DocServer();
			try {
				instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
				Debug.logT("DocServer", "✅ 服务启动: " + getIndexUrl());
			} catch (IOException e) {
				Debug.logErrT("DocServer", "启动失败: " + e.getMessage());
			}
		}
	}

	public static void stopServer() {
		if (instance != null) {
			instance.stop();
			instance = null;
			Debug.logT("DocServer", "服务已停止");
		}
	}

	public static String getIndexUrl() {
		return "http://localhost:" + PORT + "/index.html";
	}

	@Override
	public Response serve(IHTTPSession session) {
		String uri = session.getUri();

		// 1. 默认页处理
		if (uri.equals("/") || uri.isEmpty()) {
			uri = "/index.html";
		}

		// 2. 路径清洗
		if (uri.startsWith("/")) uri = uri.substring(1);
		// 防止路径遍历攻击 (简单的防御)
		if (uri.contains("..")) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden");

		String path = DOC_ROOT + "/" + uri;

		// 3. 资源查找策略: 优先 Local (下载的), 其次 Internal (自带的)
		FileHandle file = Gdx.files.local(path);

		if (!file.exists()) {
			file = Gdx.files.internal(path);
		}

		// 4. 响应文件的内容
		if (file.exists() && !file.isDirectory()) {
			String mime = getMimeTypeForFile(uri);
			try {
				return newChunkedResponse(Response.Status.OK, mime, file.read());
			} catch (Exception e) {
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "File Read Error");
			}
		} else {
			return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found: " + uri);
		}
	}

	public static String getMimeTypeForFile(String uri) {
		if (uri.endsWith(".html")) return MIME_HTML;
		if (uri.endsWith(".css")) return "text/css";
		if (uri.endsWith(".js")) return "application/javascript";
		if (uri.endsWith(".json")) return "application/json";
		if (uri.endsWith(".png")) return "image/png";
		if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
		if (uri.endsWith(".md")) return "text/markdown"; // Docsify 核心文件
		return MIME_PLAINTEXT;
	}
}
