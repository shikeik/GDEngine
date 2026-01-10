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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.adapter.GObjectAdapter;
import com.goldsprite.gdengine.screens.ecs.editor.adapter.GObjectWrapperCache;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTree;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.EditorListener;
import com.goldsprite.solofight.screens.tests.iconeditor.system.EditorUIProvider;
import com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;
import com.goldsprite.solofight.screens.tests.iconeditor.ui.Inspector;
import com.goldsprite.solofight.screens.tests.iconeditor.ui.UiNode;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;

import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class EditorController implements EditorListener, EditorUIProvider {
	private EditorGameScreen screen;
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

	// UI 组件
	private DragAndDrop dragAndDrop;
	private Inspector inspector;
	private VisTree<UiNode, EditorTarget> hierarchyTree;
	private VisTable hierarchyTable;
	private VisTable inspectorTable;

	// Rendering Systems (Managed manually for Editor FBOs)
	private SpriteBatch spriteBatch;
	private NeonBatch neonBatch;
	private ShapeRenderer shapeRenderer;
	private SpriteSystem spriteSystem;
	private SkeletonRenderSystem skeletonRenderSystem;

	public EditorController() {
	}
	public EditorController(EditorGameScreen screen) {
		this.screen = screen;
	}

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

		createUI();

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
		shapeRenderer = new ShapeRenderer();
		// 初始相机暂时设为 gameCamera，但在 render 时会切换
		spriteSystem = new SpriteSystem(spriteBatch, gameCamera);
		skeletonRenderSystem = new SkeletonRenderSystem(neonBatch, gameCamera);

		// 7. 设置场景根节点 (必须在添加对象之前！)
		setupSceneRoot();

		initTestScene();

		// 初始化输入处理
		// [核心修复] 重新实例化正确的 Input (使用 sceneCamera)，并绑定 Widget
		editorInput = new EditorInput(sceneCamera, sceneManager, gizmoSystem, commandManager);
		editorInput.setViewWidget(sceneWidget); // <--- 注入 Widget

		inputMultiplexer = new InputMultiplexer();
		// 优先处理 UI (防止 Toolbar 点击导致 Gizmo 失去焦点)
		inputMultiplexer.addProcessor(stage);
		// 其次处理 Gizmo 操作
		inputMultiplexer.addProcessor(editorInput);

		// 这里临时处理
		if(screen != null && screen.getImp() != null) {
			screen.getImp().addProcessor(inputMultiplexer);
		} else {
			Gdx.input.setInputProcessor(inputMultiplexer);
		}

		Gdx.app.log("EditorController", "InputProcessor initialized. Multiplexer size: " + inputMultiplexer.getProcessors().size);
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
			sprite.setRegion(new TextureRegion(tex, 0, 0, 80, 80));
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
			bgSprite.setRegion(new TextureRegion(tex, 0, 0, 80, 80));
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
			gameViewport.update(gameTarget.getFboWidth(), gameTarget.getFboHeight(), false); // 世界相机应在0,0原点
		}
	}

	public InputProcessor getInputProcessor() { return stage; }

    private void createUI() {
        // 1. Initialize Components
        dragAndDrop = new DragAndDrop();
        inspector = new Inspector(this, sceneManager, commandManager);

        // 2. Create Widgets
        createGameWidget();
        createSceneWidget();

        // Add tempBack at the bottom layer
        Image tempBack = new Image(createSolidTexture(1, 1, new Color(0.15f, 0.15f, 0.15f, 1)));
        tempBack.setFillParent(true);
        stage.addActor(tempBack);

        // 3. Build Layout
        VisTable root = new VisTable();
        root.setFillParent(true);

        // Hierarchy Panel
        hierarchyTable = new VisTable();
        hierarchyTable.setBackground(VisUI.getSkin().getDrawable("window-bg"));

        // Inspector Panel
        inspectorTable = new VisTable();
        inspectorTable.setBackground(VisUI.getSkin().getDrawable("window-bg"));

        // Central Area (Scene View + Game View + Toolbar)
        Stack centralStack = new Stack();
        
        // Split Game/Scene (Vertical Split)
        VisSplitPane viewSplit = new VisSplitPane(sceneWidget, gameWidget, true);
        viewSplit.setSplitAmount(0.5f);
        
        centralStack.add(viewSplit);
        centralStack.add(createToolbar());

        // Split Panes
        VisSplitPane rightSplit = new VisSplitPane(centralStack, inspectorTable, false);
        rightSplit.setSplitAmount(0.8f);

        VisSplitPane mainSplit = new VisSplitPane(hierarchyTable, rightSplit, false);
        mainSplit.setSplitAmount(0.2f);

        root.add(mainSplit).grow();
        stage.addActor(root);

        // Initial Update
        updateSceneHierarchy();
    }

    private void createGameWidget() {
        gameWidget = new ViewWidget(gameTarget);

        // Listener
        gameWidget.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                ((EditorGameInput) Gd.input).setTouched(true, pointer);
                if (Gd.input.getInputProcessor() != null) {
                    Vector2 fboPos = gameWidget.mapScreenToFbo(Gdx.input.getX(), Gdx.input.getY());
                    Gd.input.getInputProcessor().touchDown((int) fboPos.x, (int) fboPos.y, pointer, button);
                }
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

        gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT);

        // Joystick initialization (retained for potential use)
        Texture bg = createSolidTexture(100, 100, Color.DARK_GRAY);
        Texture knob = createSolidTexture(30, 30, Color.LIGHT_GRAY);
        Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
        style.background = new TextureRegionDrawable(new TextureRegion(bg));
        style.knob = new TextureRegionDrawable(new TextureRegion(knob));
        joystick = new Touchpad(10, style);
    }

    private void createSceneWidget() {
        sceneWidget = new ViewWidget(sceneTarget);
        sceneWidget.setDisplayMode(ViewWidget.DisplayMode.COVER);

        sceneWidget.addListener(new InputListener() {
            private boolean isEditorHandling = false;
            private float lastX, lastY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (editorInput.touchDown(Gdx.input.getX(), Gdx.input.getY(), pointer, button)) {
                    isEditorHandling = true;
                    return true;
                }
                isEditorHandling = false;
                sceneManager.selectNode(null);
                lastX = Gdx.input.getX();
                lastY = Gdx.input.getY();
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (isEditorHandling) {
                    editorInput.touchDragged(Gdx.input.getX(), Gdx.input.getY(), pointer);
                } else {
                    float currX = Gdx.input.getX();
                    float currY = Gdx.input.getY();
                    float dx = currX - lastX;
                    float dy = currY - lastY;
                    lastX = currX;
                    lastY = currY;
                    float zoom = sceneCamera.zoom;
                    float ratio = 1.0f;
                    if (sceneWidget.getWidth() > 0) {
                        ratio = sceneTarget.getFboWidth() / sceneWidget.getWidth();
                    }
                    sceneCamera.translate(-dx * ratio * zoom, dy * ratio * zoom, 0);
                    sceneCamera.update();
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (isEditorHandling) {
                    editorInput.touchUp(Gdx.input.getX(), Gdx.input.getY(), pointer, button);
                }
                isEditorHandling = false;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                sceneCamera.zoom += amountY * 0.1f * sceneCamera.zoom;
                if (sceneCamera.zoom < 0.1f) sceneCamera.zoom = 0.1f;
                if (sceneCamera.zoom > 10f) sceneCamera.zoom = 10f;
                sceneCamera.update();
                return true;
            }
        });
    }

    private Table createToolbar() {
        Table toolbar = new Table();
        toolbar.top().left().pad(5);

        addToolBtn(toolbar, "M", () -> gizmoSystem.mode = GizmoSystem.Mode.MOVE);
        addToolBtn(toolbar, "R", () -> gizmoSystem.mode = GizmoSystem.Mode.ROTATE);
        addToolBtn(toolbar, "S", () -> gizmoSystem.mode = GizmoSystem.Mode.SCALE);

        toolbar.add().width(15);

        addToolBtn(toolbar, "<", () -> commandManager.undo());
        addToolBtn(toolbar, ">", () -> commandManager.redo());

        return toolbar;
    }

    private Texture createSolidTexture(int w, int h, Color c) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

	private void addToolBtn(Table table, String text, Runnable action) {
		VisTextButton btn = new VisTextButton(text);
		btn.addListener(new ClickListener() {
			@Override public void clicked(InputEvent e, float x, float y) { action.run(); }
		});
		table.add(btn).size(30, 30).padRight(5);
	}

	public void render(float delta) {
		// 1. 逻辑更新 (Input -> Logic)// 优先使用键盘输入，如果没有键盘输入则使用摇杆
		if (player != null) {
			float speed = 200 * delta;
			float dx = 0, dy = 0;

			// 获取键盘方向
			Vector2 keyboardDir = editorInput.getKeyboardDirection();
			if (keyboardDir.len() > 0) {
				// 有键盘输入，使用键盘方向
				dx = keyboardDir.x * speed;
				dy = keyboardDir.y * speed;
			} else {
				// 没有键盘输入，使用摇杆方向
				dx = joystick.getKnobPercentX() * speed;
				dy = joystick.getKnobPercentY() * speed;
			}

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

		// 2. 更新相机 - 添加跟随玩家的逻辑
		if (player != null) {
			TransformComponent trans = player.getComponent(TransformComponent.class);
			if (trans != null) {
				// 让游戏相机跟随玩家
				float camX = gameCamera.position.x;
				float camY = gameCamera.position.y;
				float targetX = trans.position.x;
				float targetY = trans.position.y;

				// 平滑跟随
				gameCamera.position.x += (targetX - camX) * 5 * delta;
				gameCamera.position.y += (targetY - camY) * 5 * delta;
			}
		}
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
			Gdx.gl.glClearColor(0, 0, 0, 0);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			// 绘制网格
			drawGrid(sceneCamera);

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

    private void drawGrid(OrthographicCamera camera) {
        if (shapeRenderer == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        float zoom = camera.zoom;
        float step = 100f; // Grid size

        // Viewport bounds
        float w = camera.viewportWidth * zoom;
        float h = camera.viewportHeight * zoom;
        float x = camera.position.x;
        float y = camera.position.y;

        // Calculate visible range
        float startX = (float)Math.floor((x - w / 2) / step) * step;
        float endX = (float)Math.ceil((x + w / 2) / step) * step;
        float startY = (float)Math.floor((y - h / 2) / step) * step;
        float endY = (float)Math.ceil((y + h / 2) / step) * step;

        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.5f); // Grey

        for (float i = startX; i <= endX; i += step) {
            if (i == 0) continue;
            shapeRenderer.line(i, y - h / 2, i, y + h / 2);
        }

        for (float i = startY; i <= endY; i += step) {
            if (i == 0) continue;
            shapeRenderer.line(x - w / 2, i, x + w / 2, i);
        }

        // Axes (Red X, Blue Y at 0,0)
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(x - w / 2, 0, x + w / 2, 0);

        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.line(0, y - h / 2, 0, y + h / 2);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

	public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        VisUI.dispose();
        // GameWorld.dispose(); // GameWorld 目前没有 dispose 方法，也许需要添加
        if (spriteBatch != null) spriteBatch.dispose();
        if (neonBatch != null) neonBatch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
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
        if (hierarchyTable == null) return;
        hierarchyTable.clearChildren();

        hierarchyTree = new VisTree<>();
        hierarchyTree.getSelection().setProgrammaticChangeEvents(false);

        EditorTarget root = sceneManager.getRoot();
        if (root != null) {
            buildTree(root, null);
        }

        hierarchyTree.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                UiNode node = (UiNode) hierarchyTree.getNodeAt(y);
                if (node != null) {
                    sceneManager.selectNode(node.getValue());
                } else {
                    sceneManager.selectNode(null);
                }
            }
        });

        hierarchyTable.add(hierarchyTree).expand().fill().top().left();
    }

    private void buildTree(EditorTarget target, UiNode parentNode) {
        UiNode node = new UiNode(target, this);
        if (parentNode == null) {
            hierarchyTree.add(node);
        } else {
            parentNode.add(node);
        }

        // Auto expand if it has children
        if (target.getChildren().size > 0) node.setExpanded(true);

        for (EditorTarget child : target.getChildren()) {
            buildTree(child, node);
        }
    }

    private void updatePropertyPanel(EditorTarget selection) {
        if (inspectorTable == null || inspector == null) return;
        inspector.build(inspectorTable, selection);

        // Sync selection in Tree
        if (hierarchyTree != null && selection != null) {
            UiNode node = hierarchyTree.findNode(selection);
            if (node != null) {
                hierarchyTree.getSelection().clear();
                hierarchyTree.getSelection().add(node);
                node.expandTo();
            }
        }
    }

    // EditorUIProvider Implementation
    @Override public VisTree<UiNode, EditorTarget> getHierarchyTree() { return hierarchyTree; }
    @Override public SceneManager getSceneManager() { return sceneManager; }
    @Override public CommandManager getCommandManager() { return commandManager; }
    @Override public Stage getUiStage() { return stage; }
    @Override public DragAndDrop getDragAndDrop() { return dragAndDrop; }

    // 添加GObject到编辑器系统
    public void addGObjectToEditor(GObject gObject) {
        if (gObject == null) return;

        // 获取或创建GObjectAdapter
        GObjectAdapter adapter = wrapperCache.get(gObject);

        // 1. 处理 ECS 层级关系
        // 如果 GObject 已经在 ECS 中有父节点，我们不需要手动干预 Adapter 的 parent，
        // 因为 Adapter.getParent() 会动态从 GObject 获取。

        // 2. 处理 编辑器 层级关系 (挂载到 Root)
        // 如果 GObject 是顶层对象 (没有父节点)，它必须被挂载到 sceneManager.getRoot() 下，
        // 否则 SceneManager 遍历不到它。
        if (gObject.getParent() == null) {
            EditorTarget root = sceneManager.getRoot();
            // [核心修复] 显式添加到 Root 的子节点列表
            // 注意：这里不能调用 adapter.setParent(root)，因为 GObject 不能认 Root 为父。
            // 我们利用 RootAdapter 的 addChild 实现来管理这份“虚拟”关系。
            root.addChild(adapter);
        }

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
