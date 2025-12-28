//package com.goldsprite.gameframeworks.log;
//
//import com.badlogic.gdx.graphics.Color;
//import com.badlogic.gdx.graphics.g2d.Batch;
//import com.badlogic.gdx.scenes.scene2d.Group;
//import com.badlogic.gdx.scenes.scene2d.InputEvent;
//import com.badlogic.gdx.scenes.scene2d.InputListener;
//import com.badlogic.gdx.scenes.scene2d.Stage;
//import com.badlogic.gdx.scenes.scene2d.ui.*;
//import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
//import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
//import com.badlogic.gdx.utils.Logger;
//import com.goldsprite.gameframeworks.assets.ColorTextureUtils;
//import com.goldsprite.gameframeworks.assets.GlobalAssets;
//import com.goldsprite.gameframeworks.ui.dockablewindow.DockableWindow;
//import com.goldsprite.gameframeworks.utils.TimeUtils;
//import com.goldsprite.microsurvivalgame.BuildConfig;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.goldsprite.gameframeworks.utils.StringUtils.formatString;
//
//public class Debug {
//	public static final String passStr = "Y";
//	public static final boolean singleMode = false;
//	private static final List<String> logMessages = new ArrayList<>();
//	private static final List<String> logInfos = new ArrayList<>();
//	public static String singleTag = "NewCollision";
//	public static String[] showTags = {
//		"Default Y",
//		"IRunnable Y",
//		"System Y",
//		"GameSystem Y",
//		"SceneSystem Y",
//		"PhysicsSystem Y",
//		"RendererSystem Y",
//		"Physics Y",
//		"PlayerInfo Y",
//		"JsEngine Y",
//		"GFsm X",
//		"Shader Y",
//		"DayNight Y",
//		"InputSystem Y",
//		"GObject Y",
//		"Comp Y",
//		"PhysicsCallback Y",
//		"AttackTrigger Y",
//		"CollisionDemo Y",
//		"CollisionVisualDemo Y",
//		"NewCollision Y",
//		//lwjgl
//		"FullscreenManager Y",
//		//NewGame
//		"NewGameTest Y",
//		"InventoryManager Y",
//		"Inventory Y",
//		"TEST Y",           // 用于通用测试日志
//		"ItemRegistry Y",   // 用于注册表加载日志
//		"AssetManager Y",   // 用于资源加载日志 (我也建议加上这个)
//		"InputController Y",   // 输入控制器
//		"WorldRenderer Y",   // 世界渲染器
//		"WorldModel Y",
//		"Persistence Y",
//	};
//	public static Debug instance;
//	public static String LOG_TAG = BuildConfig.PROJECT_NAME;
//	private static final Logger logger = new Logger(LOG_TAG);
//	private static Label debugLogLabel, debugInfoLabel;
//	private static ScrollPane debugLogScrollPane;
//	private static boolean logViewDirty = true;
//	public Table rootLayout;
//	private Skin uiSkin;
//	private AutoRunningGroup group;
//	private DockableWindow window1;
//	private boolean debugViewVisible = true;
//	private float lastHeight, lastY;
//	private Table titleBar, content;
//
//	private Debug() {
//	}
//
//	public static Debug getInstance() {
//		if (instance == null) {
//			init();
//		}
//
//		return instance;
//	}
//
//	private static void init() {
//		instance = new Debug();
//		instance.uiSkin = GlobalAssets.getInstance().editorSkin;
//	}
//
//	private static Table createTabPage() {
//		Skin skin = instance.uiSkin;
//		String[] tabs = {"Log", "Info"};
//		Table page1 = new Table(skin);
//		{
//			Table content = new Table(skin);
//
//			debugLogLabel = new Label("", skin, "smallest");
//			content.top().left().add(debugLogLabel);
//
//			debugLogScrollPane = new ScrollPane(content, skin, "nobackground");
//			debugLogScrollPane.setCancelTouchFocus(false);
//			debugLogScrollPane.setSmoothScrolling(false);
//			page1.add(debugLogScrollPane).grow();
//		}
//		Table page2 = new Table(skin);
//		{
//			Table content = new Table(skin);
//
//			debugInfoLabel = new Label("", skin, "smallest");
//			content.top().left().add(debugInfoLabel);
//
//			ScrollPane scrollPane = new ScrollPane(content, skin, "nobackground");
//			scrollPane.setCancelTouchFocus(false);
//			scrollPane.setSmoothScrolling(false);
//			page2.add(scrollPane).grow();
//		}
//
//		Table[] pages = {page1, page2};
//		return createPagesTab(skin, tabs, pages);
//	}
//
//	private static Table createPagesTab(Skin skin, String[] tabNames, Table[] pageContents) {
//		if (tabNames.length != pageContents.length) {
//			throw new IllegalArgumentException("tabNames 和 pageContents 数量必须一致");
//		}
//
//		// 顶层总表，背景 panel1
//		Table root = new Table(skin);
////		root.setBackground("panel1");
//		root.setBackground((Drawable) null);
//
//		// 菜单栏，背景 list
//		Table menuBar = new Table(skin);
////		menuBar.setBackground("list");
//		menuBar.setBackground((Drawable) null);
//
//		// 内容，背景 list
//		Table content = new Table(skin);
////		content.setBackground("list");
//		content.setBackground((Drawable) null);
//
//		// 页面堆叠
//		Stack pages = new Stack();
//		for (Table page : pageContents) {
//			page.setVisible(false);
//			pages.add(page);
//		}
//		pageContents[0].setVisible(true);
//
//		content.add(pages).expand().fill();
//
//		TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(skin.get("default", TextButton.TextButtonStyle.class));
//		style.down = null;
//		style.up = null;
//		// 创建按钮
//		for (int i = 0; i < tabNames.length; i++) {
//			final int index = i;
//			TextButton tabButton = new TextButton(tabNames[i], style);
//
//			tabButton.addListener(new ClickListener() {
//				@Override
//				public void clicked(InputEvent event, float x, float y) {
//					for (int j = 0; j < pageContents.length; j++) {
//						pageContents[j].setVisible(j == index);
//					}
//				}
//			});
//
//			// 按钮均分宽度，最小高度 150
//			menuBar.add(tabButton).expandX().fillX().minHeight(20).pad(0);
//		}
//
//		// 布局
//		root.top().pad(0);
//		root.add(menuBar).growX().row();
//		root.add(content).expand().fill().pad(0);
//
//		return root;
//	}
//
//	public static void log(String msg, Object... args) {
//		logT("Default", msg, args);
//	}
//
//	public static void logT(String tag, String msg, Object... args) {
//		if (banTag(tag)) return;
//
//		msg = formatString("[%s] %s", tag, formatString(msg, args));
//		msg = formatString("[%s] %s", TimeUtils.formatTime("HH:mm:ss:SSS"), msg);// 添加时间戳
//
//		logger.setLevel(Logger.NONE);
//		logger.info(msg);
//
//		//提供给UI
//		logMessages.add(/*"NONE: " + */msg);
//		//打印到控制台
//		System.out.println(msg);
//		logViewDirty = true;
//	}
//
//	public static void info(String msg, Object... args) {
//		infoT("Default", msg, args);
//	}
//
//	public static void infoT(String tag, String msg, Object... args) {
//		if (banTag(tag)) return;
//
//		msg = formatString("[%s] %s", tag, formatString(msg, args));
//		logInfos.add(msg);
//	}
//
//	public static String getAllLogMessages() {
//		return String.join("\n", logMessages);
//	}
//
//	public static String getAllLogInfos() {
//		return String.join("\n", logInfos);
//	}
//
//	public static void clearLogMessages() {
//		logMessages.clear();
//	}
//
//	public static void clearLogInfos() {
//		logInfos.clear();
//	}
//
//	public static boolean banTag(String tag) {
//		if (singleMode) {
//			return !singleTag.equals(tag);
//		}
//
//		for (String tagInfo : showTags) {
//			String[] splits = tagInfo.split(" ");
//			if (splits.length < 2) continue;
//
//			String key = splits[0];
//			String show = splits[1];
//			if (key.equals(tag))
//				return !passStr.equals(show);
//		}
//
//		return true;
//	}
//
//	public void initRootLayout(Stage stage) {
//		Skin skin = uiSkin;
//
//		group = new AutoRunningGroup();
//		group.setBounds(0, 0, stage.getWidth(), stage.getHeight());
//
//		//半透明背景, 无关闭按钮
//		window1 = new DockableWindow("调试栏", skin);
//		titleBar = window1.getTitleBar();
//		TextButton closeBtn = window1.getCloseBtn();
//		content = window1.getContent();
//
//		window1.setBackground(ColorTextureUtils.createColorDrawable(Color.valueOf("00000030")));
//		titleBar.setBackground((Drawable) null);
//
//		closeBtn.clearListeners();
//		closeBtn.addListener(new InputListener() {
//			boolean moved;
//
//			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
//				closeBtn.setChecked(true);
//				moved = false;
//				return true;
//			}
//
//			public void touchDragged(InputEvent event, float x, float y, int pointer) {
//				moved = true;
//			}
//
//			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
//				closeBtn.setChecked(false);
//				if (!moved) {
//					cgLayout();
//				}
//			}
//		});
//
//		window1.setSize(300, 250);
//		window1.setPosition(200, 200);
//
//		window1.getContent().pad(10).top().left().add(createTabPage()).grow();
//		window1.registerEdgeDraggingListener(stage);
//
//		group.autoUpdateRun = () -> {
//			if (debugLogLabel.isVisible()) {
//				if (logViewDirty) {
//					debugLogScrollPane.setVelocityY(0);
//					debugLogLabel.setText(getAllLogMessages());
//					debugLogScrollPane.layout();
//					debugLogScrollPane.setScrollY(debugLogScrollPane.getMaxY());
//
//					logViewDirty = false;
//				}
//			}
//			if (debugLogLabel.isVisible())
//				debugInfoLabel.setText(getAllLogInfos());
//			clearLogInfos();
//		};
//
//		group.addActor(window1);
//		stage.addActor(group);
//
//		window1.docking(0, 9999);
//
//		cgLayout();
//	}
//
//	private void cgLayout() {
//		if (debugViewVisible) {
//			lastHeight = window1.getHeight();
//			lastY = window1.getY();
//		}
//
//		debugViewVisible = !debugViewVisible;
//
//		window1.clear();
//
//		if (!debugViewVisible) {
//			window1.add(titleBar).grow();
//		} else {
//			window1.add(titleBar).growX().row();
//			window1.add(content).grow();
//		}
//
//		float titleHeight = window1.getTitleBar().getHeight();
//		window1.setHeight(debugViewVisible ? lastHeight : titleHeight);
//		window1.setY(debugViewVisible ? lastY : group.getHeight() - titleBar.getHeight());
//	}
//
//	public static class AutoUpdateLabel extends Label {
//		public Runnable autoUpdateRun;
//
//		public AutoUpdateLabel(String text, Skin skin) {
//			super(text, skin, "smallest");
//		}
//
//		@Override
//		public void draw(Batch batch, float parentAlpha) {
//			super.draw(batch, parentAlpha);
//			autoUpdateRun.run();
//		}
//	}
//
//	private static class AutoRunningGroup extends Group {
//		public Runnable autoUpdateRun;
//
//		@Override
//		public void draw(Batch batch, float parentAlpha) {
//			super.draw(batch, parentAlpha);
//			autoUpdateRun.run();
//		}
//	}
//
//
//}
