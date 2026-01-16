package com.goldsprite.gdengine.android;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.goldsprite.gdengine.core.web.IWebBrowser;

public class AndroidWebBrowser implements IWebBrowser {
	private final Activity activity;
	private Dialog webDialog;
	private WebView webView;

	public AndroidWebBrowser(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void openUrl(String url, String title) {
		activity.runOnUiThread(() -> showWebDialog(url, title));
	}

	@Override
	public void close() {
		if (webDialog != null && webDialog.isShowing()) {
			webDialog.dismiss();
		}
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}

	private void showWebDialog(String url, String title) {
		if (webDialog == null) {
			initDialog();
		}

		// 更新标题
		if (webDialog.findViewById(101) instanceof TextView) {
			((TextView) webDialog.findViewById(101)).setText(title);
		}

		webView.loadUrl(url);
		webDialog.show();
	}

	private void initDialog() {
		// 使用全屏 Theme
		webDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		webDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// --- [Fix 2 & 3] 窗口层级配置 (刘海屏 + 全屏) ---
		Window window = webDialog.getWindow();
		if (window != null) {
			// 1. 允许延伸到刘海区域 (解决左侧黑边)
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
				window.setAttributes(lp);
			}

			// 2. 强制全屏布局
			window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

			// 3. 恢复沉浸式模式 (隐藏导航栏和状态栏)
			// 注意：Dialog 默认有自己的 Window，需要单独设置 Flag，否则会弹出状态栏
			int immersiveFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

			window.getDecorView().setSystemUiVisibility(immersiveFlags);

			// 监听焦点变化，防止键盘弹出/收起后沉浸式失效
			window.getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
				if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
					window.getDecorView().setSystemUiVisibility(immersiveFlags);
				}
			});
		}

		// 根布局
		LinearLayout root = new LinearLayout(activity);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.WHITE);

		// 1. 顶部栏 (Toolbar)
		LinearLayout toolbar = new LinearLayout(activity);
		toolbar.setOrientation(LinearLayout.HORIZONTAL);
		toolbar.setBackgroundColor(Color.parseColor("#333333"));
		toolbar.setPadding(20, 20, 20, 20);

		// 关闭按钮
		TextView closeBtn = new TextView(activity);
		closeBtn.setText("❌ 关闭");
		closeBtn.setTextColor(Color.WHITE);
		closeBtn.setTextSize(16);
		closeBtn.setOnClickListener(v -> close());

		// 标题
		TextView titleView = new TextView(activity);
		titleView.setId(101); // ID便于查找
		titleView.setText("Docs");
		titleView.setTextColor(Color.CYAN);
		titleView.setTextSize(16);
		titleView.setPadding(40, 0, 0, 0);

		toolbar.addView(closeBtn);
		toolbar.addView(titleView);

		// 2. WebView
		webView = new WebView(activity);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true); // 必须开启JS
		settings.setDomStorageEnabled(true); // 开启DOM存储(可选)
		settings.setAllowFileAccess(true);   // 允许访问文件(为离线做准备)

		// 设置 Client 防止跳转到系统浏览器
		webView.setWebViewClient(new WebViewClient());
		webView.setWebChromeClient(new WebChromeClient());

		// 布局参数
		LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		);

		root.addView(toolbar);
		root.addView(webView, webParams);

		webDialog.setContentView(root);

		// 物理返回键处理
		webDialog.setOnCancelListener(dialog -> {
			if (webView.canGoBack()) {
				webView.goBack();
			} else {
				dialog.dismiss();
			}
		});
	}
}
