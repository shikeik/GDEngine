package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.ComponentRegistry;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.input.ShortcutManager;
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.core.project.model.ProjectConfig;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.core.utils.SceneLoader;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.neonbatch.NeonStage;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.code.CodePanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.console.ConsolePanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.game.GamePanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.game.GamePresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy.HierarchyPanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy.HierarchyPresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.InspectorPanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.InspectorPresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.project.ProjectPanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.project.ProjectPresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.scene.ScenePanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.scene.ScenePresenter;
import com.goldsprite.gdengine.ui.widget.SmartTabPane;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.utils.ScreenUtils;

public class EditorController {
	private FileHandle currentProj;

	// [新增] 当前运行的用户脚本实例
	private IGameScriptEntry currentUserScript;

	private final EditorGameScreen screen;
	private NeonStage stage;

	// --- Core Logic Systems (Global) ---
	private CommandManager commandManager;
	private EditorSceneManager sceneManager;
	private ShortcutManager shortcutManager;

	// --- Shared Resources ---
	private NeonBatch neonBatch;
	private WorldRenderSystem worldRenderSystem; // 逻辑层需要，传递给 ScenePresenter 做检测
	private OrthographicCamera gameCamera;       // 逻辑层游戏相机

	// --- MVP Modules ---
	private HierarchyPanel hierarchyPanel;
	private InspectorPanel inspectorPanel;
	private ScenePanel scenePanel;
	private ScenePresenter scenePresenter;
	private GamePanel gamePanel;
	private GamePresenter gamePresenter;
	private ProjectPanel projectPanel;
	private ProjectPresenter projectPresenter;
	// 声明类型变化
	private ConsolePanel consolePanel;
	private CodePanel codePanel;

	// 中央 Tab 面板引用，用于代码跳转
	private SmartTabPane centerTabs;

	// [新增] 提升 SplitPane 为成员变量，以便控制布局
	private VisSplitPane topSectionSplit;
	private VisSplitPane leftMainSplit;
	private VisSplitPane rootSplit;
	private VisSplitPane previewSplit; // [新增]
	private boolean isCodeMaximized;
	private boolean isGameMaximized; // [新增]

	// [新增] 提升为成员变量，以便在编译失败时切换 Tab
	private SmartTabPane bottomTabs;
	// [新增] 引用 Build 按钮以便改色
	private VisTextButton btnBuild;

	// [新增] 状态标签
	private VisLabel statusLabel;

	// [新增] 布局配置
	private static class LayoutConfig {
		float rootSplit = 0.8f;
		float leftMainSplit = 0.7f;
		float topSectionSplit = 0.2f;
		float previewSplit = 0.5f;

		public static LayoutConfig createDefault() {
			return new LayoutConfig();
		}
	}

	private final LayoutConfig defaultLayout = LayoutConfig.createDefault();
	private LayoutConfig currentLayout = LayoutConfig.createDefault();

	// [新增]
	private EditorState currentEditorState = EditorState.CLEAN;

	// [新增] Run Editor 按钮引用，用于改文字/颜色
	private VisTextButton btnRunEditor;
	private FileHandle tempSceneSnapshot; // 临时快照文件

	public EditorController(EditorGameScreen screen) {
		this.screen = screen;
	}

	public void create(Viewport viewport) {
		if (!VisUI.isLoaded()) VisUI.load();

		// 1. 初始化 Stage (UI)
		stage = new NeonStage(viewport);

		// 2. 加载项目上下文
		reloadProjectContext();

		// 3. 初始化图形资源 (Batch 共享)
		neonBatch = new NeonBatch();

		// 4. 初始化 ECS 核心
		initEcsCore();

		// 5. 组装 MVP 模块
		buildModules();

		// 6. 组装 UI 布局
		buildLayout();

		// 7. 配置输入与快捷键
		setupInput();

		// 监听打开文件事件
		EditorEvents.inst().subscribeOpenFile(this::handleOpenFile);

		// 监听最大化事件
		EditorEvents.inst().subscribeToggleMaximizeCode(this::toggleCodeMaximize);
		EditorEvents.inst().subscribeToggleMaximizeGame(this::toggleGameMaximize); // [新增]

		// [新增] 监听编译状态
		EditorEvents.inst().subscribeCodeDirty(this::onCodeDirty);
		EditorEvents.inst().subscribeCodeClean(this::onCodeClean);

		// ---------------------------------------------------------------
		// [核心修改]
		// 1. 如果这里之前有 performBuild()，请删除它！
		// 2. 强制设置初始状态为 DIRTY
		//    这样进入编辑器后，Preview 视图会黑屏提示 "Please Build"，Build 按钮变红。
		//    这符合 "未编译不渲染" 的安全逻辑。
		// ---------------------------------------------------------------
		updateEditorState(EditorState.DIRTY);

		// 8. 启动初始场景 (延迟一帧以确保 UI 布局就绪)
		Gdx.app.postRunnable(this::loadInitialScene);
	}

	private void reloadProjectContext() {
		currentProj = ProjectService.inst().getCurrentProject();
		if (currentProj != null) {
			GameWorld.projectAssetsRoot = currentProj.child("assets");
			Debug.logT("Editor", "🔗 链接到项目: " + currentProj.name());

			ComponentRegistry.reloadEngineIndex(); // 加载引擎组件索引

			FileHandle indexFile = currentProj.child("project.index");
			if (indexFile.exists()) {
				ComponentRegistry.reloadUserIndex(indexFile);
			} else {
				Debug.logT("Editor", "⚠️ project.index not found.");
			}
		}
	}

	private void initEcsCore() {
		GameWorld.autoDispose();
		new GameWorld();

		// 初始化逻辑层相机和渲染系统 (用于 Ray-cast)
		gameCamera = new OrthographicCamera(1280, 720);
		worldRenderSystem = new WorldRenderSystem(neonBatch, gameCamera);

		// 绑定全局引用
		GameWorld.inst().setReferences(stage.getViewport(), gameCamera);

		commandManager = new CommandManager();
		sceneManager = new EditorSceneManager(commandManager);

		// 事件桥接：SceneManager -> EventBus
		sceneManager.onStructureChanged.add(o -> EditorEvents.inst().emitStructureChanged());
		sceneManager.onSelectionChanged.add(o -> EditorEvents.inst().emitSelectionChanged(o));
	}

	private void buildModules() {
		// Hierarchy
		hierarchyPanel = new HierarchyPanel();
		new HierarchyPresenter(hierarchyPanel, sceneManager);

		// Inspector
		inspectorPanel = new InspectorPanel();
		new InspectorPresenter(inspectorPanel, sceneManager);

		// Scene View (负责编辑渲染和交互)
		scenePanel = new ScenePanel();
		scenePanel.setHeaderVisible(false); // [Fix 4]
		// 注入 SceneManager, NeonBatch, RenderSystem (用于点击检测)
		scenePresenter = new ScenePresenter(scenePanel, sceneManager, neonBatch, worldRenderSystem);

		// Game View (负责游戏相机渲染)
		gamePanel = new GamePanel();
		gamePanel.setHeaderVisible(false); // [Fix 4]
		gamePresenter = new GamePresenter(gamePanel, neonBatch);

		// Project Module
		projectPanel = new ProjectPanel();
		projectPresenter = new ProjectPresenter(projectPanel);

		// 隐藏 ProjectPanel 的标题栏 (因为 Tab 栏已经有了标题)
		projectPanel.setHeaderVisible(false);

		// 使用新的 LogPanel
		consolePanel = new ConsolePanel();
		// 同样隐藏标题栏
		consolePanel.setHeaderVisible(false);

		// [新增] Code
		codePanel = new CodePanel();
		codePanel.setHeaderVisible(false); // Code tab 不需要标题

		// 跨模块交互：从 Hierarchy 拖拽到 Scene
		setupDragAndDrop();
	}

	float[] safePad = {20, 40, 20, 20}; // 上左下右
	// [核心重构] 布局构建
	private void buildLayout() {
		VisTable root = new VisTable();
		root.setBackground("window-bg");

		// --- 1. Top Toolbar (New) ---
		VisTable toolbar = createTopToolbar();
		root.add(toolbar).growX().height(35).row();

		// --- 2. Center Area (Preview & Code) ---
		// Tab 1: Preview (Split: Scene | Game)
		Stack previewStack = new Stack();
		previewSplit = new VisSplitPane(scenePanel, gamePanel, false); // 是否竖排列
		previewSplit.setSplitAmount(defaultLayout.previewSplit);
		previewStack.add(previewSplit);

		// SmartTabPane: [Preview] [Code]
		centerTabs = new SmartTabPane();
		centerTabs.addTab("Preview", previewStack);
		centerTabs.addTab("Code", codePanel);
		centerTabs.getTabbedPane().switchTab(0); // 默认显示 Preview

		// --- 3. Top Split: Hierarchy (Left) | CenterTabs (Right) ---
		// [修改] 赋值给成员变量
		topSectionSplit = new VisSplitPane(hierarchyPanel, centerTabs, false);
		topSectionSplit.setSplitAmount(defaultLayout.topSectionSplit);

		// --- 4. Bottom Tabs: Project & Console ---
		// [修改] 赋值给成员变量
		bottomTabs = new SmartTabPane();
		bottomTabs.addTab("Project", projectPanel);
		bottomTabs.addTab("Console", consolePanel); // 假设 Console 是第 2 个 (Index 1)
		bottomTabs.getTabbedPane().switchTab(0);

		// --- 5. Main Left Split: Top Section / Bottom Tabs ---
		// [修改] 赋值给成员变量
		leftMainSplit = new VisSplitPane(topSectionSplit, bottomTabs, true);
		leftMainSplit.setSplitAmount(defaultLayout.leftMainSplit);

		// --- 6. Root Split: LeftMain | Inspector (Right) ---
		// [修改] 赋值给成员变量
		rootSplit = new VisSplitPane(leftMainSplit, inspectorPanel, false);
		rootSplit.setSplitAmount(defaultLayout.rootSplit);

		root.add(rootSplit).grow();

		VisTable rootWrap = new VisTable();
		rootWrap.setFillParent(true);
		if(PlatformImpl.isDesktopUser()) safePad = new float[4];
		rootWrap.add(root).pad(safePad[0], safePad[1], safePad[2], safePad[3]).grow();
		stage.addActor(rootWrap);
	}

	private void applyLayout(LayoutConfig config) {
		if (rootSplit != null) {
			rootSplit.setSplitAmount(config.rootSplit);
			if (inspectorPanel != null) inspectorPanel.setVisible(config.rootSplit < 1.0f);
		}
		if (leftMainSplit != null) {
			leftMainSplit.setSplitAmount(config.leftMainSplit);
			if (bottomTabs != null) bottomTabs.setVisible(config.leftMainSplit < 1.0f);
		}
		if (topSectionSplit != null) {
			topSectionSplit.setSplitAmount(config.topSectionSplit);
			if (hierarchyPanel != null) hierarchyPanel.setVisible(config.topSectionSplit > 0.0f);
		}
		if (previewSplit != null) {
			previewSplit.setSplitAmount(config.previewSplit);
			if (scenePanel != null) scenePanel.setVisible(config.previewSplit > 0.0f);
		}
	}

	private void toggleCodeMaximize() {
		isCodeMaximized = !isCodeMaximized;
		Debug.log("toggleCodeMaximize %s", isCodeMaximized);

		if (isCodeMaximized) {
			Debug.log("进入独占");
			// [进入独占模式]
			LayoutConfig maxConfig = new LayoutConfig();
			// 1. 隐藏右侧 Inspector (Split -> 1.0)
			maxConfig.rootSplit = 1.0f;
			// 2. 保持底部 Project/Console 可见 (使用默认值)
			maxConfig.leftMainSplit = defaultLayout.leftMainSplit;
			// 3. 隐藏左侧 Hierarchy (Split -> 0.0)
			maxConfig.topSectionSplit = 0.0f;
			// 4. Preview 不需要变，因为切到 Code Tab 了
			maxConfig.previewSplit = defaultLayout.previewSplit;

			applyLayout(maxConfig);

			// 确保切到 Code
			centerTabs.getTabbedPane().switchTab(1);
			ToastUI.inst().show("Code View Expanded");
		} else {
			Debug.log("恢复 取消独占");
			applyLayout(defaultLayout);
		}
	}

	private void toggleGameMaximize() {
		isGameMaximized = !isGameMaximized;
		Debug.log("toggleGameMaximize %s", isGameMaximized);

		if (isGameMaximized) {
			Debug.log("进入游戏独占");
			LayoutConfig maxConfig = new LayoutConfig();
			// 1. Hide Inspector (Right)
			maxConfig.rootSplit = 1.0f;
			// 2. Hide Bottom (Console/Project) -> Top full
			maxConfig.leftMainSplit = 1.0f;
			// 3. Hide Hierarchy (Left) -> CenterTabs full
			maxConfig.topSectionSplit = 0.0f;
			// 4. Hide Scene (Left of Preview) -> Game full
			maxConfig.previewSplit = 0.0f;

			applyLayout(maxConfig);

			// 5. Ensure Preview Tab is selected
			if (centerTabs != null) centerTabs.getTabbedPane().switchTab(0);
			ToastUI.inst().show("Game View Expanded");
		} else {
			Debug.log("恢复 游戏独占");
			applyLayout(defaultLayout);
		}
	}

	private VisTable createTopToolbar() {
		VisTable bar = new VisTable();
		bar.setBackground("button");
		bar.pad(0, 10, 0, 10);

		// Left: Menus (Fake for now)
		bar.add(createMenuBtn("File")).padRight(5);
		bar.add(createMenuBtn("Edit")).padRight(5);
		bar.add(createMenuBtn("Assets")).padRight(5);
		bar.add(createMenuBtn("GameObject")).padRight(5);
		bar.add(createMenuBtn("Component")).padRight(5);
		bar.add(createMenuBtn("Window")).padRight(5);
		bar.add(createMenuBtn("Help"));

		bar.add().expandX(); // Spacer
		// [新增] 状态标签 (放在 Build 按钮左边)
		statusLabel = new VisLabel("[ CLEAN ]");
		statusLabel.setColor(Color.GREEN);
		bar.add(statusLabel).padRight(15);

		// Right: Functional Buttons
		// [Build]
		btnBuild = new VisTextButton("Build"); // 赋值给成员变量
		btnBuild.setColor(Color.GOLD);
		btnBuild.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				performBuild();
			}
		});
		bar.add(btnBuild).padRight(10);


		// [Run Editor]
		btnRunEditor = new VisTextButton("Run Editor");
		btnRunEditor.setColor(Color.GREEN);
		btnRunEditor.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				toggleRunEditor();
			}
		});
		bar.add(btnRunEditor).padRight(10);

		// [Run Game]
		VisTextButton btnRunGame = new VisTextButton("Run Game");
		btnRunGame.setColor(Color.CYAN);
		btnRunGame.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				// TODO: Link to existing GameRunner
				ToastUI.inst().show("Launching Runner...");
			}
		});
		bar.add(btnRunGame);

		return bar;
	}

	// [核心逻辑] 切换运行/停止状态
	private void toggleRunEditor() {
		// 安全检查：代码是否脏了
		if (currentEditorState == EditorState.DIRTY) {
			ToastUI.inst().show("Please BUILD code first!");
			return;
		}

		GameWorld world = GameWorld.inst();
		if (world.isEditorMode()) {
			// >>> 开始运行 (Start)
			startEditorRun();
		} else {
			// >>> 停止运行 (Stop)
			stopEditorRun();
		}
	}

	// [核心逻辑] 注入用户脚本生命周期
	private void startEditorRun() {
		Debug.logT("Editor", ">>> Enter PLAY Mode");

		// 1. Snapshot & Mode Switch
		tempSceneSnapshot = Gdx.files.local("build/temp_editor_snapshot.scene");
		SceneLoader.saveCurrentScene(tempSceneSnapshot);
		GameWorld.inst().setMode(GameWorld.Mode.PLAY);

		// 2. Reload Scene
		SceneLoader.load(tempSceneSnapshot);

		// 3. UI Update
		btnRunEditor.setText("Stop");
		btnRunEditor.setColor(Color.RED);
		centerTabs.getTabbedPane().switchTab(0);
		ToastUI.inst().show("Game Started");

		// [新增] 禁用 Save/Load
		scenePanel.setStorageEnabled(false);

		EditorEvents.inst().emitStructureChanged();
		sceneManager.select(null);

		// 4. [新增] 启动用户入口脚本 (IGameScriptEntry)
		// 这一步模拟 GameRunner 的启动逻辑
		launchUserScript();
	}

	private void launchUserScript() {
		if (currentProj == null) return;

		try {
			// 4.1 读取配置找入口类
			String entryClassName = "com.game.Main";
			FileHandle configFile = currentProj.child("project.json");
			if (configFile.exists()) {
				ProjectConfig cfg = new Json().fromJson(ProjectConfig.class, configFile);
				if (cfg.entryClass != null) entryClassName = cfg.entryClass;
			}

			// 4.2 反射实例化
			// 注意：必须用 Gd.scriptClassLoader，否则找不到用户类
			Class<?> cls = Class.forName(entryClassName, true, Gd.scriptClassLoader);
			if (IGameScriptEntry.class.isAssignableFrom(cls)) {
				currentUserScript = (IGameScriptEntry) cls.getDeclaredConstructor().newInstance();

				// 4.3 调用 onStart
				Debug.logT("Editor", "🚀 Launching User Script: " + entryClassName);
				currentUserScript.onStart(GameWorld.inst());
			} else {
				Debug.logT("Editor", "Entry class must implement IGameScriptEntry");
			}

		} catch (Exception e) {
			Debug.logT("Editor", "❌ Failed to launch user script: " + e.getMessage());
			e.printStackTrace();
			// 运行出错不强制停止，允许只跑场景
		}
	}

	private void stopEditorRun() {
		Debug.logT("Editor", "<<< Exit PLAY Mode");

		// 1. [新增] 清理用户脚本
		currentUserScript = null;

		// 2. Mode Switch
		GameWorld.inst().setMode(GameWorld.Mode.EDIT);

		// 3. Restore Snapshot
		if (tempSceneSnapshot != null && tempSceneSnapshot.exists()) {
			SceneLoader.load(tempSceneSnapshot);
		} else {
			GameWorld.inst().clear();
		}

		// 4. UI Update
		btnRunEditor.setText("Run Editor");
		btnRunEditor.setColor(Color.GREEN);

		// [新增] 恢复 Save/Load
		scenePanel.setStorageEnabled(true);

		ToastUI.inst().show("Game Stopped");

		EditorEvents.inst().emitStructureChanged();
		sceneManager.select(null);
	}

	// --- 状态响应 (重写) ---

	private void updateEditorState(EditorState state) {
		this.currentEditorState = state;

		// 1. 通知 GamePresenter 停止/恢复渲染
		if (gamePresenter != null) {
			gamePresenter.setEditorState(state);
		}

		// 2. 更新 UI
		if (statusLabel != null && btnBuild != null) {
			switch (state) {
				case CLEAN:
					statusLabel.setText("[ CLEAN ]");
					statusLabel.setColor(Color.GREEN);
					btnBuild.setColor(Color.GOLD);
					btnBuild.setText("Build");
					btnBuild.setDisabled(false);
					break;
				case DIRTY:
					statusLabel.setText("[ DIRTY ]");
					statusLabel.setColor(Color.ORANGE);
					btnBuild.setColor(Color.SCARLET); // 醒目红
					btnBuild.setText("Build *");
					btnBuild.setDisabled(false);
					break;
				case COMPILING:
					statusLabel.setText("[ BUILDING... ]");
					statusLabel.setColor(Color.CYAN);
					btnBuild.setColor(Color.GRAY);
					btnBuild.setText("Wait...");
					btnBuild.setDisabled(true); // 编译中禁止再次点击
					break;
			}
		}
	}

	// [核心构建逻辑] 完全复刻并优化 BuildAndRun
	private void performBuild() {
		// 1. 自动保存代码 (如同 GDEngineEditorScreen)
		codePanel.save();

		FileHandle projectDir = ProjectService.inst().getCurrentProject();
		if (projectDir == null) { ToastUI.inst().show("Error: No Project"); return; }
		if (Gd.compiler == null) { ToastUI.inst().show("Error: No Compiler"); return; }

		// 1. 设置状态为编译中
		updateEditorState(EditorState.COMPILING);
		ToastUI.inst().show("Compiling...");

		new Thread(() -> {
			try {
				// 2. [关键] 注入项目资源上下文 (抄自 buildAndRun)
				// 确保编译后的组件初始化时能找到图片等资源
				GameWorld.projectAssetsRoot = projectDir.child("assets");
				if (!GameWorld.projectAssetsRoot.exists()) {
					GameWorld.projectAssetsRoot.mkdirs();
				}

				// 3. 获取入口配置 (抄自 buildAndRun)
				String entryClass = "com.game.Main";
				FileHandle configFile = projectDir.child("project.json");
				if (configFile.exists()) {
					try {
						ProjectConfig cfg = new Json().fromJson(ProjectConfig.class, configFile);
						if (cfg != null && cfg.entryClass != null && !cfg.entryClass.isEmpty()) {
							entryClass = cfg.entryClass;
						}
					} catch (Exception e) {
						Debug.logT("Editor", "Config error: " + e.getMessage());
					}
				}

				String projectPath = projectDir.file().getAbsolutePath();
				long startTime = System.currentTimeMillis();

				// 4. 执行编译
				// DesktopScriptCompiler 会生成 .class 并返回加载了这些类的 ClassLoader 里的 MainClass
				Class<?> resultMainClass = Gd.compiler.compile(entryClass, projectPath);

				long duration = System.currentTimeMillis() - startTime;

				Gdx.app.postRunnable(() -> {
					if (resultMainClass != null) {
						// 5. [关键] 更新全局脚本加载器
						// 这样 ComponentRegistry 才能通过反射加载到用户新写的组件
						Gd.scriptClassLoader = resultMainClass.getClassLoader();
						Debug.logT("Editor", "ClassLoader Updated: " + Gd.scriptClassLoader);

						onBuildSuccess(projectDir, duration);
					} else {
						onBuildFail();
					}
				});

			} catch (Exception e) {
				e.printStackTrace();
				Gdx.app.postRunnable(() -> {
					Debug.logT("Compiler", "Exception: " + e.getMessage());
					onBuildFail();
				});
			}
		}).start();
	}

	// --- 状态响应 ---
	// 绑定事件回调
	private void onCodeDirty() {
		updateEditorState(EditorState.DIRTY);
	}

	private void onCodeClean() {
		// 只有 Build 成功才会调用这个，所以逻辑是对的
		updateEditorState(EditorState.CLEAN);
	}

	private void onBuildSuccess(FileHandle projectDir, long duration) {
		// 6. 刷新组件注册表 (使用上面更新过的 Gd.scriptClassLoader)
		FileHandle indexFile = projectDir.child("project.index");
		ComponentRegistry.reloadUserIndex(indexFile);

		// 7. 刷新 Inspector UI
		GObject currentSelection = sceneManager.getSelection();
		if (currentSelection != null) {
			EditorEvents.inst().emitSelectionChanged(currentSelection);
		}

		// 2. 恢复干净状态
		// emitCodeClean 会调用 updateEditorState(CLEAN)
		EditorEvents.inst().emitCodeClean();

		// 便于测试, 成功也打开 Console 面板
		if (bottomTabs != null) {
			bottomTabs.getTabbedPane().switchTab(1); // Console
		}

		ToastUI.inst().show("Build Success (" + duration + "ms)");
		Debug.logT("Compiler", "[GREEN]Build finished in " + duration + "ms");
	}

	private void onBuildFail() {
		// 3. 编译失败，保持 Dirty 状态 (或者是 Error 状态，这里暂用 Dirty 提示用户重试)
		updateEditorState(EditorState.DIRTY);

		if (bottomTabs != null) {
			bottomTabs.getTabbedPane().switchTab(1); // Console
		}
		ToastUI.inst().show("Build Failed!");
	}

	private VisTextButton createMenuBtn(String text) {
		VisTextButton btn = new VisTextButton(text);
		// btn.setStyle(...); // 可以设置无边框样式
		btn.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				ToastUI.inst().show("Menu: " + text);
			}
		});
		return btn;
	}

	// [核心逻辑] 处理文件打开
	private void handleOpenFile(FileHandle file) {
		if (file.isDirectory()) return;

		String ext = file.extension().toLowerCase();

		if (ext.equals("java") || ext.equals("json") || ext.equals("xml")) {
			centerTabs.getTabbedPane().switchTab(1); // Code
			codePanel.openFile(file);
		}
		else if (ext.equals("scene")) {
			centerTabs.getTabbedPane().switchTab(0); // Preview
			scenePresenter.loadScene(file);
		}
	}

	private void setupInput() {
		shortcutManager = new ShortcutManager(stage);

		// 注册快捷键 -> 代理给 ScenePresenter
		shortcutManager.register("TOOL_MOVE", () -> scenePresenter.setGizmoMode(EditorGizmoSystem.Mode.MOVE));
		shortcutManager.register("TOOL_ROTATE", () -> scenePresenter.setGizmoMode(EditorGizmoSystem.Mode.ROTATE));
		shortcutManager.register("TOOL_SCALE", () -> scenePresenter.setGizmoMode(EditorGizmoSystem.Mode.SCALE));

		shortcutManager.register("ACTION_UNDO", () -> commandManager.undo());
		shortcutManager.register("ACTION_REDO", () -> commandManager.redo());
		// [修改] Save 快捷键增加模式检查
		shortcutManager.register("ACTION_SAVE", () -> {
			// 运行时禁止保存，防止把测试状态覆盖掉源文件
			if (GameWorld.inst().isPlayMode()) {
				ToastUI.inst().show("Cannot Save in Play Mode!");
				return;
			}
			scenePresenter.saveScene();
		});
		shortcutManager.register("ACTION_DELETE", () -> sceneManager.deleteSelection());

		// 输入管线
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);           // 1. UI 优先
		multiplexer.addProcessor(shortcutManager); // 2. 快捷键

		// 3. Scene View 输入 (Gizmo, Picking, Camera) -> 委托给 Presenter
		scenePresenter.registerInput(multiplexer);

		// 应用输入处理器
		if (screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(multiplexer);
		} else {
			Gd.input.setInputProcessor(multiplexer);
		}
	}

	private void setupDragAndDrop() {
		DragAndDrop dnd = hierarchyPanel.getDragAndDrop();
		if (dnd != null) {
			// 使用 HierarchyPanel 的保护方法添加 Target
			hierarchyPanel.addSceneDropTarget(new Target(scenePanel.getDropTargetActor()) {
				@Override
				public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
					return true;
				}

				@Override
				public void drop(Source source, Payload payload, float x, float y, int pointer) {
					// 未来可以在这里处理“拖拽prefab实例化”
				}
			});
		}
	}

	private void loadInitialScene() {
		FileHandle projectScene = getSceneFile();
		if (projectScene != null && projectScene.exists()) {
			scenePresenter.loadScene();
		} else if (Gdx.files.local("scene_debug.json").exists() && currentProj == null) {
			SceneLoader.load(Gdx.files.local("scene_debug.json"));
			EditorEvents.inst().emitStructureChanged();
			EditorEvents.inst().emitSceneLoaded();
		} else {
//			initTestScene(); // 现在不需要了
			EditorEvents.inst().emitStructureChanged();
		}
	}

	private FileHandle getSceneFile() {
		if (currentProj != null) {
			return currentProj.child("scenes/main.scene");
		}
		return Gdx.files.local("scene_debug.json");
	}

	private void initTestScene() {
		// 创建默认测试场景
		GObject player = new GObject("Player");
		player.transform.setPosition(0, 0);
		SpriteComponent sp = player.addComponent(SpriteComponent.class);
		sp.setPath("gd_icon.png");
		sp.width = 100;
		sp.height = 100;

		GObject child = new GObject("Weapon");
		child.setParent(player);
		child.transform.setPosition(80, 0);
		child.transform.setScale(0.5f);
		SpriteComponent sp2 = child.addComponent(SpriteComponent.class);
		sp2.setPath("gd_icon.png");
		sp2.width = 100;
		sp2.height = 100;
		sp2.color.set(Color.RED);
	}

	// --- Loop ---

	// [新增] 在主循环中驱动用户脚本
	public void render(float delta) {
		// 1. 逻辑更新
		// 如果有用户脚本，先跑它的 onUpdate (通常处理全局逻辑/输入)
		if (currentUserScript != null && GameWorld.inst().isPlayMode()) {
			try {
				currentUserScript.onUpdate(delta);
			} catch (Exception e) {
				Debug.logT("Editor", "Script Runtime Error: " + e.getMessage());
				// 出错后为了防止刷屏，可以暂停或移除
				currentUserScript = null;
			}
		}
		// 1. 逻辑更新
		GameWorld.inst().update(delta);

		// 2. 模块渲染更新 (委托给 Presenters)
		scenePresenter.update(delta);
		gamePresenter.update(delta);

		ScreenUtils.clear(Color.LIGHT_GRAY);
		// 3. UI 渲染
		stage.act(delta);
		stage.draw();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	public void dispose() {
		if (stage != null) stage.dispose();
		if (neonBatch != null) neonBatch.dispose();

		// Modules dispose
		if (scenePanel != null) scenePanel.dispose();
		if (gamePanel != null) gamePanel.dispose();

		// 清理全局事件
		EditorEvents.inst().clear();
	}
}
