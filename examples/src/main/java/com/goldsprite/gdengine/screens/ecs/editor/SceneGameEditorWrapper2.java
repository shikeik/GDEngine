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

	@Override
	public void draw(Batch uiBatch, float parentAlpha) {
		validate();

		// 1. 获取 Widget 在屏幕上的实际位置和大小
		tempCoords.set(0, 0);
		localToStageCoordinates(tempCoords);

		int screenX = (int) tempCoords.x;
		int screenY = (int) tempCoords.y;
		int screenW = (int) getWidth();
		int screenH = (int) getHeight();

		// 2. 暂停 UI 绘制
		uiBatch.end();

		// ================== 核心修改开始 ==================

		// A. 开启裁剪测试 (Scissor Test)
		// 这一步很重要：先把绘图限制在 Widget 范围内。
		// 这样如果 FitViewport 留了黑边，或者我们在“黑边”区域清屏，就不会涂到别的窗口上去。
		Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
		HdpiUtils.glScissor(screenX, screenY, screenW, screenH);

		// B. 清除 Widget 背景 (这就形成了 Fit 模式下的“黑边”颜色)
		// 我们可以设成灰色，方便看清边界
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// C. 配置 Viewport
		// 告诉 Viewport：“你的屏幕不是全屏，而是只有 screenX,Y,W,H 这么大”
		viewport.setScreenBounds(screenX, screenY, screenW, screenH);

		// D. 更新 Viewport
		// 这会根据当前 Widget 的大小计算相机的 zoom 和 projection
		// centerCamera = true (通常游戏视图希望相机对准中心，或者你需要手动控制相机位置)
		viewport.update(screenW, screenH, false);

		// E. 应用 Viewport
		// 这一步内部会调用 HdpiUtils.glViewport()，把 OpenGL 绘制区设置到正确的位置
		// 如果是 Fit 模式，这里设置的区域可能会比 Widget 小（居中）
		viewport.apply();

		// ================== 核心修改结束 ==================

		// 3. 绘制游戏世界
		worldBatch.setProjectionMatrix(viewport.getCamera().combined);
		worldBatch.begin();
		renderWorld(worldBatch);
		worldBatch.end();

		// 4. 恢复现场
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		// 恢复全屏视口给 UI 使用
		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		// 5. 恢复 UI 绘制
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
		window.setSize(500, 400); // 稍微大点方便看 Fit 效果
		window.setPosition(50, 50);

		// 1. 创建渲染层
		// 假设我们的游戏设计分辨率是 480x320 (像素风游戏常用)
		// 如果 Widget 比例和这个不一致，Fit 模式就会出现黑边
		final GameRenderWidget gameWidget = new GameRenderWidget(worldBatch, 480, 320) {
			@Override
			public void renderWorld(Batch batch) {
				// 简单跟随
				getCamera().position.set(playerX, playerY, 0);
				getCamera().update();
				// 注意：batch 的 projection matrix 已经在 Widget 里设置好了，这里不需要再 set

				// 画背景和人
				batch.draw(bgTexture, -500, -500, 1000, 1000);
				batch.draw(playerTexture, playerX, playerY);

				// 画一个红框表示“世界边界”，方便调试 Fit 效果
				// (实际项目中用 ShapeRenderer，这里偷懒不做演示)
			}
		};

		// 2. 创建 UI 层
		Table uiLayer = new Table();
		uiLayer.setFillParent(true); // 填满 Stack

		// --- 摇杆 (左下) ---
		Touchpad.TouchpadStyle style = VisUI.getSkin().get(Touchpad.TouchpadStyle.class);
		joystick = new Touchpad(10, style);
		Table joystickTable = new Table();
		joystickTable.bottom().left().pad(20);
		joystickTable.add(joystick).size(100);

		// --- 视口切换器 (右上) ---
		VisSelectBox<String> viewSelector = new VisSelectBox<>();
		viewSelector.setItems("Extend", "Fit", "Stretch");

		viewSelector.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeListener.ChangeEvent event, Actor actor) {
				String selected = viewSelector.getSelected();
				if("Fit".equals(selected)) {
					gameWidget.setViewportType(GameRenderWidget.ViewportType.FIT);
				} else if ("Stretch".equals(selected)) {
					gameWidget.setViewportType(GameRenderWidget.ViewportType.STRETCH);
				} else {
					gameWidget.setViewportType(GameRenderWidget.ViewportType.EXTEND);
				}
			}
		});

		Table selectorTable = new Table();
		selectorTable.top().right().pad(10);
		selectorTable.add(viewSelector).width(100);

		// 把它们加到 UI 层
		uiLayer.add(joystickTable).expand().bottom().left(); // 占用左下角
		uiLayer.addActor(selectorTable); // addActor 直接覆盖在上面，或者你也用 Table 布局

		// 3. 组装 Stack
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
