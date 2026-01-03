package com.goldsprite.gdengine.android;

import android.content.Context;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import com.goldsprite.gdengine.log.Debug;

// 【关键修改】把那两个冲突的 import Main 删掉了！
// import org.eclipse.jdt.internal.compiler.batch.Main;  <-- 删除
// import com.android.dx.command.dexer.Main;           <-- 删除

public class AndroidScriptCompiler implements IScriptCompiler {
    private final File cacheDir;
    private final File dexOutputDir;

    public AndroidScriptCompiler(Context context) {
        this.cacheDir = new File(context.getCacheDir(), "compiler_cache");
        // 【之前修复的】直接赋值，不用 new File(...)
        this.dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);

        if (!cacheDir.exists()) cacheDir.mkdirs();
        if (!dexOutputDir.exists()) dexOutputDir.mkdirs();
    }

    @Override
    public Class<?> compile(String className, String sourceCode) {
        try {
            Debug.log("[Compiler] 开始编译: " + className);

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

            // 3. 调用 ECJ (Java -> Class)
            StringWriter ecjLog = new StringWriter();
            PrintWriter ecjWriter = new PrintWriter(ecjLog);

            // 【关键修改】使用全名调用 ECJ
            org.eclipse.jdt.internal.compiler.batch.Main ecjCompiler = 
                new org.eclipse.jdt.internal.compiler.batch.Main(ecjWriter, ecjWriter, false, null, null);

            // 编译参数
            String[] ecjArgs = new String[] {
                "-1.7",                 // 目标版本
                "-nowarn",              // 不显示警告
                "-proc:none",           // 【关键修复】禁用注解处理，防止寻找 javax.annotation 包
                "-d", classDir.getAbsolutePath(), // 输出目录
                javaFile.getAbsolutePath() // 输入文件
            };

            boolean success = ecjCompiler.compile(ecjArgs);

            if (!success) {
                Debug.log("[Compiler] ECJ 编译失败:\n" + ecjLog.toString());
                return null;
            }

            // 4. 调用 DX (Class -> Dex)
            File dexFile = new File(cacheDir, "script.dex");
            if (dexFile.exists()) dexFile.delete();

            String[] dxArgs = new String[] {
                "--dex",
                "--output=" + dexFile.getAbsolutePath(),
                classDir.getAbsolutePath()
            };

            // 【关键修改】使用全名调用 DX
            com.android.dx.command.dexer.Main.main(dxArgs);

            if (!dexFile.exists()) {
                Debug.log("[Compiler] DX 转换失败，未生成 dex 文件");
                return null;
            }

            // 5. 动态加载
            DexClassLoader loader = new DexClassLoader(
                dexFile.getAbsolutePath(),
                dexOutputDir.getAbsolutePath(),
                null,
                getClass().getClassLoader()
            );

           	Debug.log("[Compiler] 加载类成功！");
            return loader.loadClass(className);

        } catch (Exception e) {
            e.printStackTrace();
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
