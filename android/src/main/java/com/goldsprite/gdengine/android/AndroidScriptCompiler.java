package com.goldsprite.gdengine.android;

import android.content.Context;
import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.Diagnostic;
import com.android.tools.r8.DiagnosticsHandler;
import com.android.tools.r8.OutputMode;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import dalvik.system.DexClassLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AndroidScriptCompiler implements IScriptCompiler {
	private final Context context;
	private final File cacheDir;
	private final File dexOutputDir;
	private final File androidJarFile;

	public AndroidScriptCompiler(Context context) {
		this.context = context;
		// 源码和临时Class文件目录
		this.cacheDir = new File(context.getCacheDir(), "compiler_cache");
		// Dex加载优化目录 (必须是私有目录)
		this.dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
		// android.jar 路径
		this.androidJarFile = new File(context.getCacheDir(), "android.jar");

		if (!cacheDir.exists()) cacheDir.mkdirs();
		if (!dexOutputDir.exists()) dexOutputDir.mkdirs();

		prepareAndroidJar();
	}

	/**
	 * 核心编译方法
	 */
	@Override
	public Class<?> compile(String className, String sourceCode) {
		try {
			Debug.logT("Compiler", "=== 开始编译流程: %s ===", className);

			// 1. 准备目录 (清理旧文件)
			File srcDir = new File(cacheDir, "src");
			File classDir = new File(cacheDir, "classes");
			recreateDir(srcDir);
			recreateDir(classDir);

			// 2. 将源码保存为 .java 文件
			String simpleName = className.substring(className.lastIndexOf('.') + 1);
			File javaFile = new File(srcDir, simpleName + ".java");
			try (FileWriter writer = new FileWriter(javaFile)) {
				writer.write(sourceCode);
			}

			// 3. ECJ 编译 (Java -> Class)
			if (!runEcjCompile(javaFile, classDir)) {
				return null;
			}

			// 4. D8 转换 (Class -> Dex)
			File dexFile = new File(cacheDir, "script.dex");
			if (dexFile.exists()) dexFile.delete();

			if (!runD8Dexing(classDir, dexFile)) {
				return null;
			}

			// 5. 加载 Dex (Dex -> Class)
			Debug.logT("Compiler", "4. 加载 Dex...");
			DexClassLoader loader = new DexClassLoader(
				dexFile.getAbsolutePath(),
				dexOutputDir.getAbsolutePath(),
				null,
				getClass().getClassLoader()
			);

			Class<?> cls = loader.loadClass(className);
			Debug.logT("Compiler", "✅ 编译并加载成功!");
			return cls;

		} catch (Exception e) {
			Debug.logT("Compiler", "❌ 编译流程崩溃: %s", e.toString());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 步骤 3: 运行 ECJ 编译器
	 */
	private boolean runEcjCompile(File javaFile, File classDir) {
		Debug.logT("Compiler", "2. ECJ 编译中...");
		StringWriter ecjLog = new StringWriter();
		PrintWriter ecjWriter = new PrintWriter(ecjLog);

		org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler =
			new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

		// 设置编译参数
		String[] ecjArgs = {
			"-source", "1.8", // D8 支持 Java 8，可以使用 1.8
			"-target", "1.8",
			"-nowarn",
			"-proc:none",
			"-d", classDir.getAbsolutePath(), // 输出目录
			"-classpath", androidJarFile.getAbsolutePath(), // 依赖库
			javaFile.getAbsolutePath() // 输入文件
		};

		boolean success = ecjCompiler.compile(ecjArgs);

		if (!success) {
			Debug.logT("Compiler", "ECJ 编译失败. 日志:\n%s", ecjLog.toString());
		} else {
			// 检查是否有 Class 文件生成
			File[] files = classDir.listFiles();
			if (files == null || files.length == 0) {
				Debug.logT("Compiler", "ECJ 似乎成功了，但没有生成 Class 文件。");
				return false;
			}
		}
		return success;
	}

	/**
	 * 步骤 4: 运行 D8 转换器
	 */
	private boolean runD8Dexing(File classDir, File outputDexFile) {
		Debug.logT("Compiler", "3. D8 转换中...");

		try {
			// 扫描所有 .class 文件
			List<Path> classFiles = new ArrayList<>();
			try (Stream<Path> walk = Files.walk(Paths.get(classDir.getAbsolutePath()))) {
				classFiles = walk
					.filter(p -> p.toString().endsWith(".class"))
					.collect(Collectors.toList());
			}

			if (classFiles.isEmpty()) {
				Debug.logT("Compiler", "D8 错误: 找不到 Class 文件");
				return false;
			}

			// 【修正 1】先定义日志处理器
			DiagnosticsHandler handler = new DiagnosticsHandler() {
				@Override
				public void error(Diagnostic diagnostic) {
					Debug.logT("Compiler", "[D8 Error] %s", diagnostic.getDiagnosticMessage());
				}

				@Override
				public void warning(Diagnostic diagnostic) {
					Debug.logT("Compiler", "[D8 Warn] %s", diagnostic.getDiagnosticMessage());
				}

				@Override
				public void info(Diagnostic diagnostic) {
					// ignore
				}
			};

			// 【修正 2】在创建 builder 时直接传入 handler
			D8Command.Builder builder = D8Command.builder(handler);

			// --- 配置 ---
			builder.setMode(CompilationMode.DEBUG);
			builder.setMinApiLevel(19);
			builder.addProgramFiles(classFiles);
			builder.addLibraryFiles(Paths.get(androidJarFile.getAbsolutePath()));
			builder.setOutput(Paths.get(outputDexFile.getAbsolutePath()), OutputMode.DexIndexed);

			// 运行
			D8.run(builder.build());

			return outputDexFile.exists();

		} catch (CompilationFailedException e) {
			Debug.logT("Compiler", "D8 编译失败: %s", e.getMessage());
			return false;
		} catch (IOException e) {
			Debug.logT("Compiler", "D8 IO 异常: %s", e.getMessage());
			return false;
		}
	}

	// --- 辅助方法 ---

	private void prepareAndroidJar() {
		if (androidJarFile.exists() && androidJarFile.length() > 0) return;

		Debug.logT("Compiler", "正在从 Assets 解压 android.jar...");
		try (InputStream is = context.getAssets().open("android.jar");
			 FileOutputStream fos = new FileOutputStream(androidJarFile)) {
			byte[] buffer = new byte[2048];
			int length;
			while ((length = is.read(buffer)) > 0) fos.write(buffer, 0, length);
			Debug.logT("Compiler", "android.jar 解压完成");
		} catch (IOException e) {
			Debug.logT("Compiler", "Fatal: Assets 中找不到 android.jar!");
			e.printStackTrace();
		}
	}

	private void recreateDir(File dir) {
		if (dir.exists()) deleteRecursive(dir);
		dir.mkdirs();
	}

	private void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory()) {
			File[] files = fileOrDirectory.listFiles();
			if (files != null) for (File child : files) deleteRecursive(child);
		}
		fileOrDirectory.delete();
	}
}
