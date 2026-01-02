package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.*;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;

public class SceneGameEditorWrapper2 extends GScreen {
	private DualViewEditor d;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		d = new DualViewEditor();
		d.create();
	}

	@Override
	public void render(float delta) {
		d.render();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		d.resize(width, height);
	}
}


// 自定义Widget，用于在UI中渲染游戏世界
abstract class GameRenderWidget extends Widget {
	private final SpriteBatch worldBatch;

	// 现在不仅仅持有 camera，而是持有 viewport
	// viewport 内部会持有 camera
	public Viewport viewport;

	private final Vector2 tempCoords = new Vector2();

	// 设计分辨率 (逻辑世界的大小)
	private final float worldWidth;
	private final float worldHeight;

	public GameRenderWidget(SpriteBatch worldBatch, float worldW, float worldH) {
		this.worldBatch = worldBatch;
		this.worldWidth = worldW;
		this.worldHeight = worldH;

		// 默认先给一个 Extend
		setViewportType(ViewportType.EXTEND);
	}

	// 定义支持的类型枚举
	public enum ViewportType {
		EXTEND, FIT, STRETCH
	}

	// 切换视口类型的方法
	public void setViewportType(ViewportType type) {
		// 保留老相机的部分属性（如位置）
		Camera oldCamera = (viewport != null) ? viewport.getCamera() : null;

		Camera newCamera = new OrthographicCamera();
		if(oldCamera != null) {
			newCamera.position.set(oldCamera.position);
			newCamera.update();
		}

		switch (type) {
			case FIT:
				// Fit: 保持比例，可能有黑边
				viewport = new FitViewport(worldWidth, worldHeight, newCamera);
				break;
			case STRETCH:
				// Stretch: 强行拉伸填满，物体会变形
				viewport = new StretchViewport(worldWidth, worldHeight, newCamera);
				break;
			case EXTEND:
			default:
				// Extend: 视野变大，无黑边，物体不变形
				viewport = new ExtendViewport(worldWidth, worldHeight, newCamera);
				break;
		}
	}

	// 在 GameRenderWidget 类中
	@Override
	public void draw(Batch uiBatch, float parentAlpha) {
		validate();

		// 1. 计算位置 (保持不变)
		tempCoords.set(0, 0);
		localToStageCoordinates(tempCoords);
		int screenX = (int) tempCoords.x;
		int screenY = (int) tempCoords.y;
		int screenW = (int) getWidth();
		int screenH = (int) getHeight();

		if (screenW <= 0 || screenH <= 0) return;

		uiBatch.end();

		// 2. 剪裁 (保持不变)
		Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
		HdpiUtils.glScissor(screenX, screenY, screenW, screenH);

		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// ================== 核心修正点 ==================

		// 错误顺序 (旧代码):
		// viewport.setScreenBounds(screenX, screenY, screenW, screenH);
		// viewport.update(screenW, screenH, false); // 这句会把 x,y 重置为 0,0 ！！！

		// 正确顺序 (新代码):

		// 第一步：先 update，计算内部比例 (这会默认设 x=0, y=0)
		viewport.update(screenW, screenH, false);

		// 第二步：再覆盖 ScreenBounds，把位置设为 Widget 在屏幕上的真实位置
		viewport.setScreenBounds(screenX, screenY, screenW, screenH);

		// 第三步：最后应用到 OpenGL
		viewport.apply();

		// ==============================================

		// 3. 绘制
		worldBatch.setProjectionMatrix(viewport.getCamera().combined);
		worldBatch.begin();
		renderWorld(worldBatch);
		worldBatch.end();

		// 4. 恢复
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		uiBatch.begin();
	}

	public abstract void renderWorld(Batch batch);

	// 提供给外部获取当前相机的方法
	public Camera getCamera() {
		return viewport.getCamera();
	}
}

class DualViewEditor extends ApplicationAdapter {
	Stage stage;
	SpriteBatch worldBatch;
	Texture playerTexture;
	Texture bgTexture;

	// 游戏数据
	float playerX = 0, playerY = 0;

	// UI组件
	Touchpad joystick;

	@Override
	public void create() {
		worldBatch = new SpriteBatch();
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		// 资源加载 (随便搞两个图)
		playerTexture = new Texture(Gdx.files.internal("role.png")); // 替换你的素材
		bgTexture = new Texture(Gdx.files.internal("role.png"));     // 替换背景

		createGameWindow();
		createSceneWindow();
	}

	// ================= 1. Game View Window (摇杆控制 + 相机跟随) =================
	private void createGameWindow() {
		VisWindow window = new VisWindow("Game View");
		window.setResizable(true);
		window.setSize(500, 400);
		window.setPosition(50, 50);

		// 1. 渲染层 (Widget)
		final GameRenderWidget gameWidget = new GameRenderWidget(worldBatch, 480, 320) {
			@Override
			public void renderWorld(Batch batch) {
				getCamera().position.set(playerX, playerY, 0);
				getCamera().update();
				batch.draw(bgTexture, -500, -500, 1000, 1000);
				batch.draw(playerTexture, playerX, playerY);
			}
		};

		// 2. UI层 (摇杆 + 下拉框)
		Table uiLayer = new Table();
		// 关键：让UI层只作为容器，不拦截空白处的点击，否则可能影响底层游戏交互
		// (不过摇杆和下拉框作为子元素依然可以被点击)
		uiLayer.setFillParent(true);

		// --- 摇杆 (左下) ---
		Touchpad.TouchpadStyle style = VisUI.getSkin().get(Touchpad.TouchpadStyle.class);
		joystick = new Touchpad(10, style);

		// --- 视口切换器 (右上) ---
		final VisSelectBox<String> viewSelector = new VisSelectBox<>();
		viewSelector.setItems("Extend", "Fit", "Stretch");
		viewSelector.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String selected = viewSelector.getSelected();
				if("Fit".equals(selected)) gameWidget.setViewportType(GameRenderWidget.ViewportType.FIT);
				else if ("Stretch".equals(selected)) gameWidget.setViewportType(GameRenderWidget.ViewportType.STRETCH);
				else gameWidget.setViewportType(GameRenderWidget.ViewportType.EXTEND);
			}
		});

		// --- 布局组装 ---
		// 第一行：右上角放下拉框
		uiLayer.top(); // 整个 Table 内容靠上
		uiLayer.add(viewSelector).expandX().right().pad(5);
		uiLayer.row(); // 换行

		// 第二行：中间占位 (把摇杆挤到下面去)
		uiLayer.add().expand().fill();
		uiLayer.row();

		// 第三行：左下角放摇杆
		uiLayer.add(joystick).left().bottom().pad(20).size(100);

		// 3. Stack 组合
		Stack stack = new Stack();
		stack.add(gameWidget); // 底层
		stack.add(uiLayer);    // 顶层

		window.add(stack).grow();
		stage.addActor(window);
	}

	// ================= 2. Scene View Window (拖拽移动相机) =================
	// ================= 2. Scene View Window (修正版) =================
	private void createSceneWindow() {
		VisWindow window = new VisWindow("Scene View (Independent)");
		window.setResizable(true);
		window.setSize(400, 300);
		window.setPosition(500, 50);

		// 修复1：构造函数现在需要传参了 (worldWidth, worldHeight)
		// 这里我们可以随便传一个，因为 Scene 视图通常默认为 Extend 模式，
		// 但为了保持一致，我们还是传入 480x320
		final GameRenderWidget sceneWidget = new GameRenderWidget(worldBatch, 480, 320) {
			@Override
			public void renderWorld(Batch batch) {
				// Scene视图逻辑：
				// 不需要像 GameView 那样每帧强制 camera.position.set(player...)
				// 相机位置完全保留它当前的状态（由拖拽控制）

				// 绘制背景和玩家
				batch.draw(bgTexture, -500, -500, 1000, 1000);
				batch.draw(playerTexture, playerX, playerY);

				// (可选) 可以在这里画一个网格线，表示编辑器网格
			}
		};

		// 默认让 Scene 视图也是 EXTEND 模式，方便看全图
		sceneWidget.setViewportType(GameRenderWidget.ViewportType.EXTEND);

		// 增加拖拽控制相机
		sceneWidget.addListener(new DragListener() {
			@Override
			public void drag(InputEvent event, float x, float y, int pointer) {
				// 计算鼠标移动的差值
				float dx = getDeltaX();
				float dy = getDeltaY();

				// 修复2：无法解析 'camera'
				// 原来的 .camera 字段没了，要用 .getCamera() 方法
				// 并且 getCamera() 返回的是 Camera 抽象类，虽然通常够用了，
				// 但如果你要操作 zoom，可能需要强转一下：
				// ((OrthographicCamera)sceneWidget.getCamera()).zoom += ...

				sceneWidget.getCamera().translate(-dx, -dy, 0);
				sceneWidget.getCamera().update(); // 记得手动 update 一下
			}
		});

		// 增加滚轮缩放 (可选功能，方便编辑器操作)
		sceneWidget.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				// 获取正交相机
				OrthographicCamera cam = (OrthographicCamera) sceneWidget.getCamera();

				// 调整缩放 (amountY 是滚轮值，1 或 -1)
				cam.zoom += amountY * 0.1f;

				// 限制缩放范围，别缩太小或太大
				if (cam.zoom < 0.1f) cam.zoom = 0.1f;
				if (cam.zoom > 5.0f) cam.zoom = 5.0f;

				cam.update();
				return true;
			}
		});

		window.add(sceneWidget).grow();
		stage.addActor(window);
	}

	@Override
	public void render() {
		// 逻辑更新
		updateGameLogic();

		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act();
		stage.draw();
	}

	private void updateGameLogic() {
		// 根据摇杆更新小人位置
		float speed = 200 * Gdx.graphics.getDeltaTime();
		playerX += joystick.getKnobPercentX() * speed;
		playerY += joystick.getKnobPercentY() * speed;
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		VisUI.dispose();
		worldBatch.dispose();
		playerTexture.dispose();
		bgTexture.dispose();
		stage.dispose();
	}
}
