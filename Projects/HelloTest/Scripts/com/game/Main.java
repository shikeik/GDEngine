package com.game;

// 引入引擎日志类 (验证 Classpath 是否包含了 core.jar)
import com.goldsprite.gdengine.log.Debug;

public class Main {
	// 标准 Java 入口
	public static void main(String[] args) {
		Debug.logT("Script", "你好, 主脚本!");
	}
}
