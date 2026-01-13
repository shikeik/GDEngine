package com.goldsprite.gdengine.core.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import java.io.BufferedReader;
import java.io.IOException;

public class LogParser {

	public enum EntryType {
		OVERVIEW,   // ## [Overview] ...
		CATEGORY,   // ## [Plan] ...
		VERSION     // ### v1.0 ...
	}

	public static class LogEntry {
		public String title;
		public EntryType type;
		public StringBuilder content = new StringBuilder();
		// 用于构建树形结构的引用
		public LogEntry parentCategory;

		public LogEntry(String title, EntryType type) {
			this.title = title;
			this.type = type;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	/**
	 * 解析日志文件
	 * @param handle 文件句柄
	 * @return 解析出的节点列表 (扁平列表，包含 Category 和 Version，需由 UI 组装成树)
	 */
	public static Array<LogEntry> parse(FileHandle handle) {
		Array<LogEntry> entries = new Array<>();
		if (handle == null || !handle.exists()) return entries;

		try (BufferedReader reader = new BufferedReader(handle.reader("UTF-8"))) {
			String line;
			LogEntry currentEntry = null;
			LogEntry currentCategory = null;

			while ((line = reader.readLine()) != null) {
				line = line.trim();

				// 1. 识别一级标题 (Category / Overview)
				// 格式: ## [Type] Title
				if (line.startsWith("## ")) {
					String rawTitle = line.substring(3).trim(); // 去掉 "## "

					// 提取类型标识 [Plan], [Current], [History], [Overview]
					String typeTag = "";
					if (rawTitle.startsWith("[")) {
						int endIdx = rawTitle.indexOf("]");
						if (endIdx != -1) {
							typeTag = rawTitle.substring(1, endIdx);
							// rawTitle = rawTitle.substring(endIdx + 1).trim(); // 保留完整标题更好看
						}
					}

					EntryType type = "Overview".equalsIgnoreCase(typeTag) ? EntryType.OVERVIEW : EntryType.CATEGORY;

					currentEntry = new LogEntry(rawTitle, type);
					entries.add(currentEntry);

					// 如果是分类节点，记录为当前父级
					if (type == EntryType.CATEGORY) {
						currentCategory = currentEntry;
					} else {
						currentCategory = null; // Overview 没有子节点
					}
				}
				// 2. 识别二级标题 (Version)
				// 格式: ### `v1.10.x` Title
				else if (line.startsWith("### ")) {
					String verTitle = line.substring(4).trim();
					// 去掉 Markdown 的反引号 `
					verTitle = verTitle.replace("`", "");

					currentEntry = new LogEntry(verTitle, EntryType.VERSION);
					currentEntry.parentCategory = currentCategory; // 绑定父级
					entries.add(currentEntry);
				}
				// 3. 内容行
				else {
					if (currentEntry != null) {
						// 简单的 Markdown -> RichText 转换
						String richLine = processLine(line);
						currentEntry.content.append(richLine).append("\n");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return entries;
	}

	private static String processLine(String line) {
		if (line.isEmpty()) return "";

		// 列表项 Checkbox
		if (line.startsWith("- [ ]")) {
			line = line.replace("- [ ]", "[color=gray]□[/color]");
		} else if (line.startsWith("- [x]")) {
			line = line.replace("- [x]", "[color=green]■[/color]");
		} else if (line.startsWith("- ")) {
			line = line.replaceFirst("- ", " • ");
		}

		// 关键词高亮 (Regex)
		line = line.replaceAll("\\[New\\]", "[color=green][New][/color]");
		line = line.replaceAll("\\[Fix\\]", "[color=salmon][Fix][/color]");
		line = line.replaceAll("\\[Adj\\]", "[color=gold][Adj][/color]");
		line = line.replaceAll("\\[Refactor\\]", "[color=orange][Refactor][/color]");

		// 粗体 **text** -> [color=white]text[/color] (或者其他高亮色)
		// 简单正则替换，暂不支持嵌套
		line = line.replaceAll("\\*\\*(.*?)\\*\\*", "[color=cyan]$1[/color]");

		// 代码块 `code` -> [color=gray]code[/color]
		line = line.replaceAll("`(.*?)`", "[color=light_gray]$1[/color]");

		return line;
	}
}
