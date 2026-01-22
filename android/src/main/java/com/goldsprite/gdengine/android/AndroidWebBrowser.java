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
import android.view.WindowManager;
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

/**
 * Android 内嵌浏览器 (重构版)
 * 特性：MVP 结构、动态网格菜单、分页支持、全屏沉浸
 */
public class AndroidWebBrowser implements IWebBrowser {

	// --- Inner Models ---
	private static class ToolAction {
		String icon;
		String label;
		Runnable action;

		public ToolAction(String icon, String label, Runnable action) {
			this.icon = icon;
			this.label = label;
			this.action = action;
		}
	}

	// --- Core State ---
	private final Activity activity;
	private Dialog webDialog;
	private WebView webView;
	private boolean isNightMode = false;

	// UA State
	private String defaultUA;
	private boolean isDesktopMode = false;
	private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	// --- UI References ---
	private FrameLayout modalOverlay;
	private LinearLayout menuPanel;
	private GridLayout menuGrid;
	private LinearLayout paginationPanel;
	private LinearLayout bottomBar;

	// Theme Registry
	private final List<View> bgViews = new ArrayList<>();
	private final List<TextView> themeTextList = new ArrayList<>();

	// --- Menu Data ---
	private final List<ToolAction> tools = new ArrayList<>();
	private int currentPage = 0;
	private static final int PAGE_SIZE = 8; // 4x2

	public AndroidWebBrowser(Activity activity) {
		this.activity = activity;
		initToolActions();
	}

	// ============================================================
	// 1. Logic Layer: Action Registry
	// ============================================================

	private void initToolActions() {
		tools.clear();

		// Slot 1: 夜间模式
		tools.add(new ToolAction("🌗", "夜间模式", () -> {
			toggleNightMode();
			toggleMenu();
		}));

		// Slot 2: 桌面/移动切换
		tools.add(new ToolAction("🖥️", "桌面视图", () -> {
			toggleDesktopMode();
			toggleMenu();
		}));

		// Slot 3: 刷新
		tools.add(new ToolAction("↻", "刷新页面", () -> {
			if (webView != null) webView.reload();
			toggleMenu();
		}));

		// Slot 4: 前进
		tools.add(new ToolAction("→", "前进", () -> {
			if (webView != null && webView.canGoForward()) webView.goForward();
			toggleMenu();
		}));

		// Fillers for testing pagination (Slot 5-18)
		for (int i = 5; i <= 18; i++) {
			tools.add(new ToolAction("○", "预留 " + i, null));
		}
	}

	// ============================================================
	// 2. IWebBrowser Implementation
	// ============================================================

	@Override
	public void openUrl(String url, String title) {
		activity.runOnUiThread(() -> {
			forceOrientation(ScreenManager.Orientation.Portrait);
			showDialog(url);
		});
	}

	@Override
	public void close() {
		activity.runOnUiThread(() -> {
			if (webDialog != null && webDialog.isShowing()) {
				webDialog.dismiss();
			}
			forceOrientation(ScreenManager.Orientation.Landscape);
		});
	}

	@Override
	public boolean isEmbedded() { return true; }

	// ============================================================
	// 3. View Building Layer (Initialization)
	// ============================================================

	private void showDialog(String url) {
		if (webDialog == null) buildDialog();

		// Reset State
		if (modalOverlay != null) modalOverlay.setVisibility(View.GONE);
		if (isNightMode) toggleNightMode(); // Reset to Day
		currentPage = 0;

		webView.loadUrl(url);
		webDialog.show();
		applyWindowFlags(webDialog.getWindow());
	}

	private void buildDialog() {
		// A. Window Setup
		webDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		webDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Window win = webDialog.getWindow();
		if (win != null) {
			win.getDecorView().setPadding(0, 0, 0, 0); // No padding
			win.setBackgroundDrawable(null);
			win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = win.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
				win.setAttributes(lp);
			}
		}

		// B. Clear Theme Cache
		bgViews.clear();
		themeTextList.clear();

		// C. Layout Construction
		FrameLayout rootFrame = new FrameLayout(activity);
		rootFrame.setBackgroundColor(Color.WHITE);

		// Content Layer
		LinearLayout contentCol = new LinearLayout(activity);
		contentCol.setOrientation(LinearLayout.VERTICAL);

		setupWebView();
		contentCol.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		View toolbar = setupBottomToolbar();
		contentCol.addView(toolbar);

		rootFrame.addView(contentCol);

		// Overlay Layer
		setupMenuOverlay(rootFrame, dp2px(45)); // 45dp is toolbar height

		// D. Finalize
		webDialog.setContentView(rootFrame);
		updateThemeColors(false);
		setupBackKey();
	}

	private void setupWebView() {
		webView = new WebView(activity);
		WebSettings s = webView.getSettings();
		s.setJavaScriptEnabled(true);
		s.setDomStorageEnabled(true);
		s.setUseWideViewPort(true);
		s.setLoadWithOverviewMode(true);
		s.setSupportZoom(true);
		s.setBuiltInZoomControls(true);
		s.setDisplayZoomControls(false);

		webView.setWebViewClient(new WebViewClient() {
			@Override public boolean shouldOverrideUrlLoading(WebView v, String u) { return !u.startsWith("http"); }
		});
		webView.setWebChromeClient(new WebChromeClient());
	}

	private View setupBottomToolbar() {
		bottomBar = new LinearLayout(activity);
		bottomBar.setOrientation(LinearLayout.HORIZONTAL);
		bottomBar.setElevation(10f);
		bgViews.add(bottomBar);

		int h = dp2px(45);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(h, h);

		bottomBar.addView(createIconBtn("✕", 20, v -> close()), params);

		View spacer = new View(activity); // Spacer
		bottomBar.addView(spacer, new LinearLayout.LayoutParams(0, h, 1f));

		bottomBar.addView(createIconBtn("←", 24, v -> goBack()), params);
		bottomBar.addView(createIconBtn("☰", 22, v -> toggleMenu()), params);

		return bottomBar;
	}

	private void setupMenuOverlay(FrameLayout root, int bottomMargin) {
		modalOverlay = new FrameLayout(activity);
		modalOverlay.setBackgroundColor(Color.parseColor("#66000000"));
		modalOverlay.setVisibility(View.GONE);
		modalOverlay.setOnClickListener(v -> toggleMenu()); // Click outside to close

		menuPanel = new LinearLayout(activity);
		menuPanel.setOrientation(LinearLayout.VERTICAL);
		menuPanel.setClickable(true); // Catch clicks

		// Grid Container
		menuGrid = new GridLayout(activity);
		menuGrid.setColumnCount(4);
		menuGrid.setRowCount(2);
		int pad = dp2px(15);
		menuGrid.setPadding(pad, pad, pad, pad);

		menuPanel.addView(menuGrid);

		// Pagination Container
		paginationPanel = new LinearLayout(activity);
		paginationPanel.setOrientation(LinearLayout.HORIZONTAL);
		paginationPanel.setGravity(Gravity.CENTER);
		paginationPanel.setPadding(0, 0, 0, dp2px(10));
		menuPanel.addView(paginationPanel);

		// Position at bottom
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.BOTTOM;
		lp.bottomMargin = bottomMargin;

		modalOverlay.addView(menuPanel, lp);
		root.addView(modalOverlay);
	}

	// ============================================================
	// 4. Logic Layer: Menu & Pagination
	// ============================================================

	private void toggleMenu() {
		if (modalOverlay.getVisibility() == View.VISIBLE) {
			modalOverlay.setVisibility(View.GONE);
		} else {
			modalOverlay.setVisibility(View.VISIBLE);
			refreshMenuPage();
			// Animation
			menuPanel.setTranslationY(menuPanel.getHeight());
			menuPanel.animate().translationY(0).setDuration(200).start();
		}
	}

	private void refreshMenuPage() {
		menuGrid.removeAllViews();

		int start = currentPage * PAGE_SIZE;
		int end = Math.min(start + PAGE_SIZE, tools.size());

		// Render Items
		for (int i = start; i < end; i++) {
			addGridItem(tools.get(i));
		}

		// Fill empty slots to keep layout stable if last page
		int emptySlots = PAGE_SIZE - (end - start);
		for (int i = 0; i < emptySlots; i++) {
			addGridItem(null);
		}

		// Render Pagination Dots
		refreshPagination();

		// Re-apply theme to new views
		updateThemeColors(isNightMode);
	}

	private void refreshPagination() {
		paginationPanel.removeAllViews();
		int totalPages = (int) Math.ceil((double) tools.size() / PAGE_SIZE);

		if (totalPages <= 1) {
			paginationPanel.setVisibility(View.GONE);
			return;
		}
		paginationPanel.setVisibility(View.VISIBLE);

		for (int i = 0; i < totalPages; i++) {
			TextView dot = new TextView(activity);
			dot.setText("●");
			dot.setTextSize(12);
			dot.setPadding(10, 0, 10, 0);

			// Highlight current
			if (i == currentPage) {
				dot.setAlpha(1.0f);
				dot.setTextColor(isNightMode ? Color.CYAN : Color.parseColor("#0099FF"));
			} else {
				dot.setAlpha(0.3f);
				dot.setTextColor(Color.GRAY);
				final int p = i;
				dot.setOnClickListener(v -> {
					currentPage = p;
					refreshMenuPage();
				});
			}
			paginationPanel.addView(dot);
		}
	}

	private void addGridItem(ToolAction tool) {
		LinearLayout item = new LinearLayout(activity);
		item.setOrientation(LinearLayout.VERTICAL);
		item.setGravity(Gravity.CENTER);

		GridLayout.LayoutParams params = new GridLayout.LayoutParams();
		// Weight 1 for equal width
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
			params.width = 0;
		} else {
			params.width = activity.getResources().getDisplayMetrics().widthPixels / 4 - dp2px(8);
		}
		params.height = dp2px(70);

		if (tool != null) {
			applyRipple(item, true); // Borderless
			item.setOnClickListener(v -> { if (tool.action != null) tool.action.run(); });

			TextView icon = new TextView(activity);
			icon.setText(tool.icon);
			icon.setTextSize(24);
			icon.setGravity(Gravity.CENTER);
			themeTextList.add(icon);

			TextView label = new TextView(activity);
			label.setText(tool.label);
			label.setTextSize(10);
			label.setGravity(Gravity.CENTER);
			themeTextList.add(label);

			item.addView(icon);
			item.addView(label);
		}

		menuGrid.addView(item, params);
	}

	// ============================================================
	// 5. Feature Logic
	// ============================================================

	private void goBack() {
		if (webView.canGoBack()) webView.goBack();
		else Toast.makeText(activity, "到底了", Toast.LENGTH_SHORT).show();
	}

	private void toggleNightMode() {
		isNightMode = !isNightMode;
		String js = isNightMode
			? "document.documentElement.style.filter='invert(1) hue-rotate(180deg)';"
			: "document.documentElement.style.filter='';";
		webView.evaluateJavascript(js, null);
		updateThemeColors(isNightMode);
		Toast.makeText(activity, "夜间模式: " + (isNightMode ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
	}

	private void toggleDesktopMode() {
		WebSettings s = webView.getSettings();
		if (defaultUA == null) defaultUA = s.getUserAgentString();

		isDesktopMode = !isDesktopMode;
		s.setUserAgentString(isDesktopMode ? DESKTOP_UA : defaultUA);
		s.setUseWideViewPort(true);
		s.setLoadWithOverviewMode(true);
		webView.reload();

		Toast.makeText(activity, "UA: " + (isDesktopMode ? "Desktop" : "Mobile"), Toast.LENGTH_SHORT).show();
	}

	// ============================================================
	// 6. Style & Helpers
	// ============================================================

	private void updateThemeColors(boolean night) {
		int bg = night ? Color.parseColor("#222222") : Color.parseColor("#F5F5F5");
		int menuBg = night ? Color.parseColor("#333333") : Color.WHITE;
		int txtMain = night ? Color.parseColor("#DDDDDD") : Color.parseColor("#555555");
		int txtSub = night ? Color.parseColor("#AAAAAA") : Color.GRAY;

		// Backgrounds
		for (View v : bgViews) v.setBackgroundColor(bg);

		// Menu Panel Special Shape
		GradientDrawable shape = new GradientDrawable();
		shape.setColor(menuBg);
		shape.setCornerRadii(new float[]{30,30, 30,30, 0,0, 0,0});
		menuPanel.setBackground(shape);

		// Texts
		for (TextView tv : themeTextList) {
			float size = tv.getTextSize() / activity.getResources().getDisplayMetrics().scaledDensity;
			tv.setTextColor(size > 15 ? txtMain : txtSub);
		}
	}

	private TextView createIconBtn(String text, int size, View.OnClickListener click) {
		TextView btn = new TextView(activity);
		btn.setText(text);
		btn.setTextSize(size);
		btn.setGravity(Gravity.CENTER);
		btn.setOnClickListener(click);
		applyRipple(btn, false);
		themeTextList.add(btn);
		return btn;
	}

	private void applyRipple(View v, boolean borderless) {
		TypedValue out = new TypedValue();
		int attr = borderless ? android.R.attr.selectableItemBackgroundBorderless
			: android.R.attr.selectableItemBackground;
		activity.getTheme().resolveAttribute(attr, out, true);
		v.setBackgroundResource(out.resourceId);
	}

	private void applyWindowFlags(Window window) {
		if (window != null) {
			window.getDecorView().setSystemUiVisibility(getImmersiveFlags());
			window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		}
	}

	private void setupBackKey() {
		webDialog.setOnKeyListener((dialog, keyCode, event) -> {
			if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == 0) {
				if (modalOverlay.getVisibility() == View.VISIBLE) {
					toggleMenu(); return true;
				}
				if (webView.canGoBack()) {
					webView.goBack(); return true;
				}
				close(); return true;
			}
			return false;
		});
	}

	private void forceOrientation(ScreenManager.Orientation o) {
		if (ScreenManager.getInstance() != null) {
			ScreenManager.getInstance().setOrientation(o);
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
