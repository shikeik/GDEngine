package com.goldsprite.gdengine.android;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.goldsprite.gdengine.core.web.IWebBrowser;
import com.goldsprite.gdengine.screens.ScreenManager;

import java.util.ArrayList;
import java.util.List;

public class AndroidWebBrowser implements IWebBrowser {
	private final Activity activity;
	private Dialog webDialog;
	private WebView webView;

	// UI Components references for Theme update
	private FrameLayout modalOverlay;
	private LinearLayout menuPanel;
	private LinearLayout bottomBar;
	private final List<TextView> themeTextList = new ArrayList<>(); // 所有的文本/图标引用
	private final List<View> themeBgList = new ArrayList<>();       // 需要改背景色的容器

	private boolean isNightMode = false;

	public AndroidWebBrowser(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void openUrl(String url, String title) {
		activity.runOnUiThread(() -> {
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
		// Reset UI State
		if (modalOverlay != null) modalOverlay.setVisibility(View.GONE);

		// 重置夜间模式状态
		if (isNightMode) toggleNightMode();

		webView.loadUrl(url);

		// [回滚修复] 移除 focusable hack，回归最纯粹的显示逻辑
		// 这能避免 Dialog 闪烁或露出底部 Activity
		webDialog.show();

		Window window = webDialog.getWindow();
		if (window != null) {
			// 再次确保全屏属性
			window.getDecorView().setSystemUiVisibility(getImmersiveFlags());
			window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		}
	}

	private void initDialog() {
		// 使用全屏 Theme
		webDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		webDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		themeTextList.clear();
		themeBgList.clear();

		// --- Root ---
		FrameLayout rootFrame = new FrameLayout(activity);
		rootFrame.setBackgroundColor(Color.WHITE); // 确保不透明

		// --- Content Layer (Vertical Linear) ---
		LinearLayout contentLayout = new LinearLayout(activity);
		contentLayout.setOrientation(LinearLayout.VERTICAL);

		// WebView (Weight=1, 占据剩余空间)
		webView = new WebView(activity);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDomStorageEnabled(true);
		settings.setUseWideViewPort(true);
		settings.setLoadWithOverviewMode(true);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setDisplayZoomControls(false);

		webView.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					return !url.startsWith("http");
				}
			});
		webView.setWebChromeClient(new WebChromeClient());

		// WebView Params: width=MATCH, height=0, weight=1
		contentLayout.addView(webView, new LinearLayout.LayoutParams(
								  ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

		// Bottom Toolbar (Fixed Height)
		bottomBar = new LinearLayout(activity);
		bottomBar.setOrientation(LinearLayout.HORIZONTAL);
		bottomBar.setElevation(10f);
		themeBgList.add(bottomBar);

		int barHeight = dp2px(45);
		LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, barHeight);

		// Buttons
		TextView btnClose = createFlatButton("✕", 20, v -> close());
		View spacer = new View(activity);
		TextView btnBack = createFlatButton("←", 24, v -> {
			if (webView.canGoBack()) webView.goBack();
			else Toast.makeText(activity, "到底了", Toast.LENGTH_SHORT).show();
		});
		TextView btnMenu = createFlatButton("☰", 22, v -> toggleMenu());

		bottomBar.addView(btnClose, new LinearLayout.LayoutParams(barHeight, barHeight));
		bottomBar.addView(spacer, new LinearLayout.LayoutParams(0, barHeight, 1.0f));
		bottomBar.addView(btnBack, new LinearLayout.LayoutParams(barHeight, barHeight));
		bottomBar.addView(btnMenu, new LinearLayout.LayoutParams(barHeight, barHeight));

		contentLayout.addView(bottomBar, barParams);

		// Add Content to Root
		rootFrame.addView(contentLayout, new FrameLayout.LayoutParams(
							  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		// --- Menu Overlay Layer ---
		createMenuOverlay(rootFrame, barHeight);

		webDialog.setContentView(rootFrame);

		// Apply Initial Theme
		updateNativeTheme(false);

		// Back Key Logic
		webDialog.setOnKeyListener((dialog, keyCode, event) -> {
			if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
				if (modalOverlay.getVisibility() == View.VISIBLE) {
					toggleMenu();
					return true;
				}
				if (webView.canGoBack()) {
					webView.goBack();
					return true;
				}
				close();
				return true;
			}
			return false;
		});
	}

	private void createMenuOverlay(FrameLayout root, int bottomMargin) {
		modalOverlay = new FrameLayout(activity);
		modalOverlay.setBackgroundColor(Color.parseColor("#66000000"));
		modalOverlay.setVisibility(View.GONE);
		modalOverlay.setOnClickListener(v -> toggleMenu());

		menuPanel = new LinearLayout(activity);
		menuPanel.setOrientation(LinearLayout.VERTICAL);
		menuPanel.setClickable(true);

		// Grid (4 cols x 2 rows)
		GridLayout grid = new GridLayout(activity);
		grid.setColumnCount(4);
		grid.setRowCount(2);
		int padding = dp2px(15);
		grid.setPadding(padding, padding, padding, padding);

		// Item 1: Night Mode
		addGridItem(grid, "🌗", "夜间模式", v -> {
			toggleNightMode();
			toggleMenu();
		});

		// Items 2-8: Placeholders
		for (int i = 0; i < 7; i++) {
			addGridItem(grid, "○", "未定义", null);
		}

		menuPanel.addView(grid);

		FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		menuParams.gravity = Gravity.BOTTOM;
		menuParams.bottomMargin = bottomMargin;

		modalOverlay.addView(menuPanel, menuParams);
		root.addView(modalOverlay, new FrameLayout.LayoutParams(
						 ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	private void addGridItem(GridLayout grid, String icon, String label, View.OnClickListener action) {
		LinearLayout item = new LinearLayout(activity);
		item.setOrientation(LinearLayout.VERTICAL);
		item.setGravity(Gravity.CENTER);

		TypedValue outValue = new TypedValue();
		activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
		item.setBackgroundResource(outValue.resourceId);

		if(action != null) item.setOnClickListener(action);
		else item.setAlpha(0.3f);

		TextView iconTv = new TextView(activity);
		iconTv.setText(icon);
		iconTv.setTextSize(24);
		iconTv.setGravity(Gravity.CENTER);
		themeTextList.add(iconTv);

		TextView labelTv = new TextView(activity);
		labelTv.setText(label);
		labelTv.setTextSize(10);
		labelTv.setGravity(Gravity.CENTER);
		themeTextList.add(labelTv);

		item.addView(iconTv);
		item.addView(labelTv);

		// 使用 columnWeight 保证平分
		GridLayout.LayoutParams params = new GridLayout.LayoutParams();
		params.width = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
		} else {
			params.width = activity.getResources().getDisplayMetrics().widthPixels / 4 - dp2px(8);
		}
		params.height = dp2px(70);

		grid.addView(item, params);
	}

	private void toggleNightMode() {
		isNightMode = !isNightMode;
		String js = isNightMode 
			? "document.documentElement.style.filter='invert(1) hue-rotate(180deg)';" 
			: "document.documentElement.style.filter='';";
		webView.evaluateJavascript(js, null);

		updateNativeTheme(isNightMode);

		Toast.makeText(activity, isNightMode ? "夜间模式: 开" : "夜间模式: 关", Toast.LENGTH_SHORT).show();
	}

	private void updateNativeTheme(boolean night) {
		int bgColor = night ? Color.parseColor("#222222") : Color.parseColor("#F5F5F5");
		int menuBgColor = night ? Color.parseColor("#333333") : Color.WHITE;
		int textColor = night ? Color.parseColor("#DDDDDD") : Color.parseColor("#555555");
		int subTextColor = night ? Color.parseColor("#AAAAAA") : Color.GRAY;

		for (View v : themeBgList) {
			v.setBackgroundColor(bgColor);
		}

		GradientDrawable shape = new GradientDrawable();
		shape.setColor(menuBgColor);
		shape.setCornerRadii(new float[]{30,30, 30,30, 0,0, 0,0});
		menuPanel.setBackground(shape);

		for (TextView tv : themeTextList) {
			// Simple heuristic: large text is icon
			if (tv.getTextSize() / activity.getResources().getDisplayMetrics().scaledDensity > 15) {
				tv.setTextColor(textColor);
			} else {
				tv.setTextColor(subTextColor);
			}
		}
	}

	private TextView createFlatButton(String text, int textSize, View.OnClickListener click) {
		TextView btn = new TextView(activity);
		btn.setText(text);
		btn.setTextSize(textSize);
		btn.setGravity(Gravity.CENTER);
		btn.setOnClickListener(click);

		TypedValue outValue = new TypedValue();
		activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
		btn.setBackgroundResource(outValue.resourceId);

		themeTextList.add(btn);
		return btn;
	}

	private void toggleMenu() {
		if (modalOverlay.getVisibility() == View.VISIBLE) {
			modalOverlay.setVisibility(View.GONE);
		} else {
			modalOverlay.setVisibility(View.VISIBLE);
			menuPanel.setTranslationY(menuPanel.getHeight());
			menuPanel.animate().translationY(0).setDuration(200).start();
		}
	}

	private int dp2px(float dp) {
		return (int) (dp * activity.getResources().getDisplayMetrics().density + 0.5f);
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
