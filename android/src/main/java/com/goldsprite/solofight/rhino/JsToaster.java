package com.goldsprite.solofight.rhino;

import android.util.Log;
import android.widget.Toast;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class JsToaster {

	private static final String TAG = "JsToaster";

	/**
	 * 新增：使用函数注入的方式调用 Java 方法
	 */
	public static void showToastWithJavaCall(android.content.Context androidContext) {
		Context rhinoContext = Context.enter();
		rhinoContext.setOptimizationLevel(-1);
		Scriptable scope = rhinoContext.initStandardObjects();

		// 创建自定义的 log 函数并注入到 JavaScript 环境
		Function logFunction = new BaseFunction() {
			@Override
			public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
				if (args.length > 0) {
					String logMessage = Context.toString(args[0]);
					Toast.makeText(androidContext, logMessage, Toast.LENGTH_SHORT).show();
				}
				return Context.getUndefinedValue();
			}
		};

		// 将自定义函数设置为全局函数
		scope.put("javaLog", scope, logFunction);

		// JavaScript 代码：调用我们注入的 javaLog 函数
		String jsCode =
			"javaLog('js调用java方法<javaLog> 使用函数注入'); " +
				"'JavaScript 执行完成!'";

		Object result = rhinoContext.evaluateString(scope, jsCode, "JavaScript", 1, null);
		String message = Context.toString(result);

		Context.exit();
	}

	/**
	 * 更完整的 Java 调用示例
	 */
	public static void showToastWithCorrectJavaCalls(android.content.Context androidContext) {
		try {
			Context rhinoContext = Context.enter();
			rhinoContext.setOptimizationLevel(-1);
			Scriptable scope = rhinoContext.initStandardObjects();

			// 将 Android Context 传递给 JS
			scope.put("androidContext", scope, Context.javaToJS(androidContext, scope));

			String jsCode =
				// 调用静态方法：使用 Packages.完整类名
				"Packages.android.util.Log.v('UsePackage', '开始执行JavaScript, 调用java完整包名'); " +

					// 调用实例方法：通过对象调用
					"var toast = Packages.android.widget.Toast.makeText(androidContext, '来自JS的Toast! - UsePackage', Packages.android.widget.Toast.LENGTH_SHORT); " +
					"toast.show(); " +

					// 获取系统信息
					"var version = Packages.android.os.Build.VERSION.RELEASE; " +
					"'Android ' + version + ' - 执行完成!'";

			Object result = rhinoContext.evaluateString(scope, jsCode, "JavaScript", 1, null);
			String message = Context.toString(result);

			Log.d("UsePackage", "JS返回: " + message);

		} catch (Exception e) {
			Log.e("UsePackage", "执行失败: " + e.getMessage());
		}
	}

	/**
	 * 最可靠的方式 - 使用自定义桥接类
	 */
	public static void showToastWithBridge(android.content.Context androidContext) {
		try {
			Context rhinoContext = Context.enter();
			rhinoContext.setOptimizationLevel(-1);
			Scriptable scope = rhinoContext.initStandardObjects();

			// 使用自定义桥接类，避免直接调用 Android API
			AndroidBridge bridge = new AndroidBridge(androidContext);
			scope.put("bridge", scope, Context.javaToJS(bridge, scope));

			String jsCode =
				"bridge.log('这是日志消息 - 使用java专门定义的桥接类'); " +
					"bridge.showToast('这是Toast消息 - 使用java专门定义的桥接类'); " +
					"bridge.getDeviceInfo();";

			Object result = rhinoContext.evaluateString(scope, jsCode, "JavaScript", 1, null);
			String message = Context.toString(result);

			Log.d("UseBridgeInstance", "设备信息: " + message);

		} catch (Exception e) {
			Log.e("UseBridgeInstance", "执行失败: " + e.getMessage());
		}
	}


	public static Object eval(String js) {
		Context rhinoCtx = Context.enter();
		rhinoCtx.setOptimizationLevel(-1);
		Scriptable scope = rhinoCtx.initStandardObjects();
		Object result = null;
		{
			result = rhinoCtx.evaluateString(scope, js, "eval_js_string", 1, null);
		}
		Context.exit();
		return result;
	}


	/**
	 * 自定义 Android 桥接类
	 */
	public static class AndroidBridge {
		private final android.content.Context context;

		public AndroidBridge(android.content.Context context) {
			this.context = context;
		}

		public void log(String message) {
			Log.v("JS_BRIDGE", message);
		}

		public void showToast(String message) {
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}

		public String getDeviceInfo() {
			return "Android " + android.os.Build.VERSION.RELEASE + " - " +
				android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
		}
	}
}
