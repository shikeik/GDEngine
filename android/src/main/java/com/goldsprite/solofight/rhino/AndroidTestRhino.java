package com.goldsprite.solofight.rhino;

import android.content.Context;

public class AndroidTestRhino {
	private static Context ctx;

	public AndroidTestRhino(Context ctx) {
		AndroidTestRhino.ctx = ctx;

		//test1();

		//test2();
	}

	public static void test3() {
		Object result = JsToaster.eval(
			"var context = com.goldsprite.biowar.android.AndroidGdxLauncher.getCtx();" +
				"var ecs = com.goldsprite.gameframeworks.ecs;" +
				"var Gobject = ecs.entity.GObject;" +
				"var player = ecs.system.GameSystem.sceneSystem.findGObjectByTag(\"Player\");" +
				"var playerName = player.getName();" +
				"var playerPos = player.getTransform().getPosition();" +
				"android.widget.Toast.makeText(context, '成功找到玩家('+playerName+')的位置: '+playerPos, 0).show();" +
				"\"eval js finished.\";"
		);

		android.widget.Toast.makeText(ctx, "" + result, 0).show();
	}

	private void test1() {
		int k;
		// 方法1：使用函数注入
		JsToaster.showToastWithJavaCall(ctx);
		// 方法2：使用完整包名调用
		JsToaster.showToastWithCorrectJavaCalls(ctx);

		// 方法3：使用桥接类（推荐，最稳定）
		JsToaster.showToastWithBridge(ctx);
	}

	private void test2() {
		Object result = JsToaster.eval(
			"var context = com.goldsprite.biowar.android.AndroidGdxLauncher.getCtx();\n" +
				"android.widget.Toast.makeText(context, \"Hello Toast\", 0).show();\n" +
				"\"eval js finished.\";"
//			"" +
//			"var context = com.goldsprite.biowar.android.AndroidGdxLauncher.getCtx();" +
//			"android.widget.Toast.makeText(context, \"Hello Toast\", 0).show();"+
//			"\"eval js finished.\";" +
//			""
		);

		android.widget.Toast.makeText(ctx, "" + result, 0).show();
	}
}
