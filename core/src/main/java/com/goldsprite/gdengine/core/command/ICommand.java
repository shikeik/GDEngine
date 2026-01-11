package com.goldsprite.gdengine.core.command;

public interface ICommand {
	/**
	 * 执行命令逻辑
	 */
	void execute();

	/**
	 * 撤销命令逻辑
	 */
	void undo();

	/**
	 * 获取命令名称（用于 UI 显示）
	 * e.g. "Move Object", "Change Color"
	 */
	String getName();
	
	/**
	 * 获取操作来源（可选）
	 * e.g. "Gizmo", "Inspector", "Shortcut"
	 */
	default String getSource() { return "System"; }
	
	/**
	 * 获取图标文本或ID（可选）
	 */
	default String getIcon() { return "CMD"; }
}
