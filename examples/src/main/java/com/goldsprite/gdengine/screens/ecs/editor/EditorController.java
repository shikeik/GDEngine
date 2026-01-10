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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.input.ShortcutManager;
import com.goldsprite.gdengine.core.utils.GdxJsonSetup;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.FsmComponent;
import com.goldsprite.gdengine.ecs.component.NeonAnimatorComponent;
import com.goldsprite.gdengine.ecs.component.SkeletonComponent;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.SkeletonRenderSystem;
import com.goldsprite.gdengine.ecs.system.SpriteSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.ui.input.SmartBooleanInput;
import com.goldsprite.gdengine.ui.input.SmartColorInput;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.goldsprite.solofight.ui.widget.ToastUI;
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

	// UI
	private VisTree<GObjectNode, GObject> hierarchyTree;
	private VisTable hierarchyContainer;
	private VisTable inspectorContainer;

	// Rendering
	private SpriteBatch spriteBatch;
	private NeonBatch neonBatch;
	private ShapeRenderer shapeRenderer;
	private SpriteSystem spriteSystem;
	private SkeletonRenderSystem skeletonRenderSystem;
	private Stack gameWidgetStack;

	private boolean hierarchyDirty = false;

	public EditorController(EditorGameScreen screen) {
		this.screen = screen;
	}

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		int fboW = 1280, fboH = 720;
		gameTarget = new ViewTarget(fboW, fboH);
		sceneTarget = new ViewTarget(fboW, fboH);
		sceneCamera = new OrthographicCamera(fboW, fboH);
		gameCamera = new OrthographicCamera();

		float scl = PlatformImpl.isAndroidUser() ? 1.3f : 2.0f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		commandManager = new CommandManager();
		sceneManager = new EditorSceneManager(commandManager);
		gizmoSystem = new EditorGizmoSystem(sceneManager);
		dragAndDrop = new DragAndDrop(); // [新增] 初始化 D&D

		Gd.init(Gd.Mode.EDITOR, new EditorGameInput(gameWidget), new EditorGameGraphics(gameTarget), Gd.compiler);

		if (GameWorld.inst() == null) new GameWorld();
		GameWorld.inst().setReferences(stage.getViewport(), gameCamera);
		reloadGameViewport();

		spriteBatch = new SpriteBatch();
		neonBatch = new NeonBatch();
		shapeRenderer = new ShapeRenderer();
		spriteSystem = new SpriteSystem(spriteBatch, gameCamera);
		skeletonRenderSystem = new SkeletonRenderSystem(neonBatch, gameCamera);

		createUI();

		shortcutManager = new ShortcutManager(stage);
		registerShortcuts();

		sceneManager.onStructureChanged.add(o -> hierarchyDirty = true);
		sceneManager.onSelectionChanged.add(this::refreshInspector);

		NativeEditorInput editorInput = new NativeEditorInput();
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(shortcutManager);
		multiplexer.addProcessor(editorInput);

		if (screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(multiplexer);
		} else {
			Gdx.input.setInputProcessor(multiplexer);
		}

		if (Gdx.files.local("scene_debug.json").exists()) loadScene();
		else initTestScene();
	}

	// ... [中间的方法保持不变：registerShortcuts, initTestScene, saveScene, loadScene] ...
	// 为了节省篇幅，这里略过未修改的方法，请保持原样
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
		child.transform.scale = 0.5f;
		SpriteComponent sp2 = child.addComponent(SpriteComponent.class);
		sp2.setPath("gd_icon.png");
		sp2.width = 100; sp2.height = 100;
		sp2.color.set(Color.RED);
		hierarchyDirty = true;
	}

	private void saveScene() {
		try {
			Json json = GdxJsonSetup.create();
			List<GObject> roots = GameWorld.inst().getRootEntities();
			String text = json.prettyPrint(roots);
			FileHandle file = Gdx.files.local("scene_debug.json");
			file.writeString(text, false);
			Debug.logT("Editor", "Scene saved: " + file.path());
			ToastUI.inst().show("Scene Saved");
		} catch (Exception e) {
			Debug.logT("Editor", "Save Failed: " + e.getMessage());
			e.printStackTrace();
			ToastUI.inst().show("Save Failed!");
		}
	}

	private void loadScene() {
		FileHandle file = Gdx.files.local("scene_debug.json");
		if (!file.exists()) return;
		try {
			List<GObject> currentRoots = new ArrayList<>(GameWorld.inst().getRootEntities());
			for(GObject obj : currentRoots) obj.destroyImmediate();
			sceneManager.select(null);
			Json json = GdxJsonSetup.create();
			@SuppressWarnings("unchecked")
			ArrayList<GObject> newRoots = json.fromJson(ArrayList.class, GObject.class, file);
			Debug.logT("Editor", "Scene loaded: " + newRoots.size() + " roots.");
			hierarchyDirty = true;
			ToastUI.inst().show("Scene Loaded");
		} catch (Exception e) {
			Debug.logT("Editor", "Load Failed: " + e.getMessage());
			e.printStackTrace();
			ToastUI.inst().show("Load Failed!");
		}
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
			// [新增] 核心布局修复：强制宽度等于 Tree 的宽度
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

			// ... (原本的蓝色插入线绘制逻辑保持不变) ...
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

	// ... [createUI, createGameWidget 等其他 UI 代码保持不变] ...
	// 请确保 createUI 方法里保留了 addActor(new ToastUI()) 和 hierarchyContainer 的 Listener

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

	// ... [其他方法保持不变] ...
	// (createGameWidget, createSceneWidget, addToolBtn, reloadGameViewport, refreshInspector, buildComponentUI, showAddComponentMenu, render, resize, dispose)
	// 请确保这些方法依然存在

	private void createGameWidget() {
		gameWidget = new ViewWidget(gameTarget);
		gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				String mode = box.getSelected();
				if(mode.equals("FIT")) Gd.config.viewportType = Gd.ViewportType.FIT;
				if(mode.equals("STRETCH")) Gd.config.viewportType = Gd.ViewportType.STRETCH;
				if(mode.equals("EXTEND")) Gd.config.viewportType = Gd.ViewportType.EXTEND;
				reloadGameViewport();
			}
		});
		VisTable uiOverlay = new VisTable();
		uiOverlay.add(box).top().right().expand().pad(5);
		gameWidgetStack = new Stack();
		gameWidgetStack.add(gameWidget);
		gameWidgetStack.add(uiOverlay);
	}

	private void createSceneWidget() {
		sceneWidget = new ViewWidget(sceneTarget);
		sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);
		sceneWidget.addListener(new InputListener() {
			float lastX, lastY;
			@Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if(button == Input.Buttons.RIGHT) { lastX = x; lastY = y; return true; }
				return false;
			}
			@Override public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
					float z = sceneCamera.zoom;
					sceneCamera.translate(-(x - lastX)*z, (y - lastY)*z);
					sceneCamera.update();
					lastX = x; lastY = y;
				}
			}
			@Override public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				sceneCamera.zoom += amountY * 0.1f * sceneCamera.zoom;
				sceneCamera.zoom = MathUtils.clamp(sceneCamera.zoom, 0.1f, 10f);
				sceneCamera.update();
				return true;
			}
		});
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
		Field[] fields = c.getClass().getFields();
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;
			if (Modifier.isTransient(f.getModifiers())) continue;
			try {
				String name = f.getName();
				Object val = f.get(c);
				Class<?> type = f.getType();
				body.add(new VisLabel(name)).left().width(80).padRight(5);
				if (type == float.class || type == Float.class) {
					body.add(new SmartNumInput(null, (float)val, 0.1f, v -> { try{f.setFloat(c,v);}catch(Exception e){} })).growX();
				} else if (type == int.class || type == Integer.class) {
					body.add(new SmartNumInput(null, (float)(int)val, 1f, v -> { try{f.setInt(c, v.intValue());}catch(Exception e){} })).growX();
				} else if (type == boolean.class || type == Boolean.class) {
					body.add(new SmartBooleanInput(null, (boolean)val, v -> { try{f.setBoolean(c,v);}catch(Exception e){} })).growX();
				} else if (type == String.class) {
					body.add(new SmartTextInput(null, (String)val, v -> {
						try{
							f.set(c,v);
							if (c instanceof SpriteComponent && name.equals("assetPath")) ((SpriteComponent)c).reloadRegion();
						}catch(Exception e){}
					})).growX();
				} else if (type == Color.class) {
					body.add(new SmartColorInput(null, (Color)val, v -> { try{((Color)f.get(c)).set(v);}catch(Exception e){} })).growX();
				} else if (type == Vector2.class) {
					Vector2 v = (Vector2)val;
					Table vecTable = new Table();
					vecTable.add(new SmartNumInput("X", v.x, 0.1f, newVal -> v.x = newVal)).growX().padRight(5);
					vecTable.add(new SmartNumInput("Y", v.y, 0.1f, newVal -> v.y = newVal)).growX();
					body.add(vecTable).growX();
				} else {
					body.add(new VisLabel(val != null ? val.toString() : "null")).growX();
				}
				body.row();
			} catch (Exception e) {}
		}
		inspectorContainer.add(body).growX().colspan(2).row();
	}

	private void showAddComponentMenu(GObject selection, float x, float y) {
		PopupMenu menu = new PopupMenu();
		registerCompMenuItem(menu, selection, SpriteComponent.class);
		registerCompMenuItem(menu, selection, SkeletonComponent.class);
		registerCompMenuItem(menu, selection, NeonAnimatorComponent.class);
		registerCompMenuItem(menu, selection, FsmComponent.class);
		menu.showMenu(stage, x, y);
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

	public void render(float delta) {
		GameWorld.inst().update(delta);
		if (hierarchyDirty) { refreshHierarchy(); hierarchyDirty = false; }
		gameCamera.update();
		sceneCamera.update();
		gameTarget.renderToFbo(() -> {
			gameViewport.apply();
			spriteSystem.setCamera(gameCamera);
			spriteSystem.update(delta);
			skeletonRenderSystem.setCamera(gameCamera);
			skeletonRenderSystem.update(delta);
		});
		sceneTarget.renderToFbo(() -> {
			Gdx.gl.glViewport(0, 0, sceneTarget.getFboWidth(), sceneTarget.getFboHeight());
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			drawGrid(sceneCamera);
			spriteSystem.setCamera(sceneCamera);
			spriteSystem.update(delta);
			skeletonRenderSystem.setCamera(sceneCamera);
			skeletonRenderSystem.update(delta);
			neonBatch.setProjectionMatrix(sceneCamera.combined);
			neonBatch.begin();
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
		stage.act(delta);
		stage.draw();
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
		private boolean isDragging = false;
		private enum DragMode { NONE, BODY, MOVE_X, MOVE_Y, ROTATE, SCALE_X, SCALE_Y }
		private DragMode currentDragMode = DragMode.NONE;
		private float lastX, lastY;

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (button != Input.Buttons.LEFT) return false;
			Vector2 wPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
			GObject sel = sceneManager.getSelection();
			if (sel != null) {
				DragMode gizmoHit = hitTestGizmo(sel, wPos);
				if (gizmoHit != DragMode.NONE) {
					startDrag(gizmoHit, wPos);
					return true;
				}
			}
			GObject hit = hitTestGObject(wPos);
			if (hit != null) {
				if (hit != sel) sceneManager.select(hit);
				startDrag(DragMode.BODY, wPos);
				return true;
			}
			if (sel != null) sceneManager.select(null);
			return false;
		}
		private void startDrag(DragMode mode, Vector2 pos) {
			currentDragMode = mode;
			lastX = pos.x;
			lastY = pos.y;
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
				return true;
			}
			return false;
		}
		private void applyTransform(GObject t, float dx, float dy, Vector2 currPos) {
			float rot = t.transform.worldRotation;
			float rad = rot * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad);
			float s = MathUtils.sin(rad);
			Vector2 targetWorldPos = t.transform.worldPosition.cpy();
			switch (currentDragMode) {
				case BODY:
					targetWorldPos.add(dx, dy);
					applyWorldPosToLocal(t, targetWorldPos);
					break;
				case MOVE_X:
					float projX = dx * c + dy * s;
					targetWorldPos.add(projX * c, projX * s);
					applyWorldPosToLocal(t, targetWorldPos);
					break;
				case MOVE_Y:
					float projY = dx * (-s) + dy * c;
					targetWorldPos.add(-projY * s, projY * c);
					applyWorldPosToLocal(t, targetWorldPos);
					break;
				case ROTATE:
					float cx = t.transform.worldPosition.x;
					float cy = t.transform.worldPosition.y;
					Vector2 prevDir = new Vector2(lastX - cx, lastY - cy);
					Vector2 currDir = new Vector2(currPos.x - cx, currPos.y - cy);
					t.transform.rotation += currDir.angleDeg() - prevDir.angleDeg();
					break;
				case SCALE_X: case SCALE_Y:
					float distOld = Vector2.dst(lastX, lastY, t.transform.worldPosition.x, t.transform.worldPosition.y);
					float distNew = Vector2.dst(currPos.x, currPos.y, t.transform.worldPosition.x, t.transform.worldPosition.y);
					if (distOld > 0.1f) t.transform.scale *= distNew / distOld;
					break;
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
				if (pos.dst(tx + c * axisLen, ty + s * axisLen) < hitR) return DragMode.SCALE_X;
			}
			if (pos.dst(tx, ty) < 15 * zoom) return DragMode.BODY;
			return DragMode.NONE;
		}
		private GObject hitTestGObject(Vector2 p) {
			for(GObject root : GameWorld.inst().getRootEntities()) {
				if(p.dst(root.transform.worldPosition) < 60) return root;
				GObject childHit = hitTestRecursive(root, p);
				if (childHit != null) return childHit;
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
