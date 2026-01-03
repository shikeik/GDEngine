package com.goldsprite.gdengine.android;

import android.content.Context;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug; // 【关键】引入你的 Debug 类
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

// 避免导入冲突，依然使用全限定名调用 Main

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
            Debug.logT("Compiler", "使用缓存的 android.jar");
            return;
        }

        try (InputStream is = context.getAssets().open("android.jar");
		FileOutputStream fos = new FileOutputStream(androidJarFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            Debug.logT("Compiler", "android.jar 解压完成");
        } catch (IOException e) {
            e.printStackTrace();
            Debug.logT("Compiler", "Fatal Error: 无法从 Assets 读取 android.jar!");
        }
    }

    @Override
    public Class<?> compile(String className, String sourceCode) {
        try {
            Debug.logT("Compiler", "1. 准备环境: %s", className);

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

            // 3. 调用 ECJ
            Debug.logT("Compiler", "2. ECJ 编译 Java -> Class");
            StringWriter ecjLog = new StringWriter();
            PrintWriter ecjWriter = new PrintWriter(ecjLog);

            org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler = 
                new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

            String[] ecjArgs = new String[] {
                "-1.7",
                "-nowarn",
                "-proc:none", // 禁用注解处理
                "-d", classDir.getAbsolutePath(),
                "-classpath", androidJarFile.getAbsolutePath(),
                javaFile.getAbsolutePath()
            };

            boolean success = ecjCompiler.compile(ecjArgs);

            if (!success) {
                Debug.logT("Compiler", "ECJ 失败:\n%s", ecjLog.toString());
                return null;
            }

            // 检查 Class 文件
            String classRelPath = className.replace('.', '/') + ".class";
            File generatedClassFile = new File(classDir, classRelPath);

            if (!generatedClassFile.exists()) {
                Debug.logT("Compiler", "Fatal: ECJ 成功但未生成文件! \n路径: %s", generatedClassFile.getAbsolutePath());
                return null;
            }
            Debug.logT("Compiler", "Class 生成成功: %s", generatedClassFile.getName());

            // 4. 调用 DX (Class -> Dex)
            Debug.logT("Compiler", "3. DX 转换 Class -> Dex");
            File dexFile = new File(cacheDir, "script.dex");
            if (dexFile.exists()) dexFile.delete();

            // 【核心修复】
            // DX 的输入参数改为 classDir (目录)，而不是具体的文件
            // 这样 DX 才能正确识别 com.test 包结构
            String[] dxArgs = new String[] {
                "--dex",
                "--output=" + dexFile.getAbsolutePath(),
                classDir.getAbsolutePath() // <--- 改回传目录！
            };

            // 为了防止 UsageException 没有任何提示，我们尝试捕获标准输出
            // 但 DX 的 main 方法直接用 System.out/err，我们先直接调用
            // 只要参数对，就不会抛 UsageException
            com.android.dx.command.dexer.Main.main(dxArgs);

            if (!dexFile.exists()) {
                Debug.logT("Compiler", "DX 失败，未生成 script.dex");
                return null;
            }

            // 5. 动态加载
            Debug.logT("Compiler", "4. DexClassLoader 加载");
            DexClassLoader loader = new DexClassLoader(
                dexFile.getAbsolutePath(),
                dexOutputDir.getAbsolutePath(),
                null,
                getClass().getClassLoader()
            );

            Class<?> cls = loader.loadClass(className);
            Debug.logT("Compiler", "=== 成功! 类已加载 ===");
            return cls;

        } catch (Exception e) {
            e.printStackTrace();
            Debug.logT("Compiler", "异常: %s", e.toString());
            return null;
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }
}
