package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.ComponentRegistry;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.input.ShortcutManager;
import com.goldsprite.gdengine.core.utils.GdxJsonSetup;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.RenderComponent;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.InspectorBuilder;
import com.goldsprite.gdengine.screens.ecs.hub.GDEngineHubScreen;
import com.goldsprite.gdengine.ui.input.SmartInput;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.goldsprite.gdengine.ui.widget.AddComponentDialog;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.gdengine.utils.SimpleCameraController;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTree;
import java.util.ArrayList;
import java.util.List;

public class EditorController {
	private EditorGameScreen screen;
	private Stage stage;

	// Core Systems
	private ViewTarget gameTarget, sceneTarget;
	private ViewWidget gameWidget, sceneWidget;
	private OrthographicCamera sceneCamera, gameCamera;
	private Viewport gameViewport;

	private CommandManager commandManager;
	private EditorSceneManager sceneManager;
	private EditorGizmoSystem gizmoSystem;
	private ShortcutManager shortcutManager;
	private DragAndDrop dragAndDrop; // [新增] 拖拽管理器
	// [新增] 场景相机控制器
	private SimpleCameraController sceneCamController;

	// UI
	private VisTree<GObjectNode, GObject> hierarchyTree;
	private VisTable hierarchyContainer;
	private VisTable inspectorContainer;

	// Rendering
	private SpriteBatch spriteBatch;
	private NeonBatch neonBatch;
	private ShapeRenderer shapeRenderer;
	private WorldRenderSystem worldRenderSystem; // New
	private Stack gameWidgetStack;

	private boolean hierarchyDirty = false;

	private FileHandle currentProj;

	public EditorController(EditorGameScreen screen) {
		this.screen = screen;
	}

	// [修改] 提取加载逻辑
    private void reloadProjectContext() {
        currentProj = GDEngineHubScreen.ProjectManager.currentProject;
        if (currentProj != null) {
            GameWorld.projectAssetsRoot = currentProj.child("assets");
			Debug.logT("Editor", "🔗 链接到项目: " + currentProj.name());

            FileHandle indexFile = currentProj.child("project.index");
            if (indexFile.exists()) {
                Debug.logT("Editor", "🔄 Reloading User Index from: " + indexFile.path());
                // 强制刷新注册表
                ComponentRegistry.reloadUserIndex(indexFile);
            } else {
				Debug.logT("Editor", "⚠ ⚠️ project.index not found. (Compile to generate)");
            }
        }
    }

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		// 1. 初始化 FBO, Camera, Stage, CommandManager, SceneManager, Gizmo ...
		// (这部分代码保持不变，省略以节省篇幅)
		int fboW = 1280; int fboH = 720;
		gameTarget = new ViewTarget(fboW, fboH);
		sceneTarget = new ViewTarget(fboW, fboH);
		sceneCamera = new OrthographicCamera(fboW, fboH);
		gameCamera = new OrthographicCamera();

		float scl = PlatformImpl.isAndroidUser() ? 1.3f : 2.0f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		commandManager = new CommandManager();
		sceneManager = new EditorSceneManager(commandManager);
		gizmoSystem = new EditorGizmoSystem(sceneManager);
		dragAndDrop = new DragAndDrop();

		Gd.init(Gd.Mode.EDITOR, new EditorGameInput(gameWidget), new EditorGameGraphics(gameTarget), Gd.compiler);

		// 2. [核心修改] 注入项目上下文
		reloadProjectContext();

		// 3. 初始化 ECS (保持不变)
		if(GameWorld.inst() == null) new GameWorld();
		GameWorld.inst().setReferences(stage.getViewport(), gameCamera);
		reloadGameViewport();

		spriteBatch = new SpriteBatch();
		neonBatch = new NeonBatch();
		shapeRenderer = new ShapeRenderer();
		// [修改] 注册统一渲染系统
		worldRenderSystem = new WorldRenderSystem(neonBatch, gameCamera);
		// 注意：WorldRenderSystem 内部会处理 batch.begin/end，它使用 NeonBatch (兼容 SpriteBatch)

		createUI();

		shortcutManager = new ShortcutManager(stage);
		registerShortcuts();

		sceneManager.onStructureChanged.add(o -> hierarchyDirty = true);
		sceneManager.onSelectionChanged.add(this::refreshInspector);

		// 在 create() 中初始化控制器
		// [核心] 初始化控制器并注入映射策略
		sceneCamController = new SimpleCameraController(sceneCamera);
		sceneCamController.setCoordinateMapper((sx, sy) ->
			sceneWidget.screenToWorld(sx, sy, sceneCamera)
		);
		NativeEditorInput editorInput = new NativeEditorInput();
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(shortcutManager);
		multiplexer.addProcessor(editorInput);
		multiplexer.addProcessor(sceneCamController); // 添加相机事件监听管线

		if (screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(multiplexer);
		} else {
			Gd.input.setInputProcessor(multiplexer);
		}


		// 4. [核心修改] 智能加载场景
		// 优先加载项目内的 main.scene，其次加载沙盒 scene_debug.json，最后新建
		FileHandle projectScene = getSceneFile();

		if (projectScene != null && projectScene.exists()) {
			loadScene(); // loadScene 内部会调用 getSceneFile
		} else if (Gdx.files.local("scene_debug.json").exists() && currentProj == null) {
			// 只有在没项目时才加载沙盒缓存
			loadSceneFromHandle(Gdx.files.local("scene_debug.json"));
		} else {
			initTestScene();
		}
	}
	// [新增] 获取当前应该读写的场景文件
	private FileHandle getSceneFile() {
		if (GDEngineHubScreen.ProjectManager.currentProject != null) {
			return GDEngineHubScreen.ProjectManager.currentProject.child("assets/main.scene");
		}
		return Gdx.files.local("scene_debug.json"); // 沙盒回退
	}

	private void saveScene() {
		try {
			Json json = GdxJsonSetup.create();
			List<GObject> roots = GameWorld.inst().getRootEntities();
			String text = json.prettyPrint(roots);

			// [修改] 使用动态获取的文件句柄
			FileHandle file = getSceneFile();
			file.writeString(text, false);

			Debug.logT("Editor", "Scene saved: " + file.path());
			ToastUI.inst().show("Saved: " + file.name());
		} catch (Exception e) {
			Debug.logT("Editor", "Save Failed: " + e.getMessage());
			e.printStackTrace();
			ToastUI.inst().show("Save Failed!");
		}
	}

	private void loadScene() {
		loadSceneFromHandle(getSceneFile());
	}

	// 提取出来的底层加载逻辑
	private void loadSceneFromHandle(FileHandle file) {
		if (file == null || !file.exists()) return;
		try {
			List<GObject> currentRoots = new ArrayList<>(GameWorld.inst().getRootEntities());
			for(GObject obj : currentRoots) obj.destroyImmediate();
			sceneManager.select(null);

			Json json = GdxJsonSetup.create();
			@SuppressWarnings("unchecked")
				ArrayList<GObject> newRoots = json.fromJson(ArrayList.class, GObject.class, file);

			Debug.logT("Editor", "Scene loaded: " + file.name());
			hierarchyDirty = true;
			ToastUI.inst().show("Loaded: " + file.name());
		} catch (Exception e) {
			Debug.logT("Editor", "Load Failed: " + e.getMessage());
			e.printStackTrace();
			ToastUI.inst().show("Load Failed!");
		}
	}

	private void registerShortcuts() {
		shortcutManager.register("TOOL_MOVE", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.MOVE);
		shortcutManager.register("TOOL_ROTATE", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.ROTATE);
		shortcutManager.register("TOOL_SCALE", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.SCALE);
		shortcutManager.register("ACTION_UNDO", () -> commandManager.undo());
		shortcutManager.register("ACTION_REDO", () -> commandManager.redo());
		shortcutManager.register("ACTION_SAVE", this::saveScene);
		shortcutManager.register("ACTION_DELETE", sceneManager::deleteSelection);
	}

	private void initTestScene() {
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
		hierarchyDirty = true;
	}

	// [修改] refreshHierarchy: 每次重建树时都要清理旧的 DragAndDrop 目标，防止内存泄漏或逻辑混乱
	private void refreshHierarchy() {
		dragAndDrop.clear(); // 清理旧的 Sources/Targets

		hierarchyContainer.clearChildren();
		hierarchyTree = new VisTree<>();
		hierarchyTree.setIndentSpacing(20f);
		hierarchyTree.getSelection().setProgrammaticChangeEvents(false);

		List<GObject> roots = GameWorld.inst().getRootEntities();
		for(GObject root : roots) {
			buildTreeNode(root, null);
		}
		hierarchyContainer.add(hierarchyTree).grow().top();
	}

	private void buildTreeNode(GObject obj, GObjectNode parent) {
		GObjectNode node = new GObjectNode(obj);
		if(parent == null) hierarchyTree.add(node);
		else parent.add(node);

		for(GObject child : obj.getChildren()) {
			buildTreeNode(child, node);
		}
		node.setExpanded(true);
	}

	// =======================================================
	// 核心类: GObjectNode (还原 UI Node 逻辑)
	// =======================================================

	// 拖拽状态枚举
	enum DropState { NONE, INSERT_ABOVE, INSERT_BELOW, REPARENT }

	// 自定义 Actor 用于绘制插入线
	class NodeContentTable extends VisTable {
		GObjectNode node;

		public NodeContentTable(GObjectNode node) {
			this.node = node;
			setBackground("button");
			setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			// 核心布局修复：强制宽度等于 Tree 的宽度
			// 这样 expandX 才能生效，把名字推左边，把手柄推右边
			if (hierarchyTree != null) {
				// 减去节点的缩进 X (getX())，得到剩余可用宽度
				float targetWidth = hierarchyTree.getWidth() - getX();
				if (targetWidth > 0 && getWidth() != targetWidth) {
					setWidth(targetWidth);
					invalidate(); // 触发布局重算
				}
			}

			super.draw(batch, parentAlpha);

			// 蓝色插入线绘制逻辑
			if (node != null && node.dropState != DropState.NONE) {
				// ...
				Drawable white = VisUI.getSkin().getDrawable("white");
				float x = getX(); float y = getY();
				float w = getWidth(); float h = getHeight();

				Color old = batch.getColor();
				batch.setColor(Color.CYAN);

				if (node.dropState == DropState.INSERT_ABOVE) {
					white.draw(batch, x, y + h - 2, w, 2);
				} else if (node.dropState == DropState.INSERT_BELOW) {
					white.draw(batch, x, y, w, 2);
				} else if (node.dropState == DropState.REPARENT) {
					white.draw(batch, x, y, w, 2);
					white.draw(batch, x, y + h - 2, w, 2);
					white.draw(batch, x, y, 2, h);
					white.draw(batch, x + w - 2, y, 2, h);
				}
				batch.setColor(old);
			}
		}
	}

	class GObjectNode extends Tree.Node<GObjectNode, GObject, NodeContentTable> {
		DropState dropState = DropState.NONE;

		public GObjectNode(GObject obj) {
			super(new NodeContentTable(null));
			setValue(obj);

			NodeContentTable table = getActor();
			table.node = this;

			// 1. 名字 Label (左侧)
			VisLabel lbl = new VisLabel(obj.getName());
			// 使用 expandX() 占据所有剩余空间
			table.add(lbl).expandX().fillX().left().padLeft(5);

			// 2. 拖拽手柄 (右侧)
			// [修改] 直接用 VisLabel，颜色设为灰色
			VisLabel handle = new VisLabel("::");
			handle.setColor(Color.GRAY);
			// 这里不需要 expandX 了，因为左边的 lbl 已经把空间占了，这会自动被挤到最右边
			table.add(handle).right().padRight(10).width(20);

			// --- 交互逻辑 ---

			// 左键点击: 选中
			table.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					sceneManager.select(obj);
				}
			});

			// 右键菜单
			table.addListener(new ActorGestureListener() {
				@Override public void tap(InputEvent event, float x, float y, int count, int button) {
					if (button == Input.Buttons.RIGHT) {
						showHierarchyMenu(obj, event.getStageX(), event.getStageY());
					}
				}
			});

			// Hover 高亮手柄
			handle.addListener(new ClickListener() {
				@Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
					handle.setColor(Color.CYAN);
				}
				@Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
					handle.setColor(Color.GRAY);
				}
			});

			// --- 拖拽源 (Source) ---
			dragAndDrop.addSource(new Source(handle) {
				@Override
				public Payload dragStart(InputEvent event, float x, float y, int pointer) {
					Payload payload = new Payload();
					payload.setObject(obj); // 传递 GObject

					// 拖拽时的影子
					Label dragActor = new Label(obj.getName(), VisUI.getSkin());
					dragActor.setColor(Color.YELLOW);
					payload.setDragActor(dragActor);
					return payload;
				}
			});

			// --- 拖拽目标 (Target) ---
			dragAndDrop.addTarget(new Target(table) {
				@Override
				public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
					GObject draggingObj = (GObject) payload.getObject();
					// 不能拖给自己，也不能拖给自己的子孙
					if (draggingObj == obj) return false;
					// 简单的层级检查 (这里简化处理，EditorSceneManager.moveEntity 会做最终检查)

					float h = getActor().getHeight();
					// 上1/4插入，下1/4插入，中间1/2变子级
					if (y > h * 0.75f) dropState = DropState.INSERT_ABOVE;
					else if (y < h * 0.25f) dropState = DropState.INSERT_BELOW;
					else dropState = DropState.REPARENT;

					return true;
				}

				@Override
				public void drop(Source source, Payload payload, float x, float y, int pointer) {
					GObject draggingObj = (GObject) payload.getObject();

					// 计算逻辑
					GObject newParent = null;
					int index = -1;

					if (dropState == DropState.INSERT_ABOVE) {
						newParent = obj.getParent();
						index = getSiblingIndex(obj); // 插在它前面
					}
					else if (dropState == DropState.INSERT_BELOW) {
						newParent = obj.getParent();
						index = getSiblingIndex(obj) + 1; // 插在它后面
					}
					else if (dropState == DropState.REPARENT) {
						newParent = obj;
						index = -1; // 追加到末尾
					}

					// 执行操作
					sceneManager.moveEntity(draggingObj, newParent, index);
					dropState = DropState.NONE;
				}

				@Override
				public void reset(Source source, Payload payload) {
					dropState = DropState.NONE;
				}
			});
		}

		private int getSiblingIndex(GObject target) {
			List<GObject> list = (target.getParent() != null) ? target.getParent().getChildren() : GameWorld.inst().getRootEntities();
			return list.indexOf(target);
		}
	}

	private void createUI() {
		createGameWidget();
		createSceneWidget();

		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");

		// Left: Hierarchy
		hierarchyContainer = new VisTable();
		hierarchyContainer.setBackground("button");
		hierarchyContainer.top().left();
		VisScrollPane hierarchyScroll = new VisScrollPane(hierarchyContainer);
		hierarchyScroll.setFadeScrollBars(false);

		// 右键背景菜单
		hierarchyContainer.addListener(new ActorGestureListener() {
			@Override
			public void tap(InputEvent event, float x, float y, int count, int button) {
				if (button == Input.Buttons.RIGHT && hierarchyTree.getOverNode() == null) {
					showHierarchyMenu(null, event.getStageX(), event.getStageY());
				}
			}
		});

		inspectorContainer = new VisTable();
		inspectorContainer.setBackground("button");
		inspectorContainer.top().left();
		VisScrollPane inspectorScroll = new VisScrollPane(inspectorContainer);

		Stack centerStack = new Stack();
		VisSplitPane viewSplit = new VisSplitPane(sceneWidget, gameWidgetStack, true);
		viewSplit.setSplitAmount(0.5f);
		centerStack.add(viewSplit);

		Table toolbar = new Table();
		toolbar.top().left().pad(5);
		addToolBtn(toolbar, "Save", this::saveScene);
		addToolBtn(toolbar, "Load", this::loadScene);
		toolbar.add().width(20);
		addToolBtn(toolbar, "M", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.MOVE);
		addToolBtn(toolbar, "R", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.ROTATE);
		addToolBtn(toolbar, "S", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.SCALE);
		centerStack.add(toolbar);

		VisSplitPane rightSplit = new VisSplitPane(centerStack, inspectorScroll, false);
		rightSplit.setSplitAmount(0.75f);

		VisSplitPane mainSplit = new VisSplitPane(hierarchyScroll, rightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();
		stage.addActor(root);

		stage.addActor(new ToastUI()); // Toast

		refreshHierarchy();
	}

	// ... [showHierarchyMenu, createGObject 等辅助方法保持不变] ...
	// ... [NativeEditorInput 保持不变] ...

	// (为了代码完整性，以下是 showHierarchyMenu 和 createGObject)

	private void showHierarchyMenu(GObject target, float x, float y) {
		PopupMenu menu = new PopupMenu();
		if (target == null) {
			menu.addItem(new MenuItem("Create Empty", new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) { createGObject(null); }
			}));
		} else {
			menu.addItem(new MenuItem("Create Child", new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) { createGObject(target); }
			}));
			MenuItem delItem = new MenuItem("Delete");
			delItem.getLabel().setColor(Color.RED);
			delItem.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) { sceneManager.deleteSelection(); }
			});
			menu.addItem(delItem);
		}
		menu.showMenu(stage, x, y);
	}

	private void createGObject(GObject parent) {
		GObject obj = new GObject("GameObject");
		if (parent != null) obj.setParent(parent);
		hierarchyDirty = true;
		sceneManager.select(obj);
	}

	// [修改] 修复视口切换逻辑
	private void createGameWidget() {
		gameWidget = new ViewWidget(gameTarget);
		gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT); // 默认

		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				String mode = box.getSelected();

				// [修复] 不仅要改 Config，还要改 Widget 的显示模式
				if(mode.equals("FIT")) {
					Gd.config.viewportType = Gd.ViewportType.FIT;
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
				}
				else if(mode.equals("STRETCH")) {
					Gd.config.viewportType = Gd.ViewportType.STRETCH;
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.STRETCH);
				}
				else if(mode.equals("EXTEND")) {
					Gd.config.viewportType = Gd.ViewportType.EXTEND;
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);
				}

				reloadGameViewport();
			}
		});
		VisTable uiOverlay = new VisTable();
		uiOverlay.add(box).top().right().expand().pad(5);
		gameWidgetStack = new Stack();
		gameWidgetStack.add(gameWidget);
		gameWidgetStack.add(uiOverlay);
	}

	// [修改] 集成 SimpleCameraController
	private void createSceneWidget() {
		sceneWidget = new ViewWidget(sceneTarget);
		sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);
	}

	private void addToolBtn(Table t, String text, Runnable act) {
		VisTextButton b = new VisTextButton(text);
		b.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { act.run(); } });
		t.add(b).padRight(5);
	}

	private void reloadGameViewport() {
		Gd.Config conf = Gd.config;
		if (conf.viewportType == Gd.ViewportType.FIT) gameViewport = new FitViewport(conf.logicWidth, conf.logicHeight, gameCamera);
		else if (conf.viewportType == Gd.ViewportType.STRETCH) gameViewport = new StretchViewport(conf.logicWidth, conf.logicHeight, gameCamera);
		else gameViewport = new ExtendViewport(conf.logicWidth, conf.logicHeight, gameCamera);
		if (gameTarget != null) gameViewport.update(gameTarget.getFboWidth(), gameTarget.getFboHeight());
	}

	private void refreshInspector(GObject selection) {
		inspectorContainer.clearChildren();
		if (selection == null) {
			inspectorContainer.add(new VisLabel("No Selection")).pad(10);
			return;
		}
		inspectorContainer.add(new VisLabel("Name:")).left();
		inspectorContainer.add(new SmartTextInput(null, selection.getName(), v -> {
			selection.setName(v);
			hierarchyDirty = true;
		})).growX().row();
		inspectorContainer.add(new VisLabel("Tag:")).left();
		inspectorContainer.add(new SmartTextInput(null, selection.getTag(), selection::setTag)).growX().row();
		for (List<Component> comps : selection.getComponentsMap().values()) {
			for (Component c : comps) {
				buildComponentUI(c, selection);
			}
		}
		VisTextButton btnAdd = new VisTextButton("Add Component");
		btnAdd.setColor(Color.GREEN);
		btnAdd.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				showAddComponentMenu(selection, event.getStageX(), event.getStageY());
			}
		});
		inspectorContainer.add(btnAdd).growX().padTop(20).padBottom(10).colspan(2);
	}

	private void buildComponentUI(Component c, GObject owner) {
		VisTable header = new VisTable();
		header.setBackground("button");
		header.add(new VisLabel(c.getClass().getSimpleName())).expandX().left().pad(5);
		if (!(c instanceof TransformComponent)) {
			VisTextButton btnRemove = new VisTextButton("X");
			btnRemove.setColor(Color.RED);
			btnRemove.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					c.destroyImmediate();
					refreshInspector(owner);
				}
			});
			header.add(btnRemove).size(25, 25).right();
		}
		inspectorContainer.add(header).growX().colspan(2).padTop(5).row();
		VisTable body = new VisTable();
		body.padLeft(10);

        // [核心替换] 一行代码搞定所有反射逻辑！
        InspectorBuilder.build(body, c);

        inspectorContainer.add(body).growX().colspan(2).row();
    }

	// [修改] 替换原来的 showAddComponentMenu 方法
	private void showAddComponentMenu(GObject selection, float x, float y) {
		// 使用新的对话框
		// 传入回调：当组件添加成功后，刷新 Inspector
		new AddComponentDialog(selection, () -> refreshInspector(selection)).show(stage);
	}

	private void registerCompMenuItem(PopupMenu menu, GObject obj, Class<? extends Component> clazz) {
		MenuItem item = new MenuItem(clazz.getSimpleName());
		item.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				obj.addComponent(clazz);
				refreshInspector(obj);
			}
		});
		menu.addItem(item);
	}

	// render 循环
	public void render(float delta) {
		// 1. 跑逻辑
		GameWorld.inst().update(delta);
		
		// 相机更新
		gameCamera.update();
		sceneCamera.update();
		
		if (hierarchyDirty) { refreshHierarchy(); hierarchyDirty = false; }

		// 2. 画 Game View (FBO)
		gameTarget.renderToFbo(() -> {
			gameViewport.apply();
			GameWorld.inst().render(neonBatch, gameCamera);
		});

		// 3. 画 Scene View (FBO)
		sceneTarget.renderToFbo(() -> {
			Gdx.gl.glViewport(0, 0, sceneTarget.getFboWidth(), sceneTarget.getFboHeight());
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			drawGrid(sceneCamera);

			// 复用渲染管线
			GameWorld.inst().render(neonBatch, sceneCamera);

			// 画 Gizmo
			neonBatch.setProjectionMatrix(sceneCamera.combined);
			neonBatch.begin();
			// 选中框
			if(sceneManager.getSelection() != null) {
				GObject sel = sceneManager.getSelection();
				float x = sel.transform.worldPosition.x;
				float y = sel.transform.worldPosition.y;
				neonBatch.drawRect(x-25, y-25, 50, 50, sel.transform.worldRotation, 2, Color.YELLOW, false);
			}
			gizmoSystem.render(neonBatch, sceneCamera.zoom);
			neonBatch.end();
		});
		
		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		updateSmartInputs(inspectorContainer);

		stage.act(delta);
		stage.draw();
	}

	private void updateSmartInputs(Actor actor) {
		if (actor instanceof SmartInput<?> smartInput) {
			smartInput.updateUI();
		} else if (actor instanceof Group group) {
			for (Actor child : group.getChildren()) {
				updateSmartInputs(child);
			}
		}
	}

	private void drawGrid(OrthographicCamera cam) {
		shapeRenderer.setProjectionMatrix(cam.combined);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(1, 1, 1, 0.1f);
		float s = 1000;
		shapeRenderer.line(-s, 0, s, 0);
		shapeRenderer.line(0, -s, 0, s);
		shapeRenderer.end();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	public void dispose() {
		stage.dispose();
		gameTarget.dispose(); sceneTarget.dispose();
		spriteBatch.dispose(); neonBatch.dispose(); shapeRenderer.dispose();
	}

	private class NativeEditorInput extends InputAdapter {
		private enum DragMode { NONE, BODY, MOVE_X, MOVE_Y, ROTATE, SCALE_X, SCALE_Y, SCALE }
		private DragMode currentDragMode = DragMode.NONE;
		private float lastX, lastY;

		// [新增] 记录拖拽开始时的初始状态
		private Vector2 startScale = new Vector2();
		private Vector2 startDragPos = new Vector2(); // 记录按下时的鼠标世界坐标

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			// 多指操作 或 右键/中键 -> 视为相机操作，不拦截，返回 false 让它穿透到 sceneWidget 的 Listener
			if (pointer > 0 || button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
				return false;
			}

			// 左键单指 -> 检测 Gizmo
			if (button == Input.Buttons.LEFT) {
				Vector2 wPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
				GObject sel = sceneManager.getSelection();
				if (sel != null) {
					DragMode gizmoHit = hitTestGizmo(sel, wPos);
					if (gizmoHit != DragMode.NONE) {
						startDrag(gizmoHit, wPos, sel);
						return true; // 拦截
					}
				}
				GObject hit = hitTestGObject(wPos);
				if (hit != null) {
					if (hit != sel) sceneManager.select(hit);
					startDrag(DragMode.BODY, wPos, hit);
					return true; // 拦截
				}

				// 点空了 -> 取消选中
				if (sel != null) sceneManager.select(null);
			}

			// 既没点中 Gizmo 也没点中物体，返回 false，
			// 让事件穿透到 sceneWidget，从而触发 CameraController 的单指拖拽(如果有定义)
			// 不过 SimpleCameraController 默认只处理右键平移。
			// 如果想支持左键空白处平移，可以在这里返回 false。
			return false;
		}
		// [修改] startDrag: 增加 activeScaleHandle 的设置
		private void startDrag(DragMode mode, Vector2 pos, GObject target) {
			currentDragMode = mode;
			lastX = pos.x;
			lastY = pos.y;
			startDragPos.set(pos);
			if(target != null) {
				startScale.set(target.transform.scale);
			}

			// [修正] 映射逻辑
			switch (mode) {
				case MOVE_X:
				case SCALE_X:
					gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_X;
					break;
				case MOVE_Y:
				case SCALE_Y:
					gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_Y;
					break;

				case ROTATE:
					// 旋转手柄使用专用 ID
					gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_ROTATE;
					break;

				case BODY:  // 移动中心点
				case SCALE: // 缩放中心点
					// 统一使用 CENTER ID
					gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_CENTER;
					break;

				default:
					gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_NONE;
					break;
			}
		}
		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (currentDragMode == DragMode.NONE || sceneManager.getSelection() == null) return false;
			Vector2 wPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
			float dx = wPos.x - lastX;
			float dy = wPos.y - lastY;
			GObject t = sceneManager.getSelection();
			applyTransform(t, dx, dy, wPos);
			refreshInspector(t);
			lastX = wPos.x;
			lastY = wPos.y;
			return true;
		}
		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (currentDragMode != DragMode.NONE) {
				currentDragMode = DragMode.NONE;
				gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_NONE; // 还原
				return true;
			}
			return false;
		}
		private void applyTransform(GObject t, float dx, float dy, Vector2 currPos) {
			// [修正] 直接读取 Transform 缓存的世界旋转，不再手动反解矩阵
			float rot = t.transform.worldRotation;

			float rad = rot * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad);
			float s = MathUtils.sin(rad);

			float cx = t.transform.worldPosition.x;
			float cy = t.transform.worldPosition.y;

			// 复制当前物体世界坐标作为计算基准
			Vector2 targetWorldPos = t.transform.worldPosition.cpy();

			// [新增] 缩放步进灵敏度 (每 100 像素 = 1.0 倍率变化)
			float scaleSensitivity = 0.01f;
			// 最小缩放极限 (绝对值)
			float minScaleLimit = 0.01f;

			switch (currentDragMode) {
				case BODY:
					// 自由移动：直接叠加世界位移 -> 逆解 Local
					targetWorldPos.add(dx, dy);
					t.transform.setWorldPosition(targetWorldPos);
					break;

				case MOVE_X:
					// X轴投影移动
					// 轴向向量: (c, s)
					float projX = dx * c + dy * s;
					targetWorldPos.add(projX * c, projX * s);
					t.transform.setWorldPosition(targetWorldPos);
					break;

				case MOVE_Y:
					// Y轴投影移动
					// 轴向向量: (-s, c)
					float projY = dx * (-s) + dy * c;
					targetWorldPos.add(-projY * s, projY * c);
					t.transform.setWorldPosition(targetWorldPos);
					break;

				case ROTATE:
					// 旋转计算：计算鼠标相对于物体中心的角度差

					// lastX, lastY 是上一帧鼠标的世界坐标
					Vector2 prevDir = new Vector2(lastX - cx, lastY - cy);
					Vector2 currDir = new Vector2(currPos.x - cx, currPos.y - cy);
					float angleDelta = currDir.angleDeg() - prevDir.angleDeg();

					// 旋转直接累加到 Local Rotation 即可 (相对增量)
					t.transform.rotation += angleDelta;
					break;

				// [重构] 缩放逻辑：本地轴向投影
				case SCALE_X:
				case SCALE_Y:
				case SCALE: {
					// 1. 构建本地方向向量
					Vector2 dirX = new Vector2(c, s);        // 本地 X 正方向
					Vector2 dirY = new Vector2(-s, c);       // 本地 Y 正方向
					Vector2 dirUni = new Vector2(c-s, s+c).nor(); // 本地 (1,1) 方向 (右上)

					// 2. 计算鼠标拖拽向量 (相对于按下点)
					Vector2 dragVec = new Vector2(currPos).sub(startDragPos);

					// 3. 计算投影增量 (Dot Product)
					// 投影值 > 0 表示沿正方向拖动，< 0 表示沿负方向
					float delta = 0;

					if (currentDragMode == DragMode.SCALE_X) {
						delta = dragVec.dot(dirX) * scaleSensitivity;
					}
					else if (currentDragMode == DragMode.SCALE_Y) {
						delta = dragVec.dot(dirY) * scaleSensitivity;
					}
					else {
						// 等比: 投影到右上角方向
						delta = dragVec.dot(dirUni) * scaleSensitivity;
					}

					// 4. 应用增量 (代数叠加)
					float newSx = startScale.x;
					float newSy = startScale.y;

					if (currentDragMode == DragMode.SCALE_X) {
						newSx += delta;
					} else if (currentDragMode == DragMode.SCALE_Y) {
						newSy += delta;
					} else {
						newSx += delta;
						newSy += delta;
					}

					// 5. 零点限制 (禁止跨越 0)
					// 如果初始是正，结果必须 >= 0.01
					// 如果初始是负，结果必须 <= -0.01
					if (startScale.x > 0) newSx = Math.max(minScaleLimit, newSx);
					else newSx = Math.min(-minScaleLimit, newSx);

					if (startScale.y > 0) newSy = Math.max(minScaleLimit, newSy);
					else newSy = Math.min(-minScaleLimit, newSy);

					// 6. 赋值
					t.transform.scale.x = newSx;
					t.transform.scale.y = newSy;
					break;
				}
			}
		}
		private void applyWorldPosToLocal(GObject t, Vector2 targetWorldPos) {
			GObject parent = t.getParent();
			if (parent != null) {
				Vector2 local = new Vector2();
				parent.transform.worldToLocal(targetWorldPos, local);
				t.transform.position.set(local);
			} else {
				t.transform.position.set(targetWorldPos);
			}
		}
		private DragMode hitTestGizmo(GObject t, Vector2 pos) {
			float zoom = sceneCamera.zoom * 1.4f;
			float axisLen = EditorGizmoSystem.AXIS_LEN * zoom;
			float hitR = 20f * zoom;

			float tx = t.transform.worldPosition.x;
			float ty = t.transform.worldPosition.y;
			float rot = t.transform.worldRotation;
			float rad = rot * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad);
			float s = MathUtils.sin(rad);

			EditorGizmoSystem.Mode mode = gizmoSystem.mode;

			if (mode == EditorGizmoSystem.Mode.MOVE) {
				if (pos.dst(tx + c * axisLen, ty + s * axisLen) < hitR) return DragMode.MOVE_X;
				if (pos.dst(tx - s * axisLen, ty + c * axisLen) < hitR) return DragMode.MOVE_Y;
			}
			else if (mode == EditorGizmoSystem.Mode.ROTATE) {
				if (pos.dst(tx + c * axisLen, ty + s * axisLen) < hitR) return DragMode.ROTATE;
			}
			else if (mode == EditorGizmoSystem.Mode.SCALE) {
				// [新增] 1. 中心方块检测 (优先检测)
				if (pos.dst(tx, ty) < 12f * zoom) return DragMode.SCALE;

				// [修复] 2. X轴手柄检测
				if (pos.dst(tx + c * axisLen, ty + s * axisLen) < hitR) return DragMode.SCALE_X;

				// [修复] 3. Y轴手柄检测 (之前缺失)
				if (pos.dst(tx - s * axisLen, ty + c * axisLen) < hitR) return DragMode.SCALE_Y;
			}

			// 点击物体中心 (Body)
			// 注意：如果是 SCALE 模式，中心已经被 DragMode.SCALE 抢占了，所以这里要避开
			if (mode != EditorGizmoSystem.Mode.SCALE && pos.dst(tx, ty) < 15 * zoom) return DragMode.BODY;

			return DragMode.NONE;
		}
		// [重构] 选中检测：基于渲染层级 (所见即所得)
		private GObject hitTestGObject(Vector2 p) {
			// 1. 获取当前帧已排序的渲染列表 (底 -> 顶)
			List<RenderComponent> renderables = worldRenderSystem.getSortedRenderables();

			// 2. 倒序遍历 (顶 -> 底)
			for (int i = renderables.size() - 1; i >= 0; i--) {
				RenderComponent rc = renderables.get(i);
				// 检查点击是否在范围内
				if (rc.contains(p.x, p.y)) {
					return rc.getGObject();
				}
			}

			return null;
		}
		private GObject hitTestRecursive(GObject parent, Vector2 p) {
			for (GObject child : parent.getChildren()) {
				if(p.dst(child.transform.worldPosition) < 60) return child;
				GObject hit = hitTestRecursive(child, p);
				if (hit != null) return hit;
			}
			return null;
		}
	}
}
