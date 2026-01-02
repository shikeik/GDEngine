package com.goldsprite.solofight.screens.editor.tests;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * EditorScene2D
 *
 * 单文件、可运行、可扩展的 2D Scene View 编辑器骨架
 */
public class EditorScene2D extends ApplicationAdapter {

	private SceneStage stage;
	private SpriteBatch batch;

	@Override
	public void create() {
		batch = new SpriteBatch();
		stage = new SceneStage();

		Gdx.input.setInputProcessor(stage);

		stage.build();
	}

	@Override
	public void render() {
		ScreenUtils.clear(0.15f, 0.15f, 0.18f, 1f);

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	@Override
	public void dispose() {
		stage.dispose();
		batch.dispose();
	}

	// =========================================================
	// Scene2D UI 层
	// =========================================================

	private class SceneStage extends Stage {

		private SceneViewWidget sceneView;

		SceneStage() {
			super(new ScreenViewport(), batch);
		}

		void build() {
			Table root = new Table();
			root.setFillParent(true);
			addActor(root);

			sceneView = new SceneViewWidget();
			root.add(sceneView).grow();
		}

		@Override
		public void dispose() {
			super.dispose();
			sceneView.dispose();
		}
	}

	/**
	 * SceneView 在 UI 中的“壳”
	 * 只负责尺寸、输入转发、贴图显示
	 */
	private class SceneViewWidget extends Widget {

		private final SceneView sceneView;

		SceneViewWidget() {
			sceneView = new SceneView();
			addListener(new EditorInputRouter(sceneView));
		}

		@Override
		protected void sizeChanged() {
			sceneView.resize((int) getWidth(), (int) getHeight());
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			sceneView.render();

			TextureRegion region = sceneView.getOutput();
			batch.draw(region, getX(), getY(), getWidth(), getHeight());
		}

		void dispose() {
			sceneView.dispose();
		}
	}

	// =========================================================
	// Scene View 核心（FBO + Camera）
	// =========================================================

	private static class SceneView {

		private FrameBuffer fbo;
		private OrthographicCamera camera;

		private SceneCamera cameraController;
		private SceneRenderer renderer;
		private GizmoLayer gizmoLayer;
		private PickingContext picking;

		private TextureRegion output;

		SceneView() {
			camera = new OrthographicCamera();
			cameraController = new SceneCamera(camera);
			renderer = new SceneRenderer();
			gizmoLayer = new GizmoLayer();
			picking = new PickingContext(camera);
		}

		void resize(int width, int height) {
			if (fbo != null) fbo.dispose();

			fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
			output = new TextureRegion(fbo.getColorBufferTexture());
			output.flip(false, true);

			camera.setToOrtho(false, width, height);
			camera.update();
		}

		void render() {
			fbo.begin();
			Gdx.gl.glClearColor(0.12f, 0.12f, 0.14f, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			renderer.render(camera);
			gizmoLayer.render(camera);

			fbo.end();
		}

		TextureRegion getOutput() {
			return output;
		}

		PickingContext picking() {
			return picking;
		}

		SceneCamera cameraController() {
			return cameraController;
		}

		void dispose() {
			if (fbo != null) fbo.dispose();
		}
	}

	// =========================================================
	// 编辑器相机（OCP：可替换）
	// =========================================================

	private static class SceneCamera {

		private final OrthographicCamera camera;

		SceneCamera(OrthographicCamera camera) {
			this.camera = camera;
		}

		void pan(float dx, float dy) {
			camera.position.add(-dx * camera.zoom, dy * camera.zoom, 0);
			camera.update();
		}

		void zoom(float amount, Vector2 pivotWorld) {
			float oldZoom = camera.zoom;
			camera.zoom = MathUtils.clamp(camera.zoom * (1f + amount), 0.1f, 10f);
			camera.update();

			Vector2 after = worldFromScreen(pivotWorld);
			Vector2 delta = after.sub(pivotWorld);
			camera.position.sub(delta.x, delta.y, 0);
			camera.update();
		}

		Vector2 worldFromScreen(Vector2 screen) {
			Vector3 v = new Vector3(screen.x, screen.y, 0);
			camera.unproject(v);
			return new Vector2(v.x, v.y);
		}
	}

	// =========================================================
	// 世界渲染（扩展点）
	// =========================================================

	private static class SceneRenderer {

		private final ShapeRenderer shapes = new ShapeRenderer();

		void render(OrthographicCamera camera) {
			shapes.setProjectionMatrix(camera.combined);
			shapes.begin(ShapeRenderer.ShapeType.Line);

			// 示例：世界原点
			shapes.setColor(Color.RED);
			shapes.line(-20, 0, 20, 0);
			shapes.line(0, -20, 0, 20);

			shapes.end();
		}
	}

	// =========================================================
	// Gizmo 层（编辑器专用）
	// =========================================================

	private static class GizmoLayer {

		private final ShapeRenderer shapes = new ShapeRenderer();

		void render(OrthographicCamera camera) {
			shapes.setProjectionMatrix(camera.combined);
			shapes.begin(ShapeRenderer.ShapeType.Line);
			shapes.setColor(Color.GRAY);

			// 无限网格（简化版）
			for (int i = -500; i <= 500; i += 50) {
				shapes.line(i, -500, i, 500);
				shapes.line(-500, i, 500, i);
			}

			shapes.end();
		}
	}

	// =========================================================
	// Picking / 坐标映射
	// =========================================================

	private static class PickingContext {

		private final OrthographicCamera camera;

		PickingContext(OrthographicCamera camera) {
			this.camera = camera;
		}

		Vector2 screenToWorld(float x, float y) {
			Vector3 v = new Vector3(x, y, 0);
			camera.unproject(v);
			return new Vector2(v.x, v.y);
		}
	}

	// =========================================================
	// 输入路由（UI ⇄ Scene 解耦）
	// =========================================================

	private static class EditorInputRouter extends InputListener {

		private final SceneView sceneView;
		private float lastX, lastY;

		EditorInputRouter(SceneView sceneView) {
			this.sceneView = sceneView;
		}

		@Override
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			lastX = x;
			lastY = y;
			return true;
		}

		@Override
		public void touchDragged(InputEvent event, float x, float y, int pointer) {
			float dx = x - lastX;
			float dy = y - lastY;

			sceneView.cameraController().pan(dx, dy);

			lastX = x;
			lastY = y;
		}

		@Override
		public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
			Vector2 world = sceneView.picking().screenToWorld(x, y);
			sceneView.cameraController().zoom(amountY * 0.1f, world);
			return true;
		}
	}
}
