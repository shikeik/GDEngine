package com.goldsprite.gdengine.screens.ecs.editor;

public enum EditorState {
	CLEAN,      // 代码已编译，运行环境一致
	DIRTY,      // 代码已修改，需编译
	COMPILING   // 正在编译中
}
