package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.ecs.GameRunnerScreen;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.IDEConsole;
import com.goldsprite.solofight.ui.widget.BioCodeEditor;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisTree;

import java.util.HashSet;
import java.util.Set;

public class GDEngineEditorScreen extends GScreen {

	private Stage stage;
	private BioCodeEditor codeEditor;
	private VisTree<FileNode, FileHandle> fileTree;
	private VisLabel statusLabel;
	private FileHandle currentEditingFile;
	private IDEConsole console;
	private VisTextButton btnUpdate;
	private VisLabel verLabel;
	private VisTable toolbar;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	protected void initViewport() {
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.5f : 2.0f;
		super.initViewport();
	}

	@Override
	public void show() {
		super.show();
		Debug.showDebugUI = false; // Hide Global DebugUI

		// 重置状态
		if (codeEditor != null) codeEditor.setText("");
		currentEditingFile = null;
		if (statusLabel != null) {
			statusLabel.setText("Ready");
			statusLabel.setColor(Color.LIGHT_GRAY);
		}
		reloadProjectTree();
		createToolbar(toolbar);
	}

	@Override
	public void hide() {
		super.hide();
		Debug.showDebugUI = true; // Restore Global DebugUI
	}

	@Override
	public void create() {
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		VisTable root = new VisTable();
		float pad = 35;
		root.padLeft(pad).padRight(pad);
		root.setFillParent(true);
		root.setBackground("window-bg");
		stage.addActor(root);

		// 1. Toolbar
		toolbar = new VisTable();
		toolbar.setBackground("window-bg");
		createToolbar(toolbar);
		root.add(toolbar).growX().height(50).row();

		// 2. Main Content (SplitPane: Tree + Code)
		VisTable leftPanel = new VisTable();
		leftPanel.setBackground("window-bg");

		fileTree = new VisTree<>();
		fileTree.getSelection().setProgrammaticChangeEvents(false);

		VisScrollPane treeScroll = new VisScrollPane(fileTree);
		treeScroll.setFadeScrollBars(false);
		leftPanel.add(treeScroll).grow().pad(5);

		VisTable rightPanel = new VisTable();
		codeEditor = new BioCodeEditor(PlatformImpl.isAndroidUser()? 1f : 1.5f);
		// [新增] 注入 Ctrl+S 回调
		codeEditor.setOnSave(this::saveCurrentFile);
		rightPanel.add(codeEditor).grow();

		VisSplitPane splitPane = new VisSplitPane(leftPanel, rightPanel, false);
		splitPane.setSplitAmount(0.25f);
		root.add(splitPane).grow().row();

		// 3. Console
		console = new IDEConsole();
		root.add(console).growX();

		reloadProjectTree();
	}

	private void createToolbar(Table toolbar) {
		toolbar.clear();

		VisTextButton btnBack = new VisTextButton("<< Back");
		btnBack.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { getScreenManager().popLastScreen(); }});

		VisTextButton btnSave = new VisTextButton("Save");
		btnSave.setColor(Color.YELLOW);
		btnSave.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { saveCurrentFile(); }});

		VisTextButton btnRun = new VisTextButton("▶ Run");
		btnRun.setColor(Color.CYAN);
		btnRun.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { buildAndRun(); }});

		statusLabel = new VisLabel("Ready");
		statusLabel.setColor(Color.LIGHT_GRAY);

		toolbar.add(btnBack).padRight(10);
		toolbar.add(btnSave).padRight(10);
		toolbar.add(btnRun).padRight(20);

		// [修改] 仅在开发者模式下显示 Export 按钮
		boolean isDevMode = Boolean.getBoolean("gd.dev") || System.getenv("GD_DEV") != null;

		if (isDevMode) {
			VisTextButton btnExport = new VisTextButton("Export Tpl");
			btnExport.setColor(Color.ORANGE);
			btnExport.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (currentEditingFile != null) saveCurrentFile();
					FileHandle proj = GDEngineHubScreen.ProjectManager.currentProject;
					if (proj != null) {
						new GDEngineHubScreen.ExportTemplateDialog(proj).show(stage);
					}
				}
			});
			toolbar.add(btnExport).padRight(20);
		}

		// [新增] 版本检查与更新按钮
		checkEngineUpdate(toolbar);

		toolbar.add(statusLabel).expandX().left();
	}

	private void checkEngineUpdate(Table toolbar) {
		FileHandle projectDir = GDEngineHubScreen.ProjectManager.currentProject;
		if (projectDir == null) return;

		try {
			FileHandle configFile = projectDir.child("project.json");
			if (configFile.exists()) {
				GDEngineHubScreen.ProjectManager.ProjectConfig cfg = new Json().fromJson(
					GDEngineHubScreen.ProjectManager.ProjectConfig.class, configFile);

				String currentEngineVer = com.goldsprite.solofight.BuildConfig.DEV_VERSION;
				String projectEngineVer = cfg.engineVersion;

				if (projectEngineVer == null || !projectEngineVer.equals(currentEngineVer)) {
					// 显示更新按钮
					btnUpdate = new VisTextButton("");
					btnUpdate.addListener(new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							showUpdateConfirmDialog(projectDir, currentEngineVer);
						}
					});
					toolbar.add(btnUpdate).padRight(20);
				} else {
					verLabel = new VisLabel();
					toolbar.add(verLabel).padRight(20);
				}

				refreshBtnUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void refreshBtnUpdate() {
		if(btnUpdate == null && verLabel == null) return;

		FileHandle projectDir = GDEngineHubScreen.ProjectManager.currentProject;
		FileHandle configFile = projectDir.child("project.json");
		if(!configFile.exists()){
			btnUpdate.setText("v?");
			return;
		}
		GDEngineHubScreen.ProjectManager.ProjectConfig cfg = new Json().fromJson(
			GDEngineHubScreen.ProjectManager.ProjectConfig.class, configFile);
		String currentEngineVer = com.goldsprite.solofight.BuildConfig.DEV_VERSION;
		String projectEngineVer = cfg.engineVersion;

		if (projectEngineVer == null || !projectEngineVer.equals(currentEngineVer)) {
			String txt = "Update Libs (" + (projectEngineVer == null ? "?" : projectEngineVer) + "->" + currentEngineVer + ")";
			btnUpdate.setText(txt);
			btnUpdate.setColor(Color.YELLOW);
		}else{
			verLabel.setText("v" + projectEngineVer);
			verLabel.setColor(Color.DARK_GRAY);
		}
	}

	private void showUpdateConfirmDialog(FileHandle projectDir, String targetVer) {
		new BaseDialog("Update Engine Libs") {
			@Override
			protected void result(Object object) {
				if ((boolean) object) performUpdate(projectDir, targetVer);
			}
		}.text("Update libs to v" + targetVer + "?\nOverwrite 'libs/' folder.")
			.button("Update", true).button("Cancel", false).show(stage);
	}

	private void performUpdate(FileHandle projectDir, String targetVer) {
		try {
			// 1. Update Libs (修正路径: libs/)
			FileHandle sourceLibs = Gd.files.internal("engine/libs");
			FileHandle targetLibs = projectDir.child("libs");

			if (targetLibs.exists()) targetLibs.deleteDirectory();
			targetLibs.mkdirs();

			for (FileHandle jar : sourceLibs.list(".jar")) {
				jar.copyTo(targetLibs);
			}

			// 2. Update Config
			FileHandle configFile = projectDir.child("project.json");
			Json json = new Json();
			GDEngineHubScreen.ProjectManager.ProjectConfig cfg = json.fromJson(
				GDEngineHubScreen.ProjectManager.ProjectConfig.class, configFile);

			cfg.engineVersion = targetVer;
			configFile.writeString(json.prettyPrint(cfg), false, "UTF-8");

			statusLabel.setText("Updated to v" + targetVer);
			statusLabel.setColor(Color.GREEN);

			// 简单刷新：重新进入界面以更新按钮状态
			getScreenManager().popLastScreen();
			getScreenManager().setCurScreen(new GDEngineEditorScreen());

		} catch (Exception e) {
			statusLabel.setText("Update Failed: " + e.getMessage());
			statusLabel.setColor(Color.RED);
		}
	}

	// =========================================================================================
	// Core Logic: Build & Run
	// =========================================================================================
	private void buildAndRun() {
		if (currentEditingFile != null) saveCurrentFile();

		FileHandle projectDir = GDEngineHubScreen.ProjectManager.currentProject;
		if (projectDir == null) { statusLabel.setText("Error: No Project"); return; }

		if (Gd.compiler == null) {
			statusLabel.setText("Error: No Compiler");
			return;
		}

		statusLabel.setText("Compiling...");
		statusLabel.setColor(Color.YELLOW);

		new Thread(() -> {
			try {
				// [核心修复] 动态读取入口类名
				String entryClass = "com.game.Main"; // 默认兜底
				FileHandle configFile = projectDir.child("project.json");

				if (configFile.exists()) {
					try {
						GDEngineHubScreen.ProjectManager.ProjectConfig cfg = new Json().fromJson(GDEngineHubScreen.ProjectManager.ProjectConfig.class, configFile);
						if (cfg != null && cfg.entryClass != null && !cfg.entryClass.isEmpty()) {
							entryClass = cfg.entryClass;
						}
					} catch (Exception e) {
						Debug.logT("Editor", "⚠️ 读取配置失败，使用默认入口: " + e.getMessage());
					}
				}

				String projectPath = projectDir.file().getAbsolutePath();

				Debug.logT("Editor", "Building: %s (Entry: %s)", projectPath, entryClass);


				// [新增] 注入资源上下文到 Core 层
				// 这样 GameRunner 里的组件就能通过 GameWorld.getAsset() 找到图了
				GameWorld.projectAssetsRoot = projectDir.child("assets");
				if (!GameWorld.projectAssetsRoot.exists()) {
					GameWorld.projectAssetsRoot.mkdirs(); // 防御性创建
				}

				// 编译
				Class<?> clazz = Gd.compiler.compile(entryClass, projectPath);

				if (clazz == null) {
					Gdx.app.postRunnable(() -> { statusLabel.setText("Compile Failed"); statusLabel.setColor(Color.RED); });
					return;
				}

				if (!IGameScriptEntry.class.isAssignableFrom(clazz)) {
					Gdx.app.postRunnable(() -> { statusLabel.setText("Invalid Entry"); Debug.logT("Editor", "Main must implement IGameScriptEntry"); });
					return;
				}

				Object instance = clazz.getDeclaredConstructor().newInstance();
				IGameScriptEntry gameEntry = (IGameScriptEntry) instance;

				Gdx.app.postRunnable(() -> {
					statusLabel.setText("Running...");
					statusLabel.setColor(Color.GREEN);
					getScreenManager().setCurScreen(new GameRunnerScreen(gameEntry));
				});

			} catch (Exception e) {
				e.printStackTrace();
				Gdx.app.postRunnable(() -> { statusLabel.setText("Error: " + e.getMessage()); statusLabel.setColor(Color.RED); });
			}
		}).start();
	}

	// =========================================================================================
	// Core Logic: File Tree & Interaction
	// =========================================================================================

	private void reloadProjectTree() {
		// 1. 保存展开状态
		Set<String> expandedPaths = new HashSet<>();
		// [修复] 检查 size > 0
		if (fileTree.getNodes().size > 0) {
			// [修复] 显式转换泛型或直接传递，下面的方法已修正为 Array<FileNode>
			saveExpansionState(fileTree.getNodes(), expandedPaths);
		}

		fileTree.clearChildren();

		FileHandle currentProj = GDEngineHubScreen.ProjectManager.currentProject;
		if (currentProj == null || !currentProj.exists()) {
			statusLabel.setText("Error: No Project Loaded");
			return;
		}

		FileHandle scriptsDir = currentProj.child("src").child("main").child("java");
		if (!scriptsDir.exists()) scriptsDir.mkdirs();

		FileNode rootNode = new FileNode(currentProj);
		rootNode.setExpanded(true);
		fileTree.add(rootNode);

		recursiveAddNodes(rootNode, scriptsDir);

		// 2. 恢复展开状态
		restoreExpansionState(fileTree.getNodes(), expandedPaths);
	}

	private void recursiveAddNodes(FileNode parentNode, FileHandle dir) {
		FileHandle[] files = dir.list();
		java.util.Arrays.sort(files, (a, b) -> {
			if (a.isDirectory() && !b.isDirectory()) return -1;
			if (!a.isDirectory() && b.isDirectory()) return 1;
			return a.name().compareTo(b.name());
		});

		for (FileHandle file : files) {
			FileNode node = new FileNode(file);
			parentNode.add(node);

			Actor actor = node.getActor();

			// [重构] 交互逻辑：使用 ActorGestureListener 统一接管
			// 参数: halfTapSquareSize=20, tapCountInterval=0.4, longPressDuration=0.4 (变快), maxFlingDelay=0.15
			actor.addListener(new ActorGestureListener(20, 0.4f, 0.4f, 0.15f) {

				@Override
				public void tap(InputEvent event, float x, float y, int count, int button) {
					// 左键单击 -> 展开/打开
					if (button == Input.Buttons.LEFT) {
						if (file.isDirectory()) {
							node.setExpanded(!node.isExpanded());
						} else {
							loadFile(file);
						}
					}
					// 右键单击 -> 菜单 (PC习惯)
					else if (button == Input.Buttons.RIGHT) {
						showContextMenu(node, event.getStageX(), event.getStageY());
					}
				}

				@Override
				public boolean longPress(Actor actor, float x, float y) {
					// 长按 -> 菜单 (Mobile习惯)
					// longPress 返回 true 会阻止后续可能的事件，但 tap 是在 touchUp 触发，
					// 而 longPress 是在按下 0.4s 后触发。
					// 只要这里触发了菜单，用户松手时虽然技术上可能触发 tap，但菜单已经模态遮挡或者逻辑上已拦截。
					// 实际上 ActorGestureListener 内部机制保证了 longPress 触发后通常不会再回调 tap。

					com.badlogic.gdx.math.Vector2 v = actor.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(x, y));
					showContextMenu(node, v.x, v.y);
					return true;
				}
			});

			if (file.isDirectory()) {
				recursiveAddNodes(node, file);
			}
		}
	}

	private void showContextMenu(FileNode node, float x, float y) {
		FileHandle file = node.getValue();
		PopupMenu menu = new PopupMenu();

		// [新增] 针对文件的逻辑：检查是否为脚本入口
		if (!file.isDirectory() && file.extension().equals("java")) {
			// 简单的文本扫描，避免编译开销
			String content = file.readString("UTF-8"); // 确保用UTF-8读取

			// 简单的正则匹配：implements ... IGameScriptEntry
			// 兼容带包名和不带包名的写法
			boolean isEntry = content.contains("implements IGameScriptEntry") ||
				content.contains("implements com.goldsprite.gdengine.core.scripting.IGameScriptEntry");

			if (isEntry) {
				MenuItem itemSetMain = new MenuItem("Set as Launch Entry");
				itemSetMain.getLabel().setColor(Color.CYAN);
				itemSetMain.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						setProjectEntry(file);
					}
				});
				menu.addItem(itemSetMain);
				menu.addSeparator();
			}
		}

		if (file.isDirectory()) {
			// ... (原有的创建菜单代码) ...
			MenuItem itemScript = new MenuItem("New Script");
			itemScript.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { showCreateDialog(file, true); }});
			menu.addItem(itemScript);

			MenuItem itemFolder = new MenuItem("New Folder");
			itemFolder.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { showCreateDialog(file, false); }});
			menu.addItem(itemFolder);

			menu.addSeparator();
		}

		// ... (原有的删除菜单代码) ...
		MenuItem itemDelete = new MenuItem("Delete");
		// ...
		menu.addItem(itemDelete);

		menu.showMenu(stage, x, y);
	}

	// [新增] 设置启动入口逻辑
	private void setProjectEntry(FileHandle file) {
		FileHandle projectDir = GDEngineHubScreen.ProjectManager.currentProject;
		FileHandle srcRoot = projectDir.child("src").child("main").child("java");

		// 1. 计算全类名
		// 路径: .../src/main/java/com/mygame/MyScript.java
		// 相对: com/mygame/MyScript.java
		String fullPath = file.path();
		String rootPath = srcRoot.path();

		if (!fullPath.startsWith(rootPath)) {
			statusLabel.setText("Error: File not in src path!");
			return;
		}

		String relPath = fullPath.substring(rootPath.length());
		if (relPath.startsWith("/") || relPath.startsWith("\\")) relPath = relPath.substring(1);

		// com/mygame/MyScript.java -> com.mygame.MyScript
		String className = relPath.replace(".java", "").replace("/", ".").replace("\\", ".");

		// 2. 修改 project.json
		try {
			FileHandle configFile = projectDir.child("project.json");
			GDEngineHubScreen.ProjectManager.ProjectConfig cfg;
			Json json = new Json();

			if (configFile.exists()) {
				cfg = json.fromJson(GDEngineHubScreen.ProjectManager.ProjectConfig.class, configFile);
			} else {
				cfg = new GDEngineHubScreen.ProjectManager.ProjectConfig();
				cfg.name = projectDir.name();
			}

			cfg.entryClass = className;

			// 3. 保存
			configFile.writeString(json.prettyPrint(cfg), false, "UTF-8");

			statusLabel.setText("Entry set: " + className);
			statusLabel.setColor(Color.GREEN);
			statusLabel.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
				com.badlogic.gdx.scenes.scene2d.actions.Actions.color(Color.GREEN, 0.5f),
				com.badlogic.gdx.scenes.scene2d.actions.Actions.color(Color.LIGHT_GRAY, 1f)
			));

		} catch (Exception e) {
			statusLabel.setText("Config Error: " + e.getMessage());
			statusLabel.setColor(Color.RED);
			e.printStackTrace();
		}
	}

	// =========================================================================================
	// Dialogs & Helpers
	// =========================================================================================

	// [修改] 使用 BaseDialog
	private void showDeleteConfirm(FileHandle file) {
		new BaseDialog("Delete") {
			@Override
			protected void result(Object object) {
				if ((boolean) object) {
					if (file.isDirectory()) file.deleteDirectory(); else file.delete();
					if (currentEditingFile != null && currentEditingFile.equals(file)) {
						codeEditor.setText("");
						currentEditingFile = null;
						statusLabel.setText("Deleted.");
					}
					reloadProjectTree();
				}
			}
		}.text("Are you sure to delete?\n" + file.name())
			.button("Yes", true)
			.button("No", false)
			.show(stage); // 使用封装的 show 方法
	}

	// [修改] 使用 BaseDialog
	private void showCreateDialog(FileHandle targetDir, boolean isScript) {
		BaseDialog dialog = new BaseDialog(isScript ? "New Script" : "New Folder");

		dialog.add(new VisLabel("Name:")).left();
		VisTextField nameField = new VisTextField();
		dialog.add(nameField).width(200).row();

		VisLabel errLabel = new VisLabel(""); errLabel.setColor(Color.RED);
		dialog.add(errLabel).colspan(2).row();

		VisTextButton btnCreate = new VisTextButton("Create");
		dialog.add(btnCreate).colspan(2).right();

		btnCreate.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String name = nameField.getText().trim();
				if (name.isEmpty() || !name.matches("[a-zA-Z0-9_]+")) { errLabel.setText("Invalid Name"); dialog.pack(); return; }

				if (isScript) {
					FileHandle newFile = targetDir.child(name + ".java");
					if (newFile.exists()) { errLabel.setText("Exists!"); dialog.pack(); return; }

					FileHandle scriptsRoot = GDEngineHubScreen.ProjectManager.currentProject.child("src").child("main").child("java");
					String relPath = targetDir.path().replace(scriptsRoot.path(), "");
					if (relPath.startsWith("/")) relPath = relPath.substring(1);
					if (relPath.startsWith("\\")) relPath = relPath.substring(1);

					String pkg = relPath.replace("/", ".").replace("\\", ".");
					String pkgStmt = pkg.isEmpty() ? "" : "package " + pkg + ";\n\n";

					String content = pkgStmt +
						"import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;\n" +
						"import com.goldsprite.gdengine.ecs.GameWorld;\n" +
						"import com.goldsprite.gdengine.log.Debug;\n\n" +
						"public class " + name + " implements IGameScriptEntry {\n" +
						"    @Override\n" +
						"    public void onStart(GameWorld world) {\n" +
						"        Debug.logT(\"Script\", \"" + name + " started!\");\n" +
						"    }\n" +
						"}";
					newFile.writeString(content, false);
					loadFile(newFile);
				} else {
					FileHandle newDir = targetDir.child(name);
					if (newDir.exists()) { errLabel.setText("Exists!"); dialog.pack(); return; }
					newDir.mkdirs();
				}
				dialog.fadeOut(); reloadProjectTree();
			}
		});

		dialog.show(stage);
	}

	private void loadFile(FileHandle file) {
		this.currentEditingFile = file;
		codeEditor.setText(file.readString("UTF-8"));
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
		}
	}

	// --- State Persistence Helpers ---
	// [修复] 明确泛型类型 Array<FileNode>
	private void saveExpansionState(Array<FileNode> nodes, Set<String> paths) {
		for (FileNode node : nodes) {
			if (node.isExpanded()) paths.add(node.getValue().path());
			// getChildren() 返回的就是 Array<FileNode>，现在匹配了
			if (node.getChildren().size > 0) saveExpansionState(node.getChildren(), paths);
		}
	}

	private void restoreExpansionState(Array<FileNode> nodes, Set<String> paths) {
		for (FileNode node : nodes) {
			if (paths.contains(node.getValue().path())) node.setExpanded(true);
			if (node.getChildren().size > 0) restoreExpansionState(node.getChildren(), paths);
		}
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
