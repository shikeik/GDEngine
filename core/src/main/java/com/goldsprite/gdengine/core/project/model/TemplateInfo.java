package com.goldsprite.gdengine.core.project.model;

import com.badlogic.gdx.files.FileHandle;

/**
 * 模板信息数据模型
 */
public class TemplateInfo {
	public String id; // 文件夹名
	public String displayName;
	public String description;
	public String originEntry; // "com.mygame.Main"
	public String version;       // 模板自身版本
	public String engineVersion; // 适配的引擎版本
	
	// transient 表示这个字段不会被序列化到 json 中，只在内存中存在
	public transient FileHandle dirHandle; 
}