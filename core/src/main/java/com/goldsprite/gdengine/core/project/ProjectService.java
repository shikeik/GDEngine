package com.goldsprite.gdengine.core.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.project.model.ProjectConfig;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.log.Debug;

/**
 * 项目管理服务 (Model 层核心)
 * <p>
 * 职责：负责所有与文件系统交互、项目增删改查的逻辑。
 * 架构：单例模式 (Singleton)，供 Presenter 层调用。
 * 原则：绝对不包含任何 UI 代码 (VisUI/Scene2D)。
 * </p>
 */
public class ProjectService {
	private static ProjectService instance;
	// 当前打开的项目 (运行时状态)
	private FileHandle currentProject;
	private final Json json;

	// 私有构造，单例模式
	private ProjectService() {
		json = new Json();
		json.setIgnoreUnknownFields(true); // 忽略未知的 JSON 字段，防止版本差异导致崩溃
		json.setOutputType(JsonWriter.OutputType.json);
	}

	public static ProjectService inst() {
		if (instance == null) instance = new ProjectService();
		return instance;
	}

	public FileHandle getCurrentProject() {
		return currentProject;
	}

	public void setCurrentProject(FileHandle project) {
		this.currentProject = project;
	}

	// =========================================================================================
	// 查询逻辑 (Read)
	// =========================================================================================

	/**
	 * 扫描用户项目目录，返回所有项目文件夹
	 */
	public Array<FileHandle> listProjects() {
		// 防御性检查：如果引擎配置未加载，返回空列表而不是抛异常
		if (Gd.engineConfig == null) {
			return new Array<>();
		}

		FileHandle root = Gd.engineConfig.getProjectsDir();
		FileHandle[] files = root.list();
		Array<FileHandle> projects = new Array<>();

		if (files != null) {
			for (FileHandle f : files) {
				if (f.isDirectory()) projects.add(f);
			}
		}
		return projects;
	}

	/**
	 * 扫描模板 (合并 内置模板 + 用户导出模板)
	 */
	public Array<TemplateInfo> listTemplates() {
		Array<TemplateInfo> list = new Array<>();

		// 1. 内置模板 (Internal)
		scanTemplates(Gd.files.internal("engine/templates"), list);

		// 2. [新增] 用户导出模板 (Local)
		// 对应 TemplateExporter 导出的位置
		FileHandle localTpl = Gd.files.local("LocalTemplates");
		if (localTpl.exists()) {
			scanTemplates(localTpl, list);
		}

		return list;
	}

	// 提取公共扫描逻辑
	private void scanTemplates(FileHandle root, Array<TemplateInfo> list) {
		if (!root.exists()) return;

		for (FileHandle dir : root.list()) {
			if (!dir.isDirectory()) continue;

			TemplateInfo info = new TemplateInfo();
			info.id = dir.name();
			info.dirHandle = dir;

			FileHandle metaFile = dir.child("template.json");
			if (metaFile.exists()) {
				try {
					TemplateInfo meta = new Json().fromJson(TemplateInfo.class, metaFile);
					info.displayName = meta.displayName;
					info.description = meta.description;
					info.originEntry = meta.originEntry;
					info.version = meta.version;
					info.engineVersion = meta.engineVersion;
				} catch (Exception e) {
					info.displayName = info.id + " (Error)";
				}
			} else {
				info.displayName = info.id;
				info.originEntry = "com.game.Main";
			}
			list.add(info);
		}
	}

	// =========================================================================================
	// 操作逻辑 (Write)
	// =========================================================================================

	/**
	 * 创建新项目
	 * 核心流程：验证 -> 创建临时目录 -> 复制模板并重构代码 -> 注入依赖 -> 移动到最终目录
	 *
	 * @param tmpl 选中的模板
	 * @param name 项目名称
	 * @param packageName 目标包名 (如 com.user.game)
	 * @return 错误信息字符串，成功则返回 null
	 */
	public String createProject(TemplateInfo tmpl, String name, String packageName) {
		// 1. 基础校验
		if (name == null || name.trim().isEmpty()) return "Name cannot be empty.";
		if (!name.matches("[a-zA-Z0-9_]+")) return "Invalid project name (Alphanumeric only).";
		if (packageName == null || packageName.trim().isEmpty()) return "Package cannot be empty.";

		FileHandle finalTarget = Gd.engineConfig.getProjectsDir().child(name);
		if (finalTarget.exists()) return "Project already exists!";

		// 解析原始包名 (用于代码重构替换)
		String originPkg = "";
		if (tmpl.originEntry != null && tmpl.originEntry.contains(".")) {
			originPkg = tmpl.originEntry.substring(0, tmpl.originEntry.lastIndexOf('.'));
		}
		String targetPkg = packageName;

		Debug.logT("ProjectService", "Creating project '%s' from template '%s'", name, tmpl.id);

		// 2. 创建临时构建目录 (原子操作保证：要么全成功，要么全失败不留垃圾)
		FileHandle tempRoot = Gdx.files.local("build/tmp_creation").child(name);
		if (tempRoot.exists()) tempRoot.deleteDirectory();
		tempRoot.mkdirs();

		try {
			// 3. 处理模板核心文件 (src, assets, project.json)
			processRootDirectory(tmpl.dirHandle, tempRoot, originPkg, targetPkg, name, tmpl);

			// 4. [新增] 注入通用构建脚本 (来自模板根目录的上一级，即 engine/templates/)
			// 这样所有模板共享一套 build.gradle，便于引擎维护
			FileHandle templatesRoot = tmpl.dirHandle.parent();
			copyIfExists(templatesRoot.child("build.gradle"), tempRoot.child("build.gradle"), null);
			copyIfExists(templatesRoot.child("settings.gradle"), tempRoot.child("settings.gradle"),
				content -> content.replace("${PROJECT_NAME}", name));

			// 5. [新增] 注入引擎依赖库 (libs/)
			// 自动探测：开发环境(assets/engine/libs) vs 发布环境(engine/libs)
			FileHandle libsSource = Gd.files.internal("engine/libs");
			if (!libsSource.exists()) libsSource = Gd.files.internal("assets/engine/libs");

			FileHandle libsTarget = tempRoot.child("libs");
			libsTarget.mkdirs();
			// 复制所有 jar 包
			if (libsSource.exists()) {
				for (FileHandle jar : libsSource.list(".jar")) {
					jar.copyTo(libsTarget);
				}
			}

			// 6. 交付：将准备好的临时目录移动到 UserProjects
			tempRoot.copyTo(finalTarget.parent()); // copyTo 的参数是目标父目录
			tempRoot.deleteDirectory(); // 清理临时区

			return null; // 成功
		} catch (Exception e) {
			e.printStackTrace();
			// 失败回滚：清理临时目录
			if (tempRoot.exists()) tempRoot.deleteDirectory();
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * 删除项目
	 */
	public void deleteProject(FileHandle projectDir) {
		if (projectDir.exists()) {
			projectDir.deleteDirectory();
			Debug.logT("ProjectService", "Deleted project: " + projectDir.name());
		}
	}

	// =========================================================================================
	// 内部辅助逻辑 (Private Helpers)
	// =========================================================================================

	/** 辅助复制：如果源文件存在则复制，并支持文本处理 */
	private void copyIfExists(FileHandle source, FileHandle target, StringProcessor processor) {
		if (source.exists()) {
			String content = source.readString("UTF-8");
			if (processor != null) content = processor.process(content);
			target.writeString(content, false, "UTF-8");
		}
	}

	private interface StringProcessor { String process(String input); }

	/**
	 * 递归处理模板根目录
	 * 区分对待 src 源码目录和其他资源文件
	 */
	private void processRootDirectory(FileHandle sourceDir, FileHandle destDir, String originPkg, String targetPkg, String projName, TemplateInfo tmpl) {
		for (FileHandle file : sourceDir.list()) {
			// 跳过元数据和预览图，不复制到用户项目
			if (file.name().equals("template.json") || file.name().equals("preview.png")) continue;

			if (file.isDirectory()) {
				if (file.name().equals("src")) {
					// [核心] 源码目录：进入重构模式
					// 假设标准结构 src/main/java
					FileHandle srcJavaSource = file.child("main").child("java");
					if (srcJavaSource.exists()) {
						FileHandle srcJavaTarget = destDir.child("src").child("main").child("java");
						processSourceCode(srcJavaSource, srcJavaTarget, originPkg, targetPkg);
					} else {
						// 非标准结构？直接复制整个 src
						file.copyTo(destDir);
					}
				} else {
					// 其他目录 (如 assets)，直接复制
					file.copyTo(destDir);
				}
			} else {
				// 根目录下的文件处理
				if (file.name().equals("project.json")) {
					// 配置文件需特殊处理（注入模板信息）
					processProjectConfig(file, destDir.child("project.json"), targetPkg, projName, tmpl);
				} else if (file.name().equals("settings.gradle") || file.name().equals("build.gradle")) {
					// 如果模板自带构建脚本，替换其中的占位符
					String content = file.readString("UTF-8");
					content = content.replace("${PROJECT_NAME}", projName);
					if (!originPkg.isEmpty()) content = content.replace(originPkg, targetPkg);
					destDir.child(file.name()).writeString(content, false, "UTF-8");
				} else {
					// 其他文件原样复制
					file.copyTo(destDir);
				}
			}
		}
	}

	/**
	 * 核心源码处理：递归查找 .java 文件，重构目录结构并替换包名
	 * @param sourceRoot 模板源码根 (e.g. templates/MyGame/src/main/java)
	 * @param targetRoot 目标源码根 (e.g. UserProjects/NewGame/src/main/java)
	 */
	private void processSourceCode(FileHandle sourceRoot, FileHandle targetRoot, String originPkg, String targetPkg) {
		// 1. 递归收集所有 .java 文件
		Array<FileHandle> javaFiles = new Array<>();
		findJavaFiles(sourceRoot, javaFiles);

		// 准备路径替换前缀 (如 com/mygame -> com/user/newgame)
		String originPathPrefix = originPkg.replace('.', '/');
		String targetPathPrefix = targetPkg.replace('.', '/');

		for (FileHandle javaFile : javaFiles) {
			// 2. 计算相对路径
			String fullPath = javaFile.path();
			String rootPath = sourceRoot.path();
			String relativePath = fullPath.substring(rootPath.length());

			// 统一去头部的斜杠
			if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
				relativePath = relativePath.substring(1);
			}

			// 3. 路径重构 (Package Directory Refactoring)
			// 如果文件位于原始包路径下，将其移动到新包路径下
			String newRelativePath = relativePath;
			String checkPath = relativePath.replace('\\', '/'); // 统一分隔符比较

			if (!originPathPrefix.isEmpty() && checkPath.startsWith(originPathPrefix)) {
				newRelativePath = checkPath.replaceFirst(originPathPrefix, targetPathPrefix);
			}

			FileHandle targetFile = targetRoot.child(newRelativePath);

			// 4. 内容重构 (File Content Replacement)
			String content = javaFile.readString("UTF-8");
			if (!originPkg.isEmpty()) {
				// 替换 package 声明
				content = content.replace("package " + originPkg, "package " + targetPkg);
				// 替换 import 语句
				content = content.replace("import " + originPkg, "import " + targetPkg);
				// 替换代码中的其他引用
				content = content.replace(originPkg, targetPkg);
			}

			targetFile.writeString(content, false, "UTF-8");
		}
	}

	private void findJavaFiles(FileHandle dir, Array<FileHandle> out) {
		for (FileHandle f : dir.list()) {
			if (f.isDirectory()) findJavaFiles(f, out);
			else if (f.extension().equals("java")) out.add(f);
		}
	}

	/**
	 * 处理 project.json 配置文件
	 */
	private void processProjectConfig(FileHandle source, FileHandle target, String targetPkg, String projName, TemplateInfo tmpl) {
		try {
			String content = source.readString("UTF-8");

			// 1. 文本替换：先做通用包名替换
			if (tmpl.originEntry != null && tmpl.originEntry.contains(".")) {
				String originPkg = tmpl.originEntry.substring(0, tmpl.originEntry.lastIndexOf('.'));
				content = content.replace(originPkg, targetPkg);
			}

			// 2. JSON 对象修改：注入项目元数据
			ProjectConfig cfg = json.fromJson(ProjectConfig.class, content);
			cfg.name = projName;
			cfg.engineVersion = tmpl.engineVersion;

			// 记录使用了哪个模板 (便于后续追溯)
			ProjectConfig.TemplateRef ref = new ProjectConfig.TemplateRef();
			ref.sourceName = tmpl.id;
			ref.version = tmpl.version;
			ref.engineVersion = tmpl.engineVersion;
			cfg.template = ref;

			target.writeString(json.prettyPrint(cfg), false, "UTF-8");
		} catch (Exception e) {
			Debug.logT("ProjectService", "Config process failed, fallback copy: " + e.getMessage());
			// 如果解析失败，至少保证文件过去了
			source.copyTo(target);
		}
	}
}
