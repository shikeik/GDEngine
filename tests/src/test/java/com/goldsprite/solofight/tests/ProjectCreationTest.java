package com.goldsprite.solofight.tests;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.screens.ecs.hub.GDEngineHubScreen.ProjectManager;
import com.goldsprite.solofight.GdxTestRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(GdxTestRunner.class)
public class ProjectCreationTest {

    private FileHandle sandboxRoot;
    private FileHandle projectsDir;

    @Before
    public void setUp() {
        System.out.println("\n>>> [Setup] 初始化测试环境...");

        // 现在源码已经支持自动探测路径，我们不再需要 Hack Gdx.files 了！
        // 只需要确保 Gd 代理指向正确的 Gdx 实现即可
        Gd.files = Gdx.files;
        Gd.app = Gdx.app;
        
        // [Debug] 探测路径 (此时应该能自动找到了)
        System.out.println("Probe 'engine/templates': " + Gd.files.internal("engine/templates").exists());

        // 1. 建立沙盒目录
        sandboxRoot = com.badlogic.gdx.Gdx.files.local("build/test_sandbox_creation");
        if (sandboxRoot.exists()) sandboxRoot.deleteDirectory();
        sandboxRoot.mkdirs();

        // 模拟引擎目录结构
        FileHandle engineRoot = sandboxRoot;
        projectsDir = engineRoot.child("UserProjects");
        projectsDir.mkdirs();
        
        // 2. Mock 引擎配置
        GDEngineConfig mockConfig = new GDEngineConfig();
        mockConfig.customProjectsPath = projectsDir.file().getAbsolutePath();
        Gd.engineConfig = mockConfig;

        System.out.println("Config Root: " + Gd.engineConfig.getActiveEngineRoot());
    }

    @After
    public void tearDown() {
        // 可选清理
    }

    @Test
    public void testProjectCreationPipeline() {
        // 1. 扫描真实模板
        Array<ProjectManager.TemplateInfo> templates = ProjectManager.listTemplates();
        
        System.out.println("Found templates: " + templates.size);
        for(ProjectManager.TemplateInfo t : templates) {
            System.out.println(" - " + t.id + " (" + t.displayName + ")");
        }
        
        Assert.assertTrue("Should have at least one template", templates.size > 0);
        
        // 2. 选择 HelloGame 模板
        ProjectManager.TemplateInfo targetTemplate = null;
        for (ProjectManager.TemplateInfo t : templates) {
            if (t.id.equals("HelloGame")) {
                targetTemplate = t;
                break;
            }
        }
        if (targetTemplate == null) targetTemplate = templates.get(0);
        
        System.out.println("Selected template: " + targetTemplate.id);

        // 3. 执行创建
        String projName = "GenTestProject";
        String pkgName = "com.test.gen";
        
        String error = ProjectManager.createProject(targetTemplate, projName, pkgName);
        Assert.assertNull("Project creation should succeed: " + error, error);
        
        // 4. 验证产物
        FileHandle projDir = projectsDir.child(projName);
        Assert.assertTrue("Project directory should exist", projDir.exists());
        
        // 4.1 验证 libs 目录复制 (核心验证点)
        FileHandle libsDir = projDir.child("libs");
        FileHandle javadocJar = libsDir.child("gdengine-javadoc.jar");
        
        System.out.println("Checking for javadoc jar at: " + javadocJar.path());
        Assert.assertTrue("gdengine-javadoc.jar MUST be copied to user project!", javadocJar.exists());
        
        // 4.2 验证 build.gradle 魔法代码注入
        FileHandle buildGradle = projDir.child("build.gradle");
        Assert.assertTrue("build.gradle should exist", buildGradle.exists());
        
        String content = buildGradle.readString("UTF-8");
        System.out.println(">>> Actual build.gradle content:\n" + content + "\n<<< End Content"); // [Debug]

        boolean hasIdeaPlugin = content.contains("id 'idea'");
        boolean hasMagicScript = content.contains("withXml") && content.contains("JAVADOC");
        
        System.out.println("Checking build.gradle content for magic script...");
        Assert.assertTrue("build.gradle should contain 'idea' plugin", hasIdeaPlugin);
        Assert.assertTrue("build.gradle should contain magic XML injection script", hasMagicScript);
        
        System.out.println(">>> ✅ All Checks Passed! The pipeline is solid.");
    }
}
