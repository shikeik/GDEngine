package com.goldsprite.solofight.screens.editor.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.neonbatch.NeonStage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisTree;
import com.goldsprite.solofight.game.SimpleCameraController;
import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.solofight.screens.editor.tests.SceneEditorScreen.SceneViewWidget;

/**
 * 【快速原型】Unity 风格编辑器布局测试
 * 包含：SceneView(裁剪视口), Hierarchy, Inspector, Project, Console
*/
public class SceneEditorScreen extends ExampleGScreen {

	private SceneEditorScreen.SceneViewWidget sceneWidget;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	// --- 模拟上下文 ---
	private EditorContext context;
	private SimpleGizmoDrawer gizmoDrawer;
	private ShapeRenderer shapeRenderer;

	private NeonStage uiStage;

	@Override
	public void create() {
		autoCenterWorldCamera = false;

		// 1. 初始化模拟数据
		context = new EditorContext();
		gizmoDrawer = new SimpleGizmoDrawer();
		shapeRenderer = new ShapeRenderer();

		// 2. 构建假场景数据 (用于 Hierarchy 和 SceneView)
		setupFakeScene();

		uiStage = new NeonStage();
		// 3. 构建 UI 布局 (核心)
		//buildUnityLayout();
	}

	// --- 布局构建区 ---

	private void buildUnityLayout() {

		VisTable root = new VisTable();
		root.setFillParent(true);
		root.setBackground("window-bg");
		uiStage.addActor(root);

		// A. 左侧栏 (Hierarchy + Project)
		VisTable leftPanel = new VisTable();
		VisTable hierarchyPanel = createHierarchyPanel();
		VisTable projectPanel = createProjectPanel();

		VisSplitPane leftSplit = new VisSplitPane(hierarchyPanel, projectPanel, true); // true = 垂直分割
		leftSplit.setSplitAmount(0.5f);
		leftPanel.add(leftSplit).grow();

		// B. 中间栏 (SceneView + Console)
		VisTable centerPanel = new VisTable();
		sceneWidget = new SceneViewWidget(context, worldCamera);
		VisTable consolePanel = createConsolePanel();

		VisSplitPane centerSplit = new VisSplitPane(sceneWidget, consolePanel, true);
		centerSplit.setSplitAmount(0.8f); // 场景占大头
		centerPanel.add(centerSplit).grow();

		// C. 右侧栏 (Inspector)
		VisTable rightPanel = createInspectorPanel();

		// D. 组合：左 + (中 + 右) ? 不，Unity 是 (左 + 中) + 右，或者 左 + 中 + 右
		// 这里用 SplitPane 嵌套来实现三列布局

		// 第一层分割：(左 + 中) vs 右
		VisSplitPane centerRightSplit = new VisSplitPane(centerPanel, rightPanel, false); // false = 水平
		centerRightSplit.setSplitAmount(0.75f); // 右边给 25%

		// 第二层分割：左 vs (中+右)
		VisSplitPane mainSplit = new VisSplitPane(leftPanel, centerRightSplit, false);
		mainSplit.setSplitAmount(0.2f); // 左边给 20%

		root.add(mainSplit).grow();
	}

	// --- 各个面板的构建逻辑 ---

	private VisTable createHierarchyPanel() {
		VisTable panel = new VisTable(true);
		panel.add(new VisLabel("Hierarchy")).left().pad(5).row();

		VisTree<Node, GObject> tree = new VisTree<>();

		// 填充假数据
		for (GObject obj : context.fakeObjects) {
			VisLabel lbl = new VisLabel(obj.getName());
			Node node = new Node(lbl);
			node.setValue(obj);
			tree.add(node);
		}

		// 监听点击：更新上下文选中项
		tree.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (!tree.getSelection().isEmpty()) {
					context.setSelection((GObject) tree.getSelection().first().getValue());
				}
			}
		});

		panel.add(new VisScrollPane(tree)).grow();
		return panel;
	}

	private VisTable createProjectPanel() {
		VisTable panel = new VisTable(true);
		panel.add(new VisLabel("Project")).left().pad(5).row();

		VisTree<Node, String> tree = new VisTree<>();
		Node root = new Node(new VisLabel("Assets"));
		root.add(new Node(new VisLabel("Sprites")));
		root.add(new Node(new VisLabel("Scripts")));
		root.add(new Node(new VisLabel("Prefabs")));
		root.setExpanded(true);
		tree.add(root);

		panel.add(new VisScrollPane(tree)).grow();
		return panel;
	}

	private VisTable createInspectorPanel() {
		VisTable panel = new VisTable(true);
		panel.add(new VisLabel("Inspector")).left().pad(5).row();

		// 用一个容器来装属性，方便刷新
		VisTable content = new VisTable();
		content.top().left();

		// 注册监听：当选中项改变时，刷新面板
		context.addListener(obj -> rebuildInspector(content, obj));

		panel.add(new VisScrollPane(content)).grow();
		return panel;
	}

	private VisTable createConsolePanel() {
		VisTable panel = new VisTable(true);
		panel.add(new VisLabel("Console")).left().pad(5).row();
		VisTextArea area = new VisTextArea("System Ready...\nEngine Initialized.\nWaiting for input...");
		area.setColor(Color.LIGHT_GRAY);
		panel.add(new VisScrollPane(area)).grow();
		return panel;
	}

	// --- 动态刷新 Inspector 逻辑 ---

	private void rebuildInspector(VisTable table, GObject obj) {
		table.clearChildren();
		if (obj == null)
			return;

		// 1. Header
		table.add(new VisLabel("Name")).left();
		table.add(new VisTextField(obj.getName())).growX().row();
		table.add(new VisTextButton("Active")).left().padBottom(10).row();

		// 2. Transform (假装是通用的)
		table.add(new VisLabel("Transform")).left().row();
		addPropRow(table, "Position X", obj.transform.position.x + "");
		addPropRow(table, "Position Y", obj.transform.position.y + "");
		addPropRow(table, "Rotation", obj.transform.rotation + "");
		addPropRow(table, "Scale", obj.transform.scale.x + "");
	}

	private void addPropRow(VisTable table, String name, String val) {
		VisTable row = new VisTable();
		row.add(new VisLabel(name)).width(80).left();
		row.add(new VisTextField(val)).growX();
		table.add(row).growX().padBottom(2).row();
	}

	// --- 内部类：上下文与假数据 ---

	private void setupFakeScene() {
		GameWorld.autoDispose();
		new GameWorld();
		GameWorld.inst().setReferences(getUIViewport(), worldCamera);

		// 创建几个假 GObject，给它们不同的坐标
		GObject p1 = new GObject("Player");
		p1.transform.setPosition(0, 0);

		GObject p2 = new GObject("Enemy_Orc");
		p2.transform.setPosition(100, 50);

		GObject p3 = new GObject("Tree_Big");
		p3.transform.setPosition(-80, -40);

		context.fakeObjects.add(p1);
		context.fakeObjects.add(p2);
		context.fakeObjects.add(p3);
	}

	public static class EditorContext {
		public com.badlogic.gdx.utils.Array<GObject> fakeObjects = new com.badlogic.gdx.utils.Array<>();
		private GObject selection;
		private com.badlogic.gdx.utils.Array<java.util.function.Consumer<GObject>> listeners = new com.badlogic.gdx.utils.Array<>();

		public void setSelection(GObject obj) {
			this.selection = obj;
			for (var l : listeners)
				l.accept(obj);
		}

		public GObject getSelection() {
			return selection;
		}
		public void addListener(java.util.function.Consumer<GObject> l) {
			listeners.add(l);
		}
	}

	public static class Node extends Tree.Node<Node, Object, VisLabel> {
		public Node(VisLabel actor) {
			super(actor);
		}
	}

	// --- 核心内部类：SceneViewWidget (视口裁剪) ---
	// --- 核心内部类：SceneViewWidget (视口裁剪 + 控制器集成) ---
	public class SceneViewWidget extends Widget {
		private EditorContext ctx;
		private OrthographicCamera cam;
		private Texture bgGrid;
		// 引用你的控制器
		private SimpleCameraController controller;

		public SceneViewWidget(EditorContext ctx, OrthographicCamera cam) {
			this.ctx = ctx;
			this.cam = cam;

			// 1. 实例化你的控制器
			this.controller = new SimpleCameraController(cam);

			// 生成网格纹理 (保持不变)
			Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
			p.setColor(0.3f, 0.3f, 0.3f, 1f);
			p.drawRectangle(0, 0, 64, 64);
			bgGrid = new Texture(p);
			p.dispose();

			// 2. 添加桥接监听器 (这就是解决红线的方法)
			this.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					// 关键：点击时获取焦点，这样滚轮才能生效
					getStage().setScrollFocus(SceneViewWidget.this);

					// 转发给控制器 (使用原始屏幕坐标 Gdx.input)
					return controller.touchDown(Gdx.input.getX(), Gdx.input.getY(), pointer, button);
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					controller.touchDragged(Gdx.input.getX(), Gdx.input.getY(), pointer);
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					controller.touchUp(Gdx.input.getX(), Gdx.input.getY(), pointer, button);
				}

				@Override
				public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
					return controller.scrolled(amountX, amountY);
				}
			});
		}

		// 必须把 update 暴露出来，因为你的控制器有键盘 WASD 逻辑
		public void update(float dt) {
			controller.update(dt);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			validate();

			// 1. 如果控件太小，直接不画，防止除以0或无效视口报错
			if (getWidth() <= 0 || getHeight() <= 0)
				return;

			batch.end(); // 暂停 UI 绘制

			// 2. 计算剪裁区域
			Vector2 screenPos = localToStageCoordinates(new Vector2(0, 0));
			Rectangle scissors = new Rectangle();
			Rectangle clipBounds = new Rectangle(screenPos.x, screenPos.y, getWidth(), getHeight());
			ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);

			// 【关键修复】必须判断 push 是否成功！
			if (ScissorStack.pushScissors(scissors)) {

				// --- 只有 push 成功了，才进行视口设置和渲染 ---

				// A. 设置 OpenGL 视口
				Gdx.gl.glViewport((int) scissors.x, (int) scissors.y, (int) scissors.width, (int) scissors.height);

				// B. 清除背景
				Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f);
				Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

				// C. 更新相机视口
				cam.viewportWidth = scissors.width;
				cam.viewportHeight = scissors.height;
				cam.update();

				// D. 绘制游戏世界
				Batch gameBatch = batch;
				gameBatch.setProjectionMatrix(cam.combined);
				gameBatch.begin();

				// ... 画网格 ...
				drawGrid(gameBatch, cam); // 抽出来或者写在里面

				// ... 画 Gizmo ...
				gizmoDrawer.draw(gameBatch, ctx.getSelection());

				gameBatch.end();

				// E. 恢复现场
				Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

				// 【关键修复】只有 push 成功了，才 pop！
				ScissorStack.popScissors();
			}

			// 恢复全屏视口 (无论 push 是否成功都要恢复，因为 glViewport 是全局状态)
			// 但为了安全，放在 if 外面更稳妥，或者在这里重置一下
			//HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			getUIViewport().apply(true);

			batch.begin(); // 恢复 UI 绘制
		}

		private void drawGrid(Batch gameBatch, OrthographicCamera cam) {
			// 画网格背景 (铺满视野)
			float z = cam.zoom;
			float startX = cam.position.x - cam.viewportWidth * z / 2;
			float startY = cam.position.y - cam.viewportHeight * z / 2;
			int countX = (int) ((cam.viewportWidth * z) / 64) + 2;
			int countY = (int) ((cam.viewportHeight * z) / 64) + 2;
			float offX = startX % 64;
			float offY = startY % 64;

			gameBatch.setColor(1, 1, 1, 0.5f);
			for (int i = -1; i < countX; i++) {
				for (int j = -1; j < countY; j++) {
					gameBatch.draw(bgGrid, startX - offX + i * 64, startY - offY + j * 64);
				}
			}
			gameBatch.setColor(Color.WHITE);
		}
	}

	// --- 简单的 Gizmo 画笔 (替代版) ---
	public class SimpleGizmoDrawer {
		Texture circleTex; // 没图就用圆圈代替

		public SimpleGizmoDrawer() {
			Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
			p.setColor(Color.YELLOW);
			p.drawCircle(16, 16, 14);
			circleTex = new Texture(p);
			p.dispose();
		}

		public void draw(Batch batch, GObject obj) {
			if (obj == null)
				return;
			float x = obj.transform.position.x;
			float y = obj.transform.position.y;

			// 画个黄色圆圈代表选中
			batch.draw(circleTex, x - 16, y - 16);

			// 画个十字架代表坐标
			// ... 简单画法，略 ...
		}
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0, 0, 0, 1f);

		// 【新增】每帧更新控制器，这样 WASD 键盘移动才能生效
		if (sceneWidget != null) {
			sceneWidget.update(delta);
		}

		GameWorld.inst().update(delta);

		uiStage.act(delta);
		uiStage.draw();
	}
}

