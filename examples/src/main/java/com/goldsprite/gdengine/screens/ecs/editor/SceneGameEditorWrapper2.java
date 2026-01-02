package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
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
	public final OrthographicCamera camera;
	private final Vector2 tempCoords = new Vector2();

	public GameRenderWidget(SpriteBatch worldBatch) {
		this.worldBatch = worldBatch;
		// 每个视图拥有独立的相机
		this.camera = new OrthographicCamera();
	}

	@Override
	public void draw(Batch uiBatch, float parentAlpha) {
		validate(); // 确保布局更新

		// 1. 计算Widget在屏幕上的绝对位置和尺寸
		tempCoords.set(0, 0);
		localToStageCoordinates(tempCoords); // 获取在Stage上的坐标

		// 这里的x, y是相对于Stage左下角的
		int screenX = (int) tempCoords.x;
		int screenY = (int) tempCoords.y;
		int screenW = (int) getWidth();
		int screenH = (int) getHeight();

		// 2. 暂停UI绘制
		uiBatch.end();

		// 3. 裁剪区域 & 设置视口 (使用HdpiUtils处理高分屏)
		// 这一步让接下来的绘制只显示在这个Widget范围内，且坐标系映射正确
		HdpiUtils.glViewport(screenX, screenY, screenW, screenH);

		// 开启裁剪测试，防止画到Widget外面（例如画到窗口标题栏后面）
		Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
		HdpiUtils.glScissor(screenX, screenY, screenW, screenH);

		// 4. 清除背景 (可选，模拟不同的背景色)
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 5. 更新相机尺寸以匹配Widget尺寸 (实现 ExtendViewport 效果)
		camera.viewportWidth = screenW;
		camera.viewportHeight = screenH;
		camera.update();

		// 6. 绘制游戏世界
		worldBatch.setProjectionMatrix(camera.combined);
		worldBatch.begin();
		renderWorld(worldBatch); // 回调抽象方法
		worldBatch.end();

		// 7. 恢复现场
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		// 恢复全屏视口给UI使用
		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		// 8. 恢复UI绘制
		uiBatch.begin();
	}

	// 子类实现具体的绘制逻辑
	public abstract void renderWorld(Batch batch);
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
		VisWindow window = new VisWindow("Game View (Extend)");
		window.setResizable(true);
		window.setSize(400, 300);
		window.setPosition(50, 50);

		// 1. 创建游戏渲染层
		GameRenderWidget gameWidget = new GameRenderWidget(worldBatch) {
			@Override
			public void renderWorld(Batch batch) {
				// 这里放你的绘制逻辑
				// 比如 camera跟随，batch.draw...
				camera.position.set(playerX, playerY, 0); // 示例
				camera.update();
				batch.setProjectionMatrix(camera.combined);
				batch.draw(bgTexture, -500, -500, 1000, 1000);
				batch.draw(playerTexture, playerX, playerY);
			}
		};

		// 2. 创建UI层（摇杆）
		Touchpad.TouchpadStyle style = VisUI.getSkin().get(Touchpad.TouchpadStyle.class);
		joystick = new Touchpad(10, style);

		// 创建一个Table专门用来放摇杆，便于控制位置
		Table uiLayer = new Table();
		// 关键：如果不设对齐，摇杆会被拉伸填满整个屏幕
		// 这里设置：靠底部、靠左、留出20像素边距
		uiLayer.bottom().left().pad(20);
		uiLayer.add(joystick).size(100); // 设定摇杆大小

		// 3. 使用 Stack 将两者重叠
		// Stack 里的东西会一层层盖上去
		com.badlogic.gdx.scenes.scene2d.ui.Stack stack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
		stack.add(gameWidget); // 底层：画面
		stack.add(uiLayer);    // 顶层：UI控制

		// 4. 将 Stack 添加到窗口中
		// 只有这一行 add，所以窗口里只有一个满屏的 Stack
		window.add(stack).grow();

		stage.addActor(window);
	}

	// ================= 2. Scene View Window (拖拽移动相机) =================
	private void createSceneWindow() {
		VisWindow window = new VisWindow("Scene View (Independent)");
		window.setResizable(true);
		window.setSize(400, 300);
		window.setPosition(500, 50);

		GameRenderWidget sceneWidget = new GameRenderWidget(worldBatch) {
			@Override
			public void renderWorld(Batch batch) {
				// 相机位置完全由拖拽控制，不跟随小人
				// 绘制一样的内容，证明是同一个世界
				batch.draw(bgTexture, -500, -500, 1000, 1000);
				batch.draw(playerTexture, playerX, playerY);

				// 可以在这里画网格线辅助 Scene 视图
			}
		};

		// 增加拖拽控制相机
		sceneWidget.addListener(new DragListener() {
			@Override
			public void drag(InputEvent event, float x, float y, int pointer) {
				// 计算鼠标移动的差值 (delta)
				float dx = getDeltaX();
				float dy = getDeltaY();

				// 移动相机 (注意：鼠标向左拖，相机应该向右移才能看到左边的东西，所以是减)
				// 乘以 zoom 可以适配缩放速度
				sceneWidget.camera.translate(-dx, -dy);
			}
		});

		// 增加滚轮缩放 (可选)
		sceneWidget.addListener(new ClickListener(){
			// 需自定义InputListener处理 scrolled，这里简化略过
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
