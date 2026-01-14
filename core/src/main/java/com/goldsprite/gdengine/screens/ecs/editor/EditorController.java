package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
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
	// [修改] 声明类型变化
	private ConsolePanel consolePanel; // 用 LogPanel 替换 ConsolePanel

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
        // 注入 SceneManager, NeonBatch, RenderSystem (用于点击检测)
        scenePresenter = new ScenePresenter(scenePanel, sceneManager, neonBatch, worldRenderSystem);

        // Game View (负责游戏相机渲染)
        gamePanel = new GamePanel();
        gamePresenter = new GamePresenter(gamePanel, neonBatch);

        // Project Module
        projectPanel = new ProjectPanel();
        projectPresenter = new ProjectPresenter(projectPanel);

		// [修改] 隐藏 ProjectPanel 的标题栏 (因为 Tab 栏已经有了标题)
		projectPanel.setHeaderVisible(false);

		// [修改] 使用新的 LogPanel
		consolePanel = new ConsolePanel();
		// 同样隐藏标题栏
		consolePanel.setHeaderVisible(false);

        // 跨模块交互：从 Hierarchy 拖拽到 Scene
        setupDragAndDrop();
    }

	// [核心修改] 布局重组
	private void buildLayout() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");

		// 1. Center Area (Scene | Game)
		Stack sceneStack = new Stack();
		VisSplitPane sceneGameSplit = new VisSplitPane(scenePanel, gamePanel, true);
		sceneGameSplit.setSplitAmount(0.5f);
		sceneStack.add(sceneGameSplit);

		// 2. Top Split (Hierarchy | SceneStack)
		VisSplitPane topSplit = new VisSplitPane(hierarchyPanel, sceneStack, false);
		topSplit.setSplitAmount(0.2f);

		// 3. Bottom Tabs: Project & Console [使用 SmartTabPane]
		SmartTabPane bottomTabs = new SmartTabPane();
		bottomTabs.addTab("Project", projectPanel);
		bottomTabs.addTab("Console", consolePanel);
		bottomTabs.getTabbedPane().switchTab(0);

		// 4. Main Left Split (Top / Bottom)
		VisSplitPane leftMainSplit = new VisSplitPane(topSplit, bottomTabs, true);
		leftMainSplit.setSplitAmount(0.7f);

		// 5. Root Split (LeftMain | Inspector)
		VisSplitPane rootSplit = new VisSplitPane(leftMainSplit, inspectorPanel, false);
		rootSplit.setSplitAmount(0.8f);

		root.add(rootSplit).grow();
		stage.addActor(root);
		stage.addActor(new ToastUI());
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
