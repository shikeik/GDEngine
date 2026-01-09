package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.command.CommandManager.CommandListener;
import com.goldsprite.gdengine.core.command.ICommand;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.solofight.screens.tests.iconeditor.model.BaseNode;
import com.goldsprite.solofight.screens.tests.iconeditor.model.CircleShape;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.model.GroupNode;
import com.goldsprite.solofight.screens.tests.iconeditor.model.RectShape;
import com.goldsprite.solofight.screens.tests.iconeditor.system.EditorInput;
import com.goldsprite.solofight.screens.tests.iconeditor.system.EditorListener;
import com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;
import com.goldsprite.solofight.screens.tests.iconeditor.ui.FileNode;
import com.goldsprite.solofight.screens.tests.iconeditor.ui.Inspector;
import com.goldsprite.solofight.screens.tests.iconeditor.ui.UiNode;
import com.goldsprite.solofight.screens.tests.iconeditor.utils.CameraController;
import com.goldsprite.solofight.ui.widget.BioCodeEditor;
import com.goldsprite.solofight.ui.widget.CommandHistoryUI;
import com.goldsprite.solofight.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisTree;

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
    private VisTree<FileNode, FileHandle> fileTree; // [New] File Tree
    private VisTable inspectorTable;
    private CommandHistoryUI historyPanel;
    private BioCodeEditor jsonEditor;
    private Stack propertiesStack;

    // --- File System ---
    private FileHandle currentFile;
    private boolean isDirty = false;
    private static final String PROJECT_DIR = "IconProjects";

    // [New] Undo/Redo Dirty State Tracking
    private ICommand savedHistoryState = null;

    // [新增] 拖拽管理器
    private DragAndDrop dragAndDrop;

    // 对齐精度档位
    private static final float[] ALIGN_PRECISIONS = {0.1f, 0.5f, 1.0f, 5.0f, 10.0f};
    private int currentPrecisionIndex = 2; // 默认1.0f

    // 网格对齐开关
    private boolean isGridAlignEnabled = true;

    // 默认旋转步长
    private float defaultRotateStep = 15.0f;

    public IconEditorDemo() {
        // [修复] 设置 UI 视口缩放系数，解决分辨率过小问题
        // 参考 GScreen 默认值，或者根据需要调整。例如 PC 端稍微放大一点视野。
        this.uiViewportScale = PlatformImpl.isAndroidUser() ? 1.0f : 1.5f;
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
    public CommandManager getCommandManager() { return commandManager; }
    public GizmoSystem getGizmoSystem() { return gizmoSystem; }
    public CameraController getCameraController() { return cameraController; }
    public Stage getUiStage() { return uiStage; }
    public DragAndDrop getDragAndDrop() { return dragAndDrop; }
    public VisTree<UiNode, EditorTarget> getHierarchyTree() { return hierarchyTree; }
    public Inspector getInspector() { return inspector; }
    public OrthographicCamera getWorldCamera() { return worldCamera; } // Expose protected worldCamera
    // 获取当前对齐精度
    public float getCurrentAlignPrecision() { return ALIGN_PRECISIONS[currentPrecisionIndex]; }

    // 获取网格大小
    public float getGridSize() {
        return isGridAlignEnabled ? 10.0f : 0.0f;
    }

    // 获取旋转步长
    public float getRotationStep() { return defaultRotateStep; }

    // 设置对齐精度
    public void setAlignPrecisionIndex(int index) {
        currentPrecisionIndex = Math.max(0, Math.min(index, ALIGN_PRECISIONS.length - 1));
    }

    // 设置网格对齐开关
    public void setGridAlignEnabled(boolean enabled) {
        isGridAlignEnabled = enabled;
    }

    @Override
    public void create() {
        neonBatch = new NeonBatch();

        worldCamera.zoom = 1.0f;
        worldCamera.position.set(0, 0, 0);
        cameraController = new CameraController(worldCamera);

        uiStage = new Stage(getUIViewport());

        // 添加ToastUI到UI舞台
        uiStage.addActor(new ToastUI());

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
                // 历史项添加现在由CommandHistoryUI自己处理
                // 不再需要在这里添加历史项
            }
            @Override public void onUndo(ICommand cmd) {
                // 撤销操作不再添加历史项，由CommandHistoryUI自己处理
            }
            @Override public void onRedo(ICommand cmd) {
                // 重做操作不再添加历史项，由CommandHistoryUI自己处理
            }
            @Override public void onHistoryChanged() {
                ICommand current = commandManager.getLastCommand();
                boolean dirty = (current != savedHistoryState);
                markDirty(dirty);
            }
            @Override public void onHistoryNavigated(int position) {
                if(historyPanel != null) {
                    historyPanel.updateCurrentIndex(position);
                }
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
    // File System
    // ========================================================================

    private void initProjectSystem() {
        FileHandle dir = Gd.files.local(PROJECT_DIR);
        if (!dir.exists()) dir.mkdirs();

        FileHandle[] files = dir.list("json");
        if (files.length == 0) {
            createDefaultProject();
        }

        reloadFileTree();

        // Load first project by default if no current file
        if (currentFile == null && fileTree.getNodes().size > 0) {
            FileNode first = fileTree.getNodes().first();
            loadProject(first.getValue());
            fileTree.getSelection().set(first);
        }
    }

    private void createDefaultProject() {
        FileHandle file = Gd.files.local(PROJECT_DIR + "/DefaultProject.json");
        GroupNode root = new GroupNode("Root");

        // Add some default content
        RectShape r = new RectShape("Rect");
        r.width = 100; r.height = 100; r.color.set(Color.CYAN);
        r.setParent(root);

        saveProjectToFile(root, file);
    }

    private void reloadFileTree() {
        fileTree.clearChildren();
        FileHandle dir = Gdx.files.local(PROJECT_DIR);
        FileHandle[] files = dir.list("json");

        for (FileHandle file : files) {
            FileNode node = new FileNode(file);

            // [New] Node Context Menu & Click Logic
            node.getActor().addListener(new ActorGestureListener(20, 0.4f, 1.0f, 0.15f) {
                private com.badlogic.gdx.utils.Timer.Task tapTask;

                @Override
                public boolean longPress(Actor actor, float x, float y) {
                    fileTree.getSelection().set(node);
                    Vector2 stageCoords = actor.localToStageCoordinates(new Vector2(x, y));
                    showFileTreeMenu(node, stageCoords.x, stageCoords.y);
                    return true; // Stop propagation
                }

                @Override
                public void tap(InputEvent event, float x, float y, int count, int button) {
                    event.stop(); // 阻止事件冒泡，防止触发背景菜单

                    if (button == Input.Buttons.RIGHT) {
                        fileTree.getSelection().set(node);
                        showFileTreeMenu(node, event.getStageX(), event.getStageY());
                    } else if (button == Input.Buttons.LEFT) {
                        fileTree.getSelection().set(node);

                        if (count == 2) {
                            // 双击：取消单击任务，直接打开
                            if (tapTask != null) tapTask.cancel();
                            loadProject(node.getValue());
                            Gdx.app.log("Editor", "Double click: Load " + node.getValue().name());
                        } else if (count == 1) {
                            // 单击：延迟打开弹窗
                            tapTask = com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                                @Override
                                public void run() {
                                    showLoadConfirmDialog(node.getValue());
                                }
                            }, 0.2f);
                        }
                    }
                }
            });

            fileTree.add(node);
            if (currentFile != null && file.equals(currentFile)) {
                fileTree.getSelection().set(node);
                if (isDirty) node.setDirty(true);
            }
        }
    }

    private void showLoadConfirmDialog(FileHandle file) {
        VisDialog dialog = new VisDialog("Open Project") {
            @Override
            protected void result(Object object) {
                if ((Boolean)object) {
                    loadProject(file);
                }
            }
        };
        dialog.text("Open '" + file.name() + "'?");
        dialog.button("Open", true);
        dialog.button("Cancel", false);
        dialog.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // Prevent click through
            }
        });
        dialog.show(uiStage);
    }

    private void showFileTreeMenu(FileNode node, float x, float y) {
        PopupMenu menu = new PopupMenu();

        if (node == null) {
            // Background Menu
            menu.addItem(new MenuItem("New Project", new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    createNewProject();
                }
            }));
        } else {
            // File Menu
            menu.addItem(new MenuItem("Delete", new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    deleteProject(node.getValue());
                }
            }));
            menu.addItem(new MenuItem("Rename", new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    renameProject(node.getValue());
                }
            }));
            menu.addItem(new MenuItem("Duplicate", new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    duplicateProject(node.getValue());
                }
            }));
        }

        menu.showMenu(uiStage, x, y);
    }

    private void createNewProject() {
        VisDialog dialog = new VisDialog("New Project");
        VisTextField nameField = new VisTextField("NewProject");
        dialog.add(new VisLabel("Name:")).padRight(5);
        dialog.add(nameField).growX().row();

        VisTextButton btnCreate = new VisTextButton("Create");
        btnCreate.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                String name = nameField.getText();
                if (name.isEmpty()) return;
                if (!name.endsWith(".json")) name += ".json";

                FileHandle file = Gdx.files.local(PROJECT_DIR + "/" + name);
                if (file.exists()) {
                    // Simple alert or just return
                    return;
                }

                GroupNode root = new GroupNode("Root");
                saveProjectToFile(root, file);
                reloadFileTree();

                // Auto load
                for (FileNode n : fileTree.getNodes()) {
                    if (n.getValue().equals(file)) {
                        fileTree.getSelection().set(n);
                        loadProject(file);
                        break;
                    }
                }

                dialog.fadeOut();
            }
        });

        dialog.add(btnCreate).colspan(2).padTop(10);
        dialog.show(uiStage);
        dialog.centerWindow();
        uiStage.setKeyboardFocus(nameField);
    }

    private void deleteProject(FileHandle file) {
        VisDialog dialog = new VisDialog("Delete Project") {
            @Override
            protected void result(Object object) {
                if ((Boolean)object) {
                    file.delete();
                    if (currentFile != null && currentFile.equals(file)) {
                        currentFile = null;
                        sceneManager.setRoot(null);
                        markDirty(false); // [修复] 删除后重置 dirty 状态
                    }
                    reloadFileTree();
                }
            }
        };
        dialog.text("Are you sure you want to delete '" + file.name() + "'?");
        dialog.button("Yes", true);
        dialog.button("No", false);
        dialog.show(uiStage);
    }

    private void renameProject(FileHandle file) {
        VisDialog dialog = new VisDialog("Rename Project");
        VisTextField nameField = new VisTextField(file.nameWithoutExtension());
        dialog.add(new VisLabel("New Name:")).padRight(5);
        dialog.add(nameField).growX().row();

        VisTextButton btnConfirm = new VisTextButton("Rename");
        btnConfirm.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                String name = nameField.getText();
                if (name.isEmpty()) return;
                if (!name.endsWith(".json")) name += ".json";

                FileHandle newFile = file.sibling(name);
                if (newFile.exists()) return;

                file.moveTo(newFile);

                if (currentFile != null && currentFile.equals(file)) {
                    currentFile = newFile;
                }

                reloadFileTree();
                dialog.fadeOut();
            }
        });

        dialog.add(btnConfirm).colspan(2).padTop(10);
        dialog.show(uiStage);
    }

    private void duplicateProject(FileHandle file) {
        String base = file.nameWithoutExtension();
        String ext = file.extension();
        int i = 1;
        while (true) {
            FileHandle copy = file.sibling(base + "_copy" + i + "." + ext);
            if (!copy.exists()) {
                file.copyTo(copy);
                reloadFileTree();
                return;
            }
            i++;
        }
    }

    public void saveProject() {
        if (currentFile == null) {
            currentFile = Gd.files.local(PROJECT_DIR + "/Project_" + System.currentTimeMillis() + ".json");
        }
        saveProjectToFile(sceneManager.getRoot(), currentFile);
        // markDirty(false); // Moved to saveProjectToFile
        reloadFileTree();
    }

    private Json createJson() {
        Json json = new Json();
        json.setOutputType(OutputType.json);
        json.setIgnoreUnknownFields(true);
        json.addClassTag("Group", GroupNode.class);
        json.addClassTag("Rectangle", RectShape.class);
        json.addClassTag("Circle", CircleShape.class);
        return json;
    }

    private void saveProjectToFile(EditorTarget root, FileHandle file) {
        Json json = createJson();
        file.writeString(json.prettyPrint(root), false, "UTF-8");

        savedHistoryState = commandManager.getLastCommand();
        markDirty(false);

        Gdx.app.log("Editor", "Saved project to " + file.path());

        // 显示保存成功提示
        ToastUI.inst().show("Saved successfully!");
    }

    private void loadProject(FileHandle file) {
        try {
            Json json = createJson();

            // Try to load as GroupNode (default root)
            GroupNode root = json.fromJson(GroupNode.class, file);

            if (root != null) {
                fixParentRefs(root);

                // [关键修复] 确保在设置新 Root 前清理旧状态
                sceneManager.selectNode(null); // 先取消选中，避免引用到旧对象
                sceneManager.setRoot(root);    // 设置新 Root，内部会触发 notifyStructureChanged -> onStructureChanged -> 重建 UI

                // [修复] 先清理旧文件的 dirty 状态，再切换 currentFile
                if (isDirty) {
                    markDirty(false);
                }

                currentFile = file;
                // markDirty(false); // 已在上面处理

                commandManager.clear();
                savedHistoryState = null;

                Gdx.app.log("Editor", "Loaded project from " + file.path());
            }
        } catch (Exception e) {
            Gdx.app.error("Editor", "Load failed", e);
        }
    }

    public void markDirty(boolean dirty) {
        if (this.isDirty == dirty) return;
        this.isDirty = dirty;

        if (currentFile != null) {
            for (FileNode node : fileTree.getNodes()) {
                if (node.getValue().equals(currentFile)) {
                    node.setDirty(dirty);
                    break;
                }
            }
        }
    }

    // ========================================================================
    // UI Layout (修复工具栏布局)
    // ========================================================================

    private void buildLayout() {
        Table root = new Table();
        root.setFillParent(true);
        uiStage.addActor(root);

        // 1. 左侧面板 (Split: FileTree / Hierarchy)
        // A. File Tree Panel
        VisTable fileTreePanel = new VisTable(true);
        fileTreePanel.setBackground("window-bg");

        VisTable fileToolbar = new VisTable();
        fileToolbar.add(new VisLabel("Projects")).expandX().left();
        // Refresh Button
        VisTextButton btnRefresh = new VisTextButton("R");
        btnRefresh.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { reloadFileTree(); } });
        fileToolbar.add(btnRefresh);
        fileTreePanel.add(fileToolbar).growX().pad(5).row();

        fileTree = new VisTree<>();
        fileTree.getSelection().setProgrammaticChangeEvents(false);

        // [New] File Tree Context Menu & Background Click & Double Click Load
        fileTreePanel.addListener(new ActorGestureListener() {
            @Override
            public boolean longPress(Actor actor, float x, float y) {
                // 如果点击的是节点，则忽略背景事件
                if (fileTree.getOverNode() != null) return false;

                showFileTreeMenu(null, actor.localToStageCoordinates(new Vector2(x, y)).x, actor.localToStageCoordinates(new Vector2(x, y)).y);
                return true;
            }
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {
                // 如果点击的是节点，则忽略背景事件
                if (fileTree.getOverNode() != null) return;

                if (button == Input.Buttons.RIGHT) {
                    showFileTreeMenu(null, event.getStageX(), event.getStageY());
                }
            }
        });

        fileTreePanel.add(new VisScrollPane(fileTree)).grow();

        // B. Hierarchy Panel
        VisTable hierarchyPanel = new VisTable(true);
        hierarchyPanel.setBackground("window-bg");

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
        hierarchyPanel.add(leftToolbar).growX().pad(5).row();

        hierarchyTree = new VisTree<>();
        // [核心修复] 增加缩进宽度，让层级更明显
        hierarchyTree.setIndentSpacing(25f);
        hierarchyTree.getSelection().setProgrammaticChangeEvents(false);
        hierarchyTree.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    if (hierarchyTree.getSelection().size() == 0) {
                        sceneManager.selectNode(null);
                        return;
                    }
                    UiNode sel = hierarchyTree.getSelection().first();
                    if (sel != null) sceneManager.selectNode(sel.getValue());
                    else sceneManager.selectNode(null);
                }
            });

        onStructureChanged();
        hierarchyPanel.add(new VisScrollPane(hierarchyTree)).grow();

        // C. Left Split Pane
        VisSplitPane leftSplit = new VisSplitPane(fileTreePanel, hierarchyPanel, true);
        leftSplit.setSplitAmount(0.3f);

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

        // 添加页面到propertiesStack
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

        topBar.add().width(15); // Separator

        // 网格对齐复选框
        com.kotcrab.vis.ui.widget.VisCheckBox gridAlignCheck = new com.kotcrab.vis.ui.widget.VisCheckBox("Grid");
        gridAlignCheck.setChecked(isGridAlignEnabled);
        gridAlignCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isGridAlignEnabled = gridAlignCheck.isChecked();
            }
        });
        topBar.add(gridAlignCheck).padRight(5);

        // 对齐精度滑条
        topBar.add(new VisLabel("Precision: ")).padRight(5);
        com.kotcrab.vis.ui.widget.VisSlider precisionSlider = new com.kotcrab.vis.ui.widget.VisSlider(0, ALIGN_PRECISIONS.length - 1, 1, false);
        precisionSlider.setValue(currentPrecisionIndex);
        VisLabel precisionValueLabel = new VisLabel(String.valueOf(ALIGN_PRECISIONS[currentPrecisionIndex]));

        precisionSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentPrecisionIndex = (int) precisionSlider.getValue();
                precisionValueLabel.setText(String.valueOf(ALIGN_PRECISIONS[currentPrecisionIndex]));
            }
        });

        topBar.add(precisionSlider).width(100).padRight(5);
        topBar.add(precisionValueLabel).padRight(5);

        topBar.add().width(15); // Separator

        addToolBtn(topBar, "SAVE", () -> saveProject());

        centerStack.add(topBar);

        // History Panel (附着于检查器面板左边下方)
        historyPanel = new CommandHistoryUI(commandManager, rightPanel);
        historyPanel.setVisible(true); // 默认显示

        // 添加到UI舞台，使用附着逻辑定位
        uiStage.addActor(historyPanel);

        // 4. 分割窗格组合
        VisSplitPane rightSplit = new VisSplitPane(centerStack, rightPanel, false);
        rightSplit.setSplitAmount(0.75f);

        VisSplitPane mainSplit = new VisSplitPane(leftSplit, rightSplit, false);
        mainSplit.setSplitAmount(0.2f);

        root.add(mainSplit).grow();

        initProjectSystem();
    }

    private void addToolBtn(Table t, String text, Runnable act) {
        VisTextButton b = new VisTextButton(text);
        b.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { act.run(); } });
        // 让按钮自动调整大小以适应内容，添加适当内边距防止文字突出
        b.getLabelCell().pad(4, 8, 4, 8); // 添加内边距
        b.pack();
        t.add(b).padRight(5);
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
            Json json = createJson();
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
            Json json = createJson();

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

                markDirty(true); // JSON edit breaks undo history, force dirty

                Gdx.app.log("Editor", "Applied JSON changes");
            }
        } catch (Exception e) {
            Gdx.app.error("Editor", "Apply failed", e);
        }
    }

    private void fixParentRefs(EditorTarget node) {
        for (EditorTarget child : node.getChildren()) {
            if (child instanceof BaseNode) {
                ((BaseNode)child).restoreParentLink(node);
            }
            fixParentRefs(child);
        }
    }

    private void onStructureChanged() {
        // markDirty(true); // Handled by CommandManager listener
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
}
