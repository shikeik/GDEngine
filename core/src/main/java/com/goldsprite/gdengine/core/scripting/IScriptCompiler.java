package com.goldsprite.gdengine.core.scripting;

public interface IScriptCompiler {
	/**
	 * 编译并加载脚本
	 * @param mainClassName  类全名 (例如 "com.mygame.MyScript")
	 * @param projectPath Java 源代码字符串
	 * @return 编译好的 Class 对象，如果失败返回 null
	 */
	Class<?> compile(String mainClassName, String projectPath);
}
