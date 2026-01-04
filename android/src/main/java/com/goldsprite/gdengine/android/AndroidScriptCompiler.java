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
import dalvik.system.InMemoryDexClassLoader;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AndroidScriptCompiler implements IScriptCompiler {
	private final Context context;
	private final File cacheDir;
	private final File dexOutputDir;
	private final File androidJarFile;

	public AndroidScriptCompiler(Context context) {
		this.context = context;

		// 1. 路径修正：使用 getCodeCacheDir 确保有执行权限
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			this.cacheDir = new File(context.getCodeCacheDir(), "compiler_cache");
		} else {
			this.cacheDir = new File(context.getCacheDir(), "compiler_cache");
		}

		this.dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
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

			// ECJ 编译
			if (!runEcjCompile(javaFile, classDir)) {
				return null;
			}

			Debug.logT("Compiler", "3. D8 转换...");

			// D8 输出为 jar (因为 D8 要求输出必须是容器)
			File dexJarFile = new File(cacheDir, "script.jar");
			if (dexJarFile.exists()) dexJarFile.delete();

			if (!runD8Dexing(classDir, dexJarFile)) {
				return null;
			}

			ClassLoader classLoader;

			// 【关键修复】分版本加载 + 格式解包
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				Debug.logT("Compiler", "4. 内存加载 (InMemory)...");

				// 1. 读取 Jar 里的 classes.dex
				byte[] dexBytes = null;
				try (ZipInputStream zis = new ZipInputStream(new FileInputStream(dexJarFile))) {
					ZipEntry entry;
					while ((entry = zis.getNextEntry()) != null) {
						if (entry.getName().equals("classes.dex")) {
							// 找到目标，读取字节
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							byte[] buffer = new byte[2048];
							int len;
							while ((len = zis.read(buffer)) > 0) {
								out.write(buffer, 0, len);
							}
							dexBytes = out.toByteArray();
							break;
						}
					}
				}

				if (dexBytes == null) {
					Debug.logT("Compiler", "Fatal: script.jar 中未找到 classes.dex");
					return null;
				}

				// 2. 将纯 DEX 字节喂给加载器
				ByteBuffer buffer = ByteBuffer.wrap(dexBytes);
				classLoader = new InMemoryDexClassLoader(
					buffer,
					getClass().getClassLoader()
				);

			} else {
				// 旧版本：直接通过文件路径加载 Jar，DexClassLoader 会自动处理解压
				Debug.logT("Compiler", "4. 文件加载 (DexClassLoader)...");
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

	// --- 辅助方法 (保持不变) ---

	private boolean runEcjCompile(File javaFile, File classDir) {
		Debug.logT("Compiler", "2. ECJ 编译中...");
		StringWriter ecjLog = new StringWriter();
		PrintWriter ecjWriter = new PrintWriter(ecjLog);

		org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler =
			new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

		String[] ecjArgs = {
			"-source", "1.8", "-target", "1.8", "-nowarn", "-proc:none",
			"-d", classDir.getAbsolutePath(),
			"-classpath", androidJarFile.getAbsolutePath(),
			javaFile.getAbsolutePath()
		};

		boolean success = ecjCompiler.compile(ecjArgs);
		if (!success) Debug.logT("Compiler", "ECJ 编译失败:\n%s", ecjLog.toString());
		return success;
	}

	private boolean runD8Dexing(File classDir, File outputDexFile) {
		try {
			List<Path> classFiles = new ArrayList<>();
			try (Stream<Path> walk = Files.walk(Paths.get(classDir.getAbsolutePath()))) {
				classFiles = walk.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList());
			}
			if (classFiles.isEmpty()) return false;

			DiagnosticsHandler handler = new DiagnosticsHandler() {
				@Override public void error(Diagnostic d) { Debug.logT("Compiler", "[D8 Error] %s", d.getDiagnosticMessage()); }
				@Override public void warning(Diagnostic d) { Debug.logT("Compiler", "[D8 Warn] %s", d.getDiagnosticMessage()); }
				@Override public void info(Diagnostic d) {}
			};

			D8Command.Builder builder = D8Command.builder(handler);
			builder.setMode(CompilationMode.DEBUG);
			builder.setMinApiLevel(19);
			builder.addProgramFiles(classFiles);
			builder.addLibraryFiles(Paths.get(androidJarFile.getAbsolutePath()));
			builder.setOutput(Paths.get(outputDexFile.getAbsolutePath()), OutputMode.DexIndexed);

			D8.run(builder.build());
			return outputDexFile.exists();
		} catch (Exception e) {
			Debug.logT("Compiler", "D8 异常: %s", e.getMessage());
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
			Debug.logT("Compiler", "Fatal: android.jar missing!");
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
