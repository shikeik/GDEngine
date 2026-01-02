package com.goldsprite.solofight.screens.editor.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.ecs.skeleton.SpriteComponent;
import com.goldsprite.solofight.ecs.skeleton.SpriteSystem;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTree;
import java.util.function.Consumer;
import com.goldsprite.gameframeworks.log.Debug;

/**
 * 【最终独立版】Unity 风格编辑器布局
 * 无外部依赖，自包含所有逻辑
 */
public class SceneEditorScreen extends ExampleGScreen {

	// 自管资源
	private Stage uiStage;
	private SpriteBatch batch;

	// 逻辑组件
	private EditorContext context;
	private SimpleGizmoDrawer gizmoDrawer;
	private SceneViewWidget sceneWidget;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		// 1. 初始化独立绘图批次 (不再依赖外部)
		batch = new SpriteBatch();

		// 2. 初始化 Stage (使用基类提供的 Viewport)
		uiStage = new Stage(getUIViewport(), batch);

		// 接管输入
		getImp().addProcessor(uiStage);

		// 【1. 核心修复】初始化 ECS 世界
		GameWorld.autoDispose(); // 清理旧的
		new GameWorld(); // 创建新的 (SceneSystem 自动就位)

		// 【2. 注册渲染系统】
		// 传入 batch 和 worldCamera (这个相机随后会被 SceneViewWidget 接管控制)
		OrthographicCamera cam = (OrthographicCamera) getWorldCamera();
		new SpriteSystem(batch, cam);

		// 3. 初始化数据和工具
		context = new EditorContext();
		gizmoDrawer = new SimpleGizmoDrawer();
		setupFakeScene();

		// 4. 构建布局
		buildUnityLayout();
	}

	private void buildUnityLayout() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg"); // 确保你的 Skin 里有这个，如果没有会变透明
		uiStage.addActor(root);

		// --- 左侧栏 ---
		VisTable hierarchy = createHierarchyPanel();
		VisTable project = createProjectPanel();
		VisSplitPane leftSplit = new VisSplitPane(hierarchy, project, true);
		leftSplit.setSplitAmount(0.5f);

		// --- 中间栏 ---
		// 获取基类的世界相机并转为正交相机
		OrthographicCamera cam = (OrthographicCamera) getWorldCamera();
		sceneWidget = new SceneViewWidget(context, cam, gizmoDrawer);

		VisTable console = createConsolePanel();
		VisSplitPane centerSplit = new VisSplitPane(sceneWidget, console, true);
		centerSplit.setSplitAmount(0.7f);

		// --- 右侧栏 ---
		VisTable inspector = createInspectorPanel();

		// --- 组合 ---
		VisSplitPane centerRightSplit = new VisSplitPane(centerSplit, inspector, false);
		centerRightSplit.setSplitAmount(0.75f);

		VisSplitPane mainSplit = new VisSplitPane(leftSplit, centerRightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();
	}

	// ==========================================
	//  Panel Creators
	// ==========================================

	private VisTable createHierarchyPanel() {
		VisTable t = new VisTable(true);
		t.add(new VisLabel("Hierarchy")).left().pad(5).row();
		VisTree<Node, GObject> tree = new VisTree<>();
		for (GObject obj : context.fakeObjects) {
			Node n = new Node(new VisLabel(obj.getName()));
			n.setValue(obj);
			tree.add(n);
		}
		tree.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (!tree.getSelection().isEmpty())
					context.setSelection(tree.getSelection().first().getValue());
			}
		});
		t.add(new VisScrollPane(tree)).grow();
		return t;
	}

	private VisTable createProjectPanel() {
		VisTable t = new VisTable(true);
		t.add(new VisLabel("Project")).left().pad(5).row();
		t.add(new VisLabel("- Assets\n  - Texture\n  - Prefab")).left().top().row();
		t.add().grow();
		return t;
	}

	private VisTable createConsolePanel() {
		VisTable t = new VisTable(true);
		t.add(new VisLabel("Console")).left().pad(5).row();
		VisTextArea area = new VisTextArea("System Ready...");
		t.add(new VisScrollPane(area)).grow();
		return t;
	}

	private VisTable createInspectorPanel() {
		VisTable t = new VisTable(true);
		t.add(new VisLabel("Inspector")).left().pad(5).row();
		VisTable content = new VisTable();
		content.top().left();
		context.addListener(obj -> rebuildInspector(content, obj));
		t.add(new VisScrollPane(content)).grow();
		return t;
	}

	private void rebuildInspector(VisTable t, GObject obj) {
		t.clearChildren();
		if (obj == null)
			return;
		t.add(new VisLabel("Name: " + obj.getName())).left().row();
		t.add(new VisLabel("Pos X: " + obj.transform.position.x)).left().row();
		t.add(new VisLabel("Pos Y: " + obj.transform.position.y)).left().row();
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

		GameWorld.inst().update(delta);
		
		// 更新 SceneWidget 的控制器
		if (sceneWidget != null)
			sceneWidget.update(delta);

		uiStage.act(delta);
		uiStage.draw();
	}

	@Override
	public void dispose() {
		if (uiStage != null)
			uiStage.dispose();
		if (batch != null)
			batch.dispose();
		super.dispose();
	}

	// ==========================================
	//  内部类定义 (全部放在这里)
	// ==========================================

	/** 上下文 */
	public static class EditorContext {
		public Array<GObject> fakeObjects = new Array<>();
		private GObject selection;
		private Array<Consumer<GObject>> listeners = new Array<>();
		public void setSelection(GObject obj) {
			this.selection = obj;
			for (var l : listeners)
				l.accept(obj);
		}
		public GObject getSelection() {
			return selection;
		}
		public void addListener(Consumer<GObject> l) {
			listeners.add(l);
		}
	}

	/** 树节点 */
	public static class Node extends Tree.Node<Node, GObject, VisLabel> {
		public Node(VisLabel a) {
			super(a);
		}
	}

	/** 简单的 Gizmo 画笔 */
	public class SimpleGizmoDrawer {
		Texture tex;
		public SimpleGizmoDrawer() {
			Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
			p.setColor(Color.YELLOW);
			p.drawCircle(16, 16, 14);
			tex = new Texture(p);
			p.dispose();
		}
		public void draw(Batch b, GObject o) {
			if (o == null)
				return;
			b.draw(tex, o.transform.position.x - 16, o.transform.position.y - 16);
		}
	}

	/** 简单的内部控制器 */
	public class InternalCameraController {
		OrthographicCamera cam;
		int lastX, lastY;
		boolean dragging;
		public InternalCameraController(OrthographicCamera c) {
			this.cam = c;
		}

		public void update(float dt) {
			float speed = 500 * cam.zoom * dt;
			if (Gdx.input.isKeyPressed(Input.Keys.A))
				cam.translate(-speed, 0);
			if (Gdx.input.isKeyPressed(Input.Keys.D))
				cam.translate(speed, 0);
			if (Gdx.input.isKeyPressed(Input.Keys.W))
				cam.translate(0, speed);
			if (Gdx.input.isKeyPressed(Input.Keys.S))
				cam.translate(0, -speed);
			cam.update();
		}
		public boolean touchDown(int x, int y, int p, int b) {
			if (b == Input.Buttons.RIGHT) {
				dragging = true;
				lastX = x;
				lastY = y;
				return true;
			}
			return false;
		}
		public boolean touchUp(int x, int y, int p, int b) {
			dragging = false;
			return false;
		}
		public boolean touchDragged(int x, int y, int p) {
			if (dragging) {
				float zoom = cam.zoom;
				cam.translate(-(x - lastX) * zoom, (y - lastY) * zoom);
				lastX = x;
				lastY = y;
				return true;
			}
			return false;
		}
		public boolean scrolled(float ax, float ay) {
			cam.zoom += ay * 0.1f;
			if (cam.zoom < 0.1f)
				cam.zoom = 0.1f;
			return true;
		}
	}

	/** 核心：SceneViewWidget */
	public class SceneViewWidget extends Widget {
		private EditorContext ctx;
		private OrthographicCamera cam;
		private SimpleGizmoDrawer drawer;
		private InternalCameraController controller;
		private Texture bgGrid;

		private Matrix4 uiProjectionCache = new Matrix4();

		public SceneViewWidget(EditorContext ctx, OrthographicCamera cam, SimpleGizmoDrawer drawer) {
			this.ctx = ctx;
			this.cam = cam;
			this.drawer = drawer;
			this.controller = new InternalCameraController(cam);

			Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
			float a=0.5f;
			p.setColor(a, a, a, 1f);
			p.drawRectangle(0, 0, 64, 64);
			bgGrid = new Texture(p);
			p.dispose();

			this.addListener(new InputListener() {
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					getStage().setScrollFocus(SceneViewWidget.this);
					return controller.touchDown(Gdx.input.getX(), Gdx.input.getY(), pointer, button);
				}
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					controller.touchDragged(Gdx.input.getX(), Gdx.input.getY(), pointer);
				}
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					controller.touchUp(Gdx.input.getX(), Gdx.input.getY(), pointer, button);
				}
				public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
					return controller.scrolled(amountX, amountY);
				}
			});
		}

		public void update(float dt) {
			controller.update(dt);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			validate();

			// 0. 防御检查
			if (getWidth() <= 1 || getHeight() <= 1) {
				Debug.logT("TEST", "不画了: 控件太小", getWidth(), getHeight());
				return;
			}

			uiProjectionCache.set(batch.getProjectionMatrix());
			batch.end();

			// 1. 计算剪裁
			Vector2 screenPos = localToStageCoordinates(new Vector2(0, 0));
			Rectangle scissors = new Rectangle();
			Rectangle clipBounds = new Rectangle(screenPos.x, screenPos.y, getWidth(), getHeight());
			ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);

			// 【调试日志】全部走 TEST 通道
			Debug.logT("TEST", "=== SceneView 布局数据 ===", 
					   "Widget尺寸:", getWidth(), getHeight(), 
					   "屏幕坐标:", screenPos.x, screenPos.y,
					   "剪裁区域(Scissor):", scissors.x, scissors.y, scissors.width, scissors.height
					   );

			if (ScissorStack.pushScissors(scissors)) {
				// 2. 设置视口
				HdpiUtils.glViewport((int) scissors.x, (int) scissors.y, (int) scissors.width, (int) scissors.height);

				// 【改动】背景改成深红色 (0.5, 0, 0) 以便观察视口范围
				Gdx.gl.glClearColor(0.5f, 0.0f, 0.0f, 1f); 

				Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

				// 3. 同步相机
				cam.viewportWidth = scissors.width;
				cam.viewportHeight = scissors.height;
				cam.update();

				// 【调试日志】相机数据
				Debug.logT("TEST", "=== Camera 数据 ===", 
						   "Cam视口:", cam.viewportWidth, cam.viewportHeight,
						   "Cam位置:", cam.position.x, cam.position.y,
						   "Zoom:", cam.zoom
						   );

				// 4. 绘制游戏
				batch.setProjectionMatrix(cam.combined);
				batch.begin();

				drawGrid(batch);
				drawer.draw(batch, ctx.getSelection());

				batch.end();

				Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
				ScissorStack.popScissors();
			} else {
				Debug.logT("TEST", "Scissor Push 失败! 可能被完全遮挡或在屏幕外");
			}

			// 5. 恢复 UI
			HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.setProjectionMatrix(uiProjectionCache);
			batch.begin();
		}

		private void drawGrid(Batch b) {
			float z = cam.zoom;
			float w = cam.viewportWidth * z;
			float h = cam.viewportHeight * z;
			float startX = cam.position.x - w / 2;
			float startY = cam.position.y - h / 2;
			b.setColor(1, 1, 1, 0.3f);
			int cntX = (int) (w / 64) + 2;
			int cntY = (int) (h / 64) + 2;
			for (int i = 0; i < cntX; i++)
				for (int j = 0; j < cntY; j++)
					b.draw(bgGrid, startX - (startX % 64) + i * 64, startY - (startY % 64) + j * 64);
			b.setColor(Color.WHITE);
		}
	}

	private void setupFakeScene() {
		// 创建一个临时的白色纹理
		Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		TextureRegion whiteReg = new TextureRegion(new Texture(p));

		// 创建物体 1
		GObject p1 = new GObject("Hero");
		p1.transform.setPosition(0, 0);
		// 【关键】添加组件并设置贴图
		SpriteComponent s1 = p1.addComponent(SpriteComponent.class);
		s1.setRegion(whiteReg);
		s1.color = Color.CYAN; // 染成青色

		// 创建物体 2
		GObject p2 = new GObject("Monster");
		p2.transform.setPosition(100, 50);
		SpriteComponent s2 = p2.addComponent(SpriteComponent.class);
		s2.setRegion(whiteReg);
		s2.color = Color.RED; // 染成红色

		// 加入编辑器上下文
		context.fakeObjects.add(p1);
		context.fakeObjects.add(p2);
	}
}

