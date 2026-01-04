package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.IDEConsole;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class GDEngineHubScreen extends GScreen {

	private Stage stage;
	private VisTable projectListTable;
	private NeonBatch neonBatch;
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
		Debug.showDebugUI = false; // 进入 Hub 隐藏全局 DebugUI
		refreshList();
	}

	@Override
	public void hide() {
		super.hide();
		Debug.showDebugUI = true; // 离开时恢复
	}

	@Override
	public void create() {
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);
		neonBatch = new NeonBatch();

		initMainLayout();
		refreshList();
	}

	private void initMainLayout() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.top().pad(20);
		stage.addActor(root);

		// 1. Top Bar
		VisTable topBar = new VisTable();
		VisLabel titleLabel = new VisLabel("GDProject Hub");
		titleLabel.setFontScale(1.5f);
		titleLabel.setColor(Color.CYAN);

		VisTextButton btnCreate = new VisTextButton("[ + New Project ]");
		btnCreate.setColor(Color.GREEN);
		btnCreate.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				stage.addActor(new CreateProjectDialog(GDEngineHubScreen.this::refreshList).fadeIn());
			}
		});

		topBar.add(titleLabel).expandX().left();
		topBar.add(btnCreate).right().height(50);
		root.add(topBar).growX().height(60).padBottom(10).row();

		// 2. Project List
		projectListTable = new VisTable();
		projectListTable.top();

		VisScrollPane scrollPane = new VisScrollPane(projectListTable);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false);

		VisTable container = new VisTable();
		container.setBackground("window-bg");
		container.add(scrollPane).grow().pad(5);

		root.add(container).grow().row();

		// 3. Console
		console = new IDEConsole();
		root.add(console).growX();
	}

	public void refreshList() {
		projectListTable.clearChildren();
		Array<FileHandle> projects = ProjectManager.listProjects();

		if (projects.size == 0) {
			VisLabel emptyLabel = new VisLabel("No projects found.\nClick [+ New Project] to start.", Align.center);
			emptyLabel.setColor(Color.GRAY);
			projectListTable.add(emptyLabel).padTop(100);
			return;
		}

		for (FileHandle projDir : projects) {
			VisTable item = new VisTable();
			item.setBackground("button");
			item.setTouchable(Touchable.enabled); // 关键：确保 Table 可点击
			item.pad(10);

			VisLabel nameLbl = new VisLabel(projDir.name());
			nameLbl.setFontScale(1.2f);
			item.add(new VisLabel("📁 ")).padRight(10);
			item.add(nameLbl).expandX().left();

			VisLabel pathLbl = new VisLabel(projDir.path());
			pathLbl.setColor(Color.GRAY);
			pathLbl.setFontScale(0.7f);
			item.add(pathLbl).right().padRight(20);

			// 整个条目点击事件
			item.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					stage.addActor(new ConfirmOpenDialog(projDir.name(), () -> {
						openProject(projDir);
					}).fadeIn());
				}
			});

			projectListTable.add(item).growX().height(80).padBottom(10).row();
		}
	}

	private void openProject(FileHandle projectDir) {
		ProjectManager.currentProject = projectDir;
		Debug.logT("Hub", "Opening project: %s", projectDir.path());
		getScreenManager().setCurScreen(GDEngineEditorScreen.class, true);
	}

	@Override
	public void render0(float delta) {
		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		float w = getWorldCamera().viewportWidth;
		float h = getWorldCamera().viewportHeight;
		float cx = getWorldCamera().position.x;
		float cy = getWorldCamera().position.y;
		neonBatch.setColor(1, 1, 1, 0.05f);
		for (float x = cx - w/2; x < cx + w/2; x+=100) neonBatch.drawLine(x, cy-h/2, x, cy+h/2, 1, Color.GRAY);
		for (float y = cy - h/2; y < cy + h/2; y+=100) neonBatch.drawLine(cx-w/2, y, cx+w/2, y, 1, Color.GRAY);
		neonBatch.setColor(Color.WHITE);
		neonBatch.end();

		stage.act(delta);
		stage.draw();
	}

	@Override
	public void dispose() {
		if (stage != null) stage.dispose();
		if (neonBatch != null) neonBatch.dispose();
	}

	// =========================================================================================
	// Logic: ProjectManager
	// =========================================================================================
	public static class ProjectManager {
		public static final String ROOT_DIR = "Projects";
		public static FileHandle currentProject;

		public static Array<FileHandle> listProjects() {
			FileHandle root = Gdx.files.local(ROOT_DIR);
			if (!root.exists()) {
				root.mkdirs();
				return new Array<>();
			}
			FileHandle[] files = root.list();
			Array<FileHandle> projects = new Array<>();
			for (FileHandle f : files) {
				if (f.isDirectory()) projects.add(f);
			}
			return projects;
		}

		public static String createProject(String name, String packageName) {
			if (name == null || name.trim().isEmpty()) return "Name cannot be empty.";
			if (!name.matches("[a-zA-Z0-9_]+")) return "Invalid project name.";
			if (packageName == null || packageName.trim().isEmpty()) return "Package cannot be empty.";

			FileHandle projectDir = Gdx.files.local(ROOT_DIR).child(name);
			if (projectDir.exists()) return "Project already exists.";

			try {
				projectDir.mkdirs();
				FileHandle srcDir = projectDir.child("Scripts");
				srcDir.mkdirs();

				ProjectConfig config = new ProjectConfig();
				config.name = name;
				config.entryClass = packageName + ".Main";
				projectDir.child("project.json").writeString(new Json().prettyPrint(config), false);

				// 根据包名生成目录
				String packagePath = packageName.replace('.', '/');
				FileHandle mainFile = srcDir.child(packagePath + "/Main.java");

				String mainCode = "package " + packageName + ";\n\n" +
					"import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;\n" +
					"import com.goldsprite.gdengine.ecs.GameWorld;\n" +
					"import com.goldsprite.gdengine.log.Debug;\n\n" +
					"public class Main implements IGameScriptEntry {\n" +
					"    @Override public void onStart(GameWorld world) {\n" +
					"        Debug.logT(\"Script\", \"Hello " + name + "!\");\n" +
					"    }\n" +
					"}";
				mainFile.writeString(mainCode, false);
				return null;
			} catch (Exception e) {
				return "Error: " + e.getMessage();
			}
		}

		public static class ProjectConfig {
			public String name;
			public String entryClass = "Main";
		}
	}

	// =========================================================================================
	// Dialogs
	// =========================================================================================
	public static class CreateProjectDialog extends VisDialog {
		private final VisTextField nameField;
		private final VisTextField pkgField;
		private final VisLabel errorLabel;
		private final Runnable onSuccess;

		public CreateProjectDialog(Runnable onSuccess) {
			super("Create Project");
			this.onSuccess = onSuccess;
			setModal(true); addCloseButton(); closeOnEscape();
			TableUtils.setSpacingDefaults(this);

			add(new VisLabel("Name:")).left();
			add(nameField = new VisTextField("MyGame")).width(250).row();

			add(new VisLabel("Package:")).left();
			add(pkgField = new VisTextField("com.mygame")).width(250).row();

			add(errorLabel = new VisLabel("")).colspan(2).row();
			errorLabel.setColor(Color.RED);

			VisTextButton createBtn = new VisTextButton("Create");
			createBtn.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					String err = ProjectManager.createProject(nameField.getText(), pkgField.getText());
					if (err == null) { onSuccess.run(); fadeOut(); }
					else { errorLabel.setText(err); pack(); }
				}
			});
			add(createBtn).colspan(2).right();
			pack(); centerWindow();
		}
	}

	public static class ConfirmOpenDialog extends VisDialog {
		private final Runnable onYes;

		public ConfirmOpenDialog(String name, Runnable onYes) {
			super("Confirm");
			this.onYes = onYes;
			setModal(true); addCloseButton(); closeOnEscape();
			TableUtils.setSpacingDefaults(this);
			text("Open project [" + name + "]?");

			button("Yes", true);
			button("No", false);

			pack(); centerWindow();
		}

		@Override
		protected void result(Object object) {
			if ((boolean) object) {
				onYes.run();
			}
		}
	}
}
