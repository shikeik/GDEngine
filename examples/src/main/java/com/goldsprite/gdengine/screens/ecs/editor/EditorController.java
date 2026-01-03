package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
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
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisWindow;

public class EditorController {
	Stage stage;
	GameWorld gameWorld;
	ViewTarget gameTarget, sceneTarget;
	ViewWidget gameWidget, sceneWidget;
	Touchpad joystick;

	private OrthographicCamera sceneCamera;

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		// 1. FBO 环境
		int fboW = 1280;
		int fboH = 720;
		gameTarget = new ViewTarget(fboW, fboH);
		sceneTarget = new ViewTarget(fboW, fboH);
		sceneCamera = new OrthographicCamera(fboW, fboH);

		// 2. UI 环境
		float scl = 1.2f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		createGameWindow();
		createSceneWindow();

		// 3. 代理初始化
		Gd.init(Gd.Mode.EDITOR, gameWidget, gameTarget);

		// 4. 游戏初始化 (会读取 Gd.config 默认配置)
		gameWorld = new GameWorld();
		gameWorld.init();

		// 5. 确保游戏视口大小正确 (FBO大小)
		gameWorld.getViewport().update(fboW, fboH, true);

		Gdx.input.setInputProcessor(stage);
	}

	public InputProcessor getInputProcessor() { return stage; }

	private void createGameWindow() {
		VisWindow win = new VisWindow("Game View");
		win.setResizable(true);
		win.setSize(500, 350);
		win.setPosition(50, 50);

		gameWidget = new ViewWidget(gameTarget);

		// 输入监听与注入
		gameWidget.addListener(new InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					((EditorGameInput) Gd.input).setTouched(true, pointer);
					if (Gd.input.getInputProcessor() != null) {
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchDown((int) fboPos.x, (int) fboPos.y, pointer, button);
					}
					// 坐标验证
					Vector2 worldPos = gameWidget.screenToWorld(Gdx.input.getX(), Gdx.input.getY(), gameWorld.getViewport());
					gameWorld.targetX = worldPos.x;
					gameWorld.targetY = worldPos.y;
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
					if (Gd.input.getInputProcessor() != null) {
						Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
						Gd.input.getInputProcessor().touchDragged((int) fboPos.x, (int) fboPos.y, pointer);
					}
					Vector2 worldPos = gameWidget.screenToWorld(Gdx.input.getX(), Gdx.input.getY(), gameWorld.getViewport());
					gameWorld.targetX = worldPos.x;
					gameWorld.targetY = worldPos.y;
				}
			});

		// 摇杆
		Texture bg = GameWorld.createSolidTexture(100, 100, Color.DARK_GRAY);
		Texture knob = GameWorld.createSolidTexture(30, 30, Color.LIGHT_GRAY);
		Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
		style.background = new TextureRegionDrawable(new TextureRegion(bg));
		style.knob = new TextureRegionDrawable(new TextureRegion(knob));
		joystick = new Touchpad(10, style);

		// 配置修改器 (模拟 Godot 的 Project Settings)
		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					String mode = box.getSelected();

					// 1. 修改配置数据
					if (mode.equals("FIT")) {
						Gd.config.viewportType = Gd.ViewportType.FIT;
						gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);
					} else if (mode.equals("STRETCH")) {
						Gd.config.viewportType = Gd.ViewportType.STRETCH;
						gameWidget.setDisplayMode(ViewWidget.DisplayMode.STRETCH);
					} else if (mode.equals("EXTEND")) {
						Gd.config.viewportType = Gd.ViewportType.EXTEND;
						gameWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);
					}

					// 2. 通知游戏逻辑重载
					gameWorld.reloadConfig();

					// 3. 刷新日志
					Gdx.app.log("Editor", "Viewport Changed to: " + mode);
				}
			});

		// 默认 Widget 模式
		gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);

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
		sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);

		sceneWidget.addListener(new DragListener() {
				@Override
				public void drag(InputEvent event, float x, float y, int pointer) {
					float dx = getDeltaX(); float dy = getDeltaY();
					float zoom = sceneCamera.zoom;
					sceneCamera.translate(-dx * 2 * zoom, -dy * 2 * zoom, 0);
					sceneCamera.update();
				}
				@Override
				public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
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
		float speed = 200 * delta;
		gameWorld.update(delta, joystick.getKnobPercentX() * speed, joystick.getKnobPercentY() * speed);

		// --- 渲染 Game View (使用游戏相机) ---
		gameTarget.renderToFbo(() -> {
			// 1. 应用游戏视口 (处理黑边)
			gameWorld.getViewport().apply();
			// 2. 使用游戏相机渲染
			gameWorld.render(gameWorld.getGameCamera());
			gameWorld.renderDebug(gameWorld.getGameCamera());
		});

		// --- 渲染 Scene View (使用上帝相机) ---
		sceneTarget.renderToFbo(() -> {
			// 1. 铺满全屏 (SceneView 不应该有 FIT 黑边)
			Gdx.gl.glViewport(0, 0, sceneTarget.getFboWidth(), sceneTarget.getFboHeight());
			// 2. 使用上帝相机渲染
			gameWorld.render(sceneCamera);
			gameWorld.renderDebug(sceneCamera);
		});

		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
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
