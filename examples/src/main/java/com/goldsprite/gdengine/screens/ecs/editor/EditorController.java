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
		sceneManager.onStructureChanged.add(o -> refreshHierarchy());
		sceneManager.onSelectionChanged.add(this::refreshInspector);

		// 9. 输入处理
		NativeEditorInput editorInput = new NativeEditorInput();
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(editorInput);

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
		GameWorld.inst().update(delta);
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
	// 输入处理
	// =======================================================

	private class NativeEditorInput extends InputAdapter {
		private boolean isDragging = false;

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 worldPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);

			if (button == Input.Buttons.LEFT) {
				GObject hit = hitTestGObject(worldPos);
				sceneManager.select(hit);
				if (hit != null) isDragging = true;
				return hit != null;
			}
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (isDragging && sceneManager.getSelection() != null) {
				Vector2 worldPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
				sceneManager.getSelection().transform.setPosition(worldPos.x, worldPos.y);
				refreshInspector(sceneManager.getSelection());
				return true;
			}
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			isDragging = false;
			return false;
		}

		private GObject hitTestGObject(Vector2 p) {
			for(GObject root : GameWorld.inst().getRootEntities()) {
				// 简单的碰撞检测，为了方便选中，半径稍微给大点
				if(p.dst(root.transform.worldPosition) < 60) return root;
				// 递归检测子物体 (简单实现)
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
