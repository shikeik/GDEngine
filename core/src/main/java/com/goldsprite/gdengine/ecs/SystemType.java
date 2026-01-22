package com.goldsprite.gdengine.ecs;

/**
 * 系统类型位掩码定义
 * 使用位运算组合多个类型：SystemType.UPDATE | SystemType.RENDER
 */
public class SystemType {
	// 0000
	public static final int NONE = 0;

	// 0001: 参与逻辑循环 (GameWorld.update)
	public static final int UPDATE = 1 << 0;

	// 0010: 参与物理/固定步长循环 (GameWorld.fixedUpdate)
	public static final int FIXED_UPDATE = 1 << 1;

	// 0100: 参与渲染循环 (GameWorld.render)
	public static final int RENDER = 1 << 2;

	// === 常用组合 ===
	public static final int BOTH_UPDATE = UPDATE | FIXED_UPDATE;

	// === 语义化判定 API ===

	public static boolean isUpdate(int type) {
		return (type & UPDATE) != 0;
	}

	public static boolean isFixed(int type) {
		return (type & FIXED_UPDATE) != 0;
	}

	public static boolean isRender(int type) {
		return (type & RENDER) != 0;
	}

	/** 是否包含逻辑处理 (Update 或 Fixed) */
	public static boolean isLogic(int type) {
		return isUpdate(type) || isFixed(type);
	}
	
	/** [新增] 将掩码转换为可读字符串 */
	public static String toString(int type) {
		if (type == NONE) return "NONE";

		StringBuilder sb = new StringBuilder();
		if (isUpdate(type)) sb.append("UPDATE");

		if (isFixed(type)) {
			if (sb.length() > 0) sb.append("|");
			sb.append("FIXED");
		}

		if (isRender(type)) {
			if (sb.length() > 0) sb.append("|");
			sb.append("RENDER");
		}

		return sb.toString();
	}
}
