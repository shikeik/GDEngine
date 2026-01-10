package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.SkeletonRenderSystem;
import com.goldsprite.gdengine.ecs.system.SpriteSystem;
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
import java.util.List;

/**
 * 这里的 EditorController 已经是完全“原生”的了。
 * 它直接操作 GObject，不再使用任何 Adapter 或 IconEditor 的遗留代码。
 */
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
	private ViewWidget focusedWidget;

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
		gameCamera = new OrthographicCamera(); // 大小由 Viewport 决定

		// 2. 初始化 UI 环境
		float scl = PlatformImpl.isAndroidUser() ? 1.3f : 2.0f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		// 3. 初始化编辑器内核 (Native)
		commandManager = new CommandManager();
		sceneManager = new EditorSceneManager(commandManager);
		gizmoSystem = new EditorGizmoSystem(sceneManager);

		// 4. 依赖注入引擎
		// [关键] 我们把自己包装好的 Input/Graphics 注入进去，让游戏觉得它在跑全屏
		Gd.init(Gd.Mode.EDITOR, new EditorGameInput(gameWidget), new EditorGameGraphics(gameTarget), Gd.compiler);

		// 5. 初始化 ECS 世界
		if (GameWorld.inst() == null) new GameWorld();
		// 将游戏相机绑定给世界 (用于射线检测等逻辑)
		GameWorld.inst().setReferences(stage.getViewport(), gameCamera);

		reloadGameViewport(); // 初始化游戏视口

		// 6. 初始化渲染管线
		spriteBatch = new SpriteBatch();
		neonBatch = new NeonBatch();
		shapeRenderer = new ShapeRenderer();

		// 这些 System 主要是用来画游戏内容的，我们在 render 时手动调用
		spriteSystem = new SpriteSystem(spriteBatch, gameCamera);
		skeletonRenderSystem = new SkeletonRenderSystem(neonBatch, gameCamera);

		// 7. 构建 UI 布局
		createUI();

		// 8. 监听事件 (UI 刷新)
		sceneManager.onStructureChanged.add(o -> refreshHierarchy());
		sceneManager.onSelectionChanged.add(this::refreshInspector);

		// 9. 输入处理 (Input Pipeline)
		// 创建一个内部类来处理场景操作 (Gizmo/Select)
		NativeEditorInput editorInput = new NativeEditorInput();

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage); // UI 优先
		multiplexer.addProcessor(editorInput); // 场景操作次之

		if (screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(multiplexer);
		} else {
			Gdx.input.setInputProcessor(multiplexer);
		}

		// 10. 加载初始测试场景
		initTestScene();
	}

	private void initTestScene() {
		// 清空旧物体 (如果有)
		// GameWorld.inst().clear(); // TODO: GameWorld 需要支持 clear

		GObject player = new GObject("Player");
		player.transform.setPosition(0, 0);
		SpriteComponent sp = player.addComponent(SpriteComponent.class);
		sp.setRegion(new TextureRegion(new Texture(Gdx.files.internal("gd_icon.png"))));
		sp.width = 100; sp.height = 100;

		GObject child = new GObject("Child (Weapon)");
		child.setParent(player);
		child.transform.setPosition(80, 0);
		child.transform.scale = 0.5f;
		SpriteComponent sp2 = child.addComponent(SpriteComponent.class);
		sp2.setRegion(new TextureRegion(new Texture(Gdx.files.internal("gd_icon.png"))));
		sp2.width = 100; sp2.height = 100;
		sp2.color.set(Color.RED);

		// 刷新 UI
		sceneManager.notifyStructureChanged();
	}

	private void createUI() {
		// 1. Widget 创建
		createGameWidget();
		createSceneWidget();

		// 2. 根布局
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");

		// 3. 左侧：Hierarchy
		hierarchyContainer = new VisTable();
		hierarchyContainer.setBackground("button");
		hierarchyContainer.top().left();
		VisScrollPane hierarchyScroll = new VisScrollPane(hierarchyContainer);
		hierarchyScroll.setFadeScrollBars(false);

		// 4. 右侧：Inspector
		inspectorContainer = new VisTable();
		inspectorContainer.setBackground("button");
		inspectorContainer.top().left();
		VisScrollPane inspectorScroll = new VisScrollPane(inspectorContainer);

		// 5. 中间：视图 + 工具栏
		Stack centerStack = new Stack();

		// 分割 Scene 和 Game 视图
		VisSplitPane viewSplit = new VisSplitPane(sceneWidget, gameWidgetStack, true);
		viewSplit.setSplitAmount(0.5f);
		centerStack.add(viewSplit);

		// 悬浮工具栏
		Table toolbar = new Table();
		toolbar.top().left().pad(5);
		addToolBtn(toolbar, "M (Move)", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.MOVE);
		addToolBtn(toolbar, "R (Rot)", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.ROTATE);
		addToolBtn(toolbar, "S (Scl)", () -> gizmoSystem.mode = EditorGizmoSystem.Mode.SCALE);
		centerStack.add(toolbar);

		// 6. 整体组装 (Left | Center | Right)
		VisSplitPane rightSplit = new VisSplitPane(centerStack, inspectorScroll, false);
		rightSplit.setSplitAmount(0.75f);

		VisSplitPane mainSplit = new VisSplitPane(hierarchyScroll, rightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();
		stage.addActor(root);

		refreshHierarchy();
	}

	private void createGameWidget() {
		gameWidget = new ViewWidget(gameTarget);
		gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);

		// 简单的视口切换
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

		// 简单的场景漫游输入
		sceneWidget.addListener(new InputListener() {
			float lastX, lastY;
			@Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if(button == Input.Buttons.RIGHT) { lastX = x; lastY = y; return true; }
				return false;
			}
			@Override public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
					float z = sceneCamera.zoom;
					// ViewWidget 内部可能有缩放，这里简单处理
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
	// 核心逻辑: Hierarchy (GObject Tree)
	// =======================================================

	private void refreshHierarchy() {
		hierarchyContainer.clearChildren();
		hierarchyTree = new VisTree<>();
		hierarchyTree.getSelection().setProgrammaticChangeEvents(false);

		// 从 GameWorld 获取根物体
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

	// 简单的树节点实现
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

	// =======================================================
	// 核心逻辑: Inspector (Reflection)
	// =======================================================

	private void refreshInspector(GObject selection) {
		inspectorContainer.clearChildren();
		if (selection == null) {
			inspectorContainer.add(new VisLabel("No Selection")).pad(10);
			return;
		}

		// 1. GObject 基础属性
		inspectorContainer.add(new VisLabel("Name:")).left();
		inspectorContainer.add(new SmartTextInput(null, selection.getName(), selection::setName)).growX().row();

		// 2. 遍历组件
		for (List<Component> comps : selection.getComponentsMap().values()) {
			for (Component c : comps) {
				buildComponentUI(c);
			}
		}
	}

	private void buildComponentUI(Component c) {
		// 标题栏
		VisTable header = new VisTable();
		header.setBackground("button");
		header.add(new VisLabel(c.getClass().getSimpleName())).expandX().left().pad(5);
		inspectorContainer.add(header).growX().padTop(5).row();

		VisTable body = new VisTable();
		body.padLeft(10);

		// 简单的反射 UI
		Field[] fields = c.getClass().getFields();
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;
			try {
				String name = f.getName();
				Object val = f.get(c);
				Class<?> type = f.getType();

				body.add(new VisLabel(name)).left().width(80);

				if (type == float.class) {
					body.add(new SmartNumInput(null, (float)val, 0.1f, v -> { try{f.setFloat(c,v);}catch(Exception e){} })).growX();
				} else if (type == String.class) {
					body.add(new SmartTextInput(null, (String)val, v -> { try{f.set(c,v);}catch(Exception e){} })).growX();
				} else {
					body.add(new VisLabel(val != null ? val.toString() : "null")).growX();
				}
				body.row();

			} catch (Exception e) {}
		}
		inspectorContainer.add(body).growX().row();
	}

	// =======================================================
	// 渲染循环
	// =======================================================

	public void render(float delta) {
		// 1. 更新逻辑
		GameWorld.inst().update(delta);
		gameCamera.update();
		sceneCamera.update();

		// 2. 渲染 Game View (FBO)
		gameTarget.renderToFbo(() -> {
			gameViewport.apply();
			// 手动驱动 RenderSystem (使用 GameCamera)
			spriteSystem.setCamera(gameCamera);
			spriteSystem.update(delta);
			skeletonRenderSystem.setCamera(gameCamera);
			skeletonRenderSystem.update(delta);
		});

		// 3. 渲染 Scene View (FBO)
		sceneTarget.renderToFbo(() -> {
			Gdx.gl.glViewport(0, 0, sceneTarget.getFboWidth(), sceneTarget.getFboHeight());
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			// 绘制网格
			drawGrid(sceneCamera);

			// 绘制物体 (使用 SceneCamera)
			spriteSystem.setCamera(sceneCamera);
			spriteSystem.update(delta);
			skeletonRenderSystem.setCamera(sceneCamera);
			skeletonRenderSystem.update(delta);

			// 绘制 Gizmo
			neonBatch.setProjectionMatrix(sceneCamera.combined);
			neonBatch.begin();
			// 绘制选中框
			if(sceneManager.getSelection() != null) {
				GObject sel = sceneManager.getSelection();
				float x = sel.transform.worldPosition.x;
				float y = sel.transform.worldPosition.y;
				neonBatch.drawRect(x-25, y-25, 50, 50, sel.transform.worldRotation, 2, Color.YELLOW, false);
			}
			gizmoSystem.render(neonBatch, sceneCamera.zoom);
			neonBatch.end();
		});

		// 4. 上屏 UI
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
	// 输入处理 (Inner Class for Simplicity)
	// =======================================================

	private class NativeEditorInput extends InputAdapter {
		private boolean isDragging = false;

		// 简单的 Gizmo 命中检测与拖拽逻辑
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 worldPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);

			// 1. 尝试选中 Gizmo (这里简化处理，只做物体点击)
			if (button == Input.Buttons.LEFT) {
				GObject hit = hitTestGObject(worldPos);
				sceneManager.select(hit);
				if (hit != null) isDragging = true;
				return hit != null; // 拦截事件
			}
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (isDragging && sceneManager.getSelection() != null) {
				Vector2 worldPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
				// 简单的跟随鼠标移动 (实际应该结合 Gizmo 轴向)
				sceneManager.getSelection().transform.setPosition(worldPos.x, worldPos.y);

				// 实时刷新 Inspector
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
			// 简单的距离检测 (遍历所有顶层物体)
			for(GObject root : GameWorld.inst().getRootEntities()) {
				if(p.dst(root.transform.worldPosition) < 50) return root; // 半径50
				// TODO: 递归检测子物体
			}
			return null;
		}
	}
}
