package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.game.GamePanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.game.GamePresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy.HierarchyPanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy.HierarchyPresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.InspectorPanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.InspectorPresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.scene.ScenePanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.scene.ScenePresenter;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;

public class EditorController {
	private EditorGameScreen screen;
	private Stage stage;

	// --- Core Logic Systems ---
	private CommandManager commandManager;
	private EditorSceneManager sceneManager;
	private ShortcutManager shortcutManager;

	// --- Rendering Core ---
	// 这些核心对象由 Controller 创建并分发给需要的 Presenter
	private NeonBatch neonBatch;
	private WorldRenderSystem worldRenderSystem;
	private OrthographicCamera gameCamera; // 用于 Game View 的逻辑相机

	// --- MVP Modules ---
	private HierarchyPanel hierarchyPanel;
	private InspectorPanel inspectorPanel;
	private ScenePanel scenePanel;
	private ScenePresenter scenePresenter; // 需要持有引用以处理 Shortcut
	private GamePanel gamePanel;
	private GamePresenter gamePresenter;

	public EditorController(EditorGameScreen screen) {
		this.screen = screen;
	}

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		// 1. 初始化 Stage (UI)
		float scl = PlatformImpl.isAndroidUser() ? 1.3f : 2.0f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		// 2. 加载项目上下文 (Project Index)
		reloadProjectContext();

		// 3. 初始化 ECS 核心
		initEcsCore();

		// 4. 初始化图形资源
		neonBatch = new NeonBatch();

		// 5. [核心] 组装 MVP 模块
		buildModules();

		// 6. 组装 UI 布局
		buildLayout();

		// 7. 配置输入与快捷键
		setupInput();

		// 8. 启动初始场景
		// 延迟一帧调用 Load，确保 UI 布局完成
		Gdx.app.postRunnable(() -> scenePresenter.loadScene());
	}

	private void reloadProjectContext() {
		FileHandle currentProj = ProjectService.inst().getCurrentProject();
		if (currentProj != null) {
			GameWorld.projectAssetsRoot = currentProj.child("assets");
			Debug.logT("Editor", "🔗 链接到项目: " + currentProj.name());

			FileHandle indexFile = currentProj.child("project.index");
			if (indexFile.exists()) {
				ComponentRegistry.reloadUserIndex(indexFile);
			} else {
				Debug.logT("Editor", "⚠️ project.index not found. (Compile to generate)");
			}
		}
	}

	private void initEcsCore() {
		GameWorld.autoDispose();
		new GameWorld();

		// Game Camera 属于核心逻辑的一部分 (RenderSystem 需要它)
		gameCamera = new OrthographicCamera();
		// 初始化 RenderSystem (但不负责 FBO 绘制，只负责逻辑排序和剔除)
		worldRenderSystem = new WorldRenderSystem(neonBatch, gameCamera);

		// 绑定全局引用 (GameWorld 需要知道视口大小，暂时绑定 Stage 的，GamePresenter 会更新它)
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

		// Scene View
		scenePanel = new ScenePanel();
		// ScenePresenter 需要 RenderSystem 来做点击检测，需要 NeonBatch 来画 Gizmo
		scenePresenter = new ScenePresenter(scenePanel, sceneManager, neonBatch, worldRenderSystem);

		// Game View
		gamePanel = new GamePanel();
		// GamePresenter 需要 NeonBatch 来渲染画面
		gamePresenter = new GamePresenter(gamePanel, neonBatch);

		// 跨模块交互：从 Hierarchy 拖拽到 Scene
		setupDragAndDrop();
	}

	private void buildLayout() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");

		// 中间区域：Scene | Game (上下分割)
		Stack centerStack = new Stack();
		VisSplitPane viewSplit = new VisSplitPane(scenePanel, gamePanel, true);
		viewSplit.setSplitAmount(0.5f);
		centerStack.add(viewSplit);

		// 右侧区域：中间 | Inspector
		VisSplitPane rightSplit = new VisSplitPane(centerStack, inspectorPanel, false);
		rightSplit.setSplitAmount(0.75f);

		// 主分割：Hierarchy | 右侧
		VisSplitPane mainSplit = new VisSplitPane(hierarchyPanel, rightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();
		stage.addActor(root);
		stage.addActor(new ToastUI());
	}

	private void setupInput() {
		shortcutManager = new ShortcutManager(stage);

		// 注册快捷键 -> 代理给各个 Presenter
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

		// 3. Scene View 输入 (Gizmo, Picking, Camera)
		scenePresenter.registerInput(multiplexer);

		// 应用输入处理器
		if (screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(multiplexer);
		} else {
			Gd.input.setInputProcessor(multiplexer);
		}

		// 初始化 Gd.input 代理 (暂时使用 Scene View 的代理作为主输入，或根据鼠标位置切换)
		// 这一步比较微妙，为了简单起见，我们暂不设置 Gd.input 的代理，因为编辑器模式下逻辑 Update 不依赖点击输入
		// 如果需要测试游戏输入，GamePresenter 可能会接管。目前保持默认。
	}

	private void setupDragAndDrop() {
		DragAndDrop dnd = hierarchyPanel.getDragAndDrop();
		if (dnd != null) {
			dnd.addTarget(new Target(scenePanel.getDropTargetActor()) {
				@Override
				public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
					return true;
				}
				@Override
				public void drop(Source source, Payload payload, float x, float y, int pointer) {
					// 暂无特殊逻辑，可视作放入场景
				}
			});
		}
	}

	// --- Loop ---

	public void render(float delta) {
		// 1. 逻辑更新
		GameWorld.inst().update(delta);

		// 2. 模块渲染更新 (相机更新、FBO 绘制)
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
	}
}
