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
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
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
import com.goldsprite.gdengine.ui.widget.SimpleNumPad;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTree;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Flat Icon 设计器原型 (Phase 1 - Fix Gizmo)
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
	private final Inspector inspector = new Inspector(this, context);

	// --- UI 引用 ---
	private VisTree<UiNode, EditorTarget> hierarchyTree;
	private VisTable inspectorTable;

	// --- 数据根节点 ---
	private EditorTarget rootNode;

	// [新增] 拖拽管理器
    private DragAndDrop dragAndDrop;
	
	
	// [重构] 修复布局、菜单坐标、拖拽逻辑
	public static class UiNode extends Tree.Node<UiNode, EditorTarget, VisTable> {
		
		public UiNode(EditorTarget target, IconEditorDemo screen) {
			super(new VisTable() {
				@Override
				public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
					if (screen.hierarchyTree != null) {
						float targetWidth = screen.hierarchyTree.getWidth() - getX();
						if (targetWidth > 0 && getWidth() != targetWidth) {
							setWidth(targetWidth);
							invalidate(); 
							validate();
						}
					}
					super.draw(batch, parentAlpha);
				}
			});
			setValue(target);
			
			VisTable table = getActor();
			// 设为透明背景测试，或者给一个 hover listener 变色
			// table.setBackground("button"); 
			
			// 1. 名字 Label (左侧，填满剩余空间)
			VisLabel nameLbl = new VisLabel(target.getName());
			// 左键点击 -> 选中
			nameLbl.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					screen.selectNode(target);
				}
			});
			// 右键/长按 -> 菜单
			ActorGestureListener menuListener = new ActorGestureListener() {
				@Override
				public boolean longPress(Actor actor, float x, float y) {
					showMenu(); return true;
				}
				@Override
				public void tap(InputEvent event, float x, float y, int count, int button) {
					if (button == Input.Buttons.RIGHT) showMenu();
				}
				
				private void showMenu() {
					PopupMenu menu = new PopupMenu();
					
					// 1. Add Shape (SubMenu)
					MenuItem addItem = new MenuItem("Add Shape");
					PopupMenu subMenu = new PopupMenu();
					subMenu.addItem(new MenuItem("Circle", new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { screen.addNewShape(target, "Circle"); } }));
					subMenu.addItem(new MenuItem("Rectangle", new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { screen.addNewShape(target, "Rectangle"); } }));
					subMenu.addItem(new MenuItem("Group", new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { screen.addNewShape(target, "Group"); } }));
					addItem.setSubMenu(subMenu);
					menu.addItem(addItem);

					// 2. Delete (根节点不能删除)
					if (target != screen.rootNode) {
						MenuItem delItem = new MenuItem("Delete");
						delItem.getLabel().setColor(Color.RED);
						delItem.addListener(new ChangeListener() {
							@Override public void changed(ChangeEvent event, Actor actor) {
								screen.deleteNode(target);
							}
						});
						menu.addItem(delItem);
					}
					
					// [修复] 坐标系转换 Screen -> Stage
					Vector2 stagePos = screen.uiStage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
					menu.showMenu(screen.uiStage, stagePos.x, stagePos.y);
				}
			};
			nameLbl.addListener(menuListener);
			
			// [修复] 使用 expandX().fillX() 撑开宽度，实现左对齐文字，右对齐把手
			table.add(nameLbl).expandX().fillX().left().padLeft(5);
			
			// 2. 拖拽把手 (右侧)
			if (target != screen.rootNode) {
				VisLabel handle = new VisLabel("::"); 
				handle.setColor(Color.GRAY);
				
				// [修复] 增加交互反馈
				handle.addListener(new ClickListener() {
					@Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
						handle.setColor(Color.CYAN);
					}
					@Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
						handle.setColor(Color.GRAY);
					}
				});
				
				// [修复] 靠右对齐
				table.add(handle).right().padRight(10).width(20);
				
				// 注册拖拽源
				screen.dragAndDrop.addSource(new Source(handle) {
					@Override
					public Payload dragStart(InputEvent event, float x, float y, int pointer) {
						Payload payload = new Payload();
						payload.setObject(target);
						
						VisLabel dragActor = new VisLabel(target.getName());
						dragActor.setColor(Color.YELLOW);
						payload.setDragActor(dragActor);
						
						return payload;
					}
				});
			} else {
				// 根节点占位，保持对齐
				table.add().width(20).padRight(10);
			}
			
			// 注册拖拽目标
			screen.dragAndDrop.addTarget(new Target(table) {
				@Override
				public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
					EditorTarget dragging = (EditorTarget) payload.getObject();
					if (dragging == target) return false;
					if (isDescendant(dragging, target)) return false;
					
					getActor().setColor(Color.CYAN);
					return true;
				}

				@Override
				public void drop(Source source, Payload payload, float x, float y, int pointer) {
					EditorTarget dragging = (EditorTarget) payload.getObject();
					if (dragging == target) return;

					float h = getActor().getHeight();
					boolean handled = false;
					
					// Insert Before (Top 25%)
					if (y > h * 0.75f && target.getParent() != null) {
						EditorTarget parent = target.getParent();
						dragging.setParent(parent);
						int targetIndex = parent.getChildren().indexOf(target, true);
						parent.getChildren().removeValue(dragging, true);
						if (targetIndex >= 0) parent.getChildren().insert(targetIndex, dragging);
						else parent.getChildren().add(dragging);
						handled = true;
					} 
					// Insert After (Bottom 25%)
					else if (y < h * 0.25f && target.getParent() != null) {
						EditorTarget parent = target.getParent();
						dragging.setParent(parent);
						int targetIndex = parent.getChildren().indexOf(target, true);
						parent.getChildren().removeValue(dragging, true);
						int newIndex = targetIndex + 1;
						if (newIndex > parent.getChildren().size) newIndex = parent.getChildren().size;
						if (targetIndex >= 0) parent.getChildren().insert(newIndex, dragging);
						else parent.getChildren().add(dragging);
						handled = true;
					}
					
					if (!handled) {
						dragging.setParent(target);
					}
					
					screen.rebuildTree();
					screen.selectNode(dragging);
				}
				
				@Override
				public void reset(Source source, Payload payload) {
					getActor().setColor(Color.WHITE);
				}
			});
		}
		
		private boolean isDescendant(EditorTarget root, EditorTarget check) {
			if (root == check) return true;
			for (EditorTarget child : root.getChildren()) {
				if (isDescendant(child, check)) return true;
			}
			return false;
		}
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	protected void initViewport() {
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.2f : 1.5f;
		super.initViewport();
		autoCenterWorldCamera = false;
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

		worldCamera.zoom = 1.0f;
		worldCamera.position.set(0, 0, 0);
		cameraController = new CameraController(worldCamera);

		uiStage = new Stage(getUIViewport());
		
		// [新增] 初始化拖拽系统
        dragAndDrop = new DragAndDrop();
        dragAndDrop.setDragActorPosition(0, 0); // 拖拽时图标跟随鼠标偏移

		getImp().addProcessor(uiStage);
		getImp().addProcessor(sceneInput);
		getImp().addProcessor(cameraController);

		buildTestScene();
		buildLayout();

		if (rootNode.getChildren().size > 0) {
			selectNode(rootNode.getChildren().get(0));
		}
	}

	public CameraController getCameraController() { return cameraController; }

	private void buildTestScene() {
		rootNode = new GroupNode("Root");

		RectShape bg = new RectShape("Background");
		bg.width = 200; bg.height = 200; bg.color.set(Color.DARK_GRAY);
		bg.setParent(rootNode);

		CircleShape circle = new CircleShape("Circle");
		circle.radius = 60; circle.color.set(Color.CYAN);
		circle.setParent(rootNode);

		RectShape bar = new RectShape("Bar");
		bar.width = 150; bar.height = 20; bar.y = -50; bar.color.set(Color.ORANGE);
		bar.setParent(rootNode);
	}

	// ========================================================================
	// UI Layout (修复工具栏布局)
	// ========================================================================

	private void buildLayout() {
		Table root = new Table();
		root.setFillParent(true);
		uiStage.addActor(root);

		// 1. 左侧面板
		VisTable leftPanel = new VisTable(true);
		leftPanel.setBackground("window-bg");

		VisTable leftToolbar = new VisTable();
		leftToolbar.add(new VisLabel("Hierarchy")).expandX().left();
		VisTextButton btnAdd = new VisTextButton("+");
		btnAdd.addListener(new ClickListener() { 
			@Override public void clicked(InputEvent e, float x, float y) { 
				PopupMenu menu = new PopupMenu();
				menu.addItem(new MenuItem("Circle", new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { addNewShape(rootNode, "Circle"); } }));
				menu.addItem(new MenuItem("Rectangle", new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { addNewShape(rootNode, "Rectangle"); } }));
				menu.addItem(new MenuItem("Group", new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { addNewShape(rootNode, "Group"); } }));
				menu.showMenu(uiStage, e.getStageX(), e.getStageY());
			} 
		});
		leftToolbar.add(btnAdd);
		leftPanel.add(leftToolbar).growX().pad(5).row();

		hierarchyTree = new VisTree<>();
		// [核心修复] 增加缩进宽度，让层级更明显
		hierarchyTree.setIndentSpacing(25f); 
		hierarchyTree.getSelection().setProgrammaticChangeEvents(false);
		hierarchyTree.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					UiNode sel = hierarchyTree.getSelection().first();
					if (sel != null) selectNode(sel.getValue());
					else selectNode(null);
				}
			});

		rebuildTree();
		leftPanel.add(new VisScrollPane(hierarchyTree)).grow();

		// 2. 右侧面板
		VisTable rightPanel = new VisTable(true);
		rightPanel.setBackground("window-bg");
		rightPanel.add(new VisLabel("Properties")).pad(5).left().row();

		inspectorTable = new VisTable();
		inspectorTable.top().left();
		rightPanel.add(new VisScrollPane(inspectorTable)).grow();

		// 3. 中间区域 (Stack 布局：视口占位符 + 悬浮工具栏)
		Stack centerStack = new Stack();

		// 占位符 (让出渲染空间)
		Widget viewPlaceholder = new Widget(); 
		centerStack.add(viewPlaceholder);

		// 工具栏 (左上角悬浮)
		Table topBar = new Table();
		topBar.top().left().pad(10);
		addToolBtn(topBar, "M", () -> gizmoSystem.mode = GizmoSystem.Mode.MOVE);
		addToolBtn(topBar, "R", () -> gizmoSystem.mode = GizmoSystem.Mode.ROTATE);
		addToolBtn(topBar, "S", () -> gizmoSystem.mode = GizmoSystem.Mode.SCALE);
		centerStack.add(topBar);

		// 4. 分割窗格组合
		VisSplitPane rightSplit = new VisSplitPane(centerStack, rightPanel, false);
		rightSplit.setSplitAmount(0.75f);

		VisSplitPane mainSplit = new VisSplitPane(leftPanel, rightSplit, false);
		mainSplit.setSplitAmount(0.2f);

		root.add(mainSplit).grow();
	}

	private void addToolBtn(Table t, String text, Runnable act) {
		VisTextButton b = new VisTextButton(text);
		b.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { act.run(); } });
		t.add(b).size(30).padRight(5);
	}

	public void addNewShape(EditorTarget parent, String type) {
		// 1. 确定父节点
		if (parent == null) parent = rootNode;

		// 2. 创建
		EditorTarget newShape = null;
		if ("Circle".equals(type)) {
			CircleShape c = new CircleShape("New Circle");
			c.radius = 40;
			newShape = c;
		} else if ("Rectangle".equals(type)) {
			RectShape r = new RectShape("New Rect");
			r.width = 80; r.height = 80;
			newShape = r;
		} else if ("Group".equals(type)) {
			newShape = new GroupNode("New Group");
		}
		
		if (newShape != null) {
			// 随机位置避免重叠 (仅对非 Group 有意义，但统一处理也没问题)
			newShape.setX(MathUtils.random(-20, 20));
			newShape.setY(MathUtils.random(-20, 20));
			
			// 3. 建立关系
			newShape.setParent(parent);
			
			// 4. 刷新
			rebuildTree();
			selectNode(newShape);
		}
	}

	public void changeNodeType(EditorTarget target, String newType) {
		if (target == null || target == rootNode) return;
		
		// 1. Create new node
		EditorTarget newNode = null;
		if ("Circle".equals(newType)) {
			newNode = new CircleShape(target.getName());
			if (target instanceof CircleShape) ((CircleShape)newNode).radius = ((CircleShape)target).radius;
			if (target instanceof RectShape) ((CircleShape)newNode).color.set(((RectShape)target).color);
			if (target instanceof CircleShape) ((CircleShape)newNode).color.set(((CircleShape)target).color);
		} else if ("Rectangle".equals(newType)) {
			newNode = new RectShape(target.getName());
			if (target instanceof RectShape) {
				((RectShape)newNode).width = ((RectShape)target).width;
				((RectShape)newNode).height = ((RectShape)target).height;
			}
			if (target instanceof RectShape) ((RectShape)newNode).color.set(((RectShape)target).color);
			if (target instanceof CircleShape) ((RectShape)newNode).color.set(((CircleShape)target).color);
		} else if ("Group".equals(newType)) {
			newNode = new GroupNode(target.getName());
		}
		
		if (newNode == null) return;
		
		// 2. Copy properties
		newNode.setX(target.getX());
		newNode.setY(target.getY());
		newNode.setRotation(target.getRotation());
		newNode.setScaleX(target.getScaleX());
		newNode.setScaleY(target.getScaleY());
		
		// 3. Move children
		Array<EditorTarget> children = new Array<>(target.getChildren());
		for (EditorTarget child : children) {
			child.setParent(newNode);
		}
		
		// 4. Replace in parent
		EditorTarget parent = target.getParent();
		if (parent != null) {
			int index = parent.getChildren().indexOf(target, true);
			target.removeFromParent();
			newNode.setParent(parent); // This appends
			// Fix order
			parent.getChildren().removeValue(newNode, true);
			parent.getChildren().insert(index, newNode);
		}
		
		// 5. Refresh
		rebuildTree();
		selectNode(newNode);
	}

	// [修复] 确保删除逻辑健壮
	public void deleteNode(EditorTarget node) {
		if (node == null || node == rootNode) return;
		Gdx.app.log("Editor", "Deleting node: " + node.getName());
		// 1. 数据层移除
		node.removeFromParent();
		// 2. 如果删除的是当前选中的，清空选中
		if (context.selection == node) {
			selectNode(null);
		}
		// 3. UI 刷新
		rebuildTree();
	}

	// ========================================================================
	// Logic & Render
	// ========================================================================

	public void selectNode(EditorTarget node) {
		context.selection = node;
		inspector.build(inspectorTable, node);
		
		// [修复] 同步 Hierarchy 选中状态
		if (hierarchyTree != null) {
			UiNode uiNode = hierarchyTree.findNode(node);
			if (uiNode != null) {
				if (!hierarchyTree.getSelection().contains(uiNode)) {
					hierarchyTree.getSelection().set(uiNode);
				}
			} else {
				hierarchyTree.getSelection().clear();
			}
		}
	}

	// [修复] 重建树前清理 DragAndDrop
	private void rebuildTree() {
		// 1. 清理 UI
		hierarchyTree.clearChildren();
		// 2. [关键] 清理旧的拖拽源和目标，防止幽灵节点
		if (dragAndDrop != null) {
			dragAndDrop.clear();
		}
		// 3. 重建
		buildTreeRecursive(rootNode, null);
		
		// 4. [修复] 恢复选中状态
		if (context.selection != null) {
			selectNode(context.selection);
		}
	}

	private void buildTreeRecursive(EditorTarget node, UiNode parent) {
		// [修改] 传入 this (screen 实例)
		UiNode uiNode = new UiNode(node, this);

		if(parent == null) hierarchyTree.add(uiNode);
		else parent.add(uiNode);

		for(EditorTarget child : node.getChildren()) {
			buildTreeRecursive(child, uiNode);
		}
		uiNode.setExpanded(true);
	}

	@Override
	public void render0(float delta) {
		cameraController.update(delta);

		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		drawGrid(neonBatch);
		drawNodeRecursive(rootNode, neonBatch);

		// [修复] Gizmo 渲染
		gizmoSystem.render(neonBatch, getWorldCamera().zoom);

		neonBatch.end();

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
	// 1. Interfaces & Data
	// ========================================================================

	public interface EditorTarget {
		String getName();
		void setName(String name);
		float getX(); void setX(float v);
		float getY(); void setY(float v);
		float getRotation(); void setRotation(float v);
		float getScaleX(); void setScaleX(float v);
		float getScaleY(); void setScaleY(float v);
		// [新增] 亲缘关系
		EditorTarget getParent();
		void setParent(EditorTarget parent);
		void removeFromParent(); // 从父级移除自己
		Array<EditorTarget> getChildren();
		void addChild(EditorTarget child); // 仅添加数据，不处理逻辑
		void render(NeonBatch batch);
		void inspect(Inspector inspector);
	}

	public static abstract class BaseNode implements EditorTarget {
		public String name;
		public float x, y, rotation = 0, scaleX = 1, scaleY = 1;
		// [新增] 父节点引用
		protected EditorTarget parent;
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
		// [新增] 实现亲缘管理
		@Override public EditorTarget getParent() { return parent; }

		@Override public void setParent(EditorTarget newParent) {
			if (this.parent == newParent) return;

			// 1. 从旧父级移除
			if (this.parent != null) {
				this.parent.getChildren().removeValue(this, true);
			}

			this.parent = newParent;

			// 2. 加入新父级
			if (newParent != null) {
				newParent.addChild(this);
			}
		}

		@Override public void removeFromParent() {
			setParent(null);
		}
		
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

	public static class GroupNode extends BaseNode {
		public GroupNode(String name) { super(name); }
		@Override public void render(NeonBatch batch) {}
	}

	public static class RectShape extends BaseNode {
		public float width = 100, height = 100;
		public Color color = new Color(Color.WHITE);
		public RectShape(String name) { super(name); }
		@Override public void render(NeonBatch batch) {
			batch.drawRect(x - width/2*scaleX, y - height/2*scaleY, width*scaleX, height*scaleY, rotation, 0, color, true);
		}
		@Override public void inspect(Inspector inspector) {
			super.inspect(inspector);
			inspector.addSection("Rectangle");
			inspector.addFloat("Width", () -> width, v -> width = v);
			inspector.addFloat("Height", () -> height, v -> height = v);
			inspector.addColor("Color", () -> color, c -> color.set(c));
		}
	}

	public static class CircleShape extends BaseNode {
		public float radius = 50;
		public Color color = new Color(Color.WHITE);
		public CircleShape(String name) { super(name); }
		@Override public void render(NeonBatch batch) {
			batch.drawCircle(x, y, radius * Math.max(Math.abs(scaleX), Math.abs(scaleY)), 0, color, 32, true);
		}
		@Override public void inspect(Inspector inspector) {
			super.inspect(inspector);
			inspector.addSection("Circle");
			inspector.addFloat("Radius", () -> radius, v -> radius = v);
			inspector.addColor("Color", () -> color, c -> color.set(c));
		}
	}

	// ========================================================================
	// 2. Editor Context & Systems (修复 Gizmo 绘制)
	// ========================================================================

	public static class EditorContext {
		public EditorTarget selection;
	}

	public static class GizmoSystem {
		public enum Mode { MOVE, ROTATE, SCALE }
		public Mode mode = Mode.MOVE;
		private final EditorContext ctx;

		// [复刻] 视觉配置 (参考 BioGizmoDrawer)
		static float HANDLE_SIZE = 15f;
		static float AXIS_LEN = 60f;
		static float GIZMO_OUTLINE_WIDTH = 0.8f;

		// 缓存数组 (绘制箭头用)
		private static final float[] tmpPoly = new float[8];

		public GizmoSystem(EditorContext ctx) { this.ctx = ctx; }

		public void render(NeonBatch batch, float zoom) {
			EditorTarget t = ctx.selection;
			if (t == null) return;

			float x = t.getX();
			float y = t.getY();
			float rot = t.getRotation();
			float s = zoom * 1.4f; // 缩放系数调整

			// 1. 中心点
			drawDualCircle(batch, s, x, y, 5f * s, Color.YELLOW, true);

			float rad = rot * MathUtils.degreesToRadians;
			float cos = MathUtils.cos(rad);
			float sin = MathUtils.sin(rad);
			float centerDist = AXIS_LEN * s;

			// 2. 根据模式绘制手柄
			if (mode == Mode.MOVE) {
				float arrowSize = 14f * s;
				float halfArrow = arrowSize * 0.6f;
				float lineLen = centerDist - halfArrow;

				// X轴 (红)
				float endXx = x + cos * lineLen;
				float endXy = y + sin * lineLen;
				drawDualLine(batch, s, x, y, endXx, endXy, 2f * s, Color.RED);
				drawArrowHead(batch, s, endXx, endXy, rot, arrowSize, Color.RED);

				// Y轴 (绿)
				float endYx = x - sin * lineLen;
				float endYy = y + cos * lineLen;
				drawDualLine(batch, s, x, y, endYx, endYy, 2f * s, Color.GREEN);
				drawArrowHead(batch, s, endYx, endYy, rot + 90f, arrowSize, Color.GREEN);
			} 
			else if (mode == Mode.ROTATE) {
				float hx = x + cos * centerDist;
				float hy = y + sin * centerDist;
				drawDualLine(batch, s, x, y, hx, hy, 1.5f * s, Color.YELLOW);
				drawDualCircle(batch, s, x, y, centerDist, Color.CYAN, false); // 大圆环
				drawDualCircle(batch, s, hx, hy, HANDLE_SIZE/2 * s, Color.YELLOW, true); // 手柄
			} 
			else if (mode == Mode.SCALE) {
				float boxSize = 10f * s;

				// X轴
				float endXx = x + cos * centerDist;
				float endXy = y + sin * centerDist;
				drawDualLine(batch, s, x, y, endXx, endXy, 1.5f * s, Color.RED);
				drawDualRect(batch, s, endXx, endXy, boxSize, boxSize, rot, Color.RED);

				// Y轴
				float endYx = x - sin * centerDist;
				float endYy = y + cos * centerDist;
				drawDualLine(batch, s, x, y, endYx, endYy, 1.5f * s, Color.GREEN);
				drawDualRect(batch, s, endYx, endYy, boxSize, boxSize, rot, Color.GREEN);
			}
		}

		// --- 绘图辅助 (移植自 BioGizmoDrawer) ---

		private void drawDualLine(NeonBatch batch, float s, float x1, float y1, float x2, float y2, float w, Color c) {
			batch.drawLine(x1, y1, x2, y2, w + GIZMO_OUTLINE_WIDTH*s*2, Color.WHITE); // 包边
			batch.drawLine(x1, y1, x2, y2, w, c);
		}

		private void drawDualCircle(NeonBatch batch, float s, float x, float y, float r, Color c, boolean fill) {
			float w = 1.5f * s;
			if(fill) {
				batch.drawCircle(x, y, r + GIZMO_OUTLINE_WIDTH*s, 0, Color.WHITE, 16, true);
				batch.drawCircle(x, y, r, 0, c, 16, true);
			} else {
				batch.drawCircle(x, y, r, w + GIZMO_OUTLINE_WIDTH*s*2, Color.WHITE, 32, false);
				batch.drawCircle(x, y, r, w, c, 32, false);
			}
		}int k;

		private void drawDualRect(NeonBatch batch, float s, float cx, float cy, float w, float h, float rot, Color c) {
			float ow = GIZMO_OUTLINE_WIDTH * s;
			float outW = w + ow * 2;
			float outH = h + ow * 2;
			batch.drawRect(cx - outW/2f, cy - outH/2f, outW, outH, rot, 0, Color.WHITE, true);
			batch.drawRect(cx - w/2f, cy - h/2f, w, h, rot, 0, c, true);
		}

		private void drawArrowHead(NeonBatch batch, float s, float bx, float by, float angleDeg, float size, Color c) {
			float rad = angleDeg * MathUtils.degreesToRadians;
			float cos = MathUtils.cos(rad);
			float sin = MathUtils.sin(rad);

			float tipX = bx + cos * size;
			float tipY = by + sin * size;
			float halfW = size * 0.5f;

			float p1x = bx - sin * halfW;
			float p1y = by + cos * halfW;
			float p2x = bx + sin * halfW;
			float p2y = by - cos * halfW;

			tmpPoly[0] = tipX; tmpPoly[1] = tipY;
			tmpPoly[2] = p1x;  tmpPoly[3] = p1y;
			tmpPoly[4] = p2x;  tmpPoly[5] = p2y;

			// 描边模拟
			float ow = GIZMO_OUTLINE_WIDTH * s * 2f;
			batch.drawPolygon(tmpPoly, 3, 0, Color.WHITE, true); // White BG
			batch.drawPolygon(tmpPoly, 3, ow, Color.WHITE, false); // White Outline
			batch.drawPolygon(tmpPoly, 3, 0, c, true); // Inner Color
		}
	}

	// ========================================================================
	// 3. Editor Input (修复交互逻辑)
	// ========================================================================

	public static class EditorInput extends InputAdapter {
		private final IconEditorDemo screen;
		private final EditorContext ctx;
		private final GizmoSystem gizmo;

		private enum DragMode { NONE, BODY, ROTATE, SCALE_X, SCALE_Y }
		private DragMode currentDragMode = DragMode.NONE;

		private float lastX, lastY;
		private float startValX, startValY, startValRot; // 记录初始值用于精确计算

		public EditorInput(IconEditorDemo screen, EditorContext ctx, GizmoSystem gizmo) {
			this.screen = screen; this.ctx = ctx; this.gizmo = gizmo;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 wPos = screen.screenToWorldCoord(screenX, screenY);
			EditorTarget t = ctx.selection;

			if (t != null) {
				float zoom = screen.getWorldCamera().zoom * 1.4f; // 匹配渲染缩放
				float tx = t.getX(), ty = t.getY(), rot = t.getRotation();
				float rad = rot * MathUtils.degreesToRadians;
				float c = MathUtils.cos(rad), s = MathUtils.sin(rad);
				float axisLen = GizmoSystem.AXIS_LEN * zoom;

				// Hit Threshold
				float hitR = 15f * zoom;

				// [复刻] 碰撞检测逻辑
				if (gizmo.mode == GizmoSystem.Mode.MOVE) {
					// X Axis Tip
					float xx = tx + c * axisLen;
					float xy = ty + s * axisLen;
					if (Vector2.dst(wPos.x, wPos.y, xx, xy) < hitR) {
						startDrag(DragMode.BODY, wPos); return true; // 暂简化为Body拖拽，如需轴向锁定可扩展
					}
					// Y Axis Tip
					float yx = tx - s * axisLen;
					float yy = ty + c * axisLen;
					if (Vector2.dst(wPos.x, wPos.y, yx, yy) < hitR) {
						startDrag(DragMode.BODY, wPos); return true;
					}
				}
				else if (gizmo.mode == GizmoSystem.Mode.ROTATE) {
					float hx = tx + c * axisLen;
					float hy = ty + s * axisLen;
					if (Vector2.dst(wPos.x, wPos.y, hx, hy) < hitR) {
						startDrag(DragMode.ROTATE, wPos); return true;
					}
				}
				else if (gizmo.mode == GizmoSystem.Mode.SCALE) {
					// X Scale Handle
					float xx = tx + c * axisLen;
					float xy = ty + s * axisLen;
					if (Vector2.dst(wPos.x, wPos.y, xx, xy) < hitR) {
						startDrag(DragMode.SCALE_X, wPos); return true;
					}
					// Y Scale Handle
					float yx = tx - s * axisLen;
					float yy = ty + c * axisLen;
					if (Vector2.dst(wPos.x, wPos.y, yx, yy) < hitR) {
						startDrag(DragMode.SCALE_Y, wPos); return true;
					}
				}

				// Body Hit (Center)
				if (Vector2.dst(wPos.x, wPos.y, tx, ty) < 20 * zoom) {
					startDrag(DragMode.BODY, wPos); return true;
				}
			}
			return false;
		}

		private void startDrag(DragMode mode, Vector2 pos) {
			currentDragMode = mode;
			lastX = pos.x; lastY = pos.y;
			screen.getCameraController().setInputEnabled(false);

			if(ctx.selection != null) {
				startValX = ctx.selection.getScaleX();
				startValY = ctx.selection.getScaleY();
				startValRot = ctx.selection.getRotation();
			}
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (currentDragMode == DragMode.NONE || ctx.selection == null) return false;

			Vector2 wPos = screen.screenToWorldCoord(screenX, screenY);
			float dx = wPos.x - lastX;
			float dy = wPos.y - lastY;

			EditorTarget t = ctx.selection;

			if (currentDragMode == DragMode.BODY) {
				t.setX(t.getX() + dx);
				t.setY(t.getY() + dy);
			} 
			else if (currentDragMode == DragMode.ROTATE) {
				// 计算角度差 (BioWar Logic: atan2 based)
				float oldAng = MathUtils.atan2(lastY - t.getY(), lastX - t.getX()) * MathUtils.radiansToDegrees;
				float newAng = MathUtils.atan2(wPos.y - t.getY(), wPos.x - t.getX()) * MathUtils.radiansToDegrees;
				float deltaRot = newAng - oldAng;
				t.setRotation(t.getRotation() + deltaRot);
			} 
			else if (currentDragMode == DragMode.SCALE_X || currentDragMode == DragMode.SCALE_Y) {
				// 投影增量到轴向 (Project delta onto local axis)
				float rad = t.getRotation() * MathUtils.degreesToRadians;
				float c = MathUtils.cos(rad), s = MathUtils.sin(rad);
				float axisLen = GizmoSystem.AXIS_LEN * screen.getWorldCamera().zoom * 1.4f; // 归一化参考

				if (currentDragMode == DragMode.SCALE_X) {
					// Dot product with X axis (c, s)
					float proj = dx * c + dy * s;
					t.setScaleX(t.getScaleX() + (proj / axisLen) * startValX); // 相对比例增加
				} else {
					// Dot product with Y axis (-s, c)
					float proj = dx * (-s) + dy * c;
					t.setScaleY(t.getScaleY() + (proj / axisLen) * startValY);
				}
			}

			screen.inspector.refreshValues(); // 刷新 UI 数值

			lastX = wPos.x;
			lastY = wPos.y;
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (currentDragMode != DragMode.NONE) {
				currentDragMode = DragMode.NONE;
				screen.getCameraController().setInputEnabled(true);
				return true;
			}
			return false;
		}
	}

	// ========================================================================
	// 4. Inspector (保持不变，略)
	// ========================================================================
	// ... (Inspector 代码保持原样) ...
	public static class Inspector {
		private VisTable container;
		private final IconEditorDemo screen;
		private final EditorContext ctx;
		private final Array<Runnable> refreshTasks = new Array<>();

		public Inspector(IconEditorDemo screen, EditorContext ctx) { 
			this.screen = screen;
			this.ctx = ctx; 
		}

		public void build(VisTable table, EditorTarget target) {
			this.container = table;
			table.clearChildren();
			refreshTasks.clear();

			if (target == null) {
				table.add(new VisLabel("No Selection")).pad(10);
				return;
			}

			VisTable nameRow = new VisTable();
			nameRow.add(new VisLabel("Name")).width(50);
			VisTextField nameField = new VisTextField(target.getName());
			nameField.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					target.setName(nameField.getText());
					// Update Tree Label
					if (screen.hierarchyTree != null) {
						UiNode node = screen.hierarchyTree.findNode(target);
						if (node != null && node.getActor().getChildren().size > 0) {
							Actor labelActor = node.getActor().getChildren().get(0);
							if (labelActor instanceof VisLabel) {
								((VisLabel) labelActor).setText(target.getName());
							}
						}
					}
				}
			});
			nameRow.add(nameField).growX(); 
			table.add(nameRow).growX().pad(5).row();

			// Type Selection
			if (target != screen.rootNode) {
				VisTable typeRow = new VisTable();
				typeRow.add(new VisLabel("Type")).width(50);
				
				VisSelectBox<String> typeBox = new VisSelectBox<>();
				typeBox.setItems("Group", "Rectangle", "Circle");
				
				String currentType = "Group";
				if (target instanceof RectShape) currentType = "Rectangle";
				else if (target instanceof CircleShape) currentType = "Circle";
				typeBox.setSelected(currentType);
				
				typeBox.addListener(new ChangeListener() {
					@Override public void changed(ChangeEvent event, Actor actor) {
						String newType = typeBox.getSelected();
						// Avoid trigger on init or same type
						if (target instanceof RectShape && "Rectangle".equals(newType)) return;
						if (target instanceof CircleShape && "Circle".equals(newType)) return;
						if (target instanceof GroupNode && "Group".equals(newType)) return;
						
						screen.changeNodeType(target, newType);
					}
				});
				
				typeRow.add(typeBox).growX();
				table.add(typeRow).growX().pad(5).row();
			}

			target.inspect(this);
		}

		public void addSection(String title) {
			VisLabel l = new VisLabel(title);
			l.setColor(Color.CYAN);
			container.add(l).left().padTop(10).padBottom(5).row();
		}

		public void addFloat(String label, Supplier<Float> getter, Consumer<Float> setter) {
			SmartNumInput input = new SmartNumInput(label, getter.get(), 1.0f, setter);
			// 简单的刷新逻辑：闭包捕获 getter 和 input
			refreshTasks.add(() -> {
				// 这里的 SmartNumInput 没有暴露 updateValue 接口给外部调用
				// 如果需要实时刷新 UI (拖拽 Gizmo 时)，需要给 SmartNumInput 加一个 setValue(float, notify=false)
				// 暂时忽略，或者你可以手动重建整个 Inspector
			});
			container.add(input).growX().padBottom(2).row();
		}

		public void addColor(String label, Supplier<Color> getter, Consumer<Color> setter) {
			SmartColorInput input = new SmartColorInput(label, getter.get(), setter);
			container.add(input).growX().padBottom(2).row();
		}

		public void refreshValues() {
			// 如果要完美支持拖拽时更新 UI，这里需要执行 tasks
			// 简单粗暴的方式：直接 rebuild (虽然性能有损耗，但对于 Demo 足够)
			if (ctx.selection != null && container != null) {
				build(container, ctx.selection);
			}
		}
	}

	// CameraController (保持不变，略)
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
