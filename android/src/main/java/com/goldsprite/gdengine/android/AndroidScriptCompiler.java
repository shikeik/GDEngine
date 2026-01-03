package com.goldsprite.gdengine.android;

import android.content.Context;
import android.os.Build;
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
import dalvik.system.InMemoryDexClassLoader; // 新增引用

import java.io.*;
import java.nio.ByteBuffer;
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

		// 【修复 1】路径修正
		// Android 10+ 禁止在 getCacheDir() 执行代码。
		// getCodeCacheDir() 是系统专门预留给动态代码生成的目录，拥有执行权限。
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			this.cacheDir = new File(context.getCodeCacheDir(), "compiler_cache");
		} else {
			this.cacheDir = new File(context.getCacheDir(), "compiler_cache");
		}

		// DexClassLoader 需要的解压目录（仅在旧版本Android使用）
		this.dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);

		// android.jar 依然放普通缓存即可，因为它只是读取依赖，不执行
		this.androidJarFile = new File(context.getCacheDir(), "android.jar");

		if (!cacheDir.exists()) cacheDir.mkdirs();
		if (!dexOutputDir.exists()) dexOutputDir.mkdirs();

		prepareAndroidJar();
	}

	@Override
	public Class<?> compile(String className, String sourceCode) {
		try {
			Debug.logT("Compiler", "=== 开始编译流程: %s ===", className);

			File srcDir = new File(cacheDir, "src");
			File classDir = new File(cacheDir, "classes");
			recreateDir(srcDir);
			recreateDir(classDir);

			String simpleName = className.substring(className.lastIndexOf('.') + 1);
			File javaFile = new File(srcDir, simpleName + ".java");
			try (FileWriter writer = new FileWriter(javaFile)) {
				writer.write(sourceCode);
			}

			if (!runEcjCompile(javaFile, classDir)) {
				return null;
			}

			Debug.logT("Compiler", "3. D8 转换...");

			// 输出文件：script.jar
			File dexJarFile = new File(cacheDir, "script.jar");
			if (dexJarFile.exists()) dexJarFile.delete();

			if (!runD8Dexing(classDir, dexJarFile)) {
				return null;
			}

			// 【修复 2】加载策略分流
			ClassLoader classLoader;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				// 方案 A: Android 8.0+ (API 26+) -> 内存加载
				// 直接读取 jar 字节流加载，无需写入“可执行文件”，完美规避 W^X 权限崩溃
				Debug.logT("Compiler", "4. 内存加载 (InMemoryDexClassLoader)...");

				byte[] dexBytes = Files.readAllBytes(dexJarFile.toPath());
				ByteBuffer buffer = ByteBuffer.wrap(dexBytes);

				classLoader = new InMemoryDexClassLoader(
					buffer,
					getClass().getClassLoader()
				);

			} else {
				// 方案 B: 老版本 Android -> 文件加载
				// 只要文件在 getCodeCacheDir() 中，系统通常允许执行
				Debug.logT("Compiler", "4. 文件加载 (DexClassLoader)...");

				// 设置只读是个好习惯，部分系统会检查这个
				dexJarFile.setReadOnly();

				classLoader = new DexClassLoader(
					dexJarFile.getAbsolutePath(),
					dexOutputDir.getAbsolutePath(),
					null,
					getClass().getClassLoader()
				);
			}

			Class<?> cls = classLoader.loadClass(className);
			Debug.logT("Compiler", "✅ 编译加载成功!");
			return cls;

		} catch (Exception e) {
			Debug.logT("Compiler", "❌ 编译流程崩溃: %s", e.toString());
			e.printStackTrace();
			return null;
		}
	}

	// --- 保持不变的辅助方法 ---

	private boolean runEcjCompile(File javaFile, File classDir) {
		Debug.logT("Compiler", "2. ECJ 编译中...");
		StringWriter ecjLog = new StringWriter();
		PrintWriter ecjWriter = new PrintWriter(ecjLog);

		org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler =
			new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

		String[] ecjArgs = {
			"-source", "1.8",
			"-target", "1.8",
			"-nowarn",
			"-proc:none",
			"-d", classDir.getAbsolutePath(),
			"-classpath", androidJarFile.getAbsolutePath(),
			javaFile.getAbsolutePath()
		};

		boolean success = ecjCompiler.compile(ecjArgs);

		if (!success) {
			Debug.logT("Compiler", "ECJ 编译失败. 日志:\n%s", ecjLog.toString());
		} else {
			File[] files = classDir.listFiles();
			if (files == null || files.length == 0) {
				Debug.logT("Compiler", "ECJ 似乎成功了，但没有生成 Class 文件。");
				return false;
			}
		}
		return success;
	}

	private boolean runD8Dexing(File classDir, File outputDexFile) {
		try {
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
				public void info(Diagnostic diagnostic) {}
			};

			D8Command.Builder builder = D8Command.builder(handler);
			builder.setMode(CompilationMode.DEBUG);
			builder.setMinApiLevel(19);
			builder.addProgramFiles(classFiles);
			builder.addLibraryFiles(Paths.get(androidJarFile.getAbsolutePath()));
			builder.setOutput(Paths.get(outputDexFile.getAbsolutePath()), OutputMode.DexIndexed);

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

	private void prepareAndroidJar() {
		if (androidJarFile.exists() && androidJarFile.length() > 0) return;
		try (InputStream is = context.getAssets().open("android.jar");
			 FileOutputStream fos = new FileOutputStream(androidJarFile)) {
			byte[] buffer = new byte[2048];
			int length;
			while ((length = is.read(buffer)) > 0) fos.write(buffer, 0, length);
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
