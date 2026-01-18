package com.goldsprite.gdengine.screens.ecs.editor.mvp.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.core.utils.GdxJsonSetup;
import com.goldsprite.gdengine.core.utils.SceneLoader;
import com.goldsprite.gdengine.ecs.ComponentManager;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.RenderComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.RenderLayerManager;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorGizmoSystem;
import com.goldsprite.gdengine.screens.ecs.editor.core.EditorSceneManager;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.gdengine.utils.SimpleCameraController;

import java.util.ArrayList;
import java.util.List;

public class ScenePresenter {

	private final ISceneView view;
	private final EditorSceneManager sceneManager;

	// Rendering Core
	private final OrthographicCamera camera;
	private final NeonBatch neonBatch;
	private final ShapeRenderer shapeRenderer;
	private final WorldRenderSystem renderSystem; // 用于点击检测
	private final EditorGizmoSystem gizmoSystem;

	// Input
	private final SimpleCameraController cameraController;
	private final SceneInputProcessor inputProcessor;

	public ScenePresenter(ISceneView view, EditorSceneManager sceneManager, NeonBatch neonBatch, WorldRenderSystem renderSystem) {
		this.view = view;
		this.sceneManager = sceneManager;
		this.neonBatch = neonBatch;
		this.renderSystem = renderSystem;

		view.setPresenter(this);

		// Init Camera
		camera = new OrthographicCamera(1280, 720);
		camera.zoom = 1.0f;

		// Init Gizmo
		gizmoSystem = new EditorGizmoSystem(sceneManager);

		// Init Tools
		shapeRenderer = new ShapeRenderer();

		// Init Input
		cameraController = new SimpleCameraController(camera);
		cameraController.setCoordinateMapper((sx, sy) -> view.screenToWorld(sx, sy, camera));

		// [核心修复] 配置看门狗
		// 只有当 ScenePanel (View) 处于 hover 状态 (鼠标在上面) 时，相机才响应
		// [最终修复]
		cameraController.setActivationCondition(view::isMouseOver);

		inputProcessor = new SceneInputProcessor();
	}

	public void update(float delta) {
		camera.update();
		// 渲染到 FBO
		view.getRenderTarget().renderToFbo(this::renderSceneContent);
	}

	private void renderSceneContent() {
		Gdx.gl.glViewport(0, 0, view.getRenderTarget().getFboWidth(), view.getRenderTarget().getFboHeight());
		Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 1. Grid
		drawGrid();

		// 2. Entities
		GameWorld.inst().render(neonBatch, camera);

		// 3. Gizmos & Selection
		neonBatch.setProjectionMatrix(camera.combined);
		neonBatch.begin();

		GObject sel = sceneManager.getSelection();
		if(sel != null && !sel.isDestroyed()) {
			float x = sel.transform.worldPosition.x;
			float y = sel.transform.worldPosition.y;
			// 绘制黄色选中框
			neonBatch.drawRect(x-25, y-25, 50, 50, sel.transform.worldRotation, 2, Color.YELLOW, false);
			// 绘制 Gizmo
			gizmoSystem.render(neonBatch, camera.zoom);
		}
		neonBatch.end();
	}

	private void drawGrid() {
		shapeRenderer.setProjectionMatrix(camera.combined);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(1, 1, 1, 0.1f);
		float s = 2000;
		shapeRenderer.line(-s, 0, s, 0);
		shapeRenderer.line(0, -s, 0, s);
		// 画一些辅助线
		for(float i = -1000; i<=1000; i+=200) {
			shapeRenderer.line(i, -1000, i, 1000);
			shapeRenderer.line(-1000, i, 1000, i);
		}
		shapeRenderer.end();
	}

	// --- Actions ---

	public void setGizmoMode(EditorGizmoSystem.Mode mode) {
		gizmoSystem.mode = mode;
		view.updateGizmoModeUI(mode.ordinal());
	}

	public void saveScene() {
		try {
			Json json = GdxJsonSetup.create();
			List<GObject> roots = GameWorld.inst().getRootEntities();
			String text = json.prettyPrint(roots);

			FileHandle file = com.goldsprite.gdengine.core.project.ProjectService.inst().getCurrentProject().child("scenes/main.scene");
			file.writeString(text, false);

			Debug.logT("Editor", "Scene saved: " + file.path());
			ToastUI.inst().show("Saved!");
		} catch (Exception e) {
			ToastUI.inst().show("Save Failed: " + e.getMessage());
		}
	}

	public void loadScene() {
		// 原有的默认加载保留，或者让它调用带参版本
		FileHandle file = com.goldsprite.gdengine.core.project.ProjectService.inst().getCurrentProject().child("scenes/main.scene");
		loadScene(file);
	}

	// [新增] 加载指定场景文件
	public void loadScene(FileHandle file) {
		if(file != null && file.exists()) {
			SceneLoader.load(file);
			EditorEvents.inst().emitStructureChanged();
			EditorEvents.inst().emitSceneLoaded();
			sceneManager.select(null);

			// 提示一下
			ToastUI.inst().show("Scene Loaded: " + file.nameWithoutExtension());
		}
	}

	/** 供外部注册输入管线 */
	public void registerInput(InputMultiplexer multiplexer) {
		multiplexer.addProcessor(inputProcessor);
		multiplexer.addProcessor(cameraController);
	}

	// =========================================================
	// Inner Class: Input Logic (Migrated from NativeEditorInput)
	// =========================================================

	private class SceneInputProcessor extends InputAdapter {
		private enum DragMode { NONE, BODY, MOVE_X, MOVE_Y, ROTATE, SCALE_X, SCALE_Y, SCALE }
		private DragMode currentDragMode = DragMode.NONE;
		private float lastX, lastY;
		private final Vector2 startScale = new Vector2();
		private final Vector2 startDragPos = new Vector2();

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			// [Fix] Gizmo 操作只响应单指 (pointer == 0)
			// 同时忽略右键和中键
			if (pointer > 0 || button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) return false;
			if (!view.isMouseOver()) return false;

			if (button == Input.Buttons.LEFT) {
				Vector2 wPos = view.screenToWorld(screenX, screenY, camera);
				GObject sel = sceneManager.getSelection();

				// 1. Gizmo Detection
				if (sel != null && !sel.isDestroyed()) {
					DragMode gizmoHit = hitTestGizmo(sel, wPos);
					if (gizmoHit != DragMode.NONE) {
						startDrag(gizmoHit, wPos, sel);
						return true; // Consume
					}
				}

				// 2. Object Picking
				GObject hit = hitTestGObject(wPos);
				if (hit != null) {
					if (hit != sel) {
						sceneManager.select(hit);
						EditorEvents.inst().emitSelectionChanged(hit);
					}
					startDrag(DragMode.BODY, wPos, hit);
					return true;
				}

				// 3. Deselect
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

			// Map DragMode to Gizmo Handle ID for visual feedback
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
			// [Fix] 严格限制拖拽只响应主指针 (pointer 0)
			if (pointer > 0) return false;
			if (currentDragMode == DragMode.NONE || sceneManager.getSelection() == null) return false;

			Vector2 wPos = view.screenToWorld(screenX, screenY, camera);
			float dx = wPos.x - lastX;
			float dy = wPos.y - lastY;

			GObject t = sceneManager.getSelection();
			if(t != null && !t.isDestroyed()) {
				applyTransform(t, dx, dy, wPos);
				// 这里不需要每帧 emitProperty，因为 Inspector act 里面有轮询刷新
			}

			lastX = wPos.x; lastY = wPos.y;
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			// [Fix] 严格限制抬起只响应主指针 (pointer 0)
			// 否则如果多指同时操作，第二根手指抬起可能会意外中断主指的拖拽
			if (pointer > 0) return false;
			if (currentDragMode != DragMode.NONE) {
				currentDragMode = DragMode.NONE;
				gizmoSystem.activeHandle = EditorGizmoSystem.HANDLE_NONE;
				EditorEvents.inst().emitPropertyChanged(); // 拖拽结束，通知刷新
				return true;
			}
			return false;
		}

		// --- Logic Helpers (Copied from old EditorController) ---

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
			float zoom = camera.zoom * 1.4f;
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
			// [Fix] 调用 RenderSystem 的无副作用查询接口
			List<RenderComponent> candidates = renderSystem.queryRenderables();

			// 倒序遍历 (从最上层开始检测)
			for (int i = candidates.size() - 1; i >= 0; i--) {
				RenderComponent rc = candidates.get(i);
				if (rc.contains(p.x, p.y)) {
					return rc.getGObject();
				}
			}
			return null;
		}
	}
}
