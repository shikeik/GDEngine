package com.goldsprite.gdengine.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.function.BooleanSupplier;

public class SimpleCameraController implements InputProcessor {

	private final OrthographicCamera camera;
	private final GestureDetector gestureDetector;
	private final InputAdapter mouseInputAdapter;
	private boolean inputEnabled = true;

	public static float minZoom = 0.001f;
	public static float maxZoom = 100.0f;

	// [新增] 坐标映射策略接口
	public interface CoordinateMapper {
		Vector2 map(float screenX, float screenY);
	}

	// 默认策略: 标准全屏 Unproject
	private CoordinateMapper mapper;

	// [新增] 激活条件
	private BooleanSupplier activationCondition;

	public SimpleCameraController(OrthographicCamera camera) {
		this.camera = camera;

		// 默认实现
		this.mapper = (x, y) -> {
			Vector3 v = camera.unproject(new Vector3(x, y, 0));
			return new Vector2(v.x, v.y);
		};

		GestureDetector.GestureListener listener = new CameraGestureListener();
		this.gestureDetector = new GestureDetector(20, 0.4f, 1.1f, 0.15f, listener);
		this.mouseInputAdapter = new CameraMouseListener();
	}

	public void setCoordinateMapper(CoordinateMapper mapper) {
		this.mapper = mapper;
	}

	// [新增] 设置激活条件
	public void setActivationCondition(BooleanSupplier condition) {
		this.activationCondition = condition;
	}

	// [新增] 内部检查
	private boolean shouldIgnore() {
		// 如果 inputEnabled 为 false，或者设置了条件但条件不满足，则忽略
		return !inputEnabled || (activationCondition != null && !activationCondition.getAsBoolean());
	}

	// --- 代理 InputProcessor ---
	@Override public boolean touchDown(int x, int y, int pointer, int button) {
		// [修改] 增加检查
		if (shouldIgnore()) return false;

		// 优先处理鼠标逻辑 (PC右键)
		if (mouseInputAdapter.touchDown(x, y, pointer, button)) return true;
		// 其次处理手势 (Android双指/单指拖拽)
		return gestureDetector.touchDown(x, y, pointer, button);
	}
	@Override public boolean touchUp(int x, int y, int pointer, int button) {
		mouseInputAdapter.touchUp(x, y, pointer, button);
		return gestureDetector.touchUp(x, y, pointer, button);
	}
	@Override public boolean touchDragged(int x, int y, int pointer) {
		// 拖拽过程中通常不需要检查 condition (因为 touchDown 已经检查过了)
		// 但为了安全，如果 inputEnabled 被强关，还是断开比较好
		if (shouldIgnore()) return false;
		if (mouseInputAdapter.touchDragged(x, y, pointer)) return true;
		return gestureDetector.touchDragged(x, y, pointer);
	}
	@Override public boolean scrolled(float amountX, float amountY) {
		// [修改] 增加检查 (解决滚轮串味的核心)
		if (shouldIgnore()) return false;
		return mouseInputAdapter.scrolled(amountX, amountY);
	}
	// ... 其他方法直接返回 false 或调用 super ...
	@Override public boolean keyDown(int keycode) { return false; }
	@Override public boolean keyUp(int keycode) { return false; }
	@Override public boolean keyTyped(char character) { return false; }
	@Override public boolean mouseMoved(int screenX, int screenY) { return false; }
	@Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }

	// --- 手势逻辑 (Mobile / Touch) ---
	private class CameraGestureListener extends GestureDetector.GestureAdapter {
		private float initialScale = 1f;

		@Override
		public boolean pan(float x, float y, float deltaX, float deltaY) {
			// Android 单指平移 (如果需要的话，或者交给 MouseListener 的 touchDragged 处理)
			// 这里为了跟手，我们也应该使用 mapper。
			// 但 GestureDetector 的 pan delta 是屏幕像素。
			// 简单实现：将屏幕 delta 转为世界 delta
			// 世界增量 = 屏幕增量 * (世界宽 / 屏幕宽)
			// 或者利用两个点：
			Vector2 p1 = mapper.map(x, y);
			Vector2 p2 = mapper.map(x - deltaX, y - deltaY);
			camera.translate(p2.x - p1.x, p2.y - p1.y);
			return true;
		}

		@Override
		public boolean zoom(float initialDistance, float distance) {
			// 双指缩放
			float ratio = initialDistance / distance;
			float newZoom = MathUtils.clamp(initialScale * ratio, minZoom, maxZoom);
			camera.zoom = newZoom;
			return true;
		}

		@Override
		public boolean touchDown(float x, float y, int pointer, int button) {
			initialScale = camera.zoom;
			return false;
		}
	}

	// --- 鼠标逻辑 (PC / Mouse) ---
	private class CameraMouseListener extends InputAdapter {
		private Vector2 lastWorldPos = new Vector2();
		private boolean isPanning = false;

		@Override
		public boolean touchDown(int x, int y, int pointer, int button) {
			// 允许 右键 或 中键 拖拽
			if (button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
				isPanning = true;
				// 记录按下时的世界坐标锚点
				lastWorldPos.set(mapper.map(x, y));
				return true;
			}
			return false;
		}

		@Override
		public boolean touchUp(int x, int y, int pointer, int button) {
			if (button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
				isPanning = false;
				return true;
			}
			return false;
		}

		@Override
		public boolean touchDragged(int x, int y, int pointer) {
			if (isPanning) {
				// [核心跟手逻辑]
				// 1. 获取当前鼠标位置对应的"新"世界坐标 (假设相机没动)
				Vector2 currWorldPos = mapper.map(x, y);

				// 2. 计算差异：鼠标当前指着的点，和上一帧指着的点，在世界空间差了多少
				// 实际上我们希望鼠标底下的世界点不动。
				// 所以相机需要移动，抵消这个差值。
				float dx = lastWorldPos.x - currWorldPos.x;
				float dy = lastWorldPos.y - currWorldPos.y;

				camera.translate(dx, dy);
				camera.update();

				// 注意：相机移动后，mapper.map(x,y) 的结果会变，
				// 但因为我们是把"上一帧的世界点"对齐到"当前鼠标位置"，
				// 所以下一帧比较的基准(lastWorldPos)依然应该是那个固定的世界点吗？
				// 不，最简单的做法是每帧重置锚点：
				// 移动完相机后，重新采样当前的鼠标世界位置作为下一帧的基准。
				lastWorldPos.set(mapper.map(x, y));
				return true;
			}
			return false;
		}

		@Override
		public boolean scrolled(float amountX, float amountY) {
			// [锚点缩放]
			// 1. 获取缩放中心 (鼠标位置)
			float mouseX = Gdx.input.getX();
			float mouseY = Gdx.input.getY();
			Vector2 anchor = mapper.map(mouseX, mouseY);

			// 2. 执行缩放
			float zoomFactor = 0.1f;
			float targetZoom = camera.zoom + amountY * zoomFactor * camera.zoom;
			camera.zoom = MathUtils.clamp(targetZoom, minZoom, maxZoom);
			camera.update();

			// 3. 补偿位移：缩放后，鼠标指向的新世界坐标变了，要把相机移回去
			Vector2 newAnchor = mapper.map(mouseX, mouseY);
			camera.translate(anchor.x - newAnchor.x, anchor.y - newAnchor.y);
			camera.update();

			return true;
		}
	}
}
