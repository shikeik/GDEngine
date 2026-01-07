package tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

/**
 * 一个极简的 HTTP 静态文件服务器。
 * 用于解决 Docsify 在 file:// 协议下无法加载 Markdown 的 CORS 问题。
 * 无需任何第三方依赖，仅依赖 JDK。
 */
public class SimpleDocServer {

    private static final int PORT = 8899;
    private static final String DOC_ROOT = "./docs/manual";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler(DOC_ROOT));
        server.setExecutor(null); // creates a default executor
        server.start();

        System.out.println("=================================================");
        System.out.println(" GDEngine Documentation Server Started");
        System.out.println(" URL: http://localhost:" + PORT + "/");
        System.out.println(" Root: " + new File(DOC_ROOT).getAbsolutePath());
        System.out.println("=================================================");
        System.out.println("Press Ctrl+C to stop.");
    }

    static class StaticFileHandler implements HttpHandler {
        private final String rootPath;

        public StaticFileHandler(String rootPath) {
            this.rootPath = rootPath;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String uri = t.getRequestURI().getPath();
            
            // 默认访问 index.html
            if (uri.equals("/")) {
                uri = "/index.html";
            }

            File file = new File(rootPath + uri);
            
            // 安全检查：防止目录遍历攻击
            if (!file.getCanonicalPath().startsWith(new File(rootPath).getCanonicalPath())) {
                send404(t);
                return;
            }

            if (file.exists() && file.isFile()) {
                String mimeType = guessMimeType(file.getName());
                t.getResponseHeaders().set("Content-Type", mimeType);
                t.sendResponseHeaders(200, file.length());
                
                try (OutputStream os = t.getResponseBody();
                     FileInputStream fs = new FileInputStream(file)) {
                    final byte[] buffer = new byte[0x10000];
                    int count;
                    while ((count = fs.read(buffer)) >= 0) {
                        os.write(buffer, 0, count);
                    }
                }
            } else {
                send404(t);
            }
        }

        private void send404(HttpExchange t) throws IOException {
            String response = "404 Not Found";
            t.sendResponseHeaders(404, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private String guessMimeType(String fileName) {
            if (fileName.endsWith(".html")) return "text/html";
            if (fileName.endsWith(".css")) return "text/css";
            if (fileName.endsWith(".js")) return "application/javascript";
            if (fileName.endsWith(".md")) return "text/markdown";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }
}
