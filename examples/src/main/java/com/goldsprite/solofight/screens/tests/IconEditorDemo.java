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
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.input.SmartColorInput;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.goldsprite.gdengine.ui.input.SmartSelectInput;
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
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.goldsprite.solofight.ui.widget.BioCodeEditor;
import com.goldsprite.solofight.ui.widget.CommandHistoryUI;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.command.ICommand;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat Icon 设计器原型 (Phase 1 - Fix Gizmo)
 */
public class IconEditorDemo extends GScreen {

	// --- 核心组件 ---
	private NeonBatch neonBatch;
	private Stage uiStage;
	private CameraController cameraController;

	// --- 业务逻辑 ---
	private final CommandManager commandManager = new CommandManager();
	private final SceneManager sceneManager = new SceneManager(commandManager);
	private final GizmoSystem gizmoSystem = new GizmoSystem(sceneManager, commandManager);
	private final EditorInput sceneInput = new EditorInput(this, sceneManager, gizmoSystem, commandManager);
	private final Inspector inspector = new Inspector(this, sceneManager, commandManager);

	// --- UI 引用 ---
	private VisTree<UiNode, EditorTarget> hierarchyTree;
	private VisTable inspectorTable;
	private CommandHistoryUI historyPanel;
	private BioCodeEditor jsonEditor;
	private Stack propertiesStack;

	// --- 数据根节点 ---
	// private EditorTarget rootNode; // Moved to SceneManager

	// [新增] 拖拽管理器
    private DragAndDrop dragAndDrop;
    
    // --- Event System ---
    public interface EditorListener {
        default void onStructureChanged() {}
        default void onSelectionChanged(EditorTarget selection) {}
    }
    
    public static class SceneManager {
        public interface ShapeFactory {
            EditorTarget create(String name);
        }

        private EditorTarget rootNode;
		private EditorTarget selection;
		private final List<EditorListener> listeners = new ArrayList<>();
		private final Map<String, ShapeFactory> shapeRegistry = new LinkedHashMap<>();
		private final CommandManager commandManager;
		
		public SceneManager(CommandManager cm) {
			this.commandManager = cm;
			registerDefaultShapes();
		}
        
        private void registerDefaultShapes() {
            shapeRegistry.put("Group", GroupNode::new);
            shapeRegistry.put("Rectangle", name -> {
                RectShape r = new RectShape(name);
                r.width = 80; r.height = 80;
                return r;
            });
            shapeRegistry.put("Circle", name -> {
                CircleShape c = new CircleShape(name);
                c.radius = 40;
                return c;
            });
        }
        
        public void setRoot(EditorTarget root) {
            this.rootNode = root;
            notifyStructureChanged();
        }
        
        public EditorTarget getRoot() { return rootNode; }
        public EditorTarget getSelection() { return selection; }
        public Map<String, ShapeFactory> getShapeRegistry() { return shapeRegistry; }
        
        public void addListener(EditorListener l) { listeners.add(l); }
        public void removeListener(EditorListener l) { listeners.remove(l); }
        
        public void notifyStructureChanged() {
            for(EditorListener l : listeners) l.onStructureChanged();
        }
        
        public void selectNode(EditorTarget node) {
            this.selection = node;
            for(EditorListener l : listeners) l.onSelectionChanged(node);
        }
        
        public void deleteNode(EditorTarget node) {
            if (node == null || node == rootNode) return;
            commandManager.execute(new DeleteNodeCommand(this, node));
        }

        public void internalDeleteNode(EditorTarget node) {
            if (node == null || node == rootNode) return;
            node.removeFromParent();
            if (selection == node) selectNode(null);
            notifyStructureChanged();
        }
        
        public void addNode(EditorTarget parent, String type) {
            if (parent == null) parent = rootNode;
            commandManager.execute(new AddNodeCommand(this, parent, type));
        }
        
        public EditorTarget internalAddNode(EditorTarget parent, String type) {
            if (parent == null) parent = rootNode;
            ShapeFactory factory = shapeRegistry.get(type);
            if (factory == null) return null;
            
            String name = getUniqueName(parent, "New " + type);
            EditorTarget newShape = factory.create(name);
            
            if (newShape != null) {
                newShape.setX(MathUtils.random(-20, 20));
                newShape.setY(MathUtils.random(-20, 20));
                newShape.setParent(parent);
                notifyStructureChanged();
                selectNode(newShape);
            }
            return newShape;
        }

        public void internalAttachNode(EditorTarget node, EditorTarget parent, int index) {
            if (node == null || parent == null) return;
            node.setParent(parent);
            if (index >= 0 && index <= parent.getChildren().size) {
                parent.getChildren().removeValue(node, true);
                parent.getChildren().insert(index, node);
            }
            notifyStructureChanged();
            selectNode(node);
        }
        
        public void moveNode(EditorTarget node, EditorTarget newParent, int index) {
			if (node == null || newParent == null) return;
			commandManager.execute(new ReparentCommand(this, node, newParent, index));
		}
		
		public void internalMoveNode(EditorTarget node, EditorTarget newParent, int index) {
			if (node == null || newParent == null) return;
			
			node.setParent(newParent);
			
			if (index >= 0) {
				newParent.getChildren().removeValue(node, true);
				if (index > newParent.getChildren().size) index = newParent.getChildren().size;
				newParent.getChildren().insert(index, node);
			}
			
			notifyStructureChanged();
			selectNode(node);
		}
		
		public void changeNodeType(EditorTarget target, String newType) {
            if (target == null || target == rootNode) return;
            ShapeFactory factory = shapeRegistry.get(newType);
            if (factory == null) return;
            
            EditorTarget newNode = factory.create(target.getName());
            
            if (target instanceof BaseNode && newNode instanceof BaseNode) {
                BaseNode t = (BaseNode) target;
                BaseNode n = (BaseNode) newNode;
                n.x = t.x; n.y = t.y; n.rotation = t.rotation;
                n.scaleX = t.scaleX; n.scaleY = t.scaleY;
                n.color.set(t.color);
            }
            
            Array<EditorTarget> children = new Array<>(target.getChildren());
            for (EditorTarget child : children) child.setParent(newNode);
            
            EditorTarget parent = target.getParent();
            if (parent != null) {
                int idx = parent.getChildren().indexOf(target, true);
                target.removeFromParent();
                newNode.setParent(parent);
                parent.getChildren().removeValue(newNode, true);
                parent.getChildren().insert(idx, newNode);
            }
            
            notifyStructureChanged();
            selectNode(newNode);
        }
        
        private String getUniqueName(EditorTarget parent, String baseName) {
            boolean exists = false;
            for (EditorTarget child : parent.getChildren()) {
                if (child.getName().equals(baseName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) return baseName;
            
            int i = 1;
            while (true) {
                String newName = baseName + "_" + i;
                boolean found = false;
                for (EditorTarget child : parent.getChildren()) {
                    if (child.getName().equals(newName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return newName;
                i++;
            }
        }
    }
	
	
	// [重构] 修复布局、菜单坐标、拖拽逻辑
	public static class UiNode extends Tree.Node<UiNode, EditorTarget, VisTable> {
		
		public enum DropState { NONE, INSERT_ABOVE, INSERT_BELOW, REPARENT }
		public DropState dropState = DropState.NONE;
		
		public static class UiNodeTable extends VisTable {
			private UiNode node;
			private final IconEditorDemo screen;
			
			public UiNodeTable(IconEditorDemo screen) {
				this.screen = screen;
			}
			
			public void setNode(UiNode node) { this.node = node; }
			
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
				
				if (node != null && node.dropState != DropState.NONE) {
					com.badlogic.gdx.scenes.scene2d.utils.Drawable white = com.kotcrab.vis.ui.VisUI.getSkin().getDrawable("white");
					if (white != null) {
						float w = getWidth();
						float h = getHeight();
						float x = getX();
						float y = getY();
						
						Color oldColor = batch.getColor();
						batch.setColor(Color.CYAN);
						
						if (node.dropState == DropState.INSERT_ABOVE) {
							white.draw(batch, x, y + h - 2, w, 2);
						} else if (node.dropState == DropState.INSERT_BELOW) {
							white.draw(batch, x, y, w, 2);
						} else if (node.dropState == DropState.REPARENT) {
							white.draw(batch, x, y, w, 2); // bottom
							white.draw(batch, x, y + h - 2, w, 2); // top
							white.draw(batch, x, y, 2, h); // left
							white.draw(batch, x + w - 2, y, 2, h); // right
						}
						
						batch.setColor(oldColor);
					}
				}
			}
		}
		
		public UiNode(EditorTarget target, IconEditorDemo screen) {
			super(new UiNodeTable(screen));
			((UiNodeTable)getActor()).setNode(this);
			setValue(target);
			
			VisTable table = getActor();
			// 设为透明背景测试，或者给一个 hover listener 变色
			// table.setBackground("button"); 
			
			// 1. 名字 Label (左侧，填满剩余空间)
			VisLabel nameLbl = new VisLabel(target.getName());
			// 左键点击 -> 选中
			nameLbl.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					screen.getSceneManager().selectNode(target);
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
					for (String type : screen.getSceneManager().getShapeRegistry().keySet()) {
						subMenu.addItem(new MenuItem(type, new ChangeListener() { 
							@Override public void changed(ChangeEvent event, Actor actor) { 
								screen.getSceneManager().addNode(target, type); 
							} 
						}));
					}
					addItem.setSubMenu(subMenu);
					menu.addItem(addItem);

					// 2. Delete (根节点不能删除)
					if (target != screen.getSceneManager().getRoot()) {
						MenuItem delItem = new MenuItem("Delete");
						delItem.getLabel().setColor(Color.RED);
						delItem.addListener(new ChangeListener() {
							@Override public void changed(ChangeEvent event, Actor actor) {
								screen.getSceneManager().deleteNode(target);
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
			if (target != screen.getSceneManager().getRoot()) {
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
					
					float h = getActor().getHeight();
					if (y > h * 0.75f && target.getParent() != null) {
						dropState = DropState.INSERT_ABOVE;
					} else if (y < h * 0.25f && target.getParent() != null) {
						dropState = DropState.INSERT_BELOW;
					} else {
						dropState = DropState.REPARENT;
					}
					
					return true;
				}

				@Override
				public void drop(Source source, Payload payload, float x, float y, int pointer) {
					dropState = DropState.NONE; // Reset state
					
					EditorTarget dragging = (EditorTarget) payload.getObject();
					if (dragging == target) return;

					float h = getActor().getHeight();
					SceneManager sm = screen.getSceneManager();
					
					// Insert Before (Top 25%)
					if (y > h * 0.75f && target.getParent() != null) {
						EditorTarget parent = target.getParent();
						int targetIndex = parent.getChildren().indexOf(target, true);
						sm.moveNode(dragging, parent, targetIndex);
					} 
					// Insert After (Bottom 25%)
					else if (y < h * 0.25f && target.getParent() != null) {
						EditorTarget parent = target.getParent();
						int targetIndex = parent.getChildren().indexOf(target, true);
						sm.moveNode(dragging, parent, targetIndex + 1);
					}
					// Reparent
					else {
						sm.moveNode(dragging, target, -1);
					}
				}
				
				@Override
				public void reset(Source source, Payload payload) {
					dropState = DropState.NONE;
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

	public SceneManager getSceneManager() { return sceneManager; }
	
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

		// Initialize Root
		sceneManager.setRoot(new GroupNode("Root"));
		
		// Command Listener
		commandManager.addListener(new CommandManager.CommandListener() {
			@Override public void onCommandExecuted(ICommand cmd) {
				if(historyPanel != null) {
					String type = "raw";
					if(cmd.getSource().equals("Gizmo")) type = "move";
					historyPanel.addHistory("CMD_" + cmd.getName(), cmd.getSource(), type, cmd.getIcon());
				}
			}
			@Override public void onUndo(ICommand cmd) {
				 if(historyPanel != null) historyPanel.addHistory("UNDO " + cmd.getName(), "System", "raw", "U");
			}
			 @Override public void onRedo(ICommand cmd) {
				 if(historyPanel != null) historyPanel.addHistory("REDO " + cmd.getName(), "System", "raw", "R");
			}
		});

		buildTestScene();
		buildLayout();

		sceneManager.addListener(new EditorListener() {
			@Override public void onStructureChanged() { IconEditorDemo.this.onStructureChanged(); }
			@Override public void onSelectionChanged(EditorTarget s) { IconEditorDemo.this.onSelectionChanged(s); }
		});

		if (sceneManager.getRoot().getChildren().size > 0) {
			sceneManager.selectNode(sceneManager.getRoot().getChildren().get(0));
		}
	}

	public CameraController getCameraController() { return cameraController; }

	private void buildTestScene() {
		EditorTarget rootNode = sceneManager.getRoot();

		CircleShape c1 = new CircleShape("Circle");
		c1.radius = 60;
		c1.setX(-50);
		c1.setParent(rootNode);

		RectShape r1 = new RectShape("Rect");
		r1.setX(50);
		r1.setParent(rootNode);

		GroupNode g1 = new GroupNode("Group");
		g1.setY(100);
		g1.setParent(rootNode);

		CircleShape c2 = new CircleShape("SubCircle");
		c2.setParent(g1);
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
				for (String type : sceneManager.getShapeRegistry().keySet()) {
					menu.addItem(new MenuItem(type, new ChangeListener() { 
						@Override public void changed(ChangeEvent event, Actor actor) { 
							sceneManager.addNode(sceneManager.getRoot(), type); 
						} 
					}));
				}
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
					if (sel != null) sceneManager.selectNode(sel.getValue());
					else sceneManager.selectNode(null);
				}
			});

		onStructureChanged();
		leftPanel.add(new VisScrollPane(hierarchyTree)).grow();

		// 2. 右侧面板
		VisTable rightPanel = new VisTable(true);
		rightPanel.setBackground("window-bg");
		
		// Tabs
		VisTable tabs = new VisTable();
		VisTextButton btnInsp = new VisTextButton("Inspector");
		VisTextButton btnJson = new VisTextButton("JSON");
		tabs.add(btnInsp).growX();
		tabs.add(btnJson).growX();
		rightPanel.add(tabs).growX().row();

		propertiesStack = new Stack();
		
		// Page 1: Inspector
		VisTable pageInsp = new VisTable();
		pageInsp.add(new VisLabel("Properties")).pad(5).left().row();
		inspectorTable = new VisTable();
		inspectorTable.top().left();
		pageInsp.add(new VisScrollPane(inspectorTable)).grow();
		
		// Page 2: JSON
		VisTable pageJson = new VisTable();
		jsonEditor = new BioCodeEditor();
		VisTextButton btnApply = new VisTextButton("Apply to Scene");
		btnApply.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				applyJsonChange();
			}
		});
		pageJson.add(jsonEditor).grow().row();
		pageJson.add(btnApply).growX().pad(5);
		
		propertiesStack.add(pageInsp);
		propertiesStack.add(pageJson);
		
		// Tab Logic
		btnInsp.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				pageInsp.setVisible(true);
				pageJson.setVisible(false);
			}
		});
		btnJson.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				pageInsp.setVisible(false);
				pageJson.setVisible(true);
				updateJsonView(sceneManager.getSelection()); // Refresh on switch
			}
		});
		
		// Default
		pageJson.setVisible(false);
		
		rightPanel.add(propertiesStack).grow();

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

		topBar.add().width(15); // Separator

		addToolBtn(topBar, "<", () -> commandManager.undo());
		addToolBtn(topBar, ">", () -> commandManager.redo());

		centerStack.add(topBar);
		
		// History Panel (右上角悬浮)
		Table historyContainer = new Table();
		historyContainer.top().right().pad(10);
		
		historyPanel = new CommandHistoryUI();
		historyPanel.setVisible(false); // Default hidden
		
		VisTextButton btnHistory = new VisTextButton("History");
		btnHistory.addListener(new ClickListener() {
			@Override public void clicked(InputEvent e, float x, float y) {
				historyPanel.setVisible(!historyPanel.isVisible());
			}
		});
		
		historyContainer.add(btnHistory).right().row();
		historyContainer.add(historyPanel).right().padTop(5);
		centerStack.add(historyContainer);

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

	// ========================================================================
	// Logic & Render
	// ========================================================================

	private void onSelectionChanged(EditorTarget node) {
		inspector.build(inspectorTable, node);
		updateJsonView(node);
		
		if (hierarchyTree != null) {
			UiNode uiNode = node == null ? null : hierarchyTree.findNode(node);
			if (uiNode != null) {
				if (!hierarchyTree.getSelection().contains(uiNode)) {
					hierarchyTree.getSelection().set(uiNode);
				}
			} else {
				hierarchyTree.getSelection().clear();
			}
		}
	}

	private void updateJsonView(EditorTarget node) {
		if (jsonEditor == null) return;
		if (node == null) {
			jsonEditor.setText("");
			return;
		}
		
		try {
			Json json = new Json();
			json.setOutputType(OutputType.json);
			json.setIgnoreUnknownFields(true);
			String text = json.prettyPrint(node);
			jsonEditor.setText(text);
		} catch (Exception e) {
			Gdx.app.error("Editor", "JSON serialization failed", e);
			jsonEditor.setText("// Serialization Error: " + e.getMessage());
		}
	}

	private void applyJsonChange() {
		if (sceneManager.getSelection() == null) return;
		
		String jsonText = jsonEditor.getText();
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			
			Class<? extends EditorTarget> clazz = sceneManager.getSelection().getClass();
			EditorTarget newObj = json.fromJson(clazz, jsonText);
			
			if (newObj != null) {
				EditorTarget oldObj = sceneManager.getSelection();
				EditorTarget parent = oldObj.getParent();
				
				if (parent != null) {
					int idx = parent.getChildren().indexOf(oldObj, true);
					oldObj.removeFromParent();
					newObj.setParent(parent);
					parent.getChildren().removeValue(newObj, true);
					if (idx >= 0 && idx <= parent.getChildren().size) {
						parent.getChildren().insert(idx, newObj);
					} else {
						parent.getChildren().add(newObj);
					}
				} else if (oldObj == sceneManager.getRoot()) {
					sceneManager.setRoot(newObj);
				}
				
				fixParentRefs(newObj);
				
				sceneManager.notifyStructureChanged();
				sceneManager.selectNode(newObj);
				
				Gdx.app.log("Editor", "Applied JSON changes");
			}
		} catch (Exception e) {
			Gdx.app.error("Editor", "Apply failed", e);
		}
	}
	
	private void fixParentRefs(EditorTarget node) {
		for (EditorTarget child : node.getChildren()) {
			if (child instanceof BaseNode) {
				((BaseNode)child).parent = node;
			}
			fixParentRefs(child);
		}
	}

	private void onStructureChanged() {
		// 1. 清理 UI
		hierarchyTree.clear();
		// 2. [关键] 清理旧的拖拽源和目标
		if (dragAndDrop != null) {
			dragAndDrop.clear();
		}
		// 3. 重建
		if (sceneManager.getRoot() != null) {
			buildTreeRecursive(sceneManager.getRoot(), null);
		}
		
		// 4. 恢复选中状态
		if (sceneManager.getSelection() != null) {
			onSelectionChanged(sceneManager.getSelection());
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
		if (sceneManager.getRoot() != null) {
			drawNodeRecursive(sceneManager.getRoot(), neonBatch);
		}

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
        String getTypeName(); // [New]
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
		boolean hitTest(float x, float y);
		void render(NeonBatch batch);
	}

	public static abstract class BaseNode implements EditorTarget {
		public String name;
		public float x, y, rotation = 0, scaleX = 1, scaleY = 1;
		public Color color = new Color(Color.WHITE);
		// [新增] 父节点引用
		protected transient EditorTarget parent;
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

		@Override public boolean hitTest(float x, float y) { return false; }
	}

	public static class GroupNode extends BaseNode {
		public GroupNode(String name) { super(name); }
		@Override public String getTypeName() { return "Group"; }
		@Override public void render(NeonBatch batch) {}
	}

	public static class RectShape extends BaseNode {
		public float width = 100, height = 100;
		public RectShape(String name) { super(name); }
		@Override public String getTypeName() { return "Rectangle"; }
		@Override public void render(NeonBatch batch) {
			batch.drawRect(x - width/2*scaleX, y - height/2*scaleY, width*scaleX, height*scaleY, rotation, 0, color, true);
		}

		@Override public boolean hitTest(float wx, float wy) {
			float dx = wx - x;
			float dy = wy - y;
			float rad = -rotation * MathUtils.degreesToRadians;
			float c = MathUtils.cos(rad);
			float s = MathUtils.sin(rad);
			float lx = dx * c - dy * s;
			float ly = dx * s + dy * c;
			return Math.abs(lx) <= width * Math.abs(scaleX) / 2 && Math.abs(ly) <= height * Math.abs(scaleY) / 2;
		}
	}

	public static class CircleShape extends BaseNode {
		public float radius = 50;
		public CircleShape(String name) { super(name); }
		@Override public String getTypeName() { return "Circle"; }
		@Override public void render(NeonBatch batch) {
			batch.drawCircle(x, y, radius * Math.max(Math.abs(scaleX), Math.abs(scaleY)), 0, color, 32, true);
		}

		@Override public boolean hitTest(float wx, float wy) {
			float dx = wx - x;
			float dy = wy - y;
			float r = radius * Math.max(Math.abs(scaleX), Math.abs(scaleY));
			return dx * dx + dy * dy <= r * r;
		}
	}

	// ========================================================================
	// 2. Editor Context & Systems (修复 Gizmo 绘制)
	// ========================================================================

	public static class GizmoSystem {
		public enum Mode { MOVE, ROTATE, SCALE }
		public Mode mode = Mode.MOVE;
		private final SceneManager sceneManager;

		// [复刻] 视觉配置 (参考 BioGizmoDrawer)
		static float HANDLE_SIZE = 15f;
		static float AXIS_LEN = 60f;
		static float GIZMO_OUTLINE_WIDTH = 0.8f;

		// 缓存数组 (绘制箭头用)
		private static final float[] tmpPoly = new float[8];
		
		private final CommandManager commandManager;

		public GizmoSystem(SceneManager sm, CommandManager cm) { this.sceneManager = sm; this.commandManager = cm; }

		public void render(NeonBatch batch, float zoom) {
			EditorTarget t = sceneManager.getSelection();
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
		private final SceneManager sceneManager;
		private final GizmoSystem gizmo;

		private enum DragMode { NONE, BODY, ROTATE, SCALE_X, SCALE_Y }
		private DragMode currentDragMode = DragMode.NONE;

		private float lastX, lastY;
		private float startValX, startValY, startValRot; // 记录初始值用于精确计算
		
		private float undoStartX, undoStartY, undoStartRot, undoStartSX, undoStartSY;
		private final CommandManager commandManager;

		public EditorInput(IconEditorDemo screen, SceneManager sm, GizmoSystem gizmo, CommandManager cm) {
			this.screen = screen; this.sceneManager = sm; this.gizmo = gizmo; this.commandManager = cm;
		}
		
		@Override
		public boolean keyDown(int keycode) {
			boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
			boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

			if (ctrl) {
				if (keycode == Input.Keys.Z) {
					if (shift) {
						commandManager.redo();
					} else {
						commandManager.undo();
					}
					return true;
				} else if (keycode == Input.Keys.Y) {
					commandManager.redo();
					return true;
				}
			} else {
				// MRS Mode Switch
				if (keycode == Input.Keys.W) {
					gizmo.mode = GizmoSystem.Mode.MOVE;
					return true;
				} else if (keycode == Input.Keys.E) {
					gizmo.mode = GizmoSystem.Mode.ROTATE;
					return true;
				} else if (keycode == Input.Keys.R) {
					gizmo.mode = GizmoSystem.Mode.SCALE;
					return true;
				}
			}
			return false;
		}

		private EditorTarget findTarget(float wx, float wy) {
			return findRecursive(sceneManager.getRoot(), wx, wy);
		}

		private EditorTarget findRecursive(EditorTarget node, float wx, float wy) {
			Array<EditorTarget> children = node.getChildren();
			for (int i = children.size - 1; i >= 0; i--) {
				EditorTarget child = children.get(i);
				EditorTarget found = findRecursive(child, wx, wy);
				if (found != null) return found;
			}
			if (node.hitTest(wx, wy)) return node;
			return null;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 wPos = screen.screenToWorldCoord(screenX, screenY);
			EditorTarget t = sceneManager.getSelection();

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

			EditorTarget hit = findTarget(wPos.x, wPos.y);
			if (hit != null) {
				if (hit != t) {
					sceneManager.selectNode(hit);
				}
				startDrag(DragMode.BODY, wPos);
				return true;
			}

			if (t != null) {
				sceneManager.selectNode(null);
			}

			return false;
		}

		private void startDrag(DragMode mode, Vector2 pos) {
			currentDragMode = mode;
			lastX = pos.x; lastY = pos.y;
			screen.getCameraController().setInputEnabled(false);

			if(sceneManager.getSelection() != null) {
				EditorTarget sel = sceneManager.getSelection();
				startValX = sel.getScaleX();
				startValY = sel.getScaleY();
				startValRot = sel.getRotation();
				
				// [关键修复] 记录 Undo 初始状态
				undoStartX = sel.getX();
				undoStartY = sel.getY();
				undoStartRot = sel.getRotation();
				undoStartSX = sel.getScaleX();
				undoStartSY = sel.getScaleY();
			}
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (currentDragMode == DragMode.NONE || sceneManager.getSelection() == null) return false;

			Vector2 wPos = screen.screenToWorldCoord(screenX, screenY);
			float dx = wPos.x - lastX;
			float dy = wPos.y - lastY;

			EditorTarget t = sceneManager.getSelection();

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
				
				EditorTarget sel = sceneManager.getSelection();
				if (sel != null) {
					boolean changed = sel.getX() != undoStartX || sel.getY() != undoStartY || 
									  sel.getRotation() != undoStartRot || 
									  sel.getScaleX() != undoStartSX || sel.getScaleY() != undoStartSY;
					
					if (changed) {
						commandManager.execute(new TransformCommand(sel, 
							undoStartX, undoStartY, undoStartRot, undoStartSX, undoStartSY,
							() -> screen.inspector.refreshValues()
						));
					}
				}
				
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
		private final SceneManager sceneManager;
		private final CommandManager commandManager;
		private final Array<Runnable> refreshTasks = new Array<>();
		
		private final Map<Class<?>, InspectorStrategy> strategies = new HashMap<>();

		public interface InspectorStrategy {
			void inspect(Inspector inspector, EditorTarget target);
		}

		public Inspector(IconEditorDemo screen, SceneManager sm, CommandManager cm) { 
			this.screen = screen;
			this.sceneManager = sm; 
			this.commandManager = cm;
			registerDefaultStrategies();
		}
		
		private void registerDefaultStrategies() {
			InspectorStrategy transformStrategy = (inspector, target) -> {
				if (target instanceof BaseNode) {
					BaseNode n = (BaseNode) target;
					inspector.addSection("Transform");
					inspector.addFloat("X", n::getX, n::setX);
					inspector.addFloat("Y", n::getY, n::setY);
					inspector.addFloat("Rot", n::getRotation, n::setRotation);
					inspector.addFloat("Scl X", n::getScaleX, n::setScaleX);
					inspector.addFloat("Scl Y", n::getScaleY, n::setScaleY);
				}
			};
			
			strategies.put(GroupNode.class, transformStrategy);
			
			strategies.put(RectShape.class, (inspector, target) -> {
				transformStrategy.inspect(inspector, target);
				RectShape r = (RectShape) target;
				inspector.addSection("Rectangle");
				inspector.addFloat("Width", () -> r.width, v -> r.width = v);
				inspector.addFloat("Height", () -> r.height, v -> r.height = v);
				inspector.addColor("Color", () -> r.color, newColor -> r.color.set(newColor));
			});
			
			strategies.put(CircleShape.class, (inspector, target) -> {
				transformStrategy.inspect(inspector, target);
				CircleShape c = (CircleShape) target;
				inspector.addSection("Circle");
				inspector.addFloat("Radius", () -> c.radius, v -> c.radius = v);
				inspector.addColor("Color", () -> c.color, newColor -> c.color.set(newColor));
			});
		}

		public void build(VisTable table, EditorTarget target) {
			this.container = table;
			table.clearChildren();
			refreshTasks.clear();

			if (target == null) {
				table.add(new VisLabel("No Selection")).pad(10);
				return;
			}

			// Name Field using SmartTextInput
			SmartTextInput nameInput = new SmartTextInput("Name", target.getName(), newName -> {
				target.setName(newName);
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
			});
			// Bind Command for Undo/Redo
			nameInput.setOnCommand((oldVal, newVal) -> {
				commandManager.execute(new GenericPropertyChangeCommand<String>("Name", oldVal, newVal, 
					v -> {
						target.setName(v);
						// Force refresh logic is same as above, maybe cleaner to extract
						if (screen.hierarchyTree != null) {
							UiNode node = screen.hierarchyTree.findNode(target);
							if (node != null && node.getActor().getChildren().size > 0) {
								((VisLabel)node.getActor().getChildren().get(0)).setText(v);
							}
						}
					}, 
					() -> refreshValues()
				));
			});
			container.add(nameInput).growX().padBottom(2).row();

			// Type Selection using SmartSelectInput
			if (target != screen.sceneManager.getRoot()) {
				com.badlogic.gdx.utils.Array<String> items = new com.badlogic.gdx.utils.Array<>();
				for (String s : screen.sceneManager.getShapeRegistry().keySet()) items.add(s);
				
				SmartSelectInput<String> typeInput = new SmartSelectInput<>("Type", target.getTypeName(), items, newType -> {
					if (!newType.equals(target.getTypeName())) {
						screen.sceneManager.changeNodeType(target, newType);
					}
				});
				// ChangeNodeType is complex and likely handles its own undo/redo or is destructive
				// For now we assume changeNodeType handles logic, but SmartSelectInput also supports command.
				// However, changing type replaces the object, so the 'target' reference becomes stale.
				// This is a special case where we might not want standard property undo, but rely on sceneManager.
				// Let's just use the onChange for now as it was before.
				
				container.add(typeInput).growX().padBottom(2).row();
			}

			InspectorStrategy strategy = strategies.get(target.getClass());
			if (strategy != null) {
				strategy.inspect(this, target);
			}
		}

		public void addSection(String title) {
			VisLabel l = new VisLabel(title);
			l.setColor(Color.CYAN);
			container.add(l).left().padTop(10).padBottom(5).row();
		}

		public void addFloat(String label, Supplier<Float> getter, Consumer<Float> setter) {
			SmartNumInput input = new SmartNumInput(label, getter.get(), 1.0f, setter);
			
			input.setOnCommand((oldVal, newVal) -> {
				commandManager.execute(new PropertyChangeCommand(label, oldVal, newVal, setter, () -> refreshValues()));
			});

			refreshTasks.add(() -> {
				// 暂时忽略，或者你可以手动重建整个 Inspector
			});
			container.add(input).growX().padBottom(2).row();
		}

		public void addColor(String label, Supplier<Color> getter, Consumer<Color> setter) {
			SmartColorInput input = new SmartColorInput(label, getter.get(), setter);
			
			// [新增] 注册 Command 回调
			input.setOnCommand((oldVal, newVal) -> {
				commandManager.execute(new ColorChangeCommand(sceneManager.getSelection(), oldVal, newVal, () -> refreshValues()));
			});

			container.add(input).growX().padBottom(2).row();
		}

		public void refreshValues() {
			if (sceneManager.getSelection() != null && container != null) {
				build(container, sceneManager.getSelection());
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
			// [Fix] 禁用 WASD 移动
			// float speed = 500 * dt * cam.zoom;
			// if(Gdx.input.isKeyPressed(Input.Keys.A)) cam.translate(-speed, 0);
			// if(Gdx.input.isKeyPressed(Input.Keys.D)) cam.translate(speed, 0);
			// if(Gdx.input.isKeyPressed(Input.Keys.W)) cam.translate(0, speed);
			// if(Gdx.input.isKeyPressed(Input.Keys.S)) cam.translate(0, -speed);
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

	// ========================================================================
	// 5. Commands
	// ========================================================================

	public static class TransformCommand implements ICommand {
		private final EditorTarget target;
		private final float oldX, oldY, oldRot, oldSX, oldSY;
		private final float newX, newY, newRot, newSX, newSY;
		private final Runnable refreshUI;

		public TransformCommand(EditorTarget t, float ox, float oy, float or, float osx, float osy, Runnable refreshUI) {
			this.target = t;
			this.oldX = ox; this.oldY = oy; this.oldRot = or; this.oldSX = osx; this.oldSY = osy;
			this.newX = t.getX(); this.newY = t.getY(); this.newRot = t.getRotation(); this.newSX = t.getScaleX(); this.newSY = t.getScaleY();
			this.refreshUI = refreshUI;
		}

		@Override public void execute() { apply(newX, newY, newRot, newSX, newSY); }
		@Override public void undo() { apply(oldX, oldY, oldRot, oldSX, oldSY); }
		@Override public String getName() { return "Transform " + target.getName(); }
		@Override public String getSource() { return "Gizmo"; }
		@Override public String getIcon() { return "T"; }

		private void apply(float x, float y, float r, float sx, float sy) {
			target.setX(x); target.setY(y); target.setRotation(r); target.setScaleX(sx); target.setScaleY(sy);
			if(refreshUI != null) refreshUI.run();
		}
	}

	public static class PropertyChangeCommand implements ICommand {
		private final String name;
		private final float oldVal, newVal;
		private final Consumer<Float> setter;
		private final Runnable refreshUI;

		public PropertyChangeCommand(String name, float oldVal, float newVal, Consumer<Float> setter, Runnable refreshUI) {
			this.name = name; this.oldVal = oldVal; this.newVal = newVal; this.setter = setter; this.refreshUI = refreshUI;
		}

		@Override public void execute() { setter.accept(newVal); if(refreshUI != null) refreshUI.run(); }
		@Override public void undo() { setter.accept(oldVal); if(refreshUI != null) refreshUI.run(); }
		@Override public String getName() { return "Set " + name; }
		@Override public String getSource() { return "Inspector"; }
		@Override public String getIcon() { return "P"; }
	}
	
	public static class GenericPropertyChangeCommand<T> implements ICommand {
		private final String name;
		private final T oldVal, newVal;
		private final Consumer<T> setter;
		private final Runnable refreshUI;

		public GenericPropertyChangeCommand(String name, T oldVal, T newVal, Consumer<T> setter, Runnable refreshUI) {
			this.name = name; this.oldVal = oldVal; this.newVal = newVal; this.setter = setter; this.refreshUI = refreshUI;
		}

		@Override public void execute() { setter.accept(newVal); if(refreshUI != null) refreshUI.run(); }
		@Override public void undo() { setter.accept(oldVal); if(refreshUI != null) refreshUI.run(); }
		@Override public String getName() { return "Set " + name; }
		@Override public String getSource() { return "Inspector"; }
		@Override public String getIcon() { return "P"; }
	}

	public static class ColorChangeCommand implements ICommand {
		private final EditorTarget target;
		private final Color oldColor, newColor;
		private final Runnable refreshUI;

		public ColorChangeCommand(EditorTarget target, Color oldVal, Color newVal, Runnable refreshUI) {
			this.target = target; 
			this.oldColor = new Color(oldVal); 
			this.newColor = new Color(newVal);
			this.refreshUI = refreshUI;
		}
		@Override public void execute() { apply(newColor); }
		@Override public void undo() { apply(oldColor); }
		private void apply(Color c) { 
			if(target instanceof BaseNode) ((BaseNode)target).color.set(c);
			if(refreshUI != null) refreshUI.run(); 
		}
		@Override public String getName() { return "Color Change"; }
		@Override public String getSource() { return "Inspector"; }
		@Override public String getIcon() { return "C"; }
	}

	public static class AddNodeCommand implements ICommand {
		private final SceneManager sm;
		private final EditorTarget parent;
		private final String type;
		private EditorTarget createdNode;

		public AddNodeCommand(SceneManager sm, EditorTarget parent, String type) {
			this.sm = sm; this.parent = parent; this.type = type;
		}
		@Override public void execute() {
			if (createdNode == null) {
				createdNode = sm.internalAddNode(parent, type);
			} else {
				sm.internalAttachNode(createdNode, parent, -1);
			}
		}
		@Override public void undo() {
			if (createdNode != null) sm.internalDeleteNode(createdNode);
		}
		@Override public String getName() { return "Add " + type; }
		@Override public String getSource() { return "Hierarchy"; }
		@Override public String getIcon() { return "+"; }
	}

		public static class DeleteNodeCommand implements ICommand {
		private final SceneManager sm;
		private final EditorTarget target;
		private final EditorTarget parent;
		private final int index;

		public DeleteNodeCommand(SceneManager sm, EditorTarget target) {
			this.sm = sm; this.target = target;
			this.parent = target.getParent();
			this.index = parent.getChildren().indexOf(target, true);
		}
		@Override public void execute() { sm.internalDeleteNode(target); }
		@Override public void undo() { sm.internalAttachNode(target, parent, index); }
		@Override public String getName() { return "Delete " + target.getName(); }
		@Override public String getSource() { return "Hierarchy"; }
		@Override public String getIcon() { return "X"; }
	}

	public static class ReparentCommand implements ICommand {
		private final SceneManager sm;
		private final EditorTarget target;
		private final EditorTarget oldParent, newParent;
		private final int oldIndex, newIndex;

		public ReparentCommand(SceneManager sm, EditorTarget target, EditorTarget newParent, int newIndex) {
			this.sm = sm; this.target = target;
			this.oldParent = target.getParent();
			this.oldIndex = oldParent.getChildren().indexOf(target, true);
			this.newParent = newParent;
			this.newIndex = newIndex;
		}
		@Override public void execute() { sm.internalMoveNode(target, newParent, newIndex); }
		@Override public void undo() { sm.internalMoveNode(target, oldParent, oldIndex); }
		@Override public String getName() { return "Move " + target.getName(); }
		@Override public String getSource() { return "Hierarchy"; }
		@Override public String getIcon() { return "M"; }
	}
}
