package com.goldsprite.solofight.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PermissionUtils {
	private Activity ctx;
	private String[] permissions;
	public static int PERMISSION_REQUEST_CODE = 100;
	private Runnable callback;

	public PermissionUtils(Activity context, String[] permissions, int code, Runnable callback) {
		this.ctx = context;
		this.permissions = permissions;
		PERMISSION_REQUEST_CODE = code;
		this.callback = callback;
	}

	// 请求文件权限
	public void requestAllPermission() {
		List<String> retPerms = new ArrayList<>();
		// 1. 检查清单文件是否声明了权限
		if (!isStoragePermissionDeclared(ctx, retPerms, permissions)) {
			showDialog("申请权限失败", "AndroidManifest清单未声明以下权限: \n" + joinString(retPerms), true);
			return;
		}

		// 2. 检查是否已有权限 (真实写文件测试)
		if (hasExternalStoragePermission()) {
			if (callback != null) callback.run();
		}
		// 3. Android 11+ (R) 特殊处理：跳转“管理所有文件”权限页
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Q是10, R是11
			showDialog("权限申请", "作为引擎开发工具，本应用需要[所有文件访问权限]以读写项目文件。\n请在接下来的设置页面中开启授权。", false, () -> {
				try {
					Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
					intent.setData(Uri.parse("package:" + ctx.getPackageName()));
					ctx.startActivityForResult(intent, PERMISSION_REQUEST_CODE);
				} catch (Exception e) {
					// 某些国产ROM可能不支持标准Intent，尝试通用设置页
					Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
					ctx.startActivityForResult(intent, PERMISSION_REQUEST_CODE);
				}
			});
		}
		// 4. Android 6.0 ~ 10 普通动态权限申请
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			ctx.requestPermissions(permissions, PERMISSION_REQUEST_CODE);
		}
	}

	// 请求权限回调结果
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode != PERMISSION_REQUEST_CODE) return;

		// 同意
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			showToast("权限申请通过");
			if (callback != null) callback.run();
			return;
		}

		// 拒绝
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			// 如果用户拒绝了，但没勾选“不再询问”，继续引导
			if (ctx.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				showDialog("权限申请失败", "应用需要此权限以维持引擎项目读写，否则无法运行。", false, this::requestAllPermission);
			} else {
				// 用户拒绝且勾选了“不再询问”，弹窗引导去设置页
				permissionDialog();
			}
		}
	}

	// 从设置页返回
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (!hasExternalStoragePermission()) {
				finishWithToast("未获得授权，程序将退出。");
			} else {
				showToast("已成功获得授权。");
				if (callback != null) callback.run();
			}
		}
	}

	// 引导去设置页的弹窗
	public void permissionDialog() {
		new AlertDialog.Builder(ctx)
			.setCancelable(false)
			.setTitle("权限申请已被禁止")
			.setMessage("系统检测到存储权限被永久禁止。\n请点击[确定]前往设置页面，手动开启[存储/文件]权限。")
			.setPositiveButton("确定", (dialog, which) -> {
				Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
				intent.setData(uri);
				ctx.startActivityForResult(intent, PERMISSION_REQUEST_CODE);
			})
			.setNegativeButton("退出", (dialog, which) -> finishWithToast("未获得授权，程序退出。"))
			.show();
	}

	// 核心逻辑：真实写文件测试权限 (兼容 Android 11 Scoped Storage 绕过检测)
	public static boolean hasExternalStoragePermission() {
		// Android 11+ 优先检查 Environment.isExternalStorageManager()
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return Environment.isExternalStorageManager();
		}

		// 旧版本通过写文件测试
		boolean ret = true;
		try {
			String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/gd_perm_test_" + System.currentTimeMillis() + ".txt";
			File file = new File(path);
			if (file.createNewFile()) {
				file.delete();
			} else {
				ret = false;
			}
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isStoragePermissionDeclared(Context context, List<String> retPerms, String[] permissions) {
		retPerms.addAll(Arrays.asList(permissions));
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
			if (packageInfo.requestedPermissions != null) {
				for (String permission : packageInfo.requestedPermissions) {
					retPerms.remove(permission);
				}
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return retPerms.isEmpty();
	}

	// --- Helper Methods ---

	private void showToast(String msg) {
		Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
	}

	private void finishWithToast(String msg) {
		Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
		ctx.finish();
	}

	private void showDialog(String title, String msg, boolean finishOnCancel) {
		showDialog(title, msg, finishOnCancel, null);
	}

	private void showDialog(String title, String msg, boolean finishOnCancel, Runnable onConfirm) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
			.setTitle(title)
			.setMessage(msg)
			.setCancelable(false);

		if (onConfirm != null) {
			builder.setPositiveButton("确定", (d, w) -> onConfirm.run());
			builder.setNegativeButton("取消", (d, w) -> {
				if (finishOnCancel) finishWithToast("用户取消授权");
			});
		} else {
			builder.setPositiveButton("确定", (d, w) -> {
				if (finishOnCancel) ctx.finish();
			});
		}
		builder.show();
	}

	private String joinString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for(String s : list) sb.append(s).append("\n");
		return sb.toString();
	}
}
