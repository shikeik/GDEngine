package com.goldsprite.gdengine.core.utils;

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

			// [新增] 历史归档根节点标记
			LogEntry historyRoot = null;

			while ((line = reader.readLine()) != null) {
				line = line.trim();

				// 1. 识别二级标题 (Category / Overview)
				if (line.startsWith("## ")) {
					String rawTitle = line.substring(3).trim();

					// 类型判定
					String typeTag = "";
					if (rawTitle.startsWith("[")) {
						int endIdx = rawTitle.indexOf("]");
						if (endIdx != -1) {
							typeTag = rawTitle.substring(1, endIdx);
						}
					}

					boolean isHistoryHeader = typeTag.equalsIgnoreCase("History") || rawTitle.contains("历史");
					EntryType type = "Overview".equalsIgnoreCase(typeTag) ? EntryType.OVERVIEW : EntryType.CATEGORY;

					currentEntry = new LogEntry(rawTitle, type);

					// [核心逻辑] 归档处理
					if (isHistoryHeader) {
						// 这是一个历史总入口 (Top Level)
						historyRoot = currentEntry;
						currentCategory = currentEntry;
					} else if (historyRoot != null) {
						// 如果已经进入历史区，且这是一个普通的 ## 标题 (如 ## 1.9.x)
						// 它应该作为 historyRoot 的子节点
						currentEntry.parentCategory = historyRoot;
						// 同时它自己也是一个 Category，供后续的 ### 版本挂载
						currentCategory = currentEntry;
					} else {
						// 普通顶层节点 (Plan, Current, Overview)
						currentCategory = currentEntry;
					}

					entries.add(currentEntry);
				}
				// 2. 识别三级标题 (Version)
				else if (line.startsWith("### ")) {
					String verTitle = line.substring(4).trim().replace("`", "");

					currentEntry = new LogEntry(verTitle, EntryType.VERSION);
					currentEntry.parentCategory = currentCategory; // 挂载到最近的一个 ## 节点
					entries.add(currentEntry);
				}
				// 3. 内容行
				else {
					if (currentEntry != null) {
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

		// Checkbox
		if (line.startsWith("- [ ]")) line = line.replace("- [ ]", "[color=gray]□[/color]");
		else if (line.startsWith("- [x]")) line = line.replace("- [x]", "[color=green]■[/color]");
		else if (line.startsWith("- ")) line = line.replaceFirst("- ", " • ");

		// 关键词高亮 (Regex)
		line = line.replaceAll("\\[New\\]", "[color=green][New][/color]");
		line = line.replaceAll("\\[Fix\\]", "[color=salmon][Fix][/color]");
		line = line.replaceAll("\\[Adj\\]", "[color=gold][Adj][/color]");
		line = line.replaceAll("\\[Refactor\\]", "[color=orange][Refactor][/color]");

		// Bold & Code
		line = line.replaceAll("\\*\\*(.*?)\\*\\*", "[color=cyan]$1[/color]");

		// 代码块 `code` -> [color=gray]code[/color]
		line = line.replaceAll("`(.*?)`", "[color=light_gray]$1[/color]");

		return line;
	}
}
