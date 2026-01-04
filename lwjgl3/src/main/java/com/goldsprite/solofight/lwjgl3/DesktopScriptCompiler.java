package com.goldsprite.solofight.lwjgl3;

import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * PC 桌面端脚本编译器
 * 流程: 源码 -> ECJ -> .class -> URLClassLoader -> 运行
 */
public class DesktopScriptCompiler implements IScriptCompiler {

	private final File cacheDir;

	public DesktopScriptCompiler() {
		// 在项目根目录下创建一个临时编译目录
		// "./build/script_cache/"
		this.cacheDir = new File("build/script_cache");
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
	}

	@Override
	public Class<?> compile(String mainClassName, String projectPath) {
		try {
			File projectDir = new File(projectPath);
			Debug.logT("Compiler", "=== PC 编译开始: %s ===", projectDir.getName());

			// 1. 确定源码目录 (Projects/MyGame/Scripts)
			File scriptsDir = new File(projectDir, "Scripts");
			if (!scriptsDir.exists()) {
				Debug.logT("Compiler", "❌ 找不到 Scripts 目录: %s", scriptsDir.getAbsolutePath());
				return null;
			}

			// 2. 扫描源文件
			List<File> javaFiles = new ArrayList<>();
			recursiveFindJavaFiles(scriptsDir, javaFiles);
			if (javaFiles.isEmpty()) {
				Debug.logT("Compiler", "⚠️ 没有找到 .java 文件");
				return null;
			}

			// 3. 准备输出目录 (清理旧的 class)
			// 注意：PC 上文件锁定比较少，直接清理通常没问题
			// 但为了保险，还是建议保留目录结构，或者简单清理
			File outputDir = new File(cacheDir, "classes");
			if (!outputDir.exists()) outputDir.mkdirs();
			// PC端编译速度快，我们可以选择不强行清空，覆盖即可，或者为了干净每次清空
			// 这里选择不清空，依靠编译器覆盖

			// 4. 构建 Classpath
			// 【核心优势】直接利用 JVM 当前的 Classpath
			String currentClasspath = System.getProperty("java.class.path");
			// Debug.logT("Compiler", "Classpath: " + currentClasspath);

			// 5. 构建 ECJ 参数
			List<String> args = new ArrayList<>();
			args.add("-1.8"); // source level
			args.add("-nowarn");
			args.add("-encoding"); args.add("UTF-8");
			args.add("-d"); args.add(outputDir.getAbsolutePath());
			args.add("-cp"); args.add(currentClasspath);

			// 添加所有源文件
			for (File f : javaFiles) {
				args.add(f.getAbsolutePath());
			}

			// 6. 执行编译
			StringWriter outWriter = new StringWriter();
			StringWriter errWriter = new StringWriter();
			boolean success = BatchCompiler.compile(
				args.toArray(new String[0]),
				new PrintWriter(outWriter),
				new PrintWriter(errWriter),
				null
			);

			if (!success) {
				Debug.logT("Compiler", "❌ 编译失败:\n%s", errWriter.toString());
				return null;
			}
			Debug.logT("Compiler", "✅ 编译成功!");

			// 7. 动态加载
			// 创建一个新的 URLClassLoader，父加载器设为当前加载器(以访问引擎类)
			URL[] urls = new URL[]{ outputDir.toURI().toURL() };
			URLClassLoader loader = new URLClassLoader(urls, this.getClass().getClassLoader());

			// 8. 加载入口类
			return loader.loadClass(mainClassName);

		} catch (Exception e) {
			Debug.logT("Compiler", "❌ PC 编译异常: %s", e.toString());
			e.printStackTrace();
			return null;
		}
	}

	private void recursiveFindJavaFiles(File dir, List<File> list) {
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files) {
			if (f.isDirectory()) recursiveFindJavaFiles(f, list);
			else if (f.getName().endsWith(".java")) list.add(f);
		}
	}
}
