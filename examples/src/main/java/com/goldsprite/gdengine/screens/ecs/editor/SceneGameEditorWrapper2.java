package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.viewport.*;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisWindow;

// ==========================================
// 1. 入口包装类 (Wrapper) - 保持不变，作为 Screen 的桥梁
// ==========================================
public class SceneGameEditorWrapper2 extends GScreen {
	private DualViewEditor editorLogic;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		// 所有的脏活累活都交给 EditorLogic，Wrapper 保持清爽
		editorLogic = new DualViewEditor();
		editorLogic.create();
	}

	@Override
	public void render(float delta) {
		editorLogic.render(delta);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		editorLogic.resize(width, height);
	}

	@Override
	public void dispose() {
		editorLogic.dispose();
	}
}

// ==========================================
// 2. 核心逻辑控制器 (Controller)
// ==========================================
class DualViewEditor {
	private Stage stage;
	private SpriteBatch worldBatch;

	// 资源 (实际项目中应由 AssetManager 管理)
	private Texture playerTexture;
	private Texture bgTexture;

	// 游戏状态 (数据层)
	private float playerX = 0, playerY = 0;

	// UI 引用
	private Touchpad joystick;

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		worldBatch = new SpriteBatch();
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		loadAssets();

		// 构建 UI
		createGameViewWindow();
		createSceneViewWindow();
	}

	private void loadAssets() {
		// 简单加载，实际请用 AssetManager
		playerTexture = new Texture(Gdx.files.internal("role.png"));
		bgTexture = new Texture(Gdx.files.internal("role.png"));
	}

	// --- 构建 Game 视图窗口 (含 UI 覆盖层) ---
	private void createGameViewWindow() {
		VisWindow window = new VisWindow("Game View");
		window.setResizable(true);
		window.setSize(500, 400);
		window.setPosition(50, 50);

		// A. 渲染层
		final GameViewportWidget renderWidget = new GameViewportWidget(worldBatch, 480, 320);
		// 设置渲染回调：这里定义"画什么"
		renderWidget.setRenderer((batch, camera) -> {
			// Game 视图逻辑：相机跟随玩家
			camera.position.set(playerX, playerY, 0);
			camera.update();
			batch.setProjectionMatrix(camera.combined);

			drawWorld(batch);
		});

		// B. UI 控制层 (摇杆 + 下拉框)
		Table uiLayer = createGameOverlayUI(renderWidget);

		// C. 组合
		Stack stack = new Stack();
		stack.add(renderWidget);
		stack.add(uiLayer);

		window.add(stack).grow();
		stage.addActor(window);
	}

	// --- 构建 Scene 视图窗口 (含相机控制) ---
	private void createSceneViewWindow() {
		VisWindow window = new VisWindow("Scene View (Independent)");
		window.setResizable(true);
		window.setSize(400, 300);
		window.setPosition(600, 50);

		// A. 渲染层
		final GameViewportWidget renderWidget = new GameViewportWidget(worldBatch, 480, 320);
		renderWidget.setViewportType(GameViewportWidget.ViewportType.EXTEND); // 编辑器默认看更多

		renderWidget.setRenderer((batch, camera) -> {
			// Scene 视图逻辑：相机位置由输入控制，不跟随
			// 仅仅只需应用矩阵，位置更新交给 Listener
			// 注意：这里不用 setProjectionMatrix，因为 Widget 内部可能已经设好了，
			// 但为了保险起见，或者如果你的 renderWorld 里有其他的 matrix 操作，可以再设一次。
			// 在我们的 Widget 实现里，draw() 方法已经 set 过了。
			drawWorld(batch);
		});

		// B. 添加编辑器相机控制 (拖拽平移 + 滚轮缩放)
		// 这一步体现了“组合优于继承”，给 Widget 挂载能力
		SceneCameraInput inputController = new SceneCameraInput(renderWidget);
		renderWidget.addListener(inputController.getDragListener());
		renderWidget.addListener(inputController.getScrollListener());

		window.add(renderWidget).grow();
		stage.addActor(window);
	}

	// 抽离出来的绘制方法，复用于两个视图
	private void drawWorld(Batch batch) {
		batch.draw(bgTexture, -500, -500, 1000, 1000);
		batch.draw(playerTexture, playerX, playerY);
	}

	// 抽离 UI 布局代码
	private Table createGameOverlayUI(final GameViewportWidget targetWidget) {
		Table uiLayer = new Table();
		uiLayer.setFillParent(true);

		// 1. 摇杆
		Touchpad.TouchpadStyle style = VisUI.getSkin().get(Touchpad.TouchpadStyle.class);
		joystick = new Touchpad(10, style);

		// 2. 视口选择器
		final VisSelectBox<String> viewSelector = new VisSelectBox<>();
		viewSelector.setItems("Extend", "Fit", "Stretch");
		viewSelector.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String selected = viewSelector.getSelected();
				if ("Fit".equals(selected)) targetWidget.setViewportType(GameViewportWidget.ViewportType.FIT);
				else if ("Stretch".equals(selected)) targetWidget.setViewportType(GameViewportWidget.ViewportType.STRETCH);
				else targetWidget.setViewportType(GameViewportWidget.ViewportType.EXTEND);
			}
		});

		// 布局
		uiLayer.top().right().add(viewSelector).pad(5).width(100);
		uiLayer.row();
		uiLayer.add().expand().fill(); // 占位
		uiLayer.row();
		uiLayer.left().bottom().add(joystick).pad(20).size(100);

		return uiLayer;
	}

	public void render(float delta) {
		updateLogic(delta);

		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	private void updateLogic(float delta) {
		float speed = 200 * delta;
		playerX += joystick.getKnobPercentX() * speed;
		playerY += joystick.getKnobPercentY() * speed;
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	public void dispose() {
		VisUI.dispose();
		worldBatch.dispose();
		playerTexture.dispose();
		bgTexture.dispose();
		stage.dispose();
	}
}

// ==========================================
// 3. 通用渲染接口 (Functional Interface)
// ==========================================
interface WorldRenderer {
	void render(Batch batch, Camera camera);
}

// ==========================================
// 4. 高度封装的视口控件 (Widget)
// ==========================================
class GameViewportWidget extends Widget {
	private final SpriteBatch batch;
	private final float worldWidth, worldHeight;
	private Viewport viewport;
	private WorldRenderer renderer;
	private final Vector2 tempCoords = new Vector2();

	public enum ViewportType { EXTEND, FIT, STRETCH }

	public GameViewportWidget(SpriteBatch batch, float worldWidth, float worldHeight) {
		this.batch = batch;
		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;
		setViewportType(ViewportType.EXTEND); // 默认
	}

	public void setRenderer(WorldRenderer renderer) {
		this.renderer = renderer;
	}

	public void setViewportType(ViewportType type) {
		Camera oldCam = (viewport != null) ? viewport.getCamera() : null;

		// 修正点：变量名统一改为 newCamera
		Camera newCamera = new OrthographicCamera();

		if (oldCam != null) {
			newCamera.position.set(oldCam.position);
			newCamera.update();
			if(oldCam instanceof OrthographicCamera) {
				((OrthographicCamera)newCamera).zoom = ((OrthographicCamera)oldCam).zoom;
			}
		}

		switch (type) {
			// 现在这里就能正确识别 newCamera 了
			case FIT: viewport = new FitViewport(worldWidth, worldHeight, newCamera); break;
			case STRETCH: viewport = new StretchViewport(worldWidth, worldHeight, newCamera); break;
			case EXTEND: default: viewport = new ExtendViewport(worldWidth, worldHeight, newCamera); break;
		}
	}

	public Camera getCamera() {
		return viewport.getCamera();
	}

	@Override
	public void draw(Batch uiBatch, float parentAlpha) {
		validate();

		// 1. 获取屏幕坐标
		tempCoords.set(0, 0);
		localToStageCoordinates(tempCoords);
		int x = (int) tempCoords.x;
		int y = (int) tempCoords.y;
		int w = (int) getWidth();
		int h = (int) getHeight();

		if (w <= 0 || h <= 0) return;

		uiBatch.end();

		// 2. 裁剪区域 (防止画出界)
		Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
		HdpiUtils.glScissor(x, y, w, h);

		// 3. 清理背景 (Widget 内部的背景)
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 4. 计算并应用视口 (核心修正逻辑)
		viewport.update(w, h, false); // 计算尺寸

		// 叠加偏移量：Widget在屏幕的位置 + Viewport计算出的内部黑边偏移
		int finalX = x + viewport.getScreenX();
		int finalY = y + viewport.getScreenY();
		int finalW = viewport.getScreenWidth();
		int finalH = viewport.getScreenHeight();

		HdpiUtils.glViewport(finalX, finalY, finalW, finalH);

		// 5. 渲染回调
		if (renderer != null) {
			batch.setProjectionMatrix(viewport.getCamera().combined);
			batch.begin();
			renderer.render(batch, viewport.getCamera());
			batch.end();
		}

		// 6. 恢复
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		uiBatch.begin();
	}
}

// ==========================================
// 5. 相机输入控制器 (Input Helper)
// ==========================================
class SceneCameraInput {
	private final GameViewportWidget widget;

	public SceneCameraInput(GameViewportWidget widget) {
		this.widget = widget;
	}

	public DragListener getDragListener() {
		return new DragListener() {
			@Override
			public void drag(InputEvent event, float x, float y, int pointer) {
				float dx = getDeltaX();
				float dy = getDeltaY();
				Camera cam = widget.getCamera();

				// 根据 zoom 调整拖动速度，体验更丝滑
				float zoom = 1f;
				if (cam instanceof OrthographicCamera) {
					zoom = ((OrthographicCamera) cam).zoom;
				}

				cam.translate(-dx * zoom, -dy * zoom, 0);
				cam.update();
			}
		};
	}

	public InputListener getScrollListener() {
		return new InputListener() {
			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				Camera cam = widget.getCamera();
				if (cam instanceof OrthographicCamera) {
					OrthographicCamera ortho = (OrthographicCamera) cam;
					ortho.zoom += amountY * 0.1f * ortho.zoom; // 线性缩放优化为基于当前zoom的缩放

					// 钳制缩放范围
					if (ortho.zoom < 0.1f) ortho.zoom = 0.1f;
					if (ortho.zoom > 10.0f) ortho.zoom = 10.0f;
					ortho.update();
				}
				return true;
			}
		};
	}
}
