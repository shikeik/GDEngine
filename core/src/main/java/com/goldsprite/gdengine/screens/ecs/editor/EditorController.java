package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.ComponentRegistry;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.input.ShortcutManager;
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.core.utils.SceneLoader;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
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
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class EditorController {
    private EditorGameScreen screen;
    private Stage stage;

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

	private FileHandle currentProj;

    public EditorController(EditorGameScreen screen) {
        this.screen = screen;
    }

    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();

        // 1. 初始化 Stage (UI)
        float scl = PlatformImpl.isAndroidUser() ? 1.3f : 2.0f;
        stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

        // 2. 加载项目上下文
        reloadProjectContext();

        // 3. 初始化 ECS 核心
        initEcsCore();

        // 4. 初始化图形资源 (Batch 共享)
        neonBatch = new NeonBatch();

        // 5. 组装 MVP 模块
        buildModules();

        // 6. 组装 UI 布局
        buildLayout();

        // 7. 配置输入与快捷键
        setupInput();

		// 监听打开文件事件
		EditorEvents.inst().subscribeOpenFile(this::handleOpenFile);

        // 8. 启动初始场景 (延迟一帧以确保 UI 布局就绪)
        Gdx.app.postRunnable(this::loadInitialScene);
    }

    private void reloadProjectContext() {
        currentProj = ProjectService.inst().getCurrentProject();
        if (currentProj != null) {
            GameWorld.projectAssetsRoot = currentProj.child("assets");
            Debug.logT("Editor", "🔗 链接到项目: " + currentProj.name());

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

        // 初始化逻辑层相机和渲染系统 (用于 Raycast)
        gameCamera = new OrthographicCamera();
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

	// [核心重构] 布局构建
	private void buildLayout() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");

		// --- 1. Top Toolbar (New) ---
		VisTable toolbar = createTopToolbar();
		root.add(toolbar).growX().height(35).row();

		// --- 2. Center Area (Preview & Code) ---
		// Tab 1: Preview (Split: Scene | Game)
		Stack previewStack = new Stack();
		VisSplitPane previewSplit = new VisSplitPane(scenePanel, gamePanel, true);
		previewSplit.setSplitAmount(0.5f);
		previewStack.add(previewSplit);

		// SmartTabPane: [Preview] [Code]
		centerTabs = new SmartTabPane();
		centerTabs.addTab("Preview", previewStack);
		centerTabs.addTab("Code", codePanel);
		centerTabs.getTabbedPane().switchTab(0); // 默认显示 Preview

		// --- 3. Top Split: Hierarchy (Left) | CenterTabs (Right) ---
		VisSplitPane topSectionSplit = new VisSplitPane(hierarchyPanel, centerTabs, false);
		topSectionSplit.setSplitAmount(0.2f);

		// --- 4. Bottom Tabs: Project & Console ---
		SmartTabPane bottomTabs = new SmartTabPane();
		bottomTabs.addTab("Project", projectPanel);
		bottomTabs.addTab("Console", consolePanel);
		bottomTabs.getTabbedPane().switchTab(0);

		// --- 5. Main Left Split: Top Section / Bottom Tabs ---
		VisSplitPane leftMainSplit = new VisSplitPane(topSectionSplit, bottomTabs, true);
		leftMainSplit.setSplitAmount(0.7f);

		// --- 6. Root Split: LeftMain | Inspector (Right) ---
		VisSplitPane rootSplit = new VisSplitPane(leftMainSplit, inspectorPanel, false);
		rootSplit.setSplitAmount(0.8f);

		root.add(rootSplit).grow();
		stage.addActor(root);
		stage.addActor(new ToastUI());
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

		// Right: Functional Buttons
		// [Build]
		VisTextButton btnBuild = new VisTextButton("Build");
		btnBuild.setColor(Color.GOLD);
		btnBuild.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				ToastUI.inst().show("Build started... (Mock)");
				// TODO: Phase 2 Implement Build Logic
			}
		});
		bar.add(btnBuild).padRight(10);

		// [Run Editor]
		VisTextButton btnRunEditor = new VisTextButton("Run Editor");
		btnRunEditor.setColor(Color.GREEN);
		btnRunEditor.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				ToastUI.inst().show("Play Mode Starting... (Mock)");
				// TODO: Phase 3 Implement Play Mode
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
			// 打开 Code Tab
			centerTabs.getTabbedPane().switchTab(1); // Index 1 is Code
			codePanel.openFile(file);
		}
		else if (ext.equals("scene")) {
			// 切换到 Scene Tab (Index 0) 并加载
			centerTabs.getTabbedPane().switchTab(0);
			// 这里需要 ScenePresenter 提供加载指定文件的 API
			// 暂时先用 Toast 演示，后续打通 ScenePresenter.loadScene(file)
			ToastUI.inst().show("Loading Scene: " + file.name());

			// TODO: 调用 scenePresenter.loadScene(file);
			// 现有 loadScene 是无参的，需要改造 ScenePresenter
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
        shortcutManager.register("ACTION_SAVE", () -> scenePresenter.saveScene());
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
            initTestScene();
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
        sp.width = 100; sp.height = 100;

        GObject child = new GObject("Weapon");
        child.setParent(player);
        child.transform.setPosition(80, 0);
        child.transform.setScale(0.5f);
        SpriteComponent sp2 = child.addComponent(SpriteComponent.class);
        sp2.setPath("gd_icon.png");
        sp2.width = 100; sp2.height = 100;
        sp2.color.set(Color.RED);
    }

    // --- Loop ---

    public void render(float delta) {
        // 1. 逻辑更新
        GameWorld.inst().update(delta);

        // 2. 模块渲染更新 (委托给 Presenters)
        scenePresenter.update(delta);
        gamePresenter.update(delta);

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
