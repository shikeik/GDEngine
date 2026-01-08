package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.input.SmartColorInput;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTree;
import com.badlogic.gdx.scenes.scene2d.ui.Tree; // 引入基础 Tree 类

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Flat Icon 设计器原型 (Phase 1)
 * <p>
 * 包含：通用编辑器框架 + 基础形状实现 + 完整 Gizmo 交互
 * </p>
 */
public class IconEditorDemo extends GScreen {

	// --- 核心组件 ---
	private NeonBatch neonBatch;
	private Stage uiStage;
	private CameraController cameraController;

	// --- 编辑器子系统 ---
	private final EditorContext context = new EditorContext();
	private final GizmoSystem gizmoSystem = new GizmoSystem(context);
	private final EditorInput sceneInput = new EditorInput(this, context, gizmoSystem);
	private final Inspector inspector = new Inspector(context);

	// --- UI 引用 ---
	private VisTree<UiNode, EditorTarget> hierarchyTree;
	private VisTable inspectorTable;

	// --- 数据根节点 ---
	private EditorTarget rootNode;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	protected void initViewport() {
		// 使用 16:9 宽屏设计
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.2f : 1.5f;
		super.initViewport();
		// 编辑器不需要世界相机自动居中，我们有自己的控制逻辑
		autoCenterWorldCamera = false;
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

		// 1. 初始化相机
		worldCamera.zoom = 1.0f;
		worldCamera.position.set(0, 0, 0);
		cameraController = new CameraController(worldCamera);

		// 2. 初始化 UI Stage
		uiStage = new Stage(getUIViewport());

		// 3. 输入管线
		getImp().addProcessor(uiStage);
		getImp().addProcessor(sceneInput);
		getImp().addProcessor(cameraController);

		// 4. 构建测试场景
		buildTestScene();

		// 5. 构建 UI 布局
		buildLayout();

		// 初始选中
		if (rootNode.getChildren().size > 0) {
			selectNode(rootNode.getChildren().get(0));
		}
	}
	
	// [修复] 添加 Getter
    public CameraController getCameraController() {
		return cameraController;
	}

	private void buildTestScene() {
		rootNode = new GroupNode("Root");

		// 背景圆角矩形
		RectShape bg = new RectShape("Background");
		bg.width = 200; bg.height = 200; bg.color.set(Color.DARK_GRAY);
		rootNode.addChild(bg);

		// 中间的圆
		CircleShape circle = new CircleShape("Circle");
		circle.radius = 60; circle.color.set(Color.CYAN);
		rootNode.addChild(circle);

		// 装饰条
		RectShape bar = new RectShape("Bar");
		bar.width = 150; bar.height = 20; bar.y = -50; bar.color.set(Color.ORANGE);
		rootNode.addChild(bar);
	}

	// ========================================================================
	// UI Layout
	// ========================================================================

	private void buildLayout() {
		Table root = new Table();
		root.setFillParent(true);
		uiStage.addActor(root);

		// 1. 左侧：层级树
		VisTable leftPanel = new VisTable(true);
		leftPanel.setBackground("window-bg");

		VisTable leftToolbar = new VisTable();
		leftToolbar.add(new VisLabel("Hierarchy")).expandX().left();
		VisTextButton btnAdd = new VisTextButton("+");
		btnAdd.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { addNewShape(); } });
		leftToolbar.add(btnAdd);
		leftPanel.add(leftToolbar).growX().pad(5).row();

		hierarchyTree = new VisTree<>();
		hierarchyTree.getSelection().setProgrammaticChangeEvents(false);
		hierarchyTree.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					UiNode sel = hierarchyTree.getSelection().first();
					if (sel != null) selectNode((EditorTarget) sel.getValue());
					else selectNode(null);
				}
			});

		rebuildTree();
		leftPanel.add(new VisScrollPane(hierarchyTree)).grow();

		// 2. 右侧：属性面板
		VisTable rightPanel = new VisTable(true);
		rightPanel.setBackground("window-bg");
		rightPanel.add(new VisLabel("Properties")).pad(5).left().row();

		inspectorTable = new VisTable();
		inspectorTable.top().left();
		rightPanel.add(new VisScrollPane(inspectorTable)).grow();

		// 3. 主分割
		// 中间留空给 SceneView
		Table centerDummy = new Table(); 

		VisSplitPane rightSplit = new VisSplitPane(centerDummy, rightPanel, false);
		rightSplit.setSplitAmount(0.75f);

		VisSplitPane mainSplit = new VisSplitPane(leftPanel, rightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();

		// 顶部工具栏 (悬浮)
		Table topBar = new Table();
		topBar.top().left().pad(10);
		addToolBtn(topBar, "M", () -> gizmoSystem.mode = GizmoSystem.Mode.MOVE);
		addToolBtn(topBar, "R", () -> gizmoSystem.mode = GizmoSystem.Mode.ROTATE);
		addToolBtn(topBar, "S", () -> gizmoSystem.mode = GizmoSystem.Mode.SCALE);
		root.addActor(topBar);
	}

	private void addToolBtn(Table t, String text, Runnable act) {
		VisTextButton b = new VisTextButton(text);
		b.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { act.run(); } });
		t.add(b).size(30).padRight(5);
	}

	private void addNewShape() {
		// 简单演示：添加一个随机位置的圆
		CircleShape c = new CircleShape("New Circle");
		c.x = MathUtils.random(-50, 50);
		c.y = MathUtils.random(-50, 50);
		rootNode.addChild(c);
		rebuildTree();
		selectNode(c);
	}

	// ========================================================================
	// Logic & Render
	// ========================================================================

	public void selectNode(EditorTarget node) {
		context.selection = node;
		// 同步 UI
		inspector.build(inspectorTable, node);
		// 同步树选中状态 (略，需反向查找逻辑)
	}

	private void rebuildTree() {
		hierarchyTree.clearChildren();
		buildTreeRecursive(rootNode, null);
	}

	private void buildTreeRecursive(EditorTarget node, UiNode parent) {
		UiNode uiNode = new UiNode(new VisLabel(node.getName()));
		uiNode.setValue(node);
		if(parent == null) hierarchyTree.add(uiNode);
		else parent.add(uiNode);

		for(EditorTarget child : node.getChildren()) {
			buildTreeRecursive(child, uiNode);
		}
		uiNode.setExpanded(true);
	}

	@Override
	public void render0(float delta) {
		// 1. 更新
		cameraController.update(delta);

		// 2. 绘制场景
		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		// 网格
		drawGrid(neonBatch);

		// 绘制节点树
		drawNodeRecursive(rootNode, neonBatch);

		// 绘制 Gizmo
		gizmoSystem.render(neonBatch, getWorldCamera().zoom);

		neonBatch.end();

		// 3. 绘制 UI
		uiStage.act(delta);
		uiStage.draw();
	}

	private void drawNodeRecursive(EditorTarget node, NeonBatch batch) {
		node.render(batch);
		for(EditorTarget child : node.getChildren()) {
			drawNodeRecursive(child, batch);
		}
	}

	private void drawGrid(NeonBatch batch) {
		float size = 2000;
		float step = 100;
		Color c = new Color(1,1,1,0.1f);
		for(float i=-size; i<=size; i+=step) {
			batch.drawLine(i, -size, i, size, 1, c);
			batch.drawLine(-size, i, size, i, 1, c);
		}
		batch.drawLine(-size, 0, size, 0, 2, Color.GRAY);
		batch.drawLine(0, -size, 0, size, 2, Color.GRAY);
	}

	@Override
	public void dispose() {
		if(neonBatch!=null) neonBatch.dispose();
		if(uiStage!=null) uiStage.dispose();
	}

	// ========================================================================
	// 1. Core Interfaces & Base Classes
	// ========================================================================

	public interface EditorTarget {
		String getName();
		void setName(String name);

		// Transform
		float getX(); void setX(float v);
		float getY(); void setY(float v);
		float getRotation(); void setRotation(float v);
		float getScaleX(); void setScaleX(float v);
		float getScaleY(); void setScaleY(float v);

		// Hierarchy
		Array<EditorTarget> getChildren();
		void addChild(EditorTarget child);

		// Logic
		void render(NeonBatch batch);
		void inspect(Inspector inspector); // 让对象自己定义 UI
	}

	public static abstract class BaseNode implements EditorTarget {
		public String name;
		public float x, y, rotation = 0, scaleX = 1, scaleY = 1;
		public Array<EditorTarget> children = new Array<>();

		public BaseNode(String name) { this.name = name; }

		@Override public String getName() { return name; }
		@Override public void setName(String name) { this.name = name; }
		@Override public float getX() { return x; }
		@Override public void setX(float v) { x = v; }
		@Override public float getY() { return y; }
		@Override public void setY(float v) { y = v; }
		@Override public float getRotation() { return rotation; }
		@Override public void setRotation(float v) { rotation = v; }
		@Override public float getScaleX() { return scaleX; }
		@Override public void setScaleX(float v) { scaleX = v; }
		@Override public float getScaleY() { return scaleY; }
		@Override public void setScaleY(float v) { scaleY = v; }
		@Override public Array<EditorTarget> getChildren() { return children; }
		@Override public void addChild(EditorTarget child) { children.add(child); }

		@Override
		public void inspect(Inspector inspector) {
			inspector.addSection("Transform");
			inspector.addFloat("X", this::getX, this::setX);
			inspector.addFloat("Y", this::getY, this::setY);
			inspector.addFloat("Rot", this::getRotation, this::setRotation);
			inspector.addFloat("Scl X", this::getScaleX, this::setScaleX);
			inspector.addFloat("Scl Y", this::getScaleY, this::setScaleY);
		}
	}

	// 分组节点 (空节点)
	public static class GroupNode extends BaseNode {
		public GroupNode(String name) { super(name); }
		@Override public void render(NeonBatch batch) {}
	}

	// 矩形实现
	public static class RectShape extends BaseNode {
		public float width = 100, height = 100;
		public Color color = new Color(Color.WHITE);

		public RectShape(String name) { super(name); }

		@Override
		public void render(NeonBatch batch) {
			// 简单的矩阵变换逻辑 (实际项目可能需要更完善的 SceneGraph 矩阵传递)
			// 这里简单演示局部坐标渲染
			batch.drawRect(x - width/2*scaleX, y - height/2*scaleY, width*scaleX, height*scaleY, rotation, 0, color, true);
		}

		@Override
		public void inspect(Inspector inspector) {
			super.inspect(inspector); // 基础变换
			inspector.addSection("Rectangle");
			inspector.addFloat("Width", () -> width, v -> width = v);
			inspector.addFloat("Height", () -> height, v -> height = v);
			inspector.addColor("Color", () -> color, c -> color.set(c));
		}
	}

	// 圆形实现
	public static class CircleShape extends BaseNode {
		public float radius = 50;
		public Color color = new Color(Color.WHITE);

		public CircleShape(String name) { super(name); }

		@Override
		public void render(NeonBatch batch) {
			batch.drawCircle(x, y, radius * Math.max(Math.abs(scaleX), Math.abs(scaleY)), 0, color, 32, true);
		}

		@Override
		public void inspect(Inspector inspector) {
			super.inspect(inspector);
			inspector.addSection("Circle");
			inspector.addFloat("Radius", () -> radius, v -> radius = v);
			inspector.addColor("Color", () -> color, c -> color.set(c));
		}
	}

	// ========================================================================
	// 2. Editor Context & Systems
	// ========================================================================

	public static class EditorContext {
		public EditorTarget selection;
	}

	// 移植自 BioGizmoDrawer，适配 EditorTarget
	public static class GizmoSystem {
		public enum Mode { MOVE, ROTATE, SCALE }
		public Mode mode = Mode.MOVE;
		private final EditorContext ctx;

		// 视觉配置
		static float HANDLE_SIZE = 15f;
		static float AXIS_LEN = 60f;

		public GizmoSystem(EditorContext ctx) { this.ctx = ctx; }

		public void render(NeonBatch batch, float zoom) {
			EditorTarget t = ctx.selection;
			if (t == null) return;

			float x = t.getX();
			float y = t.getY();
			float rot = t.getRotation();
			float s = zoom * 1.2f; // 跟随缩放

			// 中心点
			batch.drawCircle(x, y, 5 * s, 0, Color.YELLOW, 8, true);

			if (mode == Mode.MOVE) {
				// X轴 (红)
				batch.drawLine(x, y, x + AXIS_LEN*s, y, 2*s, Color.RED);
				batch.drawRegularPolygon(x + AXIS_LEN*s, y, HANDLE_SIZE*s, 3, -90, 0, Color.RED, true); // 箭头
				// Y轴 (绿)
				batch.drawLine(x, y, x, y + AXIS_LEN*s, 2*s, Color.GREEN);
				batch.drawRegularPolygon(x, y + AXIS_LEN*s, HANDLE_SIZE*s, 3, 0, 0, Color.GREEN, true);
			} 
			else if (mode == Mode.ROTATE) {
				batch.drawCircle(x, y, AXIS_LEN*s, 2*s, Color.CYAN, 32, false);
				batch.drawCircle(x + AXIS_LEN*s, y, HANDLE_SIZE/2*s, 0, Color.YELLOW, 8, true); // Handle
			} 
			else if (mode == Mode.SCALE) {
				batch.drawLine(x, y, x + AXIS_LEN*s, y, 2*s, Color.RED);
				batch.drawRect(x + AXIS_LEN*s - 5*s, y - 5*s, 10*s, 10*s, 0, 0, Color.RED, true); // Box

				batch.drawLine(x, y, x, y + AXIS_LEN*s, 2*s, Color.GREEN);
				batch.drawRect(x - 5*s, y + AXIS_LEN*s - 5*s, 10*s, 10*s, 0, 0, Color.GREEN, true);
			}
		}
	}

	// 移植自 BioEditorInputProcessor
	public static class EditorInput extends InputAdapter {
		private final IconEditorDemo screen;
		private final EditorContext ctx;
		private final GizmoSystem gizmo;

		private boolean isDragging = false;
		private int dragAxis = 0; // 0=None, 1=X, 2=Y, 3=Both(Rot)
		private float lastX, lastY;

		public EditorInput(IconEditorDemo screen, EditorContext ctx, GizmoSystem gizmo) {
			this.screen = screen; this.ctx = ctx; this.gizmo = gizmo;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 worldPos = screen.screenToWorldCoord(screenX, screenY);

			if (ctx.selection != null) {
				// 简单的 Gizmo 命中检测 (简化版)
				float zoom = screen.getWorldCamera().zoom * 1.2f;
				float tX = ctx.selection.getX();
				float tY = ctx.selection.getY();
				float dist = Vector2.dst(worldPos.x, worldPos.y, tX, tY);

				// 假设点击了中心附近就开始拖拽
				if (dist < 30 * zoom) {
					isDragging = true;
					dragAxis = 3; 
					lastX = worldPos.x; lastY = worldPos.y;
					screen.getCameraController().setInputEnabled(false);
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (!isDragging || ctx.selection == null) return false;

			Vector2 worldPos = screen.screenToWorldCoord(screenX, screenY);
			float dx = worldPos.x - lastX;
			float dy = worldPos.y - lastY;

			if (gizmo.mode == GizmoSystem.Mode.MOVE) {
				ctx.selection.setX(ctx.selection.getX() + dx);
				ctx.selection.setY(ctx.selection.getY() + dy);
			} else if (gizmo.mode == GizmoSystem.Mode.ROTATE) {
				// 简单的 X 轴增量映射到旋转
				ctx.selection.setRotation(ctx.selection.getRotation() + dx * 0.5f);
			} else if (gizmo.mode == GizmoSystem.Mode.SCALE) {
				ctx.selection.setScaleX(ctx.selection.getScaleX() + dx * 0.01f);
				ctx.selection.setScaleY(ctx.selection.getScaleY() + dy * 0.01f);
			}

			// 刷新属性面板
			screen.inspector.refreshValues();

			lastX = worldPos.x;
			lastY = worldPos.y;
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (isDragging) {
				isDragging = false;
				screen.getCameraController().setInputEnabled(true);
				return true;
			}
			return false;
		}
	}

	// ========================================================================
	// 3. UI Generator (Inspector)
	// ========================================================================

	public static class Inspector {
		private VisTable container;
		private final EditorContext ctx;
		// 简单的属性刷新列表，用于拖拽时更新 UI
		private final Array<Runnable> refreshTasks = new Array<>();

		public Inspector(EditorContext ctx) { this.ctx = ctx; }

		public void build(VisTable table, EditorTarget target) {
			this.container = table;
			table.clearChildren();
			refreshTasks.clear();

			if (target == null) {
				table.add(new VisLabel("No Selection")).pad(10);
				return;
			}

			// Name
			VisTable nameRow = new VisTable();
			nameRow.add(new VisLabel("Name")).width(50);
			// 这里简单用 Label 显示，实际应为 TextField
			nameRow.add(new VisLabel(target.getName())).growX(); 
			table.add(nameRow).growX().pad(5).row();

			// 委托对象生成 UI
			target.inspect(this);
		}

		public void addSection(String title) {
			VisLabel l = new VisLabel(title);
			l.setColor(Color.CYAN);
			container.add(l).left().padTop(10).padBottom(5).row();
		}

		public void addFloat(String label, Supplier<Float> getter, Consumer<Float> setter) {
			SmartNumInput input = new SmartNumInput(label, getter.get(), 1.0f, setter);
			// 注册刷新任务
			refreshTasks.add(() -> {
				// SmartNumInput 没暴露 updateValue，这里只是演示架构
				// 实际 SmartNumInput 需要一个 setValue 方法来响应外部变化(Gizmo拖拽)
				// 暂时留空
			});
			container.add(input).growX().padBottom(2).row();
		}

		public void addColor(String label, Supplier<Color> getter, Consumer<Color> setter) {
			SmartColorInput input = new SmartColorInput(label, getter.get(), setter);
			container.add(input).growX().padBottom(2).row();
		}

		public void refreshValues() {
			// 在此调用所有 UI 组件的 updateFromModel()
			// 需要 SmartInput 支持
		}
	}
	
	// [修复] 定义一个具体的 Node 类，解决泛型和导入问题
    public static class UiNode extends Tree.Node<UiNode, EditorTarget, VisLabel> {
        public UiNode(VisLabel actor) {
            super(actor);
        }
    }

	// ========================================================================
	// 4. Utils (Internal Copies)
	// ========================================================================

	// 简易相机控制器 (如果不引用外部类)
	public static class CameraController extends InputAdapter {
		private final OrthographicCamera cam;
		private int lastX, lastY;
		private boolean enabled = true;

		public CameraController(OrthographicCamera cam) { this.cam = cam; }
		public void setInputEnabled(boolean v) { enabled = v; }

		public void update(float dt) {
			if(!enabled) return;
			float speed = 500 * dt * cam.zoom;
			if(Gdx.input.isKeyPressed(Input.Keys.A)) cam.translate(-speed, 0);
			if(Gdx.input.isKeyPressed(Input.Keys.D)) cam.translate(speed, 0);
			if(Gdx.input.isKeyPressed(Input.Keys.W)) cam.translate(0, speed);
			if(Gdx.input.isKeyPressed(Input.Keys.S)) cam.translate(0, -speed);
			cam.update();
		}

		@Override public boolean scrolled(float amountX, float amountY) {
			if(!enabled) return false;
			cam.zoom += amountY * 0.1f;
			cam.zoom = MathUtils.clamp(cam.zoom, 0.1f, 10f);
			return true;
		}

		@Override public boolean touchDown(int x, int y, int pointer, int button) {
			if(!enabled) return false;
			if(button == Input.Buttons.RIGHT) { lastX = x; lastY = y; return true; }
			return false;
		}

		@Override public boolean touchDragged(int x, int y, int pointer) {
			if(!enabled) return false;
			if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
				float z = cam.zoom;
				cam.translate(-(x - lastX)*z, (y - lastY)*z);
				lastX = x; lastY = y;
				return true;
			}
			return false;
		}
	}
}
