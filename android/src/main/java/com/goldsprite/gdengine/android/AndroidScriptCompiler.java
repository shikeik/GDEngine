package com.goldsprite.gdengine.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
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
	private final File cacheDir;
	private final File libsDir;
	private final File dexOutputDir;

	// [新增] 用于记录版本信息的 Prefs
	private static final String PREF_NAME = "engine_compiler_config";
	private static final String KEY_LAST_UPDATE = "last_apk_update_time";

	public AndroidScriptCompiler(Context context) {
		this.context = context;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			this.cacheDir = new File(context.getCodeCacheDir(), "compiler_cache");
		} else {
			this.cacheDir = new File(context.getCacheDir(), "compiler_cache");
		}

		this.libsDir = new File(context.getCacheDir(), "engine_libs");
		this.dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);

		if (!cacheDir.exists()) cacheDir.mkdirs();
		if (!libsDir.exists()) libsDir.mkdirs();
		if (!dexOutputDir.exists()) dexOutputDir.mkdirs();

		prepareDependencies();
	}

	// ... [compile 和 runEcjCompile/runD8Dexing/loadScriptJar 方法保持不变，省略以节省篇幅] ...
	// 请保留之前的 compile, runEcjCompile, runD8Dexing, loadScriptJar, extractDexFromJar, recursiveFindJavaFiles, deleteRecursive 方法

	@Override
	public Class<?> compile(String mainClassName, String projectPath) {
		// ... (保持原样)
		// 这里为了代码完整性，请确保保留之前正确的实现逻辑
		try {
			File projectDir = new File(projectPath);
			Debug.logT("Compiler", "=== 开始编译项目: %s ===", projectDir.getName());

			File scriptsDir = new File(projectDir, "src/main/java");
			if (!scriptsDir.exists()) {
				Debug.logT("Compiler", "❌ 找不到 Scripts 目录: %s", scriptsDir.getAbsolutePath());
				return null;
			}

			List<File> javaFiles = new ArrayList<>();
			recursiveFindJavaFiles(scriptsDir, javaFiles);

			if (javaFiles.isEmpty()) {
				Debug.logT("Compiler", "⚠️ 项目中没有 .java 文件");
				return null;
			}
			Debug.logT("Compiler", "扫描到 %d 个源文件", javaFiles.size());

			File classOutputDir = new File(cacheDir, "classes");
			if(classOutputDir.exists()) deleteRecursive(classOutputDir);
			classOutputDir.mkdirs();

			StringBuilder classpath = new StringBuilder();
			File[] libs = libsDir.listFiles(f -> f.getName().endsWith(".jar"));
			if (libs != null) {
				for (File jar : libs) {
					classpath.append(jar.getAbsolutePath()).append(File.pathSeparator);
				}
			}
			Debug.logT("Compiler", "Classpath 构建完成，包含 %d 个库", (libs != null ? libs.length : 0));

			if (!runEcjCompile(javaFiles, classOutputDir, classpath.toString())) {
				return null;
			}

			File dexJarFile = new File(cacheDir, "script_output.jar");
			if (dexJarFile.exists()) dexJarFile.delete();

			if (!runD8Dexing(classOutputDir, dexJarFile, libs)) {
				return null;
			}

			return loadScriptJar(dexJarFile, mainClassName);

		} catch (Exception e) {
			Debug.logT("Compiler", "❌ 编译流程异常: %s", e.toString());
			e.printStackTrace();
			return null;
		}
	}

	// ... (ECJ, D8, Loader 方法保持不变) ...

	private boolean runEcjCompile(List<File> javaFiles, File outputDir, String classpath) {
		Debug.logT("Compiler", "2. ECJ 编译中...");
		StringWriter ecjLog = new StringWriter();
		PrintWriter ecjWriter = new PrintWriter(ecjLog);

		List<String> args = new ArrayList<>();
		args.add("-1.8");
		args.add("-nowarn");
		args.add("-proc:none");
		args.add("-d"); args.add(outputDir.getAbsolutePath());
		args.add("-classpath"); args.add(classpath);
		for(File f : javaFiles) args.add(f.getAbsolutePath());

		org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler =
			new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

		boolean success = ecjCompiler.compile(args.toArray(new String[0]));
		if (!success) Debug.logT("Compiler", "ECJ 编译失败:\n%s", ecjLog.toString());
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
			builder.addProgramFiles(classFiles);
			if (libJars != null) {
				for (File jar : libJars) builder.addLibraryFiles(Paths.get(jar.getAbsolutePath()));
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

	private void recursiveFindJavaFiles(File dir, List<File> list) {
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files) {
			if (f.isDirectory()) recursiveFindJavaFiles(f, list);
			else if (f.getName().endsWith(".java")) list.add(f);
		}
	}

	private void deleteRecursive(File f) {
		if (f.isDirectory()) for (File c : f.listFiles()) deleteRecursive(c);
		f.delete();
	}

	// -------------------------------------------------------------------------
	// [核心修改] 智能依赖管理
	// -------------------------------------------------------------------------

	private void prepareDependencies() {
		boolean isNewVersion = checkAppVersionChanged();

		if (isNewVersion) {
			Debug.logT("Compiler", "♻️ 检测到应用更新(或缓存缺失)，正在刷新引擎依赖库...");
		}

		// 1. android.jar (通常很大，如果文件存在且不是新版本，就跳过)
		extractAsset("android.jar", new File(libsDir, "android.jar"), isNewVersion);

		// 2. libs/*.jar (gdx.jar, gdengine.jar)
		try {
			String[] libFiles = context.getAssets().list("engine/libs");
			if (libFiles != null) {
				for (String fileName : libFiles) {
					// 所有的 libs 下的 jar 都跟随版本刷新
					extractAsset("engine/libs/" + fileName, new File(libsDir, fileName), isNewVersion);
				}
			}
		} catch (IOException e) {
			Debug.logT("Compiler", "⚠️ 无法列出 assets/libs 目录");
		}

		// 刷新完毕，保存当前版本信息
		if (isNewVersion) {
			saveCurrentAppVersion();
		}
	}

	/**
	 * 检查 APK 是否更新了
	 * @return true 如果是新安装的版本 或者 缓存记录不存在
	 */
	private boolean checkAppVersionChanged() {
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			long currentUpdateTime = pInfo.lastUpdateTime; // APK 最后安装/更新时间

			SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
			long lastRecordedTime = prefs.getLong(KEY_LAST_UPDATE, -1);

			return currentUpdateTime != lastRecordedTime;
		} catch (Exception e) {
			return true; // 获取失败则保守策略：视为更新
		}
	}

	private void saveCurrentAppVersion() {
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
			prefs.edit().putLong(KEY_LAST_UPDATE, pInfo.lastUpdateTime).apply();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 执行解压
	 * @param overwrite 是否强制覆盖 (即使文件已存在)
	 */
	private void extractAsset(String assetName, File destFile, boolean overwrite) {
		// 智能判断：
		// 1. 如果必须要覆盖 (APK更新了)，则不管文件在不在都写
		// 2. 如果不需要覆盖 (APK没变)，但文件丢了，也得写
		// 3. 否则 (APK没变 且 文件健在)，跳过 -> 省 IO
		if (!overwrite && destFile.exists() && destFile.length() > 0) {
			return;
		}

		try (InputStream is = context.getAssets().open(assetName);
		FileOutputStream fos = new FileOutputStream(destFile)) {

			byte[] buffer = new byte[4096]; // 稍微加大 buffer
			int len;
			while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);

			// Debug.logT("Compiler", "已提取: " + assetName); // 减少日志刷屏，仅在出错或大版本更新时关注
		} catch (IOException e) {
			Debug.logT("Compiler", "❌ 提取库失败: " + assetName + " -> " + e.getMessage());
		}
	}
}
