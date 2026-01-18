package com.goldsprite.gdengine.core.web;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
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
	// [修改] 相对路径，不包含 GDEngine 前缀，因为我们会动态拼
	private static final String RELATIVE_PATH = "engine_docs";
	private boolean enableLocal = false;

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

	/**
	 * 路由处理
	 * 定义本地文档路径位置以及如何映射
	 * @param session The HTTP session
	 * @return
	 */
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

		// [修改] 详细调试日志
		Debug.logT("DocServer", "Request: " + uri);

		// 1. 优先检查配置好的引擎根
		// GDEngineConfig.activeEngineRoot 在进入GDEngineHub时必须初始化设置, 然后打开日志, 此时应当已存在
		// 这里更新为未激活则回退到默认引擎根
		FileHandle gdConfigFile = Gdx.files.absolute(getEngineDocPath() + "/" + uri);

		// 3. 最后检查local (这里是为了兼容idea运行时直接对文档源位置访问)
		FileHandle localFile = Gdx.files.local("docs/" + RELATIVE_PATH + "/" + uri);
		boolean enableLocal = this.enableLocal; // 这里用于调试生产环境时必须获取到gdConfigFile路径故取消local保底

		FileHandle target = null;
		if (gdConfigFile.exists() && !gdConfigFile.isDirectory()) {
			target = gdConfigFile;
			Debug.logT("DocServer", "Found in gdConfigFile: " + target.file().getAbsolutePath());
		}
		else if (enableLocal && localFile.exists() && !localFile.isDirectory()) {
			target = localFile;
			Debug.logT("DocServer", "Found in localFile: " + target.file().getAbsolutePath());
		}

		if (target != null) {
			String mime = getMimeTypeForFile(uri);
			try {
				return newChunkedResponse(Response.Status.OK, mime, target.read());
			} catch (Exception e) {
				Debug.logErrT("DocServer", "Read Error: " + e.getMessage());
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Read Error");
			}
		} else {
			Debug.logErrT("DocServer",
				"404 Not Found. Checked:"
					+ "\n  gdConfigFile: " + gdConfigFile.file().getAbsolutePath()
					+ "\n  localFile: " + localFile.file().getAbsolutePath());
			return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found");
		}
	}

	public static String getEngineDocPath() {
		String activeEngineRoot = GDEngineConfig.getInstance().getActiveEngineRoot();
		if (activeEngineRoot == null || activeEngineRoot.isEmpty()) {
			activeEngineRoot = GDEngineConfig.getRecommendedRoot();
		}

		return activeEngineRoot + "/" + RELATIVE_PATH + "/";
	}

	public static String getMimeTypeForFile(String uri) {
		if (uri.endsWith(".html")) return MIME_HTML;
		if (uri.endsWith(".css")) return "text/css";
		if (uri.endsWith(".js")) return "application/javascript; charset=UTF-8"; // 关键！
		if (uri.endsWith(".json")) return "application/json; charset=UTF-8";     // 关键！
		if (uri.endsWith(".png")) return "image/png";
		if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
		if (uri.endsWith(".md")) return "text/markdown"; // Docsify 核心文件
		return MIME_PLAINTEXT;
	}
}
