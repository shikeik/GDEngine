package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gdengine.screens.ecs.editor.GameWorld;
import com.goldsprite.gdengine.screens.ecs.editor.Gd;
import com.goldsprite.gdengine.screens.ecs.editor.ViewWidget;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisWindow;

// ==========================================
// 4. 控制器层 (Controller)
// ==========================================
public class EditorController {
	Stage stage;
	GameWorld gameWorld;
	ViewTarget gameTarget, sceneTarget;
	ViewWidget gameWidget, sceneWidget;
	Touchpad joystick;

	// 【修改】场景编辑器专用的上帝相机 (不再放在 ViewTarget 里)
	private OrthographicCamera sceneCamera;

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		gameWorld = new GameWorld();
		// 【修改点 1】 Gd 初始化前不调用 gameWorld.init()
		// Gd.init() 会创建 ViewportManager，GameWorld.init() 会用到它

		// --- FBO 和 Stage 设置 ---
		gameTarget = new ViewTarget(1280, 720); // FBO 模拟手机屏幕分辨率
		sceneTarget = new ViewTarget(1280, 720); // FBO 模拟手机屏幕分辨率

		float scl = 1.2f; // UI 缩放因子
		// Stage 使用 ExtendViewport，将 UI 缩放到 960x540 逻辑分辨率
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		// --- 创建 UI 窗口 ---
		createGameWindow();
		createSceneWindow();

		// --- 初始化代理 ---
		// Gd.init 需要 widget 和 target 来设置编辑器模式下的代理
		Gd.init(Gd.Mode.EDITOR, gameWidget, gameTarget);

		// 【修改点 2】GameWorld 在 Gd 初始化后初始化
		// GameWorld 现在会从 Gd 获取相机和视口
		gameWorld.init();

		// 输入处理链：Stage (UI) -> Gd (代理游戏输入)
		Gdx.input.setInputProcessor(stage);
	}

	private void createGameWindow() {
		VisWindow win = new VisWindow("Game View");
		win.setResizable(true);
		win.setSize(500, 350);
		win.setPosition(50, 50);

		gameWidget = new ViewWidget(gameTarget);

		// --- Game View 输入处理 ---
		gameWidget.addListener(new InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					// 1. 输入注入给 Gd (通知游戏有输入)
					((EditorGameInput) Gd.input).setTouched(true, pointer);

					// 2. 转发给游戏的 InputProcessor (如果游戏有的话)
					if (Gd.input.getInputProcessor() != null) {
						// 必须用全局屏幕坐标去转 FBO 坐标
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchDown((int) fboPos.x, (int) fboPos.y, pointer, button);
					}

					// 3. 编辑器逻辑：点击设置目标点 (验证坐标映射)
					//    使用 gameWidget.screenToWorld 获取游戏世界坐标
					Vector2 worldPos = gameWidget.screenToWorld(Gdx.input.getX(), Gdx.input.getY());
					gameWorld.targetX = worldPos.x;
					gameWorld.targetY = worldPos.y;
					Gdx.app.log("Editor", "GameView Clicked World: " + worldPos);

					return true;
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					((EditorGameInput) Gd.input).setTouched(false, pointer);
					if (Gd.input.getInputProcessor() != null) {
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchUp((int) fboPos.x, (int) fboPos.y, pointer, button);
					}
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					// 拖拽更新目标点
					Vector2 worldPos = gameWidget.screenToWorld(Gdx.input.getX(), Gdx.input.getY());
					gameWorld.targetX = worldPos.x;
					gameWorld.targetY = worldPos.y;

					if (Gd.input.getInputProcessor() != null) {
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchDragged((int) fboPos.x, (int) fboPos.y, pointer);
					}
				}
			});

		// --- 摇杆设置 ---
		Texture bg = GameWorld.createSolidTexture(100, 100, Color.DARK_GRAY);
		Texture knob = GameWorld.createSolidTexture(30, 30, Color.LIGHT_GRAY);
		Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
		style.background = new TextureRegionDrawable(new TextureRegion(bg));
		style.knob = new TextureRegionDrawable(new TextureRegion(knob));
		joystick = new Touchpad(10, style);

		// --- 视口模式选择器 ---
		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					String mode = box.getSelected();
					if (mode.equals("FIT")) {
						// 【修改】不再直接设置 target 的 viewportMode，而是更新 Gd 的配置
						Gd.view.setConfig(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, Gd.ViewportManager.Type.FIT);
						gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
					} else if (mode.equals("STRETCH")) {
						Gd.view.setConfig(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, Gd.ViewportManager.Type.STRETCH);
						gameWidget.setDisplayMode(ViewWidget.DisplayMode.STRETCH);
					} else if (mode.equals("EXTEND")) {
						Gd.view.setConfig(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, Gd.ViewportManager.Type.EXTEND);
						gameWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);
					}
				}
			});

		// --- UI 布局 ---
		Table uiTable = new Table();
		uiTable.setFillParent(true);
		uiTable.add(box).expandX().top().right().width(80).pad(5);
		uiTable.row();
		uiTable.add().expand().fill();
		uiTable.row();
		uiTable.add(joystick).bottom().left().pad(10);

		Stack stack = new Stack();
		stack.add(gameWidget);
		stack.add(uiTable);

		win.add(stack).grow();
		stage.addActor(win);
	}

	private void createSceneWindow() {
		VisWindow win = new VisWindow("Scene View");
		win.setResizable(true);
		win.setSize(400, 300);
		win.setPosition(600, 50);

		sceneWidget = new ViewWidget(sceneTarget);
		sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER); // Scene 通常铺满

		// --- Scene View 输入处理 (拖拽缩放) ---
		sceneWidget.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					// 【修改】使用 sceneCamera
					float dx = getDeltaX(); float dy = getDeltaY();
					float zoom = sceneCamera.zoom;
					sceneCamera.translate(-dx * 2 * zoom, -dy * 2 * zoom, 0);
					sceneCamera.update();
				}

				@Override
				public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
					// 【修改】使用 sceneCamera
					sceneCamera.zoom += amountY * 0.1f;
					if (sceneCamera.zoom < 0.1f) sceneCamera.zoom = 0.1f;
					if (sceneCamera.zoom > 5f) sceneCamera.zoom = 5f;
					sceneCamera.update();
					return true;
				}
			});

		win.add(sceneWidget).grow();
		stage.addActor(win);
	}

	public void render(float delta) {
		// --- 更新游戏逻辑 ---
		float speed = 200 * delta;
		// 摇杆输入被 Gd 代理，最终驱动 gameWorld
		gameWorld.update(delta, joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

		// --- 渲染 Game View (玩家视角) ---
		// 1. 告诉 Gd 使用游戏相机
		Gd.view.useGameCamera();
		// 2. 告诉 Gd 屏幕大小，让它更新游戏视口 (Fit/Extend...)
		Gd.view.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		// 3. GameTarget 渲染游戏逻辑
		gameTarget.renderToFbo(() -> {
			// GameWorld 内部会自己用 Gd.view.getCamera() 和 Gd.view.apply()
			gameWorld.render();
			gameWorld.renderDebug(); // GameWorld 内部会自己设置 ProjectionMatrix
		});

		// --- 渲染 Scene View (上帝视角) ---
		sceneTarget.renderToFbo(() -> {
			// 1. 告诉 Gd 使用编辑器相机 (劫持)
			Gd.view.useEditorCamera(sceneCamera);
			// 2. Scene View 不需要模拟游戏视口，直接铺满 FBO
			//    (GameWorld.render() 内部的 Gd.view.apply() 会被跳过)

			// 3. 渲染 GameWorld 内容 (使用 sceneCamera)
			gameWorld.render();
			gameWorld.renderDebug();
		});

		// --- 绘制编辑器 UI ---
		// 确保 GL 状态正确 (防止游戏逻辑乱改)
		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0, 0, 0, 1); // 清除 Stage 背景
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		// 【重要】屏幕尺寸变化时，也要通知 Gd.view 更新游戏视口
		Gd.view.update(width, height);
	}

	public void dispose() {
		VisUI.dispose();
		gameWorld.dispose();
		gameTarget.dispose();
		sceneTarget.dispose();
		gameWidget.dispose();
		stage.dispose();
	}
}
