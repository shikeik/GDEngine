package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.ComponentRegistry;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.input.ShortcutManager;
import com.goldsprite.gdengine.core.utils.GdxJsonSetup;
import com.goldsprite.gdengine.core.utils.SceneLoader;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.RenderComponent;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy.HierarchyPanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy.HierarchyPresenter;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.InspectorPanel;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.InspectorPresenter;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.gdengine.utils.SimpleCameraController;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

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
	private SimpleCameraController sceneCamController;

	// MVP Modules
	private HierarchyPanel hierarchyPanel;
	private InspectorPanel inspectorPanel;

	// Rendering
	private SpriteBatch spriteBatch;
	private NeonBatch neonBatch;
	private ShapeRenderer shapeRenderer;
	private WorldRenderSystem worldRenderSystem;
	private Stack gameWidgetStack;

	private FileHandle currentProj;

	public EditorController(EditorGameScreen screen) {
		this.screen = screen;
	}

	private void reloadProjectContext() {
		currentProj = com.goldsprite.gdengine.core.project.ProjectService.inst().getCurrentProject();
		if (currentProj != null) {
			GameWorld.projectAssetsRoot = currentProj.child("assets");
			Debug.logT("Editor", "🔗 链接到项目: " + currentProj.name());

			FileHandle indexFile = currentProj.child("project.index");
			if (indexFile.exists()) {
				Debug.logT("Editor", "🔄 Reloading User Index from: " + indexFile.path());
				ComponentRegistry.reloadUserIndex(indexFile);
			} else {
				Debug.logT("Editor", "⚠️ project.index not found. (Compile to generate)");
			}
		}
	}

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		int fboW = 1280; int fboH = 720;
		gameTarget = new ViewTarget(fboW, fboH);
		sceneTarget = new ViewTarget(fboW, fboH);
		sceneCamera = new OrthographicCamera(fboW, fboH);
		gameCamera = new OrthographicCamera();

		float scl = PlatformImpl.isAndroidUser() ? 1.3f : 2.0f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		reloadProjectContext();

		GameWorld.autoDispose();
		new GameWorld();
		worldRenderSystem = new WorldRenderSystem(neonBatch, gameCamera);
		GameWorld.inst().setReferences(stage.getViewport(), gameCamera);

		commandManager = new CommandManager();
		sceneManager = new EditorSceneManager(commandManager);
		gizmoSystem = new EditorGizmoSystem(sceneManager);

		createGameWidget();
		createSceneWidget();

		// 事件桥接
		sceneManager.onStructureChanged.add(o -> EditorEvents.inst().emitStructureChanged());
		sceneManager.onSelectionChanged.add(o -> EditorEvents.inst().emitSelectionChanged(o));

		Gd.init(Gd.Mode.EDITOR, new EditorGameInput(gameWidget), new EditorGameGraphics(gameTarget), Gd.compiler);
		reloadGameViewport();

		spriteBatch = new SpriteBatch();
		neonBatch = new NeonBatch();
		shapeRenderer = new ShapeRenderer();

		createUI();

		shortcutManager = new ShortcutManager(stage);
		registerShortcuts();

		sceneCamController = new SimpleCameraController(sceneCamera);
		sceneCamController.setCoordinateMapper((sx, sy) ->
			sceneWidget.screenToWorld(sx, sy, sceneCamera)
		);
		NativeEditorInput editorInput = new NativeEditorInput();
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(shortcutManager);
		multiplexer.addProcessor(editorInput);
		multiplexer.addProcessor(sceneCamController);

		if (screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(multiplexer);
		} else {
			Gd.input.setInputProcessor(multiplexer);
		}

		loadInitialScene();
	}

	private void loadInitialScene() {
		FileHandle projectScene = getSceneFile();
		if (projectScene != null && projectScene.exists()) {
			loadScene();
		} else if (Gdx.files.local("scene_debug.json").exists() && currentProj == null) {
			SceneLoader.load(Gdx.files.local("scene_debug.json"));
		} else {
			initTestScene();
		}
		EditorEvents.inst().emitStructureChanged();
		EditorEvents.inst().emitSceneLoaded();
	}

	private FileHandle getSceneFile() {
		if (currentProj != null) {
			return currentProj.child("scenes/main.scene");
		}
		return Gdx.files.local("scene_debug.json");
	}

	private void createUI() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");

		hierarchyPanel = new HierarchyPanel();
		new HierarchyPresenter(hierarchyPanel, sceneManager);

		inspectorPanel = new InspectorPanel();
		new InspectorPresenter(inspectorPanel, sceneManager);

		setupSceneDropTarget();

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

		VisSplitPane rightSplit = new VisSplitPane(centerStack, inspectorPanel, false);
		rightSplit.setSplitAmount(0.75f);

		VisSplitPane mainSplit = new VisSplitPane(hierarchyPanel, rightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();
		stage.addActor(root);

		stage.addActor(new ToastUI());
	}

	private void setupSceneDropTarget() {
		DragAndDrop dnd = hierarchyPanel.getDragAndDrop();
		if (dnd == null) return;

		dnd.addTarget(new Target(sceneWidget) {
			@Override
			public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
				return true;
			}

			@Override
			public void drop(Source source, Payload payload, float x, float y, int pointer) {
				GObject obj = (GObject) payload.getObject();
				// 仅作为视觉反馈，暂不执行逻辑
			}
		});
	}

	private void saveScene() {
		try {
			Json json = GdxJsonSetup.create();
			List<GObject> roots = GameWorld.inst().getRootEntities();
			String text = json.prettyPrint(roots);
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
		SceneLoader.load(getSceneFile());
		EditorEvents.inst().emitStructureChanged();
		EditorEvents.inst().emitSceneLoaded();
		sceneManager.select(null);
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

	private void createGameWidget() {
		gameWidget = new ViewWidget(gameTarget);
		gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				String mode = box.getSelected();
				if(mode.equals("FIT")) {
					Gd.config.viewportType = Gd.ViewportType.FIT;
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
				} else if(mode.equals("STRETCH")) {
					Gd.config.viewportType = Gd.ViewportType.STRETCH;
					gameWidget.setDisplayMode(ViewWidget.DisplayMode.STRETCH);
				} else if(mode.equals("EXTEND")) {
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

	public void render(float delta) {
		GameWorld.inst().update(delta);
		gameCamera.update();
		sceneCamera.update();

		gameTarget.renderToFbo(() -> {
			gameViewport.apply();
			GameWorld.inst().render(neonBatch, gameCamera);
		});

		sceneTarget.renderToFbo(() -> {
			Gdx.gl.glViewport(0, 0, sceneTarget.getFboWidth(), sceneTarget.getFboHeight());
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			drawGrid(sceneCamera);
			GameWorld.inst().render(neonBatch, sceneCamera);
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

	// --- 输入处理 ---

	private class NativeEditorInput extends com.badlogic.gdx.InputAdapter {

		private enum DragMode { NONE, BODY, MOVE_X, MOVE_Y, ROTATE, SCALE_X, SCALE_Y, SCALE }
		private DragMode currentDragMode = DragMode.NONE;
		private float lastX, lastY;
		private Vector2 startScale = new Vector2();
		private Vector2 startDragPos = new Vector2();

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (pointer > 0 || button == com.badlogic.gdx.Input.Buttons.RIGHT || button == com.badlogic.gdx.Input.Buttons.MIDDLE) return false;
			if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
				Vector2 wPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
				GObject sel = sceneManager.getSelection();
				if (sel != null) {
					DragMode gizmoHit = hitTestGizmo(sel, wPos);
					if (gizmoHit != DragMode.NONE) { startDrag(gizmoHit, wPos, sel); return true; }
				}
				GObject hit = hitTestGObject(wPos);
				if (hit != null) {
					if (hit != sel) {
						sceneManager.select(hit);
						EditorEvents.inst().emitSelectionChanged(hit);
					}
					startDrag(DragMode.BODY, wPos, hit);
					return true;
				}
				if (sel != null) {
					sceneManager.select(null);
					EditorEvents.inst().emitSelectionChanged(null);
				}
			}
			return false;
		}

		private void startDrag(DragMode mode, Vector2 pos, GObject target) {
			currentDragMode = mode;
			lastX = pos.x; lastY = pos.y;
			startDragPos.set(pos);
			if(target != null) startScale.set(target.transform.scale);
			switch (mode) {
				case MOVE_X: case SCALE_X: gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_X; break;
				case MOVE_Y: case SCALE_Y: gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_Y; break;
				case ROTATE: gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_ROTATE; break;
				case BODY: case SCALE: gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_CENTER; break;
				default: gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_NONE; break;
			}
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (currentDragMode == DragMode.NONE || sceneManager.getSelection() == null) return false;
			Vector2 wPos = sceneWidget.screenToWorld(screenX, screenY, sceneCamera);
			float dx = wPos.x - lastX; float dy = wPos.y - lastY;
			GObject t = sceneManager.getSelection();
			applyTransform(t, dx, dy, wPos);
			// 拖拽时只更新位置，Inspector 数值刷新由 InspectorPanel.act 轮询负责
			lastX = wPos.x; lastY = wPos.y;
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (currentDragMode != DragMode.NONE) {
				currentDragMode = DragMode.NONE;
				gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_NONE;
				EditorEvents.inst().emitPropertyChanged();
				return true;
			}
			return false;
		}

		private void applyTransform(GObject t, float dx, float dy, Vector2 currPos) {
			float rot = t.transform.worldRotation;
			float rad = rot * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad); float s = MathUtils.sin(rad);
			float cx = t.transform.worldPosition.x; float cy = t.transform.worldPosition.y;
			Vector2 targetWorldPos = t.transform.worldPosition.cpy();
			float scaleSensitivity = 0.01f; float minScaleLimit = 0.01f;

			switch (currentDragMode) {
				case BODY: targetWorldPos.add(dx, dy); t.transform.setWorldPosition(targetWorldPos); break;
				case MOVE_X: float projX = dx * c + dy * s; targetWorldPos.add(projX * c, projX * s); t.transform.setWorldPosition(targetWorldPos); break;
				case MOVE_Y: float projY = dx * (-s) + dy * c; targetWorldPos.add(-projY * s, projY * c); t.transform.setWorldPosition(targetWorldPos); break;
				case ROTATE:
					Vector2 prevDir = new Vector2(lastX - cx, lastY - cy);
					Vector2 currDir = new Vector2(currPos.x - cx, currPos.y - cy);
					t.transform.rotation += currDir.angleDeg() - prevDir.angleDeg();
					break;
				case SCALE_X: case SCALE_Y: case SCALE:
					Vector2 dirX = new Vector2(c, s); Vector2 dirY = new Vector2(-s, c); Vector2 dirUni = new Vector2(c-s, s+c).nor();
					Vector2 dragVec = new Vector2(currPos).sub(startDragPos);
					float delta = 0;
					if (currentDragMode == DragMode.SCALE_X) delta = dragVec.dot(dirX) * scaleSensitivity;
					else if (currentDragMode == DragMode.SCALE_Y) delta = dragVec.dot(dirY) * scaleSensitivity;
					else delta = dragVec.dot(dirUni) * scaleSensitivity;
					float newSx = startScale.x; float newSy = startScale.y;
					if (currentDragMode == DragMode.SCALE_X) newSx += delta;
					else if (currentDragMode == DragMode.SCALE_Y) newSy += delta;
					else { newSx += delta; newSy += delta; }
					if (startScale.x > 0) newSx = Math.max(minScaleLimit, newSx); else newSx = Math.min(-minScaleLimit, newSx);
					if (startScale.y > 0) newSy = Math.max(minScaleLimit, newSy); else newSy = Math.min(-minScaleLimit, newSy);
					t.transform.scale.x = newSx; t.transform.scale.y = newSy;
					break;
			}
		}

		private DragMode hitTestGizmo(GObject t, Vector2 pos) {
			float zoom = sceneCamera.zoom * 1.4f;
			float axisLen = EditorGizmoSystem.AXIS_LEN * zoom;
			float hitR = 20f * zoom;
			float tx = t.transform.worldPosition.x; float ty = t.transform.worldPosition.y;
			float rot = t.transform.worldRotation;
			float rad = rot * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad); float s = MathUtils.sin(rad);
			EditorGizmoSystem.Mode mode = gizmoSystem.mode;

			if (mode == EditorGizmoSystem.Mode.MOVE) {
				if (pos.dst(tx + c * axisLen, ty + s * axisLen) < hitR) return DragMode.MOVE_X;
				if (pos.dst(tx - s * axisLen, ty + c * axisLen) < hitR) return DragMode.MOVE_Y;
			} else if (mode == EditorGizmoSystem.Mode.ROTATE) {
				if (pos.dst(tx + c * axisLen, ty + s * axisLen) < hitR) return DragMode.ROTATE;
			} else if (mode == EditorGizmoSystem.Mode.SCALE) {
				if (pos.dst(tx, ty) < 12f * zoom) return DragMode.SCALE;
				if (pos.dst(tx + c * axisLen, ty + s * axisLen) < hitR) return DragMode.SCALE_X;
				if (pos.dst(tx - s * axisLen, ty + c * axisLen) < hitR) return DragMode.SCALE_Y;
			}
			if (mode != EditorGizmoSystem.Mode.SCALE && pos.dst(tx, ty) < 15 * zoom) return DragMode.BODY;
			return DragMode.NONE;
		}

		private GObject hitTestGObject(Vector2 p) {
			List<RenderComponent> renderables = worldRenderSystem.getSortedRenderables();
			for (int i = renderables.size() - 1; i >= 0; i--) {
				RenderComponent rc = renderables.get(i);
				if (rc.contains(p.x, p.y)) return rc.getGObject();
			}
			return null;
		}
	}
}
