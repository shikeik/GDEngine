// 文件: core/src/main/java/com/goldsprite/gdengine/core/project/TemplateExporter.java
package com.goldsprite.gdengine.core.project;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.project.model.ProjectConfig;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.log.Debug;

import java.io.IOException;

public class TemplateExporter {

	/**
	 * 导出项目为模板
	 * @param sourceProject 源项目目录
	 * @param meta 模板元数据 (ID, Name, Desc, Version)
	 * @return 错误信息，成功返回 null
	 */
	public static String exportProject(FileHandle sourceProject, TemplateInfo meta) {
		// 1. 检查目标目录
		// 目标: assets/engine/templates/<TemplateID>
		// 注意：在 Desktop 开发环境下，我们希望导出到项目的 assets 源码目录，而不是构建后的 bin 目录
		// 这样重启后模板依然存在。
		// 这里为了通用，我们尝试写入 Gdx.files.local (如果是 Android) 或 相对路径 (Desktop)

		// 策略：导出到应用运行目录下的 "LocalTemplates" 文件夹 (模拟用户自定义模板区)
		// 或者如果我们在 Dev 模式，尝试写回 assets/engine/templates
		FileHandle templatesRoot = Gd.files.local("LocalTemplates");
		// 如果是 Desktop Dev 模式，可能希望直接写回源码 assets？暂时先写到 LocalTemplates 安全

		FileHandle targetDir = templatesRoot.child(meta.id);

		if (targetDir.exists()) {
			return "Template ID '" + meta.id + "' already exists!";
		}

		try {
			targetDir.mkdirs();
			Debug.logT("Exporter", "Exporting to: " + targetDir.file().getAbsolutePath());

			// 2. 获取源项目的入口信息 (Origin Entry)
			// 这对于后续创建项目时的包名替换至关重要
			FileHandle projConfig = sourceProject.child("project.json");
			String originEntry = "com.game.Main"; // 默认兜底
			if (projConfig.exists()) {
				try {
					ProjectConfig pc = new Json().fromJson(ProjectConfig.class, projConfig);
					if (pc.entryClass != null) originEntry = pc.entryClass;
				} catch (Exception e) {
					Debug.logT("Exporter", "Warning: Could not parse project.json for origin entry.");
				}
			}
			meta.originEntry = originEntry;

			// 3. 复制文件 (带过滤)
			copyDirectoryFiltered(sourceProject, targetDir);

			// 4. 生成 template.json
			Json json = new Json();
			json.setOutputType(JsonWriter.OutputType.json);
			FileHandle metaFile = targetDir.child("template.json");
			metaFile.writeString(json.prettyPrint(meta), false, "UTF-8");

			// 5. 复制预览图 (如果有)
			// 假设项目根目录下有个 screenshot.png 或者我们就用 icon
			FileHandle icon = sourceProject.child("assets/gd_icon.png"); // 示例
			if (icon.exists()) {
				icon.copyTo(targetDir.child("preview.png"));
			}

			return null; // Success

		} catch (Exception e) {
			e.printStackTrace();
			// 失败回滚：删除可能创建了一半的目录
			if (targetDir.exists()) targetDir.deleteDirectory();
			return "Export Error: " + e.getMessage();
		}
	}

	private static void copyDirectoryFiltered(FileHandle srcDir, FileHandle destDir) throws IOException {
		for (FileHandle file : srcDir.list()) {
			String name = file.name();

			// --- [黑名单过滤] ---
			if (name.startsWith(".")) continue; // .git, .gradle, .idea
			if (name.equals("build")) continue; // 构建产物
			if (name.equals("libs")) continue;  // 引擎库不复制 (创建时会重新注入)
			if (name.equals("local.properties")) continue;
			if (name.endsWith(".iml")) continue;
			// -------------------

			if (file.isDirectory()) {
				FileHandle newDest = destDir.child(name);
				newDest.mkdirs();
				copyDirectoryFiltered(file, newDest);
			} else {
				file.copyTo(destDir);
			}
		}
	}
}
