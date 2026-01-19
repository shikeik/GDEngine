// 文件: core/src/main/java/com/goldsprite/gdengine/core/project/ProjectService.java
package com.goldsprite.gdengine.core.project;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.BuildConfig;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.log.Debug;

import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.Gdx;

/**
 * 项目管理服务 (Model 层核心)
 * <p>
 * 负责项目的增删改查以及**基于标准模板的项目生成**。
 * </p>
 */
public class ProjectService {
	private static ProjectService instance;
	private FileHandle currentProject;
	private final Json json;

	// =================================================================================
	//  [Centralized Configuration] 标准模板规则集
	//  所有占位符和路径映射都在这里定义，修改规则只需改这里。
	// =================================================================================
	private static final class TemplateRules {
		// --- 占位符定义 ---
		static final String KEY_PROJECT_NAME = "${PROJECT_NAME}";
		static final String KEY_PACKAGE = "${PACKAGE}";
		static final String KEY_MAIN_CLASS = "${MAIN_CLASS}";     // SimpleName (e.g. "Main")
		static final String KEY_ENGINE_VERSION = "${ENGINE_VERSION}";

		// --- 路径定义 ---
		// 引擎内部资源路径 (Release模式下位于 assets/engine/...)
		static final String INTERNAL_LIBS = "engine/libs";
		static final String INTERNAL_TEMPLATES_ROOT = "engine/templates";

		// 模板内部特殊目录
		static final String TPL_SCRIPTS_DIR = "scripts"; // 存放源码的目录

		// 目标项目结构
		static final String TARGET_SRC_ROOT = "src/main/java";
		static final String TARGET_ASSETS = "assets";
		static final String TARGET_LIBS = "libs";
	}

	private ProjectService() {
		json = new Json();
		json.setIgnoreUnknownFields(true);
		json.setOutputType(JsonWriter.OutputType.json);
	}

	public static ProjectService inst() {
		if (instance == null) instance = new ProjectService();
		return instance;
	}

	public FileHandle getCurrentProject() { return currentProject; }
	public void setCurrentProject(FileHandle project) { this.currentProject = project; }

	// =========================================================================================
	// 查询逻辑 (Read)
	// =========================================================================================

	public Array<FileHandle> listProjects() {
		if (Gd.engineConfig == null) return new Array<>();
		FileHandle root = Gd.engineConfig.getProjectsDir();
		if (!root.exists()) root.mkdirs();

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
	 * 扫描所有可用模板 (内置 + 本地下载)
	 */
	public Array<TemplateInfo> listTemplates() {
		Array<TemplateInfo> list = new Array<>();

		// 1. 扫描内置模板 (Internal)
		FileHandle internalRoot = Gd.files.internal(TemplateRules.INTERNAL_TEMPLATES_ROOT);
		if (!internalRoot.exists()) {
			// Fallback: 开发环境可能在 assets/ 下
			internalRoot = Gd.files.internal("assets/" + TemplateRules.INTERNAL_TEMPLATES_ROOT);
		}
		scanTemplates(internalRoot, list);

		// 2. [新增] 扫描本地下载模板 (Local)
		// 路径: <EngineRoot>/LocalTemplates
		if (Gd.engineConfig != null) {
			String activeRoot = Gd.engineConfig.getActiveEngineRoot();
			// 如果未初始化，尝试使用推荐路径作为兜底
			if (activeRoot == null) activeRoot = com.goldsprite.gdengine.core.config.GDEngineConfig.getRecommendedRoot();

			if (activeRoot != null) {
				FileHandle localRoot = Gdx.files.absolute(activeRoot).child("LocalTemplates");
				scanTemplates(localRoot, list);
			}
		}

		return list;
	}

	/**
	 * [新增] 通用扫描逻辑
	 * @param root 模板根目录 (例如 engine/templates 或 LocalTemplates)
	 * @param list 结果列表
	 */
	private void scanTemplates(FileHandle root, Array<TemplateInfo> list) {
		if (root == null || !root.exists()) return;

		for (FileHandle dir : root.list()) {
			if (!dir.isDirectory()) continue;

			TemplateInfo info = new TemplateInfo();
			info.id = dir.name();
			info.dirHandle = dir; // 暂存 Handle 用于复制

			// 读取元数据
			FileHandle metaFile = dir.child("template.json");
			if (metaFile.exists()) {
				try {
					TemplateInfo meta = json.fromJson(TemplateInfo.class, metaFile);
					info.displayName = meta.displayName;
					info.description = meta.description;
					// info.originEntry 已废弃，不再读取
					info.version = meta.version;
					info.engineVersion = meta.engineVersion;
				} catch (Exception e) {
					info.displayName = info.id + " (Error)";
					Debug.logT("ProjectService", "Template parse error: " + dir.path());
				}
			} else {
				// 没有元数据时的兜底
				info.displayName = info.id;
			}
			list.add(info);
		}
	}

	// =========================================================================================
	// 核心创建逻辑 (Create)
	// =========================================================================================

	/**
	 * 基于标准模板创建新项目
	 */
	public String createProject(TemplateInfo tmpl, String name, String packageName) {
		// 1. 基础校验
		if (name == null || name.trim().isEmpty()) return "Name cannot be empty.";
		if (!name.matches("[a-zA-Z0-9_]+")) return "Invalid project name.";
		if (packageName == null || packageName.trim().isEmpty()) return "Package cannot be empty.";

		// [新增] 包名校验
		if (!isValidPackageName(packageName)) return "Invalid Package Name (e.g. com.mygame)";
		
		FileHandle targetDir = Gd.engineConfig.getProjectsDir().child(name);
		if (targetDir.exists()) return "Project already exists!";

		Debug.logT("ProjectService", "Creating '%s' from template '%s'...", name, tmpl.id);

		// 2. 准备替换字典 (Replacements)
		Map<String, String> replacements = new HashMap<>();
		replacements.put(TemplateRules.KEY_PROJECT_NAME, name);
		replacements.put(TemplateRules.KEY_PACKAGE, packageName);
		replacements.put(TemplateRules.KEY_ENGINE_VERSION, BuildConfig.DEV_VERSION);
		// KEY_MAIN_CLASS 会在处理 scripts 时动态获取

		// 3. 开始构建流程
		try {
			targetDir.mkdirs();

			// Step A: 复制模板根目录下的通用配置 (build.gradle, settings.gradle)
			// 这些文件位于 engine/templates/ 下，与具体模板同级
			FileHandle templatesRoot = tmpl.dirHandle.parent();
			processFile(templatesRoot.child("build.gradle"), targetDir.child("build.gradle"), replacements);
			processFile(templatesRoot.child("settings.gradle"), targetDir.child("settings.gradle"), replacements);

			// Step B: 处理具体模板内容 (HelloGame/...)
			processTemplateContent(tmpl.dirHandle, targetDir, replacements);

			// Step C: 注入引擎库 (Libs)
			injectEngineLibs(targetDir);

			// Step D: 创建标准目录结构 & [新增] 注入默认资源 (gd_icon.png)
			FileHandle assetsDir = targetDir.child(TemplateRules.TARGET_ASSETS);
			assetsDir.mkdirs();
			injectDefaultAssets(assetsDir);

			return null; // Success

		} catch (Exception e) {
			e.printStackTrace();
			// 回滚：清理失败的目录
			if (targetDir.exists()) targetDir.deleteDirectory();
			return "Creation Failed: " + e.getMessage();
		}
	}

	private void processTemplateContent(FileHandle sourceDir, FileHandle targetDir, Map<String, String> replacements) {
		for (FileHandle file : sourceDir.list()) {
			String fileName = file.name();

			// 忽略元数据文件
			if (fileName.equals("template.json") || fileName.equals("preview.png")) continue;

			// 特殊处理 1: scripts 目录 -> src/main/java/package/
			if (file.isDirectory() && fileName.equals(TemplateRules.TPL_SCRIPTS_DIR)) {
				processScriptsDir(file, targetDir, replacements);
				continue;
			}

			// 特殊处理 2: project.json (需要替换内容)
			if (!file.isDirectory() && fileName.equals("project.json")) {
				// 【关键】确保 MAIN_CLASS 有默认值
				// 因为 scripts 目录的扫描可能在 project.json 之后，或者文件名为 GameEntry.java 等
				// 简单起见，我们强制约定模板的入口文件名必须是 Main.java，或者在这里给个默认值
				replacements.put(TemplateRules.KEY_MAIN_CLASS, "Main");

				processFile(file, targetDir.child(fileName), replacements);
				continue;
			}
			
			// 普通文件/目录：递归复制
            if (file.isDirectory()) {
                // 递归暂不支持普通文件夹内的文本替换，直接拷贝
                // [修复] 直接拷贝到 targetDir 下，LibGDX 会自动以 file.name() (即 "assets") 命名
                // 这样避免了 "target/assets" 存在时变成 "target/assets/assets" 的问题
                file.copyTo(targetDir); 
            } else {
				// 如果是文本文件，尝试替换；否则直接拷贝
				// 这里假设模板里的根文件都是文本配置
				processFile(file, targetDir.child(fileName), replacements);
			}
		}
	}

	/**
	 * 处理源码目录迁移
	 * source: HelloGame/scripts/
	 * target: MyGame/src/main/java/com/my/game/
	 */
	private void processScriptsDir(FileHandle scriptsDir, FileHandle projectRoot, Map<String, String> replacements) {
		String pkgPath = replacements.get(TemplateRules.KEY_PACKAGE).replace('.', '/');
		FileHandle javaRoot = projectRoot.child(TemplateRules.TARGET_SRC_ROOT).child(pkgPath);
		javaRoot.mkdirs();

		for (FileHandle srcFile : scriptsDir.list()) {
			if (srcFile.isDirectory()) continue; // 暂不处理脚本内的子文件夹，保持简单

			// 动态注入 MAIN_CLASS 变量 (以文件名为主)
			String className = srcFile.nameWithoutExtension();
			// 创建一个临时的 map 副本，针对当前文件注入类名
			Map<String, String> localReplacements = new HashMap<>(replacements);
			localReplacements.put(TemplateRules.KEY_MAIN_CLASS, className);

			processFile(srcFile, javaRoot.child(srcFile.name()), localReplacements);
		}
	}

	/**
	 * 注入引擎依赖库 (engine/libs -> project/libs)
	 */
	private void injectEngineLibs(FileHandle projectRoot) {
		// 寻找源
		FileHandle sourceLibs = Gd.files.internal(TemplateRules.INTERNAL_LIBS);
		if (!sourceLibs.exists()) sourceLibs = Gd.files.internal("assets/" + TemplateRules.INTERNAL_LIBS);

		if (sourceLibs.exists()) {
			FileHandle targetLibs = projectRoot.child(TemplateRules.TARGET_LIBS);
			targetLibs.mkdirs();
			for (FileHandle jar : sourceLibs.list(".jar")) {
				jar.copyTo(targetLibs);
			}
		} else {
			Debug.logT("ProjectService", "⚠️ Engine libs not found at " + TemplateRules.INTERNAL_LIBS);
		}
	}

	/**
	 * 注入默认资源 (如图标) 到用户项目的 assets 目录
	 */
	private void injectDefaultAssets(FileHandle targetAssetsDir) {
		// 定义需要默认拷贝的文件列表
		String[] defaultAssets = {
			"gd_icon.png"
			// 未来如果有其他默认图（如 default_font.fnt），加在这里
		};

		for (String path : defaultAssets) {
			// 从引擎内部资源寻找
			FileHandle src = Gd.files.internal(path);
			if (src.exists()) {
				try {
					src.copyTo(targetAssetsDir.child(path));
				} catch (Exception e) {
					Debug.logT("ProjectService", "⚠️ Failed to copy default asset: " + path);
				}
			} else {
				Debug.logT("ProjectService", "⚠️ Default asset source not found: " + path);
			}
		}
	}

	/**
	 * 文本替换与写入
	 */
	private void processFile(FileHandle source, FileHandle target, Map<String, String> replacements) {
		if (!source.exists()) return;

		try {
			String content = source.readString("UTF-8");

			// 执行所有替换
			for (Map.Entry<String, String> entry : replacements.entrySet()) {
				// 使用 replace (非 regex) 提高性能且避免转义问题
				content = content.replace(entry.getKey(), entry.getValue());
			}

			target.writeString(content, false, "UTF-8");
		} catch (Exception e) {
			Debug.logT("ProjectService", "Error processing file " + source.name() + ": " + e.getMessage());
		}
	}

	public void deleteProject(FileHandle projectDir) {
		if (projectDir.exists()) {
			projectDir.deleteDirectory();
			Debug.logT("ProjectService", "Deleted project: " + projectDir.name());
		}
	}
	
	/**
	 * 校验 Java 包名合法性
	 */
	public static boolean isValidPackageName(String pkg) {
		if (pkg == null || pkg.isEmpty()) return false;
		// 规则:
		// 1. 必须以字母或下划线开头
		// 2. 只能包含字母、数字、下划线、点
		// 3. 点不能在开头或结尾，不能连续
		// 4. (可选) 检查 Java 关键字 (如 int, class) 这里暂简化处理

		// 简单正则：首字母[a-zA-Z_]，后续[a-zA-Z0-9_]，以点分隔
		String regex = "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$";
		return pkg.matches(regex);
	}
}
