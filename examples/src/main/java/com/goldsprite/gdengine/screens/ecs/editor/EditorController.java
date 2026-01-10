package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
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
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.SkeletonRenderSystem;
import com.goldsprite.gdengine.ecs.system.SpriteSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.kotcrab.vis.ui.VisUI;
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

	// 渲染核心 (FBO)
	private ViewTarget gameTarget, sceneTarget;
	private ViewWidget gameWidget, sceneWidget;

	// 相机系统
	private OrthographicCamera sceneCamera; // 上帝视角
	private OrthographicCamera gameCamera;  // 游戏视角
	private Viewport gameViewport;

	// 编辑器内核
	private CommandManager commandManager;
	private EditorSceneManager sceneManager;
	private EditorGizmoSystem gizmoSystem;

	// UI 组件
	private VisTree<GObjectNode, GObject> hierarchyTree;
	private VisTable hierarchyContainer;
	private VisTable inspectorContainer;

	// 渲染系统
	private SpriteBatch spriteBatch;
	private NeonBatch neonBatch;
	private ShapeRenderer shapeRenderer;
	private SpriteSystem spriteSystem;
	private SkeletonRenderSystem skeletonRenderSystem;
	private Stack gameWidgetStack;

	private boolean hierarchyDirty = false; // UI脏标记

	private ShortcutManager shortcutManager;

	public EditorController(EditorGameScreen screen) {
		this.screen = screen;
	}

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		// 1. 初始化 FBO 环境
		int fboW = 1280;
		int fboH = 720;
		gameTarget = new ViewTarget(fboW, fboH);
		sceneTarget = new ViewTarget(fboW, fboH);

		sceneCamera = new OrthographicCamera(fboW, fboH);
		gameCamera = new OrthographicCamera();

		// 2. 初始化 UI 环境
		float scl = PlatformImpl.isAndroidUser() ? 1.3f : 2.0f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		// 3. 初始化编辑器内核
		commandManager = new CommandManager();
		sceneManager = new EditorSceneManager(commandManager);
		gizmoSystem = new EditorGizmoSystem(sceneManager);

		// 4. 依赖注入引擎
		Gd.init(Gd.Mode.EDITOR, new EditorGameInput(gameWidget), new EditorGameGraphics(gameTarget), Gd.compiler);

		// --- [新增] 初始化快捷键系统 ---
		shortcutManager = new ShortcutManager(stage);
		// 注册业务逻辑
		shortcutManager.register("TOOL_MOVE", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.MOVE);
		shortcutManager.register("TOOL_ROTATE", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.ROTATE);
		shortcutManager.register("TOOL_SCALE", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.SCALE);
		shortcutManager.register("ACTION_UNDO", () -> commandManager.undo());
		shortcutManager.register("ACTION_REDO", () -> commandManager.redo());
		shortcutManager.register("ACTION_SAVE", this::saveScene);
		shortcutManager.register("ACTION_DELETE", () -> {
			// 调用 SceneManager 删除当前选中
			sceneManager.deleteSelection(); // 假设您在 EditorSceneManager 里实现了这个方法
		});

		// 5. 初始化 ECS 世界
		if (GameWorld.inst() == null) new GameWorld();
		GameWorld.inst().setReferences(stage.getViewport(), gameCamera);

		reloadGameViewport();

		// 6. 初始化渲染管线
		spriteBatch = new SpriteBatch();
		neonBatch = new NeonBatch();
		shapeRenderer = new ShapeRenderer();

		spriteSystem = new SpriteSystem(spriteBatch, gameCamera);
		skeletonRenderSystem = new SkeletonRenderSystem(neonBatch, gameCamera);

		// 7. 构建 UI 布局
		createUI();

		// 8. 监听事件
		sceneManager.onStructureChanged.add(o -> hierarchyDirty = true);
		sceneManager.onSelectionChanged.add(this::refreshInspector);

		// 9. 输入处理
		NativeEditorInput editorInput = new NativeEditorInput();
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);           // 1. UI 优先 (点击按钮、输入框)
		multiplexer.addProcessor(shortcutManager); // 2. [新增] 快捷键次之 (Ctrl+S, W/E/R)
		multiplexer.addProcessor(editorInput);     // 3. 场景拖拽最后

		if (screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(multiplexer);
		} else {
			Gdx.input.setInputProcessor(multiplexer);
		}

		// 10. 加载初始测试场景 (如果没有存档)
		// 如果本地已有存档，可以尝试 loadScene();
		if (Gdx.files.local("scene_debug.json").exists()) {
			loadScene();
		} else {
			initTestScene();
		}
	}

	private void initTestScene() {
		// [修改] 使用 setPath 而不是 setRegion，这样 Inspector 可以显示路径，保存也可以序列化
		GObject player = new GObject("Player");
		player.transform.setPosition(0, 0);
		SpriteComponent sp = player.addComponent(SpriteComponent.class);
		sp.setPath("gd_icon.png"); // 关键：设置路径
		sp.width = 100; sp.height = 100;

		GObject child = new GObject("Weapon");
		child.setParent(player);
		child.transform.setPosition(80, 0);
		child.transform.scale = 0.5f;
		SpriteComponent sp2 = child.addComponent(SpriteComponent.class);
		sp2.setPath("gd_icon.png");
		sp2.width = 100; sp2.height = 100;
		sp2.color.set(Color.RED);

		sceneManager.notifyStructureChanged();
	}

	// =======================================================
	// 核心逻辑: Save & Load
	// =======================================================

	private void saveScene() {
		try {
			Json json = GdxJsonSetup.create();
			// 序列化根物体列表
			List<GObject> roots = GameWorld.inst().getRootEntities();
			String text = json.prettyPrint(roots);

			FileHandle file = Gdx.files.local("scene_debug.json");
			file.writeString(text, false);

			Debug.logT("Editor", "Scene saved to: " + file.path());
		} catch (Exception e) {
			Debug.logT("Editor", "Save Failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadScene() {
		FileHandle file = Gdx.files.local("scene_debug.json");
		if (!file.exists()) return;

		try {
			// 1. 清理现有场景
			// GameWorld 需要一个 clear 方法，这里暂时手动销毁所有顶层物体
			List<GObject> currentRoots = new ArrayList<>(GameWorld.inst().getRootEntities());
			for(GObject obj : currentRoots) obj.destroyImmediate();

			sceneManager.select(null); // 清除选中

			// 2. 反序列化
			Json json = GdxJsonSetup.create();
			@SuppressWarnings("unchecked")
			ArrayList<GObject> newRoots = json.fromJson(ArrayList.class, GObject.class, file);

			// 注意：GdxJsonSetup 的 read 方法里已经 new GObject() 了，
			// 而 GObject 构造函数会自动 registerGObject 到 GameWorld。
			// 所以这里不需要手动 add。

			Debug.logT("Editor", "Scene loaded: " + newRoots.size() + " roots.");
			sceneManager.notifyStructureChanged();

		} catch (Exception e) {
			Debug.logT("Editor", "Load Failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// =======================================================
	// UI 构建
	// =======================================================

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

		// Right: Inspector
		inspectorContainer = new VisTable();
		inspectorContainer.setBackground("button");
		inspectorContainer.top().left();
		VisScrollPane inspectorScroll = new VisScrollPane(inspectorContainer);

		// Center: Toolbar + Views
		Stack centerStack = new Stack();

		VisSplitPane viewSplit = new VisSplitPane(sceneWidget, gameWidgetStack, true);
		viewSplit.setSplitAmount(0.5f);
		centerStack.add(viewSplit);

		// Toolbar
		Table toolbar = new Table();
		toolbar.top().left().pad(5);

		addToolBtn(toolbar, "Save", this::saveScene);
		addToolBtn(toolbar, "Load", this::loadScene);
		toolbar.add().width(20); // Separator

		addToolBtn(toolbar, "M", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.MOVE);
		addToolBtn(toolbar, "R", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.ROTATE);
		addToolBtn(toolbar, "S", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.SCALE);

		centerStack.add(toolbar);

		// Layout Assembly
		VisSplitPane rightSplit = new VisSplitPane(centerStack, inspectorScroll, false);
		rightSplit.setSplitAmount(0.75f);

		VisSplitPane mainSplit = new VisSplitPane(hierarchyScroll, rightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();
		stage.addActor(root);

		refreshHierarchy();
	}

	// ... (createGameWidget, createSceneWidget 保持不变) ...
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

	// =======================================================
	// 核心逻辑: Hierarchy & Inspector (Updated)
	// =======================================================

	private void refreshHierarchy() {
		hierarchyContainer.clearChildren();
		hierarchyTree = new VisTree<>();
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

	class GObjectNode extends Tree.Node<GObjectNode, GObject, VisLabel> {
		public GObjectNode(GObject obj) {
			super(new VisLabel(obj.getName()));
			setValue(obj);
			getActor().addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					sceneManager.select(obj);
				}
			});
		}
	}

	private void refreshInspector(GObject selection) {
		inspectorContainer.clearChildren();
		if (selection == null) {
			inspectorContainer.add(new VisLabel("No Selection")).pad(10);
			return;
		}

		// Header
		inspectorContainer.add(new VisLabel("Name:")).left();
		inspectorContainer.add(new SmartTextInput(null, selection.getName(), v -> {
			selection.setName(v);
			refreshHierarchy(); // 改名后刷新树
		})).growX().row();

		// Components
		for (List<Component> comps : selection.getComponentsMap().values()) {
			for (Component c : comps) {
				buildComponentUI(c);
			}
		}
	}

	private void buildComponentUI(Component c) {
		VisTable header = new VisTable();
		header.setBackground("button");
		header.add(new VisLabel(c.getClass().getSimpleName())).expandX().left().pad(5);
		inspectorContainer.add(header).growX().padTop(5).row();

		VisTable body = new VisTable();
		body.padLeft(10);

		Field[] fields = c.getClass().getFields();
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;
			// 忽略 transient 字段 (如 TextureRegion)
			if (Modifier.isTransient(f.getModifiers())) continue;

			try {
				String name = f.getName();
				Object val = f.get(c);
				Class<?> type = f.getType();

				body.add(new VisLabel(name)).left().width(80);

				if (type == float.class) {
					body.add(new SmartNumInput(null, (float)val, 0.1f, v -> { try{f.setFloat(c,v);}catch(Exception e){} })).growX();
				} else if (type == String.class) {
					// 字符串输入 (例如图片路径)
					// [改进] 添加回车或失去焦点后刷新 Sprite 的逻辑
					body.add(new SmartTextInput(null, (String)val, v -> {
						try{
							f.set(c,v);
							// 特殊处理：如果是 SpriteComponent 的 assetPath，触发 reload
							if (c instanceof SpriteComponent && name.equals("assetPath")) {
								((SpriteComponent)c).reloadRegion();
							}
						}catch(Exception e){}
					})).growX();
				} else {
					body.add(new VisLabel(val != null ? val.toString() : "null")).growX();
				}
				body.row();

			} catch (Exception e) {}
		}
		inspectorContainer.add(body).growX().row();
	}

	// =======================================================
	// 渲染循环 (保持不变)
	// =======================================================

	public void render(float delta) {
		// 1. 更新逻辑 (这里会执行 flushEntities，把 pendingAdds 里的物体真正加进去)
		GameWorld.inst().update(delta);
		// [新增] 核心修复：数据准备好后，如果标记为脏，则刷新 UI
		if (hierarchyDirty) {
			refreshHierarchy();
			hierarchyDirty = false;
		}
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

	// =======================================================
	// 输入处理 (Gizmo Interaction Core)
	// =======================================================

	private class NativeEditorInput extends InputAdapter {
		private enum DragMode { NONE, BODY, MOVE_X, MOVE_Y, ROTATE, SCALE_X, SCALE_Y }
		private DragMode currentDragMode = DragMode.NONE;

		private float lastX, lastY;
		private float startValScale; // 用于缩放计算

		// 碰撞检测辅助
		private Vector2 tmpVec = new Vector2();

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (button != Input.Buttons.LEFT) return false;

			Vector2 wPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
			GObject sel = sceneManager.getSelection();

			// 1. 优先检测 Gizmo (如果已选中物体)
			if (sel != null) {
				DragMode gizmoHit = hitTestGizmo(sel, wPos);
				if (gizmoHit != DragMode.NONE) {
					startDrag(gizmoHit, wPos);
					return true; // 拦截！不让 ViewWidget 拖动相机
				}
			}

			// 2. 检测物体点击
			GObject hit = hitTestGObject(wPos);
			if (hit != null) {
				// 如果点的不是当前选中的，切换选中
				if (hit != sel) {
					sceneManager.select(hit);
				}
				startDrag(DragMode.BODY, wPos);
				return true;
			}

			// 3. 点击空白 -> 取消选中
			if (sel != null) {
				sceneManager.select(null);
			}

			return false;
		}

		private void startDrag(DragMode mode, Vector2 pos) {
			currentDragMode = mode;
			lastX = pos.x;
			lastY = pos.y;

			// 记录初始状态 (用于 Scale 计算或其他 Undo 逻辑)
			if (sceneManager.getSelection() != null) {
				startValScale = sceneManager.getSelection().transform.scale;
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
				return true;
			}
			return false;
		}

		// --- 核心数学逻辑 ---

		private void applyTransform(GObject t, float dx, float dy, Vector2 currPos) {
			// 准备数据
			float rot = t.transform.worldRotation;
			float rad = rot * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad);
			float s = MathUtils.sin(rad);

			// 如果有父级，位移需要转换到父级局部空间
			// 但 Gizmo 的计算是基于 World Delta 的投影。
			// 策略：
			// 1. 计算世界坐标下的理想位移量 (World Delta)
			// 2. 将其应用到 worldPosition (临时)
			// 3. 使用 transform.worldToLocal (父级逆矩阵) 反算 localPosition

			Vector2 targetWorldPos = t.transform.worldPosition.cpy(); // 起始世界坐标

			switch (currentDragMode) {
				case BODY:
					// 自由移动: 直接加
					targetWorldPos.add(dx, dy);
					applyWorldPosToLocal(t, targetWorldPos);
					break;

				case MOVE_X:
					// 投影到物体 X 轴 (World Space)
					// X轴方向向量: (c, s)
					float projX = dx * c + dy * s;
					targetWorldPos.add(projX * c, projX * s);
					applyWorldPosToLocal(t, targetWorldPos);
					break;

				case MOVE_Y:
					// 投影到物体 Y 轴 (World Space)
					// Y轴方向向量: (-s, c)
					float projY = dx * (-s) + dy * c;
					targetWorldPos.add(-projY * s, projY * c);
					applyWorldPosToLocal(t, targetWorldPos);
					break;

				case ROTATE:
					// 计算角度差 (Atan2)
					// 中心点
					float cx = t.transform.worldPosition.x;
					float cy = t.transform.worldPosition.y;

					Vector2 prevDir = new Vector2(lastX - cx, lastY - cy);
					Vector2 currDir = new Vector2(currPos.x - cx, currPos.y - cy);
					float angleDelta = currDir.angleDeg() - prevDir.angleDeg();

					t.transform.rotation += angleDelta;
					break;

				case SCALE_X: // 简化：统一缩放
				case SCALE_Y:
//				case SCALE: // 如果有的话
					// 计算离中心的距离变化
					float distOld = Vector2.dst(lastX, lastY, t.transform.worldPosition.x, t.transform.worldPosition.y);
					float distNew = Vector2.dst(currPos.x, currPos.y, t.transform.worldPosition.x, t.transform.worldPosition.y);

					if (distOld > 0.1f) {
						float ratio = distNew / distOld;
						t.transform.scale *= ratio;
					}
					break;
			}
		}

		// 辅助：将计算出的 World Position 转为 Local Position 并赋值
		private void applyWorldPosToLocal(GObject t, Vector2 targetWorldPos) {
			GObject parent = t.getParent();
			if (parent != null) {
				// Local = ParentWorldInv * TargetWorld
				// 使用 parent.transform.worldToLocal 可以把一个世界点转为相对于 parent 的局部点
				Vector2 local = new Vector2();
				parent.transform.worldToLocal(targetWorldPos, local);
				t.transform.position.set(local);
			} else {
				t.transform.position.set(targetWorldPos);
			}
		}

		private DragMode hitTestGizmo(GObject t, Vector2 pos) {
			float zoom = sceneCamera.zoom * 1.4f; // 匹配 Render 的缩放
			float axisLen = EditorGizmoSystem.AXIS_LEN * zoom;
			float hitR = 20f * zoom; // 点击半径

			float tx = t.transform.worldPosition.x;
			float ty = t.transform.worldPosition.y;
			float rot = t.transform.worldRotation;

			float rad = rot * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad);
			float s = MathUtils.sin(rad);

			EditorGizmoSystem.Mode mode = gizmoSystem.mode;

			if (mode == EditorGizmoSystem.Mode.MOVE) {
				// X Axis Tip
				float xx = tx + c * axisLen;
				float xy = ty + s * axisLen;
				if (pos.dst(xx, xy) < hitR) return DragMode.MOVE_X;

				// Y Axis Tip
				float yx = tx - s * axisLen;
				float yy = ty + c * axisLen;
				if (pos.dst(yx, yy) < hitR) return DragMode.MOVE_Y;
			}
			else if (mode == EditorGizmoSystem.Mode.ROTATE) {
				// Rotate Handle
				float hx = tx + c * axisLen;
				float hy = ty + s * axisLen;
				if (pos.dst(hx, hy) < hitR) return DragMode.ROTATE;
			}
			else if (mode == EditorGizmoSystem.Mode.SCALE) {
				// Scale Tip (X)
				float xx = tx + c * axisLen;
				float xy = ty + s * axisLen;
				if (pos.dst(xx, xy) < hitR) return DragMode.SCALE_X; // 暂时只用 X 代表整体缩放
			}

			// Body Hit (Center) - 如果点中中心，也可以移动
			if (pos.dst(tx, ty) < 15 * zoom) return DragMode.BODY;

			return DragMode.NONE;
		}

		private GObject hitTestGObject(Vector2 p) {
			// 遍历 GameWorld 的根物体 (简单的圆形检测)
			// 实际应该递归检测所有子物体，且按渲染顺序倒序检测
			// 这里简化：只检测所有有 Sprite 的物体

			// 为了支持点选子物体，我们需要一个递归查找方法
			// 或者利用 ECS 的 SpriteSystem 列表？
			// 最简单的方式：递归遍历 root entities

			for(GObject root : GameWorld.inst().getRootEntities()) {
				GObject hit = recursiveHitTest(root, p);
				if (hit != null) return hit;
			}
			return null;
		}

		private GObject recursiveHitTest(GObject node, Vector2 p) {
			// 先查子物体 (后渲染的在上面，所以倒序查？List 顺序通常是渲染顺序)
			// 我们应该倒序遍历子物体，以符合“点中上层”的直觉
			for (int i = node.getChildren().size() - 1; i >= 0; i--) {
				GObject hit = recursiveHitTest(node.getChildren().get(i), p);
				if (hit != null) return hit;
			}

			// 查自己
			if (checkHit(node, p)) return node;
			return null;
		}

		private boolean checkHit(GObject obj, Vector2 p) {
			// 简单的半径检测 (World Space)
			float radius = 40; // 默认半径

			// 如果有 Sprite，尝试用 Sprite 尺寸
			SpriteComponent sp = obj.getComponent(SpriteComponent.class);
			if (sp != null && sp.width > 0) {
				radius = Math.min(sp.width, sp.height) / 2f * obj.transform.worldScale;
			}

			return p.dst(obj.transform.worldPosition) < radius;
		}
	}
}
