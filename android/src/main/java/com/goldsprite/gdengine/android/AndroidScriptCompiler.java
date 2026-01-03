package com.goldsprite.gdengine.android;

import android.content.Context;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import dalvik.system.DexClassLoader;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

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
		if (androidJarFile.exists() && androidJarFile.length() > 0) return;
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
		// 备份系统流
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
			try (FileWriter writer = new FileWriter(javaFile)) {
				writer.write(sourceCode);
			}

			// 3. ECJ 编译
			Debug.logT("Compiler", "2. ECJ 编译...");
			StringWriter ecjLog = new StringWriter();
			PrintWriter ecjWriter = new PrintWriter(ecjLog);

			org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler =
				new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

			String[] ecjArgs = {
				"-source", "1.7", "-target", "1.7",
				"-nowarn", "-proc:none",
				"-d", classDir.getAbsolutePath(),
				"-classpath", androidJarFile.getAbsolutePath(),
				javaFile.getAbsolutePath()
			};

			boolean success = ecjCompiler.compile(ecjArgs);
			if (!success) {
				Debug.logT("Compiler", "ECJ 失败:\n%s", ecjLog);
				return null;
			}

			// 4. DX 转换
			Debug.logT("Compiler", "3. DX 转换...");
			File dexFile = new File(cacheDir, "script.dex");
			if (dexFile.exists()) dexFile.delete();

			// 生成 class 文件的路径
			String classRelPath = className.replace('.', '/') + ".class";
			File generatedClassFile = new File(classDir, classRelPath);

			// DX 参数 (不包含 --dex)
			String[] dxArgs = {
				"--output=" + dexFile.getAbsolutePath(),
				generatedClassFile.getAbsolutePath()
			};
			Debug.logT("Compiler", "DX Args: %s", Arrays.toString(dxArgs));

			// 拦截输出流以便查看 DX 日志
			System.setOut(new PrintStream(dxOutput));
			System.setErr(new PrintStream(dxOutput));

			try {
				// 【核心修改】不调用 Main.main，而是通过反射调用内部 run 方法
				runDxByReflection(dxArgs, dxOutput);
			} catch (Throwable e) {
				// 恢复流
				System.setOut(oldOut);
				System.setErr(oldErr);
				Debug.logT("Compiler", "DX 内部异常: %s", e.toString());
				e.printStackTrace();
				String log = dxOutput.toString();
				if (!log.isEmpty()) Debug.logT("Compiler", "DX Output:\n%s", log);
				return null;
			}

			// 恢复流
			System.setOut(oldOut);
			System.setErr(oldErr);

			if (!dexFile.exists()) {
				Debug.logT("Compiler", "DX 失败! 产物未生成. Log:\n%s", dxOutput.toString());
				return null;
			}

			// 5. 加载 Dex
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
			if (System.out != oldOut) System.setOut(oldOut);
			if (System.err != oldErr) System.setErr(oldErr);
			Debug.logT("Compiler", "流程异常: %s", e.toString());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 使用反射调用 DX，绕过 System.exit
	 * 增强版：支持非 Public 方法，支持失败时打印方法列表
	 */
	/**
	 * 使用反射调用 DX，绕过 System.exit
	 * 终极兼容版：兼容 16.0.1 (静态方法) 到 最新版 (实例方法+DxContext)
	 */
	private void runDxByReflection(String[] args, ByteArrayOutputStream captureStream) throws Exception {
		// 1. 获取类引用
		Class<?> mainClass = Class.forName("com.android.dx.command.dexer.Main");
		Class<?> argsClass = Class.forName("com.android.dx.command.dexer.Main$Arguments");

		// 2. 创建 Arguments 实例
		Constructor<?> argsCons = argsClass.getDeclaredConstructor();
		argsCons.setAccessible(true);
		Object argsObj = argsCons.newInstance();

		// 3. 解析参数 (Arguments.parse)
		Method parseMethod = null;
		try {
			parseMethod = argsClass.getMethod("parse", String[].class);
		} catch (NoSuchMethodException e) {
			// 兼容旧版本可能是非 public 的情况
			parseMethod = argsClass.getDeclaredMethod("parse", String[].class);
		}
		parseMethod.setAccessible(true);
		parseMethod.invoke(argsObj, (Object) args);

		// 4. 获取 run 方法
		Method runMethod = mainClass.getMethod("run", argsClass);
		runMethod.setAccessible(true);

		// 5. 【核心修复】判断 run 是静态还是实例方法
		Object mainInstance = null;
		if (java.lang.reflect.Modifier.isStatic(runMethod.getModifiers())) {
			// 针对 dalvik-dx:16.0.1 等旧版本：run 是静态的
			// 不需要实例化 Main，直接传 null
			mainInstance = null;
			Debug.logT("Compiler", "检测到旧版 DX (Static Run)");
		} else {
			// 针对新版本：run 是实例方法，需要构造 Main 对象
			try {
				// 尝试新版 DX (带 DxContext)
				Class<?> dxContextClass = Class.forName("com.android.dx.command.DxContext");
				Constructor<?> ctxCons = dxContextClass.getConstructor(OutputStream.class, OutputStream.class);

				PrintStream streamWrapper = new PrintStream(captureStream);
				Object dxContextObj = ctxCons.newInstance(streamWrapper, streamWrapper);

				Constructor<?> mainCons = mainClass.getConstructor(dxContextClass);
				mainInstance = mainCons.newInstance(dxContextObj);
			} catch (Throwable e) {
				// 回退到中间版本 (无参构造)
				mainInstance = mainClass.newInstance();
			}
		}

		// 6. 执行 run
		// 如果是静态方法，mainInstance 为 null，invoke 会正常执行
		int result = (Integer) runMethod.invoke(mainInstance, argsObj);

		if (result != 0) {
			throw new RuntimeException("DX returned error code: " + result);
		}
	}

	private void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory()) {
			File[] files = fileOrDirectory.listFiles();
			if (files != null) for (File child : files) deleteRecursive(child);
		}
		fileOrDirectory.delete();
	}
}
