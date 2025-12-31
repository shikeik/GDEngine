package com.goldsprite.solofight.test;

import com.goldsprite.gameframeworks.log.Debug;

public class DebugLogTest {

	public DebugLogTest() {
//		Debug.showTags[1] = "拦截 Y";
		Debug.logT("Test1", "Test1应该被输出");
		Debug.logT("Test2", "Test2应该被拦截");
	}

	public static void main(String[] args) {
		new DebugLogTest();
	}
}
