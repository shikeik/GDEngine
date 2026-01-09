package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.system.SkeletonRenderSystem;
import com.goldsprite.gdengine.ecs.system.SpriteSystem;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.adapter.GObjectAdapter;
import com.goldsprite.gdengine.screens.ecs.editor.adapter.GObjectWrapperCache;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.EditorListener;
import com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

public class EditorController implements EditorListener {
	Stage stage;
	// GameWorld gameWorld; // Replaced by core.ecs.GameWorld.inst()
	ViewTarget gameTarget, sceneTarget;
	ViewWidget gameWidget, sceneWidget;
	Touchpad joystick;

	private GObject player;

	private OrthographicCamera sceneCamera;
	private OrthographicCamera gameCamera;
	private Viewport gameViewport;

	// 编辑器系统组件
	private CommandManager commandManager;
	private SceneManager sceneManager;
	private GizmoSystem gizmoSystem;
	private GObjectWrapperCache wrapperCache;

	// 输入处理
	private EditorInput editorInput;
	private InputMultiplexer inputMultiplexer;

	// Rendering Systems (Managed manually for Editor FBOs)
	private SpriteBatch spriteBatch;
	private NeonBatch neonBatch;
	private SpriteSystem spriteSystem;
	private SkeletonRenderSystem skeletonRenderSystem;

	public void create() {
		if (!VisUI.isLoaded()) VisUI.load();

		// 1. FBO 环境
		int fboW = 1280;
		int fboH = 720;
		gameTarget = new ViewTarget(fboW, fboH);
		sceneTarget = new ViewTarget(fboW, fboH);
		sceneCamera = new OrthographicCamera(fboW, fboH);

		// Game Camera Init
		gameCamera = new OrthographicCamera();
		reloadGameViewport();

		// 2. UI 环境
		float scl = 1.2f;
		stage = new Stage(new ExtendViewport(960 * scl, 540 * scl));

		// 3. 初始化编辑器系统组件
		commandManager = new CommandManager();
		wrapperCache = new GObjectWrapperCache();
		sceneManager = new SceneManager(commandManager);
		sceneManager.addListener(this);
		gizmoSystem = new GizmoSystem(sceneManager, commandManager);

		createGameWindow();
		createSceneWindow();

		// 4. 依赖注入
		EditorGameInput inputImpl = new EditorGameInput(gameWidget);
		EditorGameGraphics graphicsImpl = new EditorGameGraphics(gameTarget);
		Gd.init(Gd.Mode.EDITOR, inputImpl, graphicsImpl, Gd.compiler);

		// 5. ECS 游戏世界初始化
		if (GameWorld.inst() == null) {
			new GameWorld();
		}
		// 注入视口和相机 (主要用于逻辑计算)
		GameWorld.inst().setReferences(stage.getViewport(), gameCamera);

		// 6. 初始化渲染系统
		spriteBatch = new SpriteBatch();
		neonBatch = new NeonBatch();
		// 初始相机暂时设为 gameCamera，但在 render 时会切换
		spriteSystem = new SpriteSystem(spriteBatch, gameCamera);
		skeletonRenderSystem = new SkeletonRenderSystem(neonBatch, gameCamera);

		// 注册到 GameWorld (可选: 如果希望 GameWorld.update 自动驱动它们，但这里我们手动驱动)
		// GameWorld.inst().registerSystem(spriteSystem);
		// GameWorld.inst().registerSystem(skeletonRenderSystem);

		// 注意：GameWorld.update() 可能会自动运行所有 registered systems
		// 为了避免在非 FBO 环境下渲染，我们选择**不**注册它们到 GameWorld，
		// 而是像之前计划的那样，在 render() 中手动调用它们的 update()。

		// 确保游戏视口大小正确
		gameViewport.update(fboW, fboH, true);

		initTestScene();

		// 7. 设置场景根节点
		setupSceneRoot();

		// 初始化输入处理
		editorInput = new EditorInput(gameCamera, sceneManager, gizmoSystem);
		inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(editorInput);
		Gdx.input.setInputProcessor(inputMultiplexer);
	}

	private void setupSceneRoot() {
		// 创建一个虚拟的根节点，作为所有GObject的父节点
		// 由于GObjectAdapter不能接受null，我们创建一个特殊的根节点适配器
		EditorTarget rootAdapter = new EditorTarget() {
			private final Array<EditorTarget> children = new Array<>();
			private String name = "Scene Root";

			@Override public String getName() { return name; }
			@Override public void setName(String name) { this.name = name; }
			@Override public String getTypeName() { return "Root"; }

			@Override public float getX() { return 0; }
			@Override public void setX(float v) {}
			@Override public float getY() { return 0; }
			@Override public void setY(float v) {}
			@Override public float getRotation() { return 0; }
			@Override public void setRotation(float v) {}
			@Override public float getScaleX() { return 1; }
			@Override public void setScaleX(float v) {}
			@Override public float getScaleY() { return 1; }
			@Override public void setScaleY(float v) {}

			@Override public EditorTarget getParent() { return null; }
			@Override public void setParent(EditorTarget parent) {}
			@Override public void removeFromParent() {}
			@Override public Array<EditorTarget> getChildren() { return children; }
			@Override public void addChild(EditorTarget child) {
				if (child != null && !children.contains(child, true)) {
					children.add(child);
				}
			}

			@Override public boolean hitTest(float wx, float wy) { return false; }
			@Override public void render(com.goldsprite.gdengine.neonbatch.NeonBatch batch) {}
		};

		sceneManager.setRoot(rootAdapter);
	}

	private void initTestScene() {
		GameWorld world = GameWorld.inst();

		// 1. 创建主角
		player = new GObject("Player");

		// Transform
		TransformComponent trans = player.addComponent(TransformComponent.class);
		trans.position.set(0, 0);
		trans.scale = 0.5f;

		// Sprite
		SpriteComponent sprite = player.addComponent(SpriteComponent.class);
		try {
			// 使用 GDX 原生加载确保稳定
			Texture tex = new Texture(Gdx.files.internal("sprites/roles/enma/enma01.png"));
			sprite.setRegion(new TextureRegion(tex));
		} catch (Exception e) {
			Gdx.app.error("Editor", "Failed to load sprite", e);
		}
		sprite.setEnable(true);

		// 添加到编辑器系统
		addGObjectToEditor(player);

		// 2. 创建一个参照物
		GObject bgObj = new GObject("Reference");
		TransformComponent bgTrans = bgObj.addComponent(TransformComponent.class);
		bgTrans.position.set(200, 100);
		SpriteComponent bgSprite = bgObj.addComponent(SpriteComponent.class);
		try {
			Texture tex = new Texture(Gdx.files.internal("sprites/roles/Abaddon01.png"));
			bgSprite.setRegion(new TextureRegion(tex));
		} catch (Exception e) {}
		bgSprite.setEnable(true);

		// 添加到编辑器系统
		addGObjectToEditor(bgObj);
	}

	private void reloadGameViewport() {
		Gd.Config conf = Gd.config;

		switch (conf.viewportType) {
			case FIT:
				gameViewport = new FitViewport(conf.logicWidth, conf.logicHeight, gameCamera);
				break;
			case STRETCH:
				gameViewport = new StretchViewport(conf.logicWidth, conf.logicHeight, gameCamera);
				break;
			case EXTEND:
				gameViewport = new ExtendViewport(conf.logicWidth, conf.logicHeight, gameCamera);
				break;
		}
		// Apply updates
		if (gameTarget != null) {
			gameViewport.update(gameTarget.getFboWidth(), gameTarget.getFboHeight(), true);
		}
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
					// 坐标验证 & 逻辑 (暂时移除旧 GameWorld 逻辑)
					// Vector2 worldPos = gameWidget.screenToWorld(Gdx.input.getX(), Gdx.input.getY(), gameViewport);
					// GameWorld.inst()... // Handle input logic here later
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
				}
			});

		// 摇杆
		Texture bg = createSolidTexture(100, 100, Color.DARK_GRAY);
		Texture knob = createSolidTexture(30, 30, Color.LIGHT_GRAY);
		Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
		style.background = new TextureRegionDrawable(new TextureRegion(bg));
		style.knob = new TextureRegionDrawable(new TextureRegion(knob));
		joystick = new Touchpad(10, style);

		// 配置修改器
		VisSelectBox<String> box = new VisSelectBox<>();
		box.setItems("FIT", "STRETCH", "EXTEND");
		box.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					String mode = box.getSelected();

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

					reloadGameViewport();
					Gdx.app.log("Editor", "Viewport Changed to: " + mode);
				}
			});

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

	private Texture createSolidTexture(int w, int h, Color c) {
		Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
		p.setColor(c);
		p.fill();
		Texture t = new Texture(p);
		p.dispose();
		return t;
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
		// 1. 逻辑更新 (Input -> Logic)
		// 简单的摇杆控制逻辑
		if (player != null) {
			float speed = 200 * delta;
			float dx = joystick.getKnobPercentX() * speed;
			float dy = joystick.getKnobPercentY() * speed;

			TransformComponent trans = player.getComponent(TransformComponent.class);
			if (trans != null) {
				trans.position.add(dx, dy);

				// 面向调整
				SpriteComponent sprite = player.getComponent(SpriteComponent.class);
				if (sprite != null && dx != 0) {
					sprite.flipX = dx < 0;
				}
			}
		}

		GameWorld.inst().update(delta);

		// 2. 更新相机
		gameCamera.update();
		sceneCamera.update();

		// 3. 渲染 Game View (使用游戏相机)
		gameTarget.renderToFbo(() -> {
			gameViewport.apply();

			// 手动驱动渲染系统
			spriteSystem.setCamera(gameCamera);
			spriteSystem.update(delta);

			skeletonRenderSystem.setCamera(gameCamera);
			skeletonRenderSystem.update(delta);

			// Debug 渲染 (如果有)
			// renderDebug(gameCamera);
		});

		// 4. 渲染 Scene View (使用上帝相机)
		sceneTarget.renderToFbo(() -> {
			Gdx.gl.glViewport(0, 0, sceneTarget.getFboWidth(), sceneTarget.getFboHeight());

			// 渲染游戏世界
			spriteSystem.setCamera(sceneCamera);
			spriteSystem.update(delta);

			skeletonRenderSystem.setCamera(sceneCamera);
			skeletonRenderSystem.update(delta);

			// 渲染编辑器系统
			renderEditorSystem(sceneCamera);
		});

		HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	private void renderEditorSystem(OrthographicCamera camera) {
		// 渲染Gizmo
		if (gizmoSystem != null) {
			float zoom = camera.zoom;
			neonBatch.setProjectionMatrix(camera.combined);
			neonBatch.begin();
			gizmoSystem.render(neonBatch, zoom);
			neonBatch.end();
		}

		// 渲染选择高亮
		EditorTarget selection = sceneManager.getSelection();
		if (selection != null && selection instanceof GObjectAdapter) {
			GObjectAdapter adapter = (GObjectAdapter) selection;
			neonBatch.setProjectionMatrix(camera.combined);
			neonBatch.begin();
			adapter.render(neonBatch);
			neonBatch.end();
		}
	}

	public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        VisUI.dispose();
        // GameWorld.dispose(); // GameWorld 目前没有 dispose 方法，也许需要添加
        if (spriteBatch != null) spriteBatch.dispose();
        if (neonBatch != null) neonBatch.dispose();
        gameTarget.dispose();
        sceneTarget.dispose();
        gameWidget.dispose();
        stage.dispose();
    }

    // EditorListener 接口实现
    @Override
    public void onStructureChanged() {
        // 当场景结构发生变化时，更新编辑器UI
        updateSceneHierarchy();
    }

    @Override
    public void onSelectionChanged(EditorTarget selection) {
        // 当选择发生变化时，更新属性面板
        updatePropertyPanel(selection);
    }

    private void updateSceneHierarchy() {
        // 更新场景层次结构视图
        // 这里可以添加更新UI的代码
    }

    private void updatePropertyPanel(EditorTarget selection) {
        // 更新属性面板
        // 这里可以添加更新UI的代码
    }

    // 添加GObject到编辑器系统
    public void addGObjectToEditor(GObject gObject) {
        if (gObject == null) return;

        // 获取或创建GObjectAdapter
        GObjectAdapter adapter = wrapperCache.get(gObject);

        // 获取父节点
        EditorTarget parent = sceneManager.getRoot();
        if (gObject.getParent() != null) {
            GObjectAdapter parentAdapter = wrapperCache.get(gObject.getParent());
            if (parentAdapter != null) {
                parent = parentAdapter;
            }
        }

        // 添加到场景管理器
        adapter.setParent(parent);

        // 通知结构变化
        sceneManager.notifyStructureChanged();
    }

    // 从编辑器系统移除GObject
    public void removeGObjectFromEditor(GObject gObject) {
        if (gObject == null) return;

        GObjectAdapter adapter = wrapperCache.get(gObject);
        if (adapter != null) {
            adapter.removeFromParent();
            sceneManager.notifyStructureChanged();
        }
    }

    // 清理编辑器系统
    public void clearEditor() {
        // 清理缓存
        wrapperCache.clear();

        // 重置选择
        sceneManager.selectNode(null);

        // 重置场景根节点
        setupSceneRoot();

        // 通知结构变化
        sceneManager.notifyStructureChanged();
    }
}
