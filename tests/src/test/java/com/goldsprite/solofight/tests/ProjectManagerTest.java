package com.goldsprite.solofight.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.screens.ecs.hub.GDEngineHubScreen.ProjectManager;
import com.goldsprite.solofight.CLogAssert;
import com.goldsprite.solofight.GdxTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(GdxTestRunner.class)
public class ProjectManagerTest {

	// 测试沙盒根目录
	private FileHandle sandboxRoot;
	private FileHandle templatesDir;
	private FileHandle projectsDir;
	private FileHandle libsDir;

	@Before
	public void setUp() {
		System.out.println("\n>>> [Setup] 初始化测试环境...");

		// 1. 建立沙盒目录
		sandboxRoot = Gdx.files.local("build/test_sandbox");
		if (sandboxRoot.exists()) sandboxRoot.deleteDirectory();
		sandboxRoot.mkdirs();

		// [修复] 将 Mock 的 Internal 根目录指向沙盒
		// 这样 ProjectManager 中的 Gdx.files.internal("engine/libs") 就会指向 build/test_sandbox/engine/libs
		GdxTestRunner.mockAssetsRoot = sandboxRoot.file().getAbsolutePath();

		FileHandle engineRoot = sandboxRoot;
		templatesDir = engineRoot.child("engine/templates");
		libsDir = engineRoot.child("engine/libs");
		projectsDir = engineRoot.child("UserProjects");

		templatesDir.mkdirs();
		projectsDir.mkdirs();
		libsDir.mkdirs();

		// 2. Mock 引擎配置
		GDEngineConfig.initialize(engineRoot.file().getAbsolutePath());
		Gd.engineConfig = GDEngineConfig.getInstance();

		System.out.println("Config Root: " + Gd.engineConfig.getActiveEngineRoot());
		System.out.println("Projects Dir: " + Gd.engineConfig.getProjectsDir().file().getAbsolutePath());

		// 3. 创建伪造的依赖库
		libsDir.child("test-lib-1.0.jar").writeString("dummy jar content", false);

		System.out.println("✅ 沙盒就绪");
	}

	@After
	public void tearDown() {
		// [修复] 清理 Mock 状态
		GdxTestRunner.mockAssetsRoot = null;
	}

	/**
	 * 核心测试：验证项目创建全流程
	 * 涵盖：文件复制、包名重构(路径+内容)、配置注入
	 */
	@Test
	public void testCreateProject() {
		System.out.println(">>> 验证: ProjectManager.createProject");

		// ---------------------------------------------------------
		// A. 准备测试模板 (Mock Template)
		// ---------------------------------------------------------
		String tplName = "TestTpl";
		FileHandle tplDir = templatesDir.child(tplName);

		// 1. 元数据
		tplDir.child("template.json").writeString(
			"{ \"displayName\": \"Test Template\", \"originEntry\": \"com.origin.app.Main\" }", false);

		// 2. 项目配置
		tplDir.child("project.json").writeString(
			"{ \"name\": \"${PROJECT_NAME}\", \"entryClass\": \"com.origin.app.Main\", \"version\": \"1.0\" }", false);

		// 3. 源码 (标准结构)
		// src/main/java/com/origin/app/Main.java
		FileHandle srcFile = tplDir.child("src/main/java/com/origin/app/Main.java");
		srcFile.parent().mkdirs();
		srcFile.writeString(
			"package com.origin.app;\n" +
				"import com.origin.app.Utils;\n" +
				"public class Main { void start() { System.out.println(\"Hello\"); } }",
			false
		);

		// 4. 资源
		tplDir.child("assets/logo.png").writeString("png_data", false);

		// 构建 TemplateInfo 对象
		ProjectManager.TemplateInfo tmplInfo = new ProjectManager.TemplateInfo();
		tmplInfo.id = tplName;
		tmplInfo.dirHandle = tplDir;
		tmplInfo.originEntry = "com.origin.app.Main";
		tmplInfo.version = "1.0";

		// ---------------------------------------------------------
		// B. 执行创建
		// ---------------------------------------------------------
		String targetName = "MyNewGame";
		String targetPkg = "com.target.game";

		String error = ProjectManager.createProject(tmplInfo, targetName, targetPkg);
		CLogAssert.assertEquals("创建过程应无错误返回", null, error);

		// ---------------------------------------------------------
		// C. 验证结果 (Assertions)
		// ---------------------------------------------------------
		FileHandle projectRoot = projectsDir.child(targetName);

		// 1. 验证根目录存在
		CLogAssert.assertTrue("项目根目录应存在", projectRoot.exists());

		// 2. 验证依赖库注入
		CLogAssert.assertTrue("引擎库应被注入", projectRoot.child("engine/libs/test-lib-1.0.jar").exists());

		// 3. 验证资源复制
		CLogAssert.assertTrue("Assets应被复制", projectRoot.child("assets/logo.png").exists());

		// 4. [关键] 验证源码路径重构
		// 期望: src/main/java/com/target/game/Main.java
		FileHandle targetSrcFile = projectRoot.child("src/main/java/com/target/game/Main.java");
		CLogAssert.assertTrue("Java文件路径应重构为新包名", targetSrcFile.exists());

		// 验证旧路径不存在
		CLogAssert.assertFalse("旧包名路径不应存在", projectRoot.child("src/main/java/com/origin").exists());

		// 5. [关键] 验证源码内容重构 (Package Refactoring)
		String codeContent = targetSrcFile.readString();
		System.out.println("Refactored Code:\n" + codeContent);

		CLogAssert.assertTrue("package 声明应被修改", codeContent.contains("package " + targetPkg + ";"));
		CLogAssert.assertTrue("import 语句应被修改", codeContent.contains("import " + targetPkg + ".Utils;"));
		CLogAssert.assertFalse("原始包名不应残留", codeContent.contains("com.origin.app"));

		// 6. 验证 project.json 注入
		String configContent = projectRoot.child("project.json").readString();
		System.out.println("Refactored Config:\n" + configContent);

		CLogAssert.assertTrue("项目名应被替换", configContent.contains("\"name\": \"MyNewGame\""));
		CLogAssert.assertTrue("模板源信息应被注入", configContent.contains("\"sourceName\": \"TestTpl\""));

		// 验证 project.json 里的 entryClass 文本替换是否生效 (虽然我们使用了 json 对象注入，但文本替换是第一步)
		// ProjectManager 先做了文本替换，再做了 JSON 解析覆盖。
		// 我们的 createProject 逻辑里，对于 project.json 主要是 JSON 解析注入。
		// 但 Main.java 的 package 声明是纯文本替换。
	}

	@Test
	public void testNestedDirectoryHandling() {
		System.out.println(">>> 验证: 嵌套目录与非源码文件处理");

		// 准备模板
		String tplName = "ComplexTpl";
		FileHandle tplDir = templatesDir.child(tplName);

		// 1. src 下的非 java 资源 (例如 src/main/resources/config.xml)
		// 预期：原样复制，路径保留
		tplDir.child("src/main/resources/data.xml").writeString("<data></data>", false);

		// 2. 根目录下的其他文件夹 (例如 docs/)
		tplDir.child("docs/readme.txt").writeString("info", false);

		ProjectManager.TemplateInfo info = new ProjectManager.TemplateInfo();
		info.id = tplName;
		info.dirHandle = tplDir;
		// 即使没有 originEntry，也应该能正常复制文件
		info.originEntry = "";

		ProjectManager.createProject(info, "ComplexGame", "com.test");

		FileHandle proj = projectsDir.child("ComplexGame");

		CLogAssert.assertTrue("src下的资源文件应保留结构", proj.child("src/main/resources/data.xml").exists());
		CLogAssert.assertTrue("普通文件夹应复制", proj.child("docs/readme.txt").exists());
	}

	/**
	 * 验证多文件引用替换
	 * 场景：Template 包含 Utils.java 和 Main.java
	 * Main.java import 了 Utils
	 * 验证：生成后 Main.java 的 import 是否指向了新包名下的 Utils
	 */
	@Test
	public void testMultiFileRefactoring() {
		System.out.println(">>> 验证: 多文件引用重构 (Import Updates)");

		String tplName = "MultiFileTpl";
		FileHandle tplDir = templatesDir.child(tplName);

		String originPkg = "com.old.lib";
		String targetPkg = "com.new.game";

		// 1. 准备 Template 元数据
		tplDir.child("template.json").writeString(
			"{ \"displayName\": \"Refactor Test\", \"originEntry\": \""+originPkg+".Main\" }", false);
		tplDir.child("project.json").writeString("{}", false); // 哑文件

		// 2. 创建文件 A: Utils.java
		FileHandle fileUtils = tplDir.child("src/main/java/com/old/lib/util/Helper.java");
		fileUtils.parent().mkdirs();
		fileUtils.writeString(
			"package com.old.lib.util;\n" +
				"public class Helper { public static int add(int a, int b) { return a+b; } }",
			false
		);

		// 3. 创建文件 B: Main.java (引用了 A)
		FileHandle fileMain = tplDir.child("src/main/java/com/old/lib/Main.java");
		fileMain.parent().mkdirs();
		fileMain.writeString(
			"package com.old.lib;\n" +
				"// 这里的 Import 必须被修改\n" +
				"import com.old.lib.util.Helper;\n" +
				"public class Main {\n" +
				"    void run() { Helper.add(1,1); }\n" +
				"}",
			false
		);

		// 4. 构建 TemplateInfo
		ProjectManager.TemplateInfo info = new ProjectManager.TemplateInfo();
		info.id = tplName;
		info.dirHandle = tplDir;
		info.originEntry = originPkg + ".Main";

		// 5. 执行创建
		ProjectManager.createProject(info, "RefactorGame", targetPkg);

		// 6. 验证
		FileHandle projRoot = projectsDir.child("RefactorGame");

		// 验证文件物理位置是否正确移动
		// com/old/lib/util/Helper.java -> com/new/game/util/Helper.java
		FileHandle targetHelper = projRoot.child("src/main/java/com/new/game/util/Helper.java");
		CLogAssert.assertTrue("Helper文件应移动到新包路径", targetHelper.exists());

		// 验证 Main.java 内容
		FileHandle targetMain = projRoot.child("src/main/java/com/new/game/Main.java");
		String mainContent = targetMain.readString();

		System.out.println("Main.java Content:\n" + mainContent);

		// 核心断言：import com.old.lib.util.Helper -> import com.new.game.util.Helper
		String expectedImport = "import " + targetPkg + ".util.Helper;";
		CLogAssert.assertTrue("Main中的import应更新为新包名", mainContent.contains(expectedImport));

		// 验证 Helper.java 内容 (自身的 package 声明)
		String helperContent = targetHelper.readString();
		String expectedPackage = "package " + targetPkg + ".util;";
		CLogAssert.assertTrue("Helper中的package应更新为新包名", helperContent.contains(expectedPackage));
	}
}
