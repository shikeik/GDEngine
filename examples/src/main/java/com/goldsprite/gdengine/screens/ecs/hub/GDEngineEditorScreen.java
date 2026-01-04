package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.ecs.GameRunnerScreen;
import com.goldsprite.gdengine.screens.ecs.editor.Gd;
import com.goldsprite.solofight.ui.widget.BioCodeEditor;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisTree;

/**
 * 屏幕 2: 引擎编辑器 (IDE)
 * 集成开发环境：代码编辑 + 编译运行
 */
public class GDEngineEditorScreen extends GScreen {

    private Stage stage;
    private BioCodeEditor codeEditor;
    private VisTree<FileNode, FileHandle> fileTree;
    private VisLabel statusLabel;
    private FileHandle currentEditingFile;

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

	@Override
	protected void initViewport() {
		uiViewportScale = 1.5f;
		super.initViewport();
	}

	@Override
	public void show() {
		super.show();
        // 每次进入时刷新文件树，防止外部修改导致不同步
        reloadProjectTree();
	}

    @Override
    public void create() {
        // 1. 使用基类视口
        stage = new Stage(getUIViewport());
        getImp().addProcessor(stage);

        // 2. 根布局
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg");
        stage.addActor(root);

        // --- 顶部工具栏 ---
        VisTable toolbar = new VisTable();
        toolbar.setBackground("button");

        VisTextButton btnBack = new VisTextButton("<< Back");
        btnBack.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					getScreenManager().popLastScreen();
				}
			});

        VisTextButton btnSave = new VisTextButton("Save");
        btnSave.setColor(Color.YELLOW);
        btnSave.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					saveCurrentFile();
				}
			});

		// [新增] 运行按钮
        VisTextButton btnRun = new VisTextButton("▶ Run");
        btnRun.setColor(Color.CYAN);
        btnRun.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					buildAndRun();
				}
			});

        VisTextButton btnAdd = new VisTextButton("+ Script");
        btnAdd.setColor(Color.GREEN);
        btnAdd.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					showCreateFileDialog();
				}
			});

        statusLabel = new VisLabel("Ready");
        statusLabel.setColor(Color.LIGHT_GRAY);

        toolbar.add(btnBack).padRight(10);
        toolbar.add(btnAdd).padRight(10);
        toolbar.add(btnSave).padRight(10);
        toolbar.add(btnRun).padRight(20); // Run 在 Save 后面
        toolbar.add(statusLabel).expandX().left();

        root.add(toolbar).growX().height(50).row();

        // --- 主体分割区域 ---

        // 左侧：文件树
        VisTable leftPanel = new VisTable();
        leftPanel.setBackground("window-bg");

        fileTree = new VisTree<>();
        fileTree.getSelection().setProgrammaticChangeEvents(false);

        VisScrollPane treeScroll = new VisScrollPane(fileTree);
        treeScroll.setFadeScrollBars(false);
        leftPanel.add(treeScroll).grow().pad(5);

        // 右侧：代码编辑器
        VisTable rightPanel = new VisTable();
        codeEditor = new BioCodeEditor();
        rightPanel.add(codeEditor).grow();

        // 分割面板
        VisSplitPane splitPane = new VisSplitPane(leftPanel, rightPanel, false);
        splitPane.setSplitAmount(0.25f);

        root.add(splitPane).grow();

        // --- 初始化数据 ---
        reloadProjectTree();
    }

    /**
     * [核心] 编译并运行逻辑
     */
    private void buildAndRun() {
        // 1. 自动保存当前文件
        if (currentEditingFile != null) {
            saveCurrentFile();
        }

        // 2. 获取项目上下文
        FileHandle projectDir = GDEngineHubScreen.ProjectManager.currentProject;
        if (projectDir == null) {
            statusLabel.setText("Error: No Project Context");
            return;
        }

        // 3. 检查编译器
        if (Gd.compiler == null) {
            statusLabel.setText("Error: No Compiler Available");
            Debug.logT("Editor", "Compiler is null. Are you on Desktop without a compiler impl?");
            return;
        }

        statusLabel.setText("Compiling...");
        statusLabel.setColor(Color.YELLOW);

        // 4. 异步编译 (防止卡死 UI)
        new Thread(() -> {
            try {
                // 目前硬编码入口类，后续可从 project.json 读取
                String entryClass = "com.game.Main"; 
                String projectPath = projectDir.file().getAbsolutePath();

                Debug.logT("Editor", "Start building project: %s", projectPath);

                // 执行编译
                Class<?> clazz = Gd.compiler.compile(entryClass, projectPath);

                if (clazz == null) {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Compile Failed");
                        statusLabel.setColor(Color.RED);
                    });
                    return;
                }

                // 验证接口契约
                if (!IGameScriptEntry.class.isAssignableFrom(clazz)) {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Error: Main must implement IGameScriptEntry");
                        statusLabel.setColor(Color.RED);
                        Debug.logT("Editor", "Class %s does not implement IGameScriptEntry", clazz.getName());
                    });
                    return;
                }

                // 实例化
                Object instance = clazz.newInstance();
                IGameScriptEntry gameEntry = (IGameScriptEntry) instance;

                // 回到主线程启动容器
                Gdx.app.postRunnable(() -> {
                    statusLabel.setText("Running...");
                    statusLabel.setColor(Color.GREEN);
                    Debug.logT("Editor", "Build success! Launching GameRunner...");

                    // 跳转到游戏容器屏
                    getScreenManager().setCurScreen(new GameRunnerScreen(gameEntry));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Gdx.app.postRunnable(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setColor(Color.RED);
                    Debug.logT("Editor", "Exception during build/run: %s", e.toString());
                });
            }
        }).start();
    }

    private void reloadProjectTree() {
        fileTree.clearChildren();

        FileHandle currentProj = GDEngineHubScreen.ProjectManager.currentProject;

        if (currentProj == null || !currentProj.exists()) {
            statusLabel.setText("Error: No Project Loaded");
            return;
        }

        FileHandle scriptsDir = currentProj.child("Scripts");
        if (!scriptsDir.exists()) scriptsDir.mkdirs();

        FileNode rootNode = new FileNode(currentProj);
        rootNode.setExpanded(true);
        fileTree.add(rootNode);

        recursiveAddNodes(rootNode, scriptsDir);
    }

    private void recursiveAddNodes(FileNode parentNode, FileHandle dir) {
        FileHandle[] files = dir.list();
        for (FileHandle file : files) {
            FileNode node = new FileNode(file);
            parentNode.add(node);

            // 绑定交互逻辑
            node.getActor().addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						if (file.isDirectory()) {
							node.setExpanded(!node.isExpanded());
						} else {
							loadFile(file);
						}
					}
				});

            if (file.isDirectory()) {
                recursiveAddNodes(node, file);
            }
        }
    }

    private void loadFile(FileHandle file) {
        this.currentEditingFile = file;
        codeEditor.setText(file.readString());
        statusLabel.setText("Editing: " + file.name());
        statusLabel.setColor(Color.LIGHT_GRAY);
    }

    private void saveCurrentFile() {
        if (currentEditingFile != null) {
            try {
                currentEditingFile.writeString(codeEditor.getText(), false);
                statusLabel.setText("Saved: " + currentEditingFile.name());
                statusLabel.setColor(Color.GREEN);
                statusLabel.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
										  com.badlogic.gdx.scenes.scene2d.actions.Actions.color(Color.GREEN, 0.2f),
										  com.badlogic.gdx.scenes.scene2d.actions.Actions.color(Color.LIGHT_GRAY, 0.5f)
									  ));
            } catch (Exception e) {
                statusLabel.setText("Save Failed: " + e.getMessage());
                statusLabel.setColor(Color.RED);
            }
        } else {
            statusLabel.setText("No file selected.");
        }
    }

    private void showCreateFileDialog() {
		FileHandle currentProj = GDEngineHubScreen.ProjectManager.currentProject;
		if (currentProj == null) return;

		FileHandle scriptsRoot = currentProj.child("Scripts");
		FileHandle targetDir = scriptsRoot;

		if (!fileTree.getSelection().isEmpty()) {
			FileNode selectedNode = fileTree.getSelection().first();
			FileHandle selectedFile = selectedNode.getValue();
			if (selectedFile.isDirectory()) {
				targetDir = selectedFile;
			} else {
				targetDir = selectedFile.parent();
			}
		}

		if (!targetDir.path().startsWith(scriptsRoot.path())) {
			targetDir = scriptsRoot;
		}

		final FileHandle finalTargetDir = targetDir;

		VisDialog dialog = new VisDialog("New Script");
		dialog.setModal(true);
		dialog.closeOnEscape();
		dialog.addCloseButton();
		TableUtils.setSpacingDefaults(dialog);

		String relativePath = finalTargetDir.path().substring(scriptsRoot.path().length());
		if(relativePath.isEmpty()) relativePath = "/ (Root)";
		dialog.add(new VisLabel("Target: " + relativePath)).colspan(2).left().row();

		dialog.add(new VisLabel("Class Name:")).left();
		VisTextField nameField = new VisTextField();
		dialog.add(nameField).width(200).row();

		VisLabel errLabel = new VisLabel("");
		errLabel.setColor(Color.RED);
		dialog.add(errLabel).colspan(2).row();

		VisTextButton btnCreate = new VisTextButton("Create");
		dialog.add(btnCreate).colspan(2).right();

		btnCreate.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					String name = nameField.getText().trim();
					if (name.isEmpty() || !name.matches("[a-zA-Z0-9_]+")) {
						errLabel.setText("Invalid Name");
						dialog.pack();
						return;
					}

					FileHandle newFile = finalTargetDir.child(name + ".java");

					if (newFile.exists()) {
						errLabel.setText("File Exists");
						dialog.pack();
						return;
					}

					String packageStmt = "";
					String relPath = finalTargetDir.path().replace(scriptsRoot.path(), "");
					if (relPath.startsWith("/")) relPath = relPath.substring(1);
					if (relPath.startsWith("\\")) relPath = relPath.substring(1);

					if (!relPath.isEmpty()) {
						String packageName = relPath.replace("/", ".").replace("\\", ".");
						packageStmt = "package " + packageName + ";\n\n";
					}

					String template = packageStmt + 
						"import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;\n" +
						"import com.goldsprite.gdengine.ecs.GameWorld;\n" +
						"import com.goldsprite.gdengine.log.Debug;\n\n" +
						"public class " + name + " implements IGameScriptEntry {\n" +
						"    @Override\n" +
						"    public void onStart(GameWorld world) {\n" +
						"        Debug.logT(\"Script\", \"" + name + " started!\");\n" +
						"    }\n" +
						"}";

					newFile.writeString(template, false);

					dialog.fadeOut();
					reloadProjectTree();
					loadFile(newFile);
				}
			});

		dialog.pack();
		dialog.centerWindow();
		stage.addActor(dialog.fadeIn());
	}

    @Override
    public void render0(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
    }

    public static class FileNode extends Tree.Node<FileNode, FileHandle, VisLabel> {
        public FileNode(FileHandle file) {
            super(new VisLabel(file.name()));
            setValue(file);
            VisLabel label = getActor();
            if (file.isDirectory()) label.setColor(Color.GOLD);
            label.setFontScale(1.1f);
        }
    }
}
