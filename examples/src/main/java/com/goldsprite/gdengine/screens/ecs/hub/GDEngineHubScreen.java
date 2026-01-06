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
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.IDEConsole;
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
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.5f : 2.5f;
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

		// [新增] 设置按钮
		VisTextButton btnSettings = new VisTextButton("⚙ Settings");
		btnSettings.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// 传入 refreshList 回调，确保修改路径后列表刷新
				new SettingsWindow(GDEngineHubScreen.this::refreshList).show(stage);
			}
		});

		// 使用 show(stage)
		VisTextButton btnCreate = new VisTextButton("[ + New Project ]");
		btnCreate.setColor(Color.GREEN);
		btnCreate.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// 使用 .show(stage) 自动处理 pack 和 center
				new CreateProjectDialog(GDEngineHubScreen.this::refreshList).show(stage);
			}
		});

		topBar.add(titleLabel).expandX().left();
		topBar.add(btnSettings).right().padRight(10).height(50); // 添加到 Create 左边
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
		container.add(scrollPane).grow().pad(20);

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
			nameLbl.setFontScale(1.3f);
			item.add(new VisLabel("📁 ")).padRight(10);
			item.add(nameLbl).expandX().left();

			VisLabel pathLabel = new VisLabel(projDir.path());
			pathLabel.setColor(Color.GRAY);
			pathLabel.setFontScale(0.8f);
			item.add(pathLabel).right().padRight(20);

			// 整个条目点击事件
			item.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					new ConfirmOpenDialog(projDir.name(), () -> {
						openProject(projDir);
					}).show(stage);
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
		public static FileHandle currentProject;

		// [修改] 核心：动态获取根目录
		public static FileHandle getProjectsRoot() {
			String path = Gd.engineConfig.projectsRootPath;
			return Gdx.files.absolute(path);
		}

		public static Array<FileHandle> listProjects() {
			// [适配] 使用 getProjectsRoot
			FileHandle root = getProjectsRoot();
			// ... (后续逻辑不变，复用 root 变量)
			FileHandle[] files = root.list();
			Array<FileHandle> projects = new Array<>();
			if(files != null) { // 防空
				for (FileHandle f : files) {
					if (f.isDirectory()) projects.add(f);
				}
			}
			return projects;
		}

		public static String createProject(String name, String packageName) {
			// 1. 校验
			if (name == null || name.trim().isEmpty()) return "Name cannot be empty.";
			if (!name.matches("[a-zA-Z0-9_]+")) return "Invalid project name.";
			if (packageName == null || packageName.trim().isEmpty()) return "Package cannot be empty.";

			// [适配] 使用 getProjectsRoot 获取目标路径
			FileHandle finalTarget = getProjectsRoot().child(name);
			if (finalTarget.exists()) {
				return "Project already exists!";
			}

			// 临时构建目录
			FileHandle tempRoot = Gdx.files.local("build/tmp_creation");
			FileHandle tempProj = tempRoot.child(name);
			if (tempProj.exists()) tempProj.deleteDirectory();
			tempProj.mkdirs();

			try {
				// ---------------------------------------------------------
				// 1. 核心文件生成 (project.json & Main.java)
				// ---------------------------------------------------------

				// project.json
				FileHandle tplConfig = Gdx.files.internal("script_project_templates/HelloGame/project.json");
				if (!tplConfig.exists()) return "Template missing: project.json";

				String jsonContent = tplConfig.readString("UTF-8");
				jsonContent = jsonContent.replace("${PROJECT_NAME}", name);
				jsonContent = jsonContent.replace("${ENTRY_CLASS}", packageName+".Main");
				tempProj.child("project.json").writeString(jsonContent, false, "UTF-8");

				// Main Script
				FileHandle tplMain = Gdx.files.internal("script_project_templates/HelloGame/Scripts/Main.java");
				if (!tplMain.exists()) return "Template missing: Main.java";

				String mainCode = tplMain.readString("UTF-8");
				mainCode = mainCode.replace("${PACKAGE_NAME}", packageName)
					.replace("${PROJECT_NAME}", name);

				String packagePath = packageName.replace('.', '/');
				FileHandle targetMain = tempProj.child("src").child("main").child("java").child(packagePath).child("Main.java");
				targetMain.parent().mkdirs();
				targetMain.writeString(mainCode, false, "UTF-8");

				// ---------------------------------------------------------
				// 2. IDE 支持 (Gradle & Libs) [新增]
				// ---------------------------------------------------------

				// 2.1 拷贝 build.gradle 模板
				FileHandle tplGradle = Gdx.files.internal("script_project_templates/build.gradle");
				if (tplGradle.exists()) {
					tplGradle.copyTo(tempProj);
				}

				// 2.1 拷贝 settings.gradle 模板
				tplGradle = Gdx.files.internal("script_project_templates/settings.gradle");
				if (tplGradle.exists()) {
					jsonContent = tplGradle.readString("UTF-8");
					jsonContent = jsonContent.replace("${PROJECT_NAME}", name);
					tempProj.child("settings.gradle").writeString(jsonContent, false, "UTF-8");
				}

				// ---------------------------------------------------------
				// [新增] 资源目录 (Assets)
				// ---------------------------------------------------------
				FileHandle assetsTarget = tempProj.child("assets");
				assetsTarget.mkdirs();

				// (可选) 可以在这里拷入一个默认图标，防止空文件夹被某些系统忽略
				 FileHandle defaultIcon = Gdx.files.internal("gd_icon.png");
				 if(defaultIcon.exists()) defaultIcon.copyTo(assetsTarget);

				// 2.2 自动注入依赖库 (遍历 assets/libs)
				// 不需要 AssetUtils，不需要索引逻辑，直接用 Gd.files
				FileHandle libsSource = Gd.files.internal("libs"); // 注意用 Gd.files
				FileHandle libsTarget = tempProj.child("libs");
				libsTarget.mkdirs();

				// 使用 suffix 过滤器，只拷 jar 包，避开可能存在的无关文件
				FileHandle[] jars = libsSource.list(".jar"); // 这一步在 PC 上会自动查 assets.txt

				if (jars != null && jars.length > 0) {
					for (FileHandle jar : jars) {
						// Android Assets 是流，不能直接拷文件夹，但可以拷单个文件
						jar.copyTo(libsTarget);
					}
				} else {
					Debug.logT("Hub", "⚠️ Warning: assets/libs is empty or not found!");
				}

				// ---------------------------------------------------------
				// 3. 交付
				// ---------------------------------------------------------
				tempProj.copyTo(finalTarget.parent());
				tempProj.deleteDirectory();

				return null;
			} catch (Exception e) {
				e.printStackTrace();
				if (tempProj.exists()) tempProj.deleteDirectory();
				return "Error: " + e.getMessage();
			}
		}

		public static class ProjectConfig {
			public String name;
			public String entryClass = "Main";
			public String version = "1.0";
		}
	}

	// =========================================================================================
	// Dialogs
	// =========================================================================================
	public static class CreateProjectDialog extends BaseDialog {
		private final VisTextField nameField;
		private final VisTextField pkgField;
		private final VisLabel errorLabel;
		private final Runnable onSuccess;

		public CreateProjectDialog(Runnable onSuccess) {
			super("Create Project");
			this.onSuccess = onSuccess;

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

	public static class ConfirmOpenDialog extends BaseDialog {
		private final Runnable onYes;

		public ConfirmOpenDialog(String name, Runnable onYes) {
			super("Confirm");
			this.onYes = onYes;

			text("Open project [" + name + "]?");
			button("Yes", true);
			button("No", false);
		}

		@Override
		protected void result(Object object) {
			if ((boolean) object) {
				onYes.run();
			}
		}
	}
}
