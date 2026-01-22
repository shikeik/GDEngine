package com.goldsprite.gdengine.core.project.model;

/**
 * 项目配置文件 (project.json) 的数据模型
 * 纯粹的数据容器，没有任何逻辑方法。
 */
public class ProjectConfig {
	public String name;
	public String entryClass;
	public TemplateRef template;
	public String engineVersion;

	public static class TemplateRef {
		public String sourceName;
		public String version;
		public String engineVersion;
	}
}