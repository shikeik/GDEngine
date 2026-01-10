package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.TransformCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

public class EditorInput extends InputAdapter {
	private final OrthographicCamera camera;
	private final SceneManager sceneManager;
	private final GizmoSystem gizmoSystem;
	private final CommandManager commandManager;
	private ViewWidget viewWidget;

	// 拖拽模式
	private enum DragMode { NONE, BODY, MOVE_X, MOVE_Y, ROTATE, SCALE_X, SCALE_Y }
	private DragMode currentDragMode = DragMode.NONE;

	private Vector2 lastTouchPos = new Vector2();
	private Vector3 tempVec3 = new Vector3();
	
	// Undo 记录初始状态
	private float undoStartX, undoStartY, undoStartRot, undoStartSX, undoStartSY;

	private boolean wPressed, aPressed, sPressed, dPressed;
	private final Vector2 keyboardDirection = new Vector2();

	public EditorInput(OrthographicCamera camera, SceneManager sceneManager, GizmoSystem gizmoSystem, CommandManager commandManager) {
		this.camera = camera;
		this.sceneManager = sceneManager;
		this.gizmoSystem = gizmoSystem;
		this.commandManager = commandManager;
	}

	public void setViewWidget(ViewWidget widget) {
		this.viewWidget = widget;
	}

	// 获取当前鼠标对应的世界坐标
	private Vector2 getWorldPos(int screenX, int screenY) {
		if (viewWidget != null) {
			return viewWidget.screenToWorld(screenX, screenY, camera);
		}
		// Fallback
		Vector3 v = camera.unproject(tempVec3.set(screenX, screenY, 0));
		return new Vector2(v.x, v.y);
	}

	private void startDrag(DragMode mode, Vector2 pos) {
		currentDragMode = mode;
		lastTouchPos.set(pos);
		
		EditorTarget t = sceneManager.getSelection();
		if (t != null) {
			undoStartX = t.getX();
			undoStartY = t.getY();
			undoStartRot = t.getRotation();
			undoStartSX = t.getScaleX();
			undoStartSY = t.getScaleY();
		}
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (button != 0) return false;

		Vector2 worldPos = getWorldPos(screenX, screenY);
		EditorTarget selection = sceneManager.getSelection();

		// 1. 优先检测 Gizmo 手柄 (如果已选中)
		if (selection != null) {
			DragMode gizmoHit = hitTestGizmo(selection, worldPos);
			if (gizmoHit != DragMode.NONE) {
				startDrag(gizmoHit, worldPos);
				return true; // [关键] 拦截事件，防止相机拖动
			}
		}

		// 2. 检测物体本体
		EditorTarget hitTarget = hitTestBody(worldPos.x, worldPos.y);
		if (hitTarget != null) {
			sceneManager.selectNode(hitTarget);
			startDrag(DragMode.BODY, worldPos);
			return true; // [关键] 拦截事件
		}

		//// 3. 点击空白处 -> 取消选择，并且不拦截事件 (让 Stage 处理相机拖动)
		//TODO: 暂时注释，因为判定需要改为仅Scene视图内
		//sceneManager.selectNode(null);
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if (currentDragMode == DragMode.NONE) return false;

		Vector2 worldPos = getWorldPos(screenX, screenY);
		float dx = worldPos.x - lastTouchPos.x;
		float dy = worldPos.y - lastTouchPos.y;

		EditorTarget t = sceneManager.getSelection();
		if (t != null) {
			// [核心] 根据模式应用变换
			applyTransform(t, dx, dy, worldPos);
		}

		lastTouchPos.set(worldPos);
		return true;
	}

	private void applyTransform(EditorTarget t, float dx, float dy, Vector2 currPos) {
		float rad = t.getRotation() * MathUtils.degreesToRadians;
		float c = MathUtils.cos(rad);
		float s = MathUtils.sin(rad);

		switch (currentDragMode) {
			case BODY:
				t.setX(t.getX() + dx);
				t.setY(t.getY() + dy);
				break;
			case MOVE_X:
				// 投影到局部X轴
				float projX = dx * c + dy * s;
				t.setX(t.getX() + projX * c);
				t.setY(t.getY() + projX * s);
				break;
			case MOVE_Y:
				// 投影到局部Y轴
				float projY = dx * (-s) + dy * c;
				t.setX(t.getX() - projY * s);
				t.setY(t.getY() + projY * c);
				break;
			case ROTATE:
				// 计算角度差
				Vector2 center = new Vector2(t.getX(), t.getY());
				Vector2 prevDir = lastTouchPos.cpy().sub(center);
				Vector2 currDir = currPos.cpy().sub(center);
				float angleDelta = currDir.angleDeg() - prevDir.angleDeg();
				t.setRotation(t.getRotation() + angleDelta);
				break;
			case SCALE_X: // 简化处理，均匀缩放
			case SCALE_Y:
				Vector2 center2 = new Vector2(t.getX(), t.getY());
				float oldDist = lastTouchPos.dst(center2);
				float newDist = currPos.dst(center2);
				if (oldDist > 0.1f) {
					float ratio = newDist / oldDist;
					t.setScaleX(t.getScaleX() * ratio);
					t.setScaleY(t.getScaleY() * ratio);
				}
				break;
		}
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (currentDragMode != DragMode.NONE) {
			currentDragMode = DragMode.NONE;
			
			// 提交 Undo 命令
			EditorTarget sel = sceneManager.getSelection();
			if (sel != null) {
				boolean changed = sel.getX() != undoStartX || sel.getY() != undoStartY ||
								  sel.getRotation() != undoStartRot ||
								  sel.getScaleX() != undoStartSX || sel.getScaleY() != undoStartSY;

				if (changed) {
					commandManager.execute(new TransformCommand(sel,
						undoStartX, undoStartY, undoStartRot, undoStartSX, undoStartSY,
						null // 不需要 refreshUI 回调，因为我们在 render loop 中实时绘制
					));
				}
			}
			return true;
		}
		return false;
	}

	// --- 辅助检测 ---

	private DragMode hitTestGizmo(EditorTarget t, Vector2 pos) {
		float zoom = camera.zoom * 1.4f;
		float axisLen = GizmoSystem.AXIS_LEN * zoom;
		float hitR = 15f * zoom;
		float tx = t.getX(), ty = t.getY();
		float rad = t.getRotation() * MathUtils.degreesToRadians;
		float c = MathUtils.cos(rad), s = MathUtils.sin(rad);

		GizmoSystem.Mode mode = gizmoSystem.mode;

		if (mode == GizmoSystem.Mode.MOVE) {
			// Check X Axis Tip
			float xx = tx + c * axisLen;
			float xy = ty + s * axisLen;
			if (pos.dst(xx, xy) < hitR) return DragMode.MOVE_X;

			// Check Y Axis Tip
			float yx = tx - s * axisLen;
			float yy = ty + c * axisLen;
			if (pos.dst(yx, yy) < hitR) return DragMode.MOVE_Y;
		}
		else if (mode == GizmoSystem.Mode.ROTATE) {
			float hx = tx + c * axisLen;
			float hy = ty + s * axisLen;
			if (pos.dst(hx, hy) < hitR) return DragMode.ROTATE;
		}
		else if (mode == GizmoSystem.Mode.SCALE) {
			float xx = tx + c * axisLen;
			float xy = ty + s * axisLen;
			if (pos.dst(xx, xy) < hitR) return DragMode.SCALE_X; // 暂时统称
		}

		return DragMode.NONE;
	}

	public EditorTarget hitTestBody(float worldX, float worldY) {
		EditorTarget root = sceneManager.getRoot();
		if (root != null) return hitTestRecursive(root, worldX, worldY);
		return null;
	}

	private EditorTarget hitTestRecursive(EditorTarget parent, float worldX, float worldY) {
		for (int i = parent.getChildren().size - 1; i >= 0; i--) {
			EditorTarget child = parent.getChildren().get(i);
			EditorTarget hit = hitTestRecursive(child, worldX, worldY);
			if (hit != null) return hit;
		}
		if (parent.hitTest(worldX, worldY)) return parent;
		return null;
	}

	// ... WASD Logic (Keep existing) ...
	@Override public boolean keyDown(int keycode) {
		boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
		boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

		if (ctrl) {
			if (keycode == Input.Keys.Z) {
				if (shift) commandManager.redo();
				else commandManager.undo();
				return true;
			} else if (keycode == Input.Keys.Y) {
				commandManager.redo();
				return true;
			}
		}

		switch(keycode) {
			case Input.Keys.W: 
				// 区分 WASD 移动和 Mode 切换
				if (!ctrl) {
					gizmoSystem.mode = GizmoSystem.Mode.MOVE;
				}
				// 同时触发漫游
				wPressed = true; updateDir();
				return true;
			case Input.Keys.E: gizmoSystem.mode = GizmoSystem.Mode.ROTATE; return true;
			case Input.Keys.R: gizmoSystem.mode = GizmoSystem.Mode.SCALE; return true;
			
			// WASD 漫游逻辑 (如果 W 被占用，可能需要其他键，或者共存)
			// 这里我们让 W 既切换工具也触发漫游 (虽然体验一般，但满足“还原快捷键”需求)
			// 或者，我们可以检测是否选中了物体。如果选中了物体，W 是切换工具？不，通常是全局的。
			// 让我们看看原版代码：原版只用 W/E/R 切换。
			// 现版有 WASD 控制 Player。
			// 决定：保留 W/E/R 快捷键。WASD 控制 Player 逻辑保留 (W 会同时触发)。
			
			// case Input.Keys.W: wPressed = true; updateDir(); return true; // W handled above
			case Input.Keys.S: sPressed = true; updateDir(); return true;
			case Input.Keys.A: aPressed = true; updateDir(); return true;
			case Input.Keys.D: dPressed = true; updateDir(); return true;
			
			case Input.Keys.G:
				gizmoSystem.mode = (gizmoSystem.mode == GizmoSystem.Mode.MOVE) ? GizmoSystem.Mode.ROTATE :
					(gizmoSystem.mode == GizmoSystem.Mode.ROTATE ? GizmoSystem.Mode.SCALE : GizmoSystem.Mode.MOVE);
				return true;
		}
		
		return false;
	}
	@Override public boolean keyUp(int keycode) {
		switch(keycode) {
			case Input.Keys.W: wPressed = false; updateDir(); return true;
			case Input.Keys.S: sPressed = false; updateDir(); return true;
			case Input.Keys.A: aPressed = false; updateDir(); return true;
			case Input.Keys.D: dPressed = false; updateDir(); return true;
		}
		return false;
	}
	private void updateDir() {
		keyboardDirection.set(0,0);
		if(wPressed) keyboardDirection.y++; if(sPressed) keyboardDirection.y--;
		if(dPressed) keyboardDirection.x++; if(aPressed) keyboardDirection.x--;
		if(keyboardDirection.len()>0) keyboardDirection.nor();
	}
	public Vector2 getKeyboardDirection() { return keyboardDirection.cpy(); }
}
