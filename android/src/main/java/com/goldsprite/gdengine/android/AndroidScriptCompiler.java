package com.goldsprite.gdengine.android;

import android.content.Context;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import dalvik.system.DexClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class AndroidScriptCompiler implements IScriptCompiler {
	private final Context context;
	private final File cacheDir;
	private final File dexOutputDir;
	private final File androidJarFile;

	public AndroidScriptCompiler(Context context) {
		this.context = context;
		this.cacheDir = new File(context.getCacheDir(), "compiler_cache");
		this.dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
		this.androidJarFile = new File(context.getCacheDir(), "android.jar");

		if (!cacheDir.exists()) cacheDir.mkdirs();
		if (!dexOutputDir.exists()) dexOutputDir.mkdirs();

		prepareAndroidJar();
	}

	private void prepareAndroidJar() {
		if (androidJarFile.exists() && androidJarFile.length() > 0) {
			// Debug.logT("Compiler", "使用缓存的 android.jar");
			return;
		}
		try (InputStream is = context.getAssets().open("android.jar");
			 FileOutputStream fos = new FileOutputStream(androidJarFile)) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) fos.write(buffer, 0, length);
			Debug.logT("Compiler", "android.jar 解压完成");
		} catch (IOException e) {
			Debug.logT("Compiler", "Fatal: android.jar missing!");
		}
	}

	@Override
	public Class<?> compile(String className, String sourceCode) {
		// 定义流变量，方便 finally 中恢复
		PrintStream oldOut = System.out;
		PrintStream oldErr = System.err;
		ByteArrayOutputStream dxOutput = new ByteArrayOutputStream();

		try {
			Debug.logT("Compiler", "1. 准备: %s", className);

			// 1. 准备目录
			File srcDir = new File(cacheDir, "src");
			File classDir = new File(cacheDir, "classes");
			if (srcDir.exists()) deleteRecursive(srcDir);
			if (classDir.exists()) deleteRecursive(classDir);
			srcDir.mkdirs();
			classDir.mkdirs();

			// 2. 保存源码
			String simpleName = className.substring(className.lastIndexOf('.') + 1);
			File javaFile = new File(srcDir, simpleName + ".java");
			FileWriter writer = new FileWriter(javaFile);
			writer.write(sourceCode);
			writer.close();

			// 3. ECJ 编译
			Debug.logT("Compiler", "2. ECJ 编译...");
			StringWriter ecjLog = new StringWriter();
			PrintWriter ecjWriter = new PrintWriter(ecjLog);

			org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler =
				new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

			String[] ecjArgs = {
				"-1.7", "-nowarn", "-proc:none",
				"-d", classDir.getAbsolutePath(),
				"-classpath", androidJarFile.getAbsolutePath(),
				javaFile.getAbsolutePath()
			};

			boolean success = ecjCompiler.compile(ecjArgs);
			if (!success) {
				Debug.logT("Compiler", "ECJ 失败: %s", ecjLog);
				return null;
			}

			// 确认 Class 文件
			String classRelPath = className.replace('.', '/') + ".class";
			File generatedClassFile = new File(classDir, classRelPath);
			if (!generatedClassFile.exists()) {
				Debug.logT("Compiler", "Fatal: Class未生成: %s", generatedClassFile);
				return null;
			}

			// 4. DX 转换 (关键步骤)
			Debug.logT("Compiler", "3. DX 转换...");
			File dexFile = new File(cacheDir, "script.dex");
			if (dexFile.exists()) dexFile.delete();

			// 【关键修改】删掉 "--dex" 参数！
			// 因为 dexer.Main 本身就是 dexer，不需要告诉它“我要转dex”
			String[] dxArgs = {
				"--output=" + dexFile.getAbsolutePath(),
				generatedClassFile.getAbsolutePath()
			};

			Debug.logT("Compiler", "DX Args: %s", java.util.Arrays.toString(dxArgs));

			// 【关键】拦截 System.out 和 System.err
			// 这样 UsageException 抛出前打印的错误信息就会被我们捕获
			System.setOut(new PrintStream(dxOutput));
			System.setErr(new PrintStream(dxOutput));

			// 执行 DX
			com.android.dx.command.dexer.Main.main(dxArgs);

			// 恢复流 (非常重要，否则后续日志都看不到了)
			System.setOut(oldOut);
			System.setErr(oldErr);

			if (!dexFile.exists()) {
				// 打印捕获到的 DX 错误信息
				Debug.logT("Compiler", "DX 失败! 详细原因:\n%s", dxOutput.toString());
				return null;
			}

			// 5. Dex 加载
			Debug.logT("Compiler", "4. 加载 Dex...");
			DexClassLoader loader = new DexClassLoader(
				dexFile.getAbsolutePath(),
				dexOutputDir.getAbsolutePath(),
				null,
				getClass().getClassLoader()
			);

			Class<?> cls = loader.loadClass(className);
			Debug.logT("Compiler", "=== 成功! ===");
			return cls;

		} catch (Exception e) {
			// 发生异常时也要确保恢复流
			System.setOut(oldOut);
			System.setErr(oldErr);

			// 如果是 DX 抛出的异常，把捕获的日志也打出来
			String dxLog = dxOutput.toString();
			if (!dxLog.isEmpty()) {
				Debug.logT("Compiler", "DX 崩溃日志:\n%s", dxLog);
			}
			Debug.logT("Compiler", "异常: %s", e.toString());
			return null;
		}
	}

	private void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles()) deleteRecursive(child);
		fileOrDirectory.delete();
	}
}
