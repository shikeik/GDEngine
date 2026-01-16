package com.goldsprite.gdengine.android;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.goldsprite.gdengine.core.web.IWebBrowser;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.ScreenManager; // [新增] 引入屏幕管理器

public class AndroidWebBrowser implements IWebBrowser {
	private final Activity activity;
	private Dialog webDialog;
	private WebView webView;

	public AndroidWebBrowser(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void openUrl(String url, String title) {
		activity.runOnUiThread(() -> {
			// [新增] 1. 强制切换为竖屏，适合阅读文档
			if (ScreenManager.getInstance() != null) {
				ScreenManager.getInstance().setOrientation(ScreenManager.Orientation.Portrait);
			}
			showWebDialog(url, title);
		});
	}

	@Override
	public void close() {
		activity.runOnUiThread(() -> {
			if (webDialog != null && webDialog.isShowing()) {
				webDialog.dismiss();
			}
			// [新增] 2. 关闭时恢复横屏 (游戏默认方向)
			if (ScreenManager.getInstance() != null) {
				ScreenManager.getInstance().setOrientation(ScreenManager.Orientation.Landscape);
			}
		});
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}

	private void showWebDialog(String url, String title) {
		if (webDialog == null) {
			initDialog();
		}

		TextView titleView = webDialog.findViewById(101);
		if (titleView != null) titleView.setText(title);

		webView.loadUrl(url);

		webDialog.show();
		webDialog.getWindow().getDecorView().setSystemUiVisibility(getImmersiveFlags());
	}

	private void initDialog() {
		webDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		webDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Window window = webDialog.getWindow();
		if (window != null) {
			// 刘海屏适配
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
				window.setAttributes(lp);
			}

			window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			window.setBackgroundDrawable(null);

			window.getDecorView().setSystemUiVisibility(getImmersiveFlags());

			window.getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
				if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
					window.getDecorView().setSystemUiVisibility(getImmersiveFlags());
				}
			});
		}

		LinearLayout root = new LinearLayout(activity);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.WHITE);

		// Toolbar
		LinearLayout toolbar = new LinearLayout(activity);
		toolbar.setOrientation(LinearLayout.HORIZONTAL);
		toolbar.setBackgroundColor(Color.parseColor("#333333"));
		toolbar.setPadding(20, 20, 20, 20);

		TextView closeBtn = new TextView(activity);
		closeBtn.setText("❌ 关闭");
		closeBtn.setTextColor(Color.WHITE);
		closeBtn.setTextSize(16);
		closeBtn.setOnClickListener(v -> close());

		TextView titleView = new TextView(activity);
		titleView.setId(101);
		titleView.setText("Docs");
		titleView.setTextColor(Color.CYAN);
		titleView.setTextSize(16);
		titleView.setPadding(40, 0, 0, 0);

		toolbar.addView(closeBtn);
		toolbar.addView(titleView);

		// WebView
		webView = new WebView(activity);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDomStorageEnabled(true);
		settings.setAllowFileAccess(true);
		settings.setUseWideViewPort(true);
		settings.setLoadWithOverviewMode(true);

		// [修复核心] 强力拦截所有非 HTTP 协议
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return handleUrl(url);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				return handleUrl(request.getUrl().toString());
			}

			private boolean handleUrl(String url) {
				if (url == null) return false;

				// 1. 允许加载 http / https
				if (url.startsWith("http://") || url.startsWith("https://")) {
					return false; // 返回 false 让 WebView 自己加载
				}

				// 2. [修改] 遇到所有其他协议 (baiduboxapp:// 等)，直接返回 true (拦截)
				// 不做任何处理，静默失败。这样就不会弹窗问用户了。
				Log.d("WebBrowser", "拦截非Http/https协议: " + url);
				return true;
			}
		});

		webView.setWebChromeClient(new WebChromeClient());

		LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		);

		root.addView(toolbar);
		root.addView(webView, webParams);

		webDialog.setContentView(root);

		// [修复] 使用 OnKeyListener 拦截物理按键
		// 必须返回 true 才能阻止 Dialog 默认的关闭行为
		webDialog.setOnKeyListener((dialog, keyCode, event) -> {
			if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
				if (webView.canGoBack()) {
					webView.goBack(); // 网页回退
					return true;      // 消费事件，Dialog 不关闭
				}
				// 网页没法回退了，关闭 Dialog (并触发 close() 里的切横屏逻辑)
				close();
				return true;
			}
			return false;
		});

		// 移除旧的 OnCancelListener，防止冲突
		webDialog.setOnCancelListener(null);
	}

	private int getImmersiveFlags() {
		return View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
	}
}
