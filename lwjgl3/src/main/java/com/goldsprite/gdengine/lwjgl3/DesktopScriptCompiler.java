package com.goldsprite.gdengine.lwjgl3;

import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * PC 桌面端脚本编译器
 * 流程: 源码 -> ECJ -> .class -> 生成索引 -> URLClassLoader -> 运行
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

			// 1. 确定源码目录
			File scriptsDir = new File(projectDir, "src/main/java");
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

			// 3. 准备输出目录
			File outputDir = new File(cacheDir, "classes");
			if (!outputDir.exists()) outputDir.mkdirs();
			// PC端编译速度快，我们可以选择不强行清空，覆盖即可，或者为了干净每次清空
			// 这里选择不清空，依靠编译器覆盖

			// 4. 构建 Classpath
			// 【核心优势】直接利用 JVM 当前的 Classpath
			String currentClasspath = System.getProperty("java.class.path");

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

			// 7. [新增] 生成项目索引文件 (project.index)
			// 这一步非常关键，它告诉引擎有哪些用户类可用
			generateProjectIndex(outputDir, projectDir);

			// 8. 动态加载
			URL[] urls = new URL[]{ outputDir.toURI().toURL() };
			URLClassLoader loader = new URLClassLoader(urls, this.getClass().getClassLoader());

			Debug.logT("Compiler", "✅ 编译成功! Index 已更新。");
			return loader.loadClass(mainClassName);

		} catch (Exception e) {
			Debug.logT("Compiler", "❌ PC 编译异常: %s", e.toString());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 扫描编译产物 (.class)，生成索引文件
	 */
	private void generateProjectIndex(File classesDir, File projectDir) {
		TreeSet<String> classNames = new TreeSet<>();
		Path start = classesDir.toPath();

		try {
			Files.walkFileTree(start, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					String fileName = file.getFileName().toString();
					// 过滤掉非 class 文件和匿名/内部类 (带 $ 的)
					if (fileName.endsWith(".class") && !fileName.contains("$")) {
						// 转换路径为类名: com/mygame/Player.class -> com.mygame.Player
						String relative = start.relativize(file).toString();
						String className = relative
							.replace(File.separatorChar, '.')
							.replace(".class", "");
						classNames.add(className);
					}
					return FileVisitResult.CONTINUE;
				}
			});

			// 写入 project.index 到项目根目录
			File indexFile = new File(projectDir, "project.index");
			StringBuilder sb = new StringBuilder();
			for (String name : classNames) {
				sb.append(name).append("\n");
			}

			// 简单的文件写入逻辑
			java.nio.file.Files.write(indexFile.toPath(), sb.toString().getBytes("UTF-8"));

		} catch (IOException e) {
			Debug.logT("Compiler", "⚠️ 索引生成失败: " + e.getMessage());
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
