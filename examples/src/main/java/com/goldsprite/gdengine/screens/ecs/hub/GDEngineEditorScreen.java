package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.solofight.ui.widget.BioCodeEditor; // 请确保包名正确
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
 * 独立文件实现
 */
public class GDEngineEditorScreen extends GScreen {

    private Stage stage;
    private BioCodeEditor codeEditor;
    // 【修复泛型】明确指定 Node 类型和 Value 类型
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
		// TODO: Implement this method
		super.show();

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
        toolbar.add(btnSave).padRight(20);
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
        rightPanel.add(codeEditor).grow(); // 确保撑满

        // 分割面板
        VisSplitPane splitPane = new VisSplitPane(leftPanel, rightPanel, false);
        splitPane.setSplitAmount(0.25f);

        root.add(splitPane).grow();

        // --- 初始化数据 ---
        reloadProjectTree();
    }

    /**
     * 刷新左侧项目树
     */
    private void reloadProjectTree() {
        fileTree.clearChildren();

        // 【修复引用】通过 HubScreen 类名访问静态内部类
        FileHandle currentProj = GDEngineHubScreen.ProjectManager.currentProject;

        if (currentProj == null || !currentProj.exists()) {
            statusLabel.setText("Error: No Project Loaded");
            return;
        }

        FileHandle scriptsDir = currentProj.child("Scripts");
        if (!scriptsDir.exists()) scriptsDir.mkdirs();

        // 根节点
        FileNode rootNode = new FileNode(currentProj);
        rootNode.setExpanded(true);
        fileTree.add(rootNode);

        // 递归加载
        recursiveAddNodes(rootNode, scriptsDir);
    }

    /**
     * 加载文件
     */
    private void loadFile(FileHandle file) {
        this.currentEditingFile = file;
        codeEditor.setText(file.readString());
        statusLabel.setText("Editing: " + file.name());
    }

    /**
     * 保存文件
     */
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

    /**
     * 新建文件弹窗
     */
    private void showCreateFileDialog() {
		FileHandle currentProj = GDEngineHubScreen.ProjectManager.currentProject;
		if (currentProj == null) return;

		// --- 新增逻辑：确定目标文件夹 ---
		FileHandle scriptsRoot = currentProj.child("Scripts");
		FileHandle targetDir = scriptsRoot; // 默认为根目录

		// 获取树中选中的节点
		if (!fileTree.getSelection().isEmpty()) {
			FileNode selectedNode = fileTree.getSelection().first(); // 获取第一个选中项
			FileHandle selectedFile = selectedNode.getValue();

			if (selectedFile.isDirectory()) {
				targetDir = selectedFile;
			} else {
				targetDir = selectedFile.parent();
			}
		}

		// 确保目标路径是在 Scripts 目录下（防止选到外部去）
		if (!targetDir.path().startsWith(scriptsRoot.path())) {
			targetDir = scriptsRoot;
		}

		final FileHandle finalTargetDir = targetDir; // 供内部类使用
		// --------------------------------

		VisDialog dialog = new VisDialog("New Script");
		dialog.setModal(true);
		dialog.closeOnEscape();
		dialog.addCloseButton();
		TableUtils.setSpacingDefaults(dialog);

		// 显示当前目标路径 (方便用户确认)
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

					// 使用计算出的 finalTargetDir
					FileHandle newFile = finalTargetDir.child(name + ".java");

					if (newFile.exists()) {
						errLabel.setText("File Exists");
						dialog.pack();
						return;
					}

					// --- 动态生成 Package 声明 ---
					// 计算相对于 Scripts 目录的路径，例如 "utils/combat" -> "package utils.combat;"
					String packageStmt = "";
					String relPath = finalTargetDir.path().replace(scriptsRoot.path(), "");
					// 去除开头的斜杠
					if (relPath.startsWith("/")) relPath = relPath.substring(1);
					if (relPath.startsWith("\\")) relPath = relPath.substring(1);

					if (!relPath.isEmpty()) {
						// 将路径分隔符转换为包点号
						String packageName = relPath.replace("/", ".").replace("\\", ".");
						packageStmt = "package " + packageName + ";\n\n";
					}
					// ---------------------------

					String template = packageStmt + 
						"import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;\n" +
						"import com.goldsprite.gdengine.ecs.GameWorld;\n" +
						"import com.goldsprite.gdengine.log.Debug;\n\n" +
						"public class " + name + " implements IGameScriptEntry {\n" +
						"    @Override\n" +
						"    public void onStart(GameWorld world) {\n" +
						"        Debug.log(\"" + name + " started!\");\n" +
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

    // =========================================================================================
    // 【修复泛型】自定义 FileNode 类
    // 继承 Tree.Node <本类类型, 值类型, Actor类型>
    // =========================================================================================
    public static class FileNode extends Tree.Node<FileNode, FileHandle, VisLabel> {

        public FileNode(FileHandle file) {
            super(new VisLabel(file.name()));
            setValue(file);

            // 设置样式
            VisLabel label = getActor();
            // 【修复颜色】使用 Color.GOLD 代替 KHAKI
            if (file.isDirectory()) label.setColor(Color.GOLD);
            label.setFontScale(1.1f);

            // 点击事件
            label.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						if (file.isDirectory()) {
							// 切换展开状态
							setExpanded(!isExpanded());
						} else {
							// 找到屏幕实例并加载 (稍微有点hack，但单例模式下可行，或者通过传参)
							// 这里我们利用 event 冒泡或者简单的上下文。
							// 由于 FileNode 是静态类，无法直接调用外部类的 loadFile。
							// 简单解法：强制转换 Stage 的查找，或者让 Tree 持有回调。
							// 为保持简单，我们假设 fileTree 的 userObject 或者是通过 Stage 找到 Screen
							// 但最稳妥的是：在创建 Node 时传入回调。
						}
					}
				});
        }
    }

    // --- 补充修正：为了让 Node 点击生效，我们需要稍微改一下 reloadProjectTree ---
    // 上面的 FileNode 静态类无法直接调用 loadFile。
    // 我们把 FileNode 改为非静态内部类，或者在 reloadProjectTree 里手动添加 Listener。
    // 为了代码结构清晰，建议在 reloadProjectTree 里**手动添加 Listener**，而不是在 Node 构造函数里。

    // ↓↓↓↓↓ 修正后的 recursiveAddNodes 方法 ↓↓↓↓↓

    private void recursiveAddNodes(FileNode parentNode, FileHandle dir) {
        FileHandle[] files = dir.list();
        for (FileHandle file : files) {
            // 创建 Node (仅负责显示)
            FileNode node = new FileNode(file);
            parentNode.add(node);

            // **在此处绑定交互逻辑** (因为这里能访问 loadFile)
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
}
