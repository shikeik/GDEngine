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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.ecs.GameRunnerScreen;
import com.goldsprite.gdengine.screens.ecs.editor.Gd;
import com.goldsprite.gdengine.ui.widget.IDEConsole;
import com.goldsprite.solofight.ui.widget.BioCodeEditor;
import com.kotcrab.vis.ui.util.TableUtils;
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

import java.util.HashSet;
import java.util.Set;

public class GDEngineEditorScreen extends GScreen {

	private Stage stage;
	private BioCodeEditor codeEditor;
	private VisTree<FileNode, FileHandle> fileTree;
	private VisLabel statusLabel;
	private FileHandle currentEditingFile;
	private IDEConsole console;

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
		Debug.showDebugUI = false; // Hide Global DebugUI

		// 重置状态
		if (codeEditor != null) codeEditor.setText("");
		currentEditingFile = null;
		if (statusLabel != null) {
			statusLabel.setText("Ready");
			statusLabel.setColor(Color.LIGHT_GRAY);
		}
		reloadProjectTree();
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
		root.setFillParent(true);
		root.setBackground("window-bg");
		stage.addActor(root);

		// 1. Toolbar
		VisTable toolbar = new VisTable();
		toolbar.setBackground("button");
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
		codeEditor = new BioCodeEditor();
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
		toolbar.add(statusLabel).expandX().left();
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
			Debug.logT("Editor", "Compiler is null. (Desktop needs implementation)");
			return;
		}

		statusLabel.setText("Compiling...");
		statusLabel.setColor(Color.YELLOW);

		new Thread(() -> {
			try {
				// TODO: Read from project.json
				String entryClass = "com.game.Main";
				String projectPath = projectDir.file().getAbsolutePath();

				Debug.logT("Editor", "Building: %s", projectPath);
				Class<?> clazz = Gd.compiler.compile(entryClass, projectPath);

				if (clazz == null) {
					Gdx.app.postRunnable(() -> { statusLabel.setText("Compile Failed"); statusLabel.setColor(Color.RED); });
					return;
				}

				if (!IGameScriptEntry.class.isAssignableFrom(clazz)) {
					Gdx.app.postRunnable(() -> { statusLabel.setText("Invalid Entry"); Debug.logT("Editor", "Main must implement IGameScriptEntry"); });
					return;
				}

				Object instance = clazz.newInstance();
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

		FileHandle scriptsDir = currentProj.child("Scripts");
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

			// 1. Click: Toggle / Open
			actor.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (event.getButton() == Input.Buttons.RIGHT) return;
					if (file.isDirectory()) {
						node.setExpanded(!node.isExpanded());
					} else {
						loadFile(file);
					}
				}
			});

			// 2. Right Click (PC)
			actor.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (event.getButton() == Input.Buttons.RIGHT) {
						showContextMenu(node, event.getStageX(), event.getStageY());
					}
				}
			});

			// 3. Long Press (Mobile)
			actor.addListener(new ActorGestureListener() {
				@Override
				public boolean longPress(Actor actor, float x, float y) {
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

		if (file.isDirectory()) {
			MenuItem itemScript = new MenuItem("New Script");
			itemScript.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { showCreateDialog(file, true); }});
			menu.addItem(itemScript);

			MenuItem itemFolder = new MenuItem("New Folder");
			itemFolder.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { showCreateDialog(file, false); }});
			menu.addItem(itemFolder);

			menu.addSeparator();
		}

		MenuItem itemDelete = new MenuItem("Delete");
		itemDelete.getLabel().setColor(Color.RED);
		itemDelete.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { showDeleteConfirm(file); }});
		menu.addItem(itemDelete);

		menu.showMenu(stage, x, y);
	}

	// =========================================================================================
	// Dialogs & Helpers
	// =========================================================================================

	private void showDeleteConfirm(FileHandle file) {
		VisDialog dialog = new VisDialog("Delete") {
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
		};
		dialog.text("Are you sure to delete?\n" + file.name());
		dialog.button("Yes", true);
		dialog.button("No", false);
		dialog.pack(); dialog.centerWindow(); stage.addActor(dialog.fadeIn());
	}

	private void showCreateDialog(FileHandle targetDir, boolean isScript) {
		VisDialog dialog = new VisDialog(isScript ? "New Script" : "New Folder");
		dialog.setModal(true);
		// [修复] 使用 dialog. 调用方法
		dialog.addCloseButton();
		dialog.closeOnEscape();

		TableUtils.setSpacingDefaults(dialog);

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

					FileHandle scriptsRoot = GDEngineHubScreen.ProjectManager.currentProject.child("Scripts");
					String relPath = targetDir.path().replace(scriptsRoot.path(), "");
					if (relPath.startsWith("/")) relPath = relPath.substring(1);
					if (relPath.startsWith("\\")) relPath = relPath.substring(1); // Win兼容

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
		dialog.pack();
		dialog.centerWindow(); // [修复]
		stage.addActor(dialog.fadeIn());
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
