package com.goldsprite.solofight;

import org.junit.Assert;

// ==========================================
// 🛠️ 自定义话唠断言工具
// ==========================================
public class CLogAssert {
	public static void assertTrue(String msg, boolean condition) {
		if (condition) {
			System.out.println("✅ PASS: " + msg);
		} else {
			System.err.println("❌ FAIL: " + msg);
			Assert.assertTrue(msg, false); // 触发 JUnit 失败
		}
	}

	public static void assertFalse(String msg, boolean condition) {
		if (!condition) {
			System.out.println("✅ PASS: " + msg);
		} else {
			System.err.println("❌ FAIL: " + msg + " (Expected False, got True)");
			Assert.assertFalse(msg, true);
		}
	}

	public static void assertEquals(String msg, Object expected, Object actual) {
		if (expected.equals(actual)) {
			System.out.println("✅ PASS: " + msg + " [Value: " + actual + "]");
		} else {
			System.err.println("❌ FAIL: " + msg + " (Expected: " + expected + ", Actual: " + actual + ")");
			Assert.assertEquals(msg, expected, actual);
		}
	}
}
