package com.goldsprite.gdengine.android;

import android.content.Context;
import android.os.Build;
import com.android.tools.r8.*;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AndroidScriptCompiler implements IScriptCompiler {
	private final Context context;
	private final File cacheDir;     // 编译缓存根目录
	private final File libsDir;      // 依赖库目录 (android.jar, gdengine.jar, gdx.jar...)
	private final File dexOutputDir; // Dex 解压目录

	public AndroidScriptCompiler(Context context) {
		this.context = context;

		// 1. 设置目录
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			this.cacheDir = new File(context.getCodeCacheDir(), "compiler_cache");
		} else {
			this.cacheDir = new File(context.getCacheDir(), "compiler_cache");
		}

		// 所有的依赖 jar 都释放到这里
		this.libsDir = new File(context.getCacheDir(), "engine_libs");
		this.dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);

		if (!cacheDir.exists()) cacheDir.mkdirs();
		if (!libsDir.exists()) libsDir.mkdirs();
		if (!dexOutputDir.exists()) dexOutputDir.mkdirs();

		// 2. 准备所有依赖库
		prepareDependencies();
	}

	/**
	 * 核心接口：编译整个项目目录
	 * @param projectPath 项目根目录 (例如 Projects/DemoGame)
	 *                   必须包含 Scripts 子目录
	 */
	@Override
	public Class<?> compile(String mainClassName, String projectPath) { // 稍微改下签名适配你的接口
		try {
			File projectDir = new File(projectPath);
			Debug.logT("Compiler", "=== 开始编译项目: %s ===", projectDir.getName());

			// 1. 确定源码目录
			File scriptsDir = new File(projectDir, "Scripts");
			if (!scriptsDir.exists()) {
				Debug.logT("Compiler", "❌ 找不到 Scripts 目录: %s", scriptsDir.getAbsolutePath());
				return null;
			}

			// 2. 扫描所有 .java 文件
			List<File> javaFiles = new ArrayList<>();
			recursiveFindJavaFiles(scriptsDir, javaFiles);

			if (javaFiles.isEmpty()) {
				Debug.logT("Compiler", "⚠️ 项目中没有 .java 文件");
				return null;
			}
			Debug.logT("Compiler", "扫描到 %d 个源文件", javaFiles.size());

			// 3. 准备输出目录
			File classOutputDir = new File(cacheDir, "classes");
			if(classOutputDir.exists()) deleteRecursive(classOutputDir);
			classOutputDir.mkdirs();

			// 4. 构建 Classpath (libsDir 下的所有 jar + android.jar)
			StringBuilder classpath = new StringBuilder();
			File[] libs = libsDir.listFiles(f -> f.getName().endsWith(".jar"));
			if (libs != null) {
				for (File jar : libs) {
					classpath.append(jar.getAbsolutePath()).append(File.pathSeparator);
				}
			}
			Debug.logT("Compiler", "Classpath 构建完成，包含 %d 个库", (libs != null ? libs.length : 0));

			// 5. ECJ 编译
			if (!runEcjCompile(javaFiles, classOutputDir, classpath.toString())) {
				return null;
			}

			// 6. D8 转换
			File dexJarFile = new File(cacheDir, "script_output.jar");
			if (dexJarFile.exists()) dexJarFile.delete();

			if (!runD8Dexing(classOutputDir, dexJarFile, libs)) {
				return null;
			}

			// 7. 加载
			return loadScriptJar(dexJarFile, mainClassName);

		} catch (Exception e) {
			Debug.logT("Compiler", "❌ 编译流程异常: %s", e.toString());
			e.printStackTrace();
			return null;
		}
	}

	// --- 内部实现 ---

	private boolean runEcjCompile(List<File> javaFiles, File outputDir, String classpath) {
		Debug.logT("Compiler", "2. ECJ 编译中...");
		StringWriter ecjLog = new StringWriter();
		PrintWriter ecjWriter = new PrintWriter(ecjLog);

		List<String> args = new ArrayList<>();
		args.add("-1.8"); // source level
		args.add("-nowarn");
		args.add("-proc:none");
		args.add("-d"); args.add(outputDir.getAbsolutePath());
		args.add("-classpath"); args.add(classpath);

		// 添加所有源文件路径
		for(File f : javaFiles) args.add(f.getAbsolutePath());

		org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler =
			new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

		boolean success = ecjCompiler.compile(args.toArray(new String[0]));

		if (!success) {
			Debug.logT("Compiler", "ECJ 编译失败:\n%s", ecjLog.toString());
		}
		return success;
	}

	private boolean runD8Dexing(File classDir, File outputDexFile, File[] libJars) {
		Debug.logT("Compiler", "3. D8 转换中...");
		try {
			List<Path> classFiles = Files.walk(Paths.get(classDir.getAbsolutePath()))
				.filter(p -> p.toString().endsWith(".class"))
				.collect(Collectors.toList());

			D8Command.Builder builder = D8Command.builder();
			builder.setMode(CompilationMode.DEBUG);
			builder.setMinApiLevel(19);
			builder.setOutput(Paths.get(outputDexFile.getAbsolutePath()), OutputMode.DexIndexed);

			// 输入文件
			builder.addProgramFiles(classFiles);

			// 库引用 (android.jar + engine libs)
			if (libJars != null) {
				for (File jar : libJars) {
					builder.addLibraryFiles(Paths.get(jar.getAbsolutePath()));
				}
			}

			D8.run(builder.build());
			return outputDexFile.exists();
		} catch (Exception e) {
			Debug.logT("Compiler", "D8 异常: %s", e.getMessage());
			return false;
		}
	}

	private Class<?> loadScriptJar(File dexJarFile, String mainClassName) throws Exception {
		ClassLoader classLoader;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Debug.logT("Compiler", "4. 内存加载...");
			byte[] dexBytes = extractDexFromJar(dexJarFile);
			if (dexBytes == null) throw new RuntimeException("No classes.dex found");

			ByteBuffer buffer = ByteBuffer.wrap(dexBytes);
			classLoader = new InMemoryDexClassLoader(buffer, getClass().getClassLoader());
		} else {
			Debug.logT("Compiler", "4. 文件加载...");
			dexJarFile.setReadOnly();
			classLoader = new DexClassLoader(
				dexJarFile.getAbsolutePath(),
				dexOutputDir.getAbsolutePath(),
				null,
				getClass().getClassLoader()
			);
		}

		Debug.logT("Compiler", "✅ 加载主类: %s", mainClassName);
		return classLoader.loadClass(mainClassName);
	}

	// 从 Jar 中提取 classes.dex
	private byte[] extractDexFromJar(File jarFile) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals("classes.dex")) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buffer = new byte[2048];
					int len;
					while ((len = zis.read(buffer)) > 0) out.write(buffer, 0, len);
					return out.toByteArray();
				}
			}
		}
		return null;
	}

	private void prepareDependencies() {
		extractAsset("android.jar", new File(libsDir, "android.jar"));

		// libs/ 下的所有文件
		try {
			String[] libFiles = context.getAssets().list("libs");
			if (libFiles != null) {
				for (String fileName : libFiles) {
					extractAsset("libs/" + fileName, new File(libsDir, fileName));
				}
			}
		} catch (IOException e) {
			Debug.logT("Compiler", "⚠️ 无法列出 libs 目录");
		}
	}

	private void extractAsset(String assetName, File destFile) {
		if (destFile.exists() && destFile.length() > 0) {
			Debug.logT("Compiler", "依赖库: %s 已缓存, 加载成功", assetName);
			return;
		}
		try (InputStream is = context.getAssets().open(assetName);
			 FileOutputStream fos = new FileOutputStream(destFile)) {
			byte[] buffer = new byte[2048];
			int len;
			while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
			Debug.logT("Compiler", "依赖库解压完成: " + assetName);
		} catch (IOException e) {
			Debug.logT("Compiler", "Fatal: 缺失依赖库 " + assetName);
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

	private void deleteRecursive(File f) { /* 同前 */
		if (f.isDirectory()) for (File c : f.listFiles()) deleteRecursive(c);
		f.delete();
	}
}
