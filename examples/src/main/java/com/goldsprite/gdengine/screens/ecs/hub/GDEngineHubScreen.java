package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Timer;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.IDEConsole;
import com.goldsprite.gdengine.BuildConfig;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

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
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.5f : 2.0f;
		super.initViewport();
	}

	@Override
	public void show() {
		super.show();
		Debug.showDebugUI = false;

		// [核心修改] 进入 Hub 时检查环境
		checkEnvironment();
	}

	private void checkEnvironment() {
		// 1. 尝试加载配置
		if (Gd.engineConfig == null) {
			if (GDEngineConfig.tryLoad()) {
				Gd.engineConfig = GDEngineConfig.getInstance();
				refreshList(); // 加载成功，刷新列表
			} else {
				// 2. 加载失败（未初始化），弹出 SetupDialog
				// 此时背景是 Hub 的空列表，或者你可以先不渲染 List
				new SetupDialog(() -> {
					// 初始化成功回调
					Gd.engineConfig = GDEngineConfig.getInstance();
					refreshList();
				}).show(stage);
			}
		} else {
			refreshList();
		}
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
		// 移除这里的refreshList()调用，改为在show()中调用
	}

	private void initMainLayout() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.top().pad(20);
		stage.addActor(root);

		// 1. Top Bar
		VisTable topBar = new VisTable();
		VisLabel titleLabel = new VisLabel("GDProject Hub");
		//titleLabel.setFontScale(1.5f);
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
			//nameLbl.setFontScale(1.3f);
			item.add(new VisLabel("📁 ")).padRight(10);
			item.add(nameLbl).expandX().left();

			// 读取项目配置获取版本
			String projEngineVer = "?";
			FileHandle conf = projDir.child("project.json");
			if (conf.exists()) {
				try {
					ProjectManager.ProjectConfig cfg = new Json().fromJson(ProjectManager.ProjectConfig.class, conf);
					if (cfg.engineVersion != null) projEngineVer = cfg.engineVersion;
				} catch(Exception e) {}
			}

			// UI 展示
			VisLabel pathLabel = new VisLabel("Engine: " + projDir.path() + " | " + projEngineVer);
			pathLabel.setColor(Color.GRAY);
			//pathLabel.setFontScale(0.8f);
			item.add(pathLabel).right().padRight(20);

			// [修改] 统一交互逻辑：单击弹窗(延时)，双击直达
			item.addListener(new ActorGestureListener(20, 0.4f, 0.4f, 0.15f) {
				private Timer.Task tapTask;

				@Override
				public void tap(InputEvent event, float x, float y, int count, int button) {
					if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
						if (count == 2) {
							// 双击 (Pro): 取消单击任务，直接打开
							if (tapTask != null && !tapTask.isScheduled()) {
								// 如果任务已经在运行中(极小概率)，取消可能没用，但在单线程模型下通常安全
							}
							if (tapTask != null) tapTask.cancel();

							openProject(projDir);
						}
						else if (count == 1) {
							// 单击 (Safe): 延迟 0.25s 执行，给双击留出时间窗
							// 如果用户手速快(0.25s内)点第二下，这个任务就会被上面的 count==2 取消
							// 如果手速慢，弹窗就会出来，挡住第二次点击(符合预期)
							tapTask = Timer.schedule(new Timer.Task() {
								@Override
								public void run() {
									new ConfirmOpenDialog(projDir.name(), () -> {
										openProject(projDir);
									}).show(stage);
								}
							}, 0.2f);
						}
					} else if (button == com.badlogic.gdx.Input.Buttons.RIGHT) {
						showProjectMenu(projDir, event.getStageX(), event.getStageY());
					}
				}

				@Override
				public boolean longPress(Actor actor, float x, float y) {
					com.badlogic.gdx.math.Vector2 v = actor.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(x, y));
					showProjectMenu(projDir, v.x, v.y);
					return true;
				}
			});

			projectListTable.add(item).growX().height(80).padBottom(10).row();
		}
	}
	// [新增] 显示项目上下文菜单
	private void showProjectMenu(FileHandle projDir, float x, float y) {
		PopupMenu menu = new PopupMenu();

		MenuItem itemDelete = new MenuItem("Delete Project");
		itemDelete.getLabel().setColor(Color.RED);
		itemDelete.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				showDeleteProjectConfirm(projDir);
			}
		});
		menu.addItem(itemDelete);

		menu.showMenu(stage, x, y);
	}

	// [新增] 删除确认弹窗
	private void showDeleteProjectConfirm(FileHandle projDir) {
		new BaseDialog("Delete Project") {
			@Override
			protected void result(Object object) {
				if ((boolean) object) {
					try {
						projDir.deleteDirectory();
						com.goldsprite.gdengine.log.Debug.logT("Hub", "Project deleted: " + projDir.name());
						refreshList();
					} catch (Exception e) {
						com.goldsprite.gdengine.log.Debug.logT("Hub", "Delete failed: " + e.getMessage());
					}
				}
			}
		}
			.text("Warning: This will PERMANENTLY delete project:\n" + projDir.name() + "\n\nCannot be undone!")
			.button("Delete", true)
			.button("Cancel", false)
			.show(stage);
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
	// Logic: ProjectManager (Full Refactor)
	// =========================================================================================
	public static class ProjectManager {
		public static FileHandle currentProject;
		private static final Json json = new Json();

		// [新增] 静态块配置 Json
		static {
			json.setIgnoreUnknownFields(true);
			json.setOutputType(com.badlogic.gdx.utils.JsonWriter.OutputType.json);
		}

		// 简单的 DTO
		public static class TemplateInfo {
			public String id; // 文件夹名
			public String displayName;
			public String description;
			public String originEntry; // "com.mygame.Main"
			public String version;       // 模板自身版本 (e.g. 1.0)
			public String engineVersion; // [新增] 适配的引擎版本 (e.g. 1.8.11.1-alpha)
			public FileHandle dirHandle; // assets/engine/templates/{id}
		}

		public static class ProjectConfig {
			public String name;
			public String entryClass;
			public TemplateRef template;
			// [新增] 记录项目关联的引擎版本
			public String engineVersion;
		}

		public static class TemplateRef {
			public String sourceName;
			public String version;
			public String engineVersion;
		}

		public static Array<FileHandle> listProjects() {
			// 添加null检查，防止NullPointerException
			if (Gd.engineConfig == null) {
				return new Array<>(); // 返回空列表而不是抛出异常
			}

			FileHandle root = Gd.engineConfig.getProjectsDir();
			FileHandle[] files = root.list();
			Array<FileHandle> projects = new Array<>();
			if(files != null) {
				for (FileHandle f : files) {
					if (f.isDirectory()) projects.add(f);
				}
			}
			return projects;
		}

		/** 扫描所有可用模板 */
		public static Array<TemplateInfo> listTemplates() {
			Array<TemplateInfo> list = new Array<>();
			FileHandle templatesRoot = Gd.files.internal("engine/templates");
			if (!templatesRoot.exists()) return list;

			for (FileHandle dir : templatesRoot.list()) {
				if (!dir.isDirectory()) continue;

				TemplateInfo info = new TemplateInfo();
				info.id = dir.name();
				info.dirHandle = dir;

				// 尝试读取 template.json
				FileHandle metaFile = dir.child("template.json");
				if (metaFile.exists()) {
					try {
						TemplateInfo meta = json.fromJson(TemplateInfo.class, metaFile);
						info.displayName = meta.displayName;
						info.description = meta.description;
						info.originEntry = meta.originEntry;
						info.version = meta.version;
						info.engineVersion = meta.engineVersion;
					} catch (Exception e) {
						Debug.logT("Hub", "Template parse error: " + dir.name());
						info.displayName = info.id + " (Error)";
					}
				} else {
					info.displayName = info.id;
					info.description = "No description.";
					info.originEntry = "com.game.Main"; // Fallback
				}
				list.add(info);
			}
			return list;
		}

		/**
		 * 通用创建逻辑
		 * 核心：Scripts/ (assets) -> src/main/java/ (user) + Package Refactoring
		 */
		public static String createProject(TemplateInfo tmpl, String name, String packageName) {
			// 1. 校验
			if (name == null || name.trim().isEmpty()) return "Name cannot be empty.";
			if (!name.matches("[a-zA-Z0-9_]+")) return "Invalid project name.";
			if (packageName == null || packageName.trim().isEmpty()) return "Package cannot be empty.";

			FileHandle finalTarget = Gd.engineConfig.getProjectsDir().child(name);
			if (finalTarget.exists()) return "Project already exists!";

			// 原始包名 (例如: com.mygame)
			String originPkg = "";
			if (tmpl.originEntry != null && tmpl.originEntry.contains(".")) {
				originPkg = tmpl.originEntry.substring(0, tmpl.originEntry.lastIndexOf('.'));
			}
			String targetPkg = packageName;

			Debug.logT("Hub", "Creating project '%s' from template '%s'", name, tmpl.id);

			// 临时目录
			FileHandle tempRoot = Gdx.files.local("build/tmp_creation").child(name);
			if (tempRoot.exists()) tempRoot.deleteDirectory();
			tempRoot.mkdirs();

			try {
				// [核心修改] 遍历模板根目录
				// 我们不再使用通用的递归，而是针对根目录的特定文件夹做处理，更安全
				processRootDirectory(tmpl.dirHandle, tempRoot, originPkg, targetPkg, name, tmpl);

				// --- [新增] 注入通用构建脚本 (来自模板根目录) ---
				FileHandle templatesRoot = tmpl.dirHandle.parent();
				FileHandle commonBuild = templatesRoot.child("build.gradle");
				FileHandle commonSettings = templatesRoot.child("settings.gradle");

				if (commonBuild.exists()) {
					String content = commonBuild.readString("UTF-8");
					// 这里的 build.gradle 已经包含了我们注入的 idea {} 魔法代码
					tempRoot.child("build.gradle").writeString(content, false, "UTF-8");
				}

				if (commonSettings.exists()) {
					String content = commonSettings.readString("UTF-8");
					// 替换项目名称占位符 (如果存在)
					// 注意：settings.gradle 通常包含 rootProject.name = '${PROJECT_NAME}'
					content = content.replace("${PROJECT_NAME}", name);
					tempRoot.child("settings.gradle").writeString(content, false, "UTF-8");
				}

				// --- 注入依赖库 ---
				// [核心修改] 路径自动探测
				FileHandle libsSource = Gdx.files.internal("engine/libs");
				if (!libsSource.exists()) {
					libsSource = Gdx.files.internal("assets/engine/libs");
				}

				FileHandle libsTarget = tempRoot.child("libs");
				libsTarget.mkdirs();
				for (FileHandle jar : libsSource.list(".jar")) {
					jar.copyTo(libsTarget);
				}

				// --- 交付 ---
				tempRoot.copyTo(finalTarget.parent());
				tempRoot.deleteDirectory();

				return null;
			} catch (Exception e) {
				e.printStackTrace();
				if (tempRoot.exists()) tempRoot.deleteDirectory();
				return "Error: " + e.getMessage();
			}
		}

		/**
		 * 处理模板根目录
		 */
		private static void processRootDirectory(FileHandle sourceDir, FileHandle destDir, String originPkg, String targetPkg, String projName, TemplateInfo tmpl) {
			for (FileHandle file : sourceDir.list()) {
				if (file.name().equals("template.json")) continue;
				if (file.name().equals("preview.png")) continue;

				if (file.isDirectory()) {
					if (file.name().equals("src")) {
						// [核心] 遇到 src 目录，进入源码处理模式
						// 假设结构标准为 src/main/java
						FileHandle srcJavaSource = file.child("main").child("java");
						if (srcJavaSource.exists()) {
							FileHandle srcJavaTarget = destDir.child("src").child("main").child("java");
							processSourceCode(srcJavaSource, srcJavaTarget, originPkg, targetPkg);
						} else {
							// 非标准结构？直接复制整个 src
							file.copyTo(destDir);
						}
					} else {
						// 其他目录 (如 assets)，直接复制
						file.copyTo(destDir);
					}
				} else {
					// 根目录下的文件处理
					if (file.name().equals("project.json")) {
						processProjectConfig(file, destDir.child("project.json"), targetPkg, projName, tmpl);
					} else if (file.name().equals("settings.gradle") || file.name().equals("build.gradle")) {
						// 处理 Gradle 文件中的文本替换
						String content = file.readString("UTF-8");
						content = content.replace("${PROJECT_NAME}", projName);
						if (!originPkg.isEmpty()) content = content.replace(originPkg, targetPkg);
						destDir.child(file.name()).writeString(content, false, "UTF-8");
					} else {
						// 其他文件原样复制
						file.copyTo(destDir);
					}
				}
			}
		}

		/**
		 * 处理源码目录：递归查找 Java 文件，重构路径并修改内容
		 * @param sourceRoot 模板里的 Scripts/ 目录
		 * @param targetRoot 目标里的 src/main/java/ 目录
		 */
		private static void processSourceCode(FileHandle sourceRoot, FileHandle targetRoot, String originPkg, String targetPkg) {
			// 1. 递归获取所有 .java 文件
			Array<FileHandle> javaFiles = new Array<>();
			findJavaFiles(sourceRoot, javaFiles);

			// 路径前缀 (如 com/mygame)
			String originPathPrefix = originPkg.replace('.', '/');
			String targetPathPrefix = targetPkg.replace('.', '/');

			for (FileHandle javaFile : javaFiles) {
				// 2. 计算相对路径
				// 假设 javaFile: .../templates/HelloGame/src/main/java/com/mygame/Main.java
				// rootPath: .../templates/HelloGame/src/main/java
				// relativePath: com/mygame/Main.java
				String fullPath = javaFile.path();
				String rootPath = sourceRoot.path();

				// 简单的路径截取
				String relativePath = fullPath.substring(rootPath.length());
				if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
					relativePath = relativePath.substring(1);
				}

				// 3. 路径重构 (Package Refactoring)
				// 如果路径以原始包路径开头，替换为目标包路径
				// com/mygame/Main.java -> com/user/new/Main.java
				String newRelativePath = relativePath;

				// 统一分隔符比较
				String checkPath = relativePath.replace('\\', '/');
				if (!originPathPrefix.isEmpty() && checkPath.startsWith(originPathPrefix)) {
					newRelativePath = checkPath.replaceFirst(originPathPrefix, targetPathPrefix);
				}

				FileHandle targetFile = targetRoot.child(newRelativePath);

				// 4. 内容重构 (Text Replacement)
				String content = javaFile.readString("UTF-8");
				if (!originPkg.isEmpty()) {
					// 替换包声明 package com.mygame; -> package com.user.new;
					content = content.replace("package " + originPkg, "package " + targetPkg);
					// 替换 import
					content = content.replace("import " + originPkg, "import " + targetPkg);
					// 替换其他可能的引用
					content = content.replace(originPkg, targetPkg);
				}

				targetFile.writeString(content, false, "UTF-8");
			}
		}

		private static void findJavaFiles(FileHandle dir, Array<FileHandle> out) {
			for (FileHandle f : dir.list()) {
				if (f.isDirectory()) findJavaFiles(f, out);
				else if (f.extension().equals("java")) out.add(f);
			}
		}

		private static void processProjectConfig(FileHandle source, FileHandle target, String targetPkg, String projName, TemplateInfo tmpl) {
			try {
				String content = source.readString("UTF-8");
				// 先做通用替换
				if (tmpl.originEntry != null && tmpl.originEntry.contains(".")) {
					String originPkg = tmpl.originEntry.substring(0, tmpl.originEntry.lastIndexOf('.'));
					content = content.replace(originPkg, targetPkg);
				}

				// 解析 JSON 对象进行精确修改
				ProjectConfig cfg = json.fromJson(ProjectConfig.class, content);
				cfg.name = projName;

				// 注入模板信息
				TemplateRef ref = new TemplateRef();
				ref.sourceName = tmpl.id;
				ref.version = tmpl.version;
				ref.engineVersion = tmpl.engineVersion;
				cfg.template = ref;
				// [新增] 注入当前引擎版本
				cfg.engineVersion = tmpl.engineVersion;

				target.writeString(json.prettyPrint(cfg), false, "UTF-8");
			} catch (Exception e) {
				// [新增] 打印错误日志，方便调试测试失败原因
				Debug.logT("Hub", "⚠️ project.json 处理失败，回退为直接复制: " + e.getMessage());
				e.printStackTrace(); // 打印堆栈
				// Fallback: 直接复制
				source.copyTo(target);
			}
		}
	}

	// =========================================================================================
	// Dialogs (Updated)
	// =========================================================================================

	public static class CreateProjectDialog extends BaseDialog {
		private final VisTextField nameField;
		private final VisTextField pkgField;
		private final VisLabel errorLabel;
		private final Runnable onSuccess;

		// 模板选择相关
		private final VisSelectBox<String> templateBox;
		private final VisImage previewImage;
		// [新增] 详情展示组件
		private final VisLabel descLabel;
		private final VisLabel versionLabel, enginVersionLabel;
		private final Array<ProjectManager.TemplateInfo> templates;

		public CreateProjectDialog(Runnable onSuccess) {
			super("New Project");
			this.onSuccess = onSuccess;

			templates = ProjectManager.listTemplates();

			// --- [布局优化] ---
			// 核心容器：设置最小宽度，让其不再窄小
			VisTable content = new VisTable();
			content.defaults().padBottom(10).left(); // 默认左对齐，增加行间距

			float labelWidth = 220;
			// 1. Template Selection
			VisTable tplRow = new VisTable();
			tplRow.add(new VisLabel("Template:")).width(labelWidth).left();
			templateBox = new VisSelectBox<>();
			Array<String> names = new Array<>();
			for(ProjectManager.TemplateInfo t : templates) names.add(t.displayName);
			templateBox.setItems(names);
			tplRow.add(templateBox).width(labelWidth*3);

			content.add(tplRow).growX().row();

			// 2. Info Area (Image + Details)
			VisTable infoTable = new VisTable();
			infoTable.setBackground(VisUI.getSkin().getDrawable("button"));
			infoTable.pad(15);

			// Left: Image
			previewImage = new VisImage();
			// [核心修复1] 使用 center() 让图片在左侧区域垂直居中
			infoTable.add(previewImage).size(100).center().left().padRight(20);

			// Right: Description 和 Version
			VisTable detailsTable = new VisTable();
			detailsTable.top().left();

			descLabel = new VisLabel("Description...");
			descLabel.setWrap(true);
			descLabel.setColor(Color.LIGHT_GRAY);
			descLabel.setAlignment(Align.center);
			// [核心修复2] 给描述文字一个明确的宽度 (Dialog宽600 - 图片100 - Padding ≈ 420)
			// 只有设置了具体宽度，setWrap(true) 才能正确计算换行高度
			detailsTable.add(descLabel).growX().center().top().row();

			versionLabel = new VisLabel("v1.0");
			versionLabel.setColor(Color.CYAN);
			versionLabel.setAlignment(Align.right);
			detailsTable.add(versionLabel).growX().right().padBottom(5).row();

			enginVersionLabel = new VisLabel("v1.0");
			enginVersionLabel.setColor(Color.GOLDENROD);
			enginVersionLabel.setAlignment(Align.right);
			detailsTable.add(enginVersionLabel).growX().right().padBottom(5);

			infoTable.add(detailsTable).grow(); // 让文字部分填满剩余空间

			// [核心修复3] 移除 height(140) 硬限制，改为 minHeight(120)
			// 这样当文字换行变多时，infoTable 会自动变高，背景也会随之拉伸
			content.add(infoTable).growX().minHeight(120).padBottom(15).row();

			// 3. Project Info
			String baseName = "MyGame";
			String finalName = baseName;
			FileHandle projectsRoot = Gd.engineConfig.getProjectsDir();
			if (projectsRoot != null && projectsRoot.exists()) {
				int counter = 1;
				while (projectsRoot.child(finalName).exists()) {
					finalName = baseName + counter;
					counter++;
				}
			}

			// Name Row
			VisTable nameRow = new VisTable();
			nameRow.add(new VisLabel("Project Name:")).width(labelWidth).left();
			nameField = new VisTextField(finalName);
			nameRow.add(nameField).growX();
			content.add(nameRow).growX().row();

			// Package Row
			VisTable pkgRow = new VisTable();
			pkgRow.add(new VisLabel("Package:")).width(labelWidth).left();
			pkgField = new VisTextField("com." + finalName.toLowerCase());
			pkgRow.add(pkgField).growX();
			content.add(pkgRow).growX().row();

			nameField.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					pkgField.setText("com." + nameField.getText().toLowerCase());
				}
			});

			// 将 content 添加到 Dialog
			add(content).minWidth(600).pad(10).row();

			// 4. Footer
			errorLabel = new VisLabel("");
			errorLabel.setColor(Color.RED);
			errorLabel.setWrap(true);
			errorLabel.setAlignment(Align.center);
			add(errorLabel).growX().padBottom(10).row();

			VisTextButton createBtn = new VisTextButton("Create Project");
			createBtn.setColor(Color.GREEN);
			createBtn.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					doCreate();
				}
			});
			content.add(createBtn).colspan(2).bottom().center().width(200).height(45).padBottom(0);

			// Init
			templateBox.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) { updateTemplateInfo(); }
			});

			pack();
			centerWindow();

			if(templates.size > 0) updateTemplateInfo();
		}

		private void updateTemplateInfo() {
			int idx = templateBox.getSelectedIndex();
			if(idx < 0 || idx >= templates.size) return;
			ProjectManager.TemplateInfo tmpl = templates.get(idx);

			// Update Text
			descLabel.setText(tmpl.description != null ? tmpl.description : "No description.");
			versionLabel.setText("template: v" + (tmpl.version != null ? tmpl.version : "1.0"));
			enginVersionLabel.setText("engine: v" + (tmpl.engineVersion != null ? tmpl.engineVersion : "1.0"));

			// Update Image
			FileHandle imgFile = tmpl.dirHandle.child("preview.png");
			if(imgFile.exists()) {
				try {
					Texture tex = new Texture(imgFile);
					previewImage.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
				} catch(Exception e) { e.printStackTrace(); }
			} else {
				previewImage.setDrawable(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("gd_icon.png")))));
			}

			// 刷新布局，因为描述文字高度可能变化
			pack();
			centerWindow();
		}

		private void doCreate() {
			int idx = templateBox.getSelectedIndex();
			if(idx < 0) { errorLabel.setText("Please select a template"); return; }

			ProjectManager.TemplateInfo tmpl = templates.get(idx);
			String name = nameField.getText().trim();
			String pkg = pkgField.getText().trim();

			String err = ProjectManager.createProject(tmpl, name, pkg);
			if (err == null) {
				onSuccess.run();
				fadeOut();
			} else {
				errorLabel.setText(err);
				pack(); // 错误信息可能很长
			}
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

	// =========================================================================================
	// Logic: TemplateExporter (Dev Tool)
	// =========================================================================================
	public static class TemplateExporter {

		/**
		 * 导出项目为内部模板
		 * @param projectDir 用户项目根目录
		 * @param meta 用户填写的模板元数据
		 * @return 错误信息，成功返回 null
		 */
		public static String exportProject(FileHandle projectDir, ProjectManager.TemplateInfo meta) {
			// 1. 定位 InternalProjectTemplates 目录
			// 假设我们在 IDE 环境下运行，根目录是项目根
			FileHandle internalRoot = Gdx.files.absolute(System.getProperty("user.dir")).child("GDEngine/InternalProjectTemplates");

			if (!internalRoot.exists()) {
				return "Error: InternalProjectTemplates not found.\nThis feature is for engine developers only.";
			}

			FileHandle targetTplDir = internalRoot.child(meta.id);

			// 2. 合规审查 (Review Pipeline)
			String reviewError = runComplianceCheck(projectDir);
			if (reviewError != null) return "Review Failed:\n" + reviewError;

			try {
				Debug.logT("Exporter", "Starting export to: " + targetTplDir.path());

				// 3. 清理旧模板
				if (targetTplDir.exists()) {
					targetTplDir.deleteDirectory();
				}
				targetTplDir.mkdirs();

				// 4. 复制核心文件
				// 4.1 src
				FileHandle src = projectDir.child("src");
				if (src.exists()) src.copyTo(targetTplDir);

				// 4.2 assets
				FileHandle assets = projectDir.child("assets");
				if (assets.exists()) assets.copyTo(targetTplDir);

				// 4.3 project.json (需清洗)
				FileHandle projJson = projectDir.child("project.json");
				if (projJson.exists()) {
					ProjectManager.ProjectConfig cfg = new Json().fromJson(ProjectManager.ProjectConfig.class, projJson);
					// 清洗：移除 template 引用信息，恢复纯净状态
					cfg.template = null;
					// 写入
					targetTplDir.child("project.json").writeString(new Json().prettyPrint(cfg), false, "UTF-8");

					// 自动填充 originEntry (如果 meta 没填)
					if (meta.originEntry == null || meta.originEntry.isEmpty()) {
						meta.originEntry = cfg.entryClass;
					}
				}

				// 4.4 生成 template.json
				// 构造干净的 meta 对象用于序列化
				ProjectManager.TemplateInfo finalMeta = new ProjectManager.TemplateInfo();
				finalMeta.displayName = meta.displayName;
				finalMeta.description = meta.description;
				finalMeta.version = meta.version;
				finalMeta.originEntry = meta.originEntry;
				// [新增] 自动注入当前引擎版本
				finalMeta.engineVersion = meta.engineVersion;
				// id 和 dirHandle 不需要写入 json

				targetTplDir.child("template.json").writeString(new Json().prettyPrint(finalMeta), false, "UTF-8");

				Debug.logT("Exporter", "✅ Export success: " + meta.id);
				return null;

			} catch (Exception e) {
				e.printStackTrace();
				return "Export Exception: " + e.getMessage();
			}
		}

		private static String runComplianceCheck(FileHandle projectDir) {
			// Check 1: Config
			FileHandle configFile = projectDir.child("project.json");
			if (!configFile.exists()) return "Missing project.json";

			String entryClass = null;
			try {
				ProjectManager.ProjectConfig cfg = new Json().fromJson(ProjectManager.ProjectConfig.class, configFile);
				entryClass = cfg.entryClass;
			} catch(Exception e) { return "Invalid project.json format"; }

			if (entryClass == null || entryClass.isEmpty()) return "Entry class not defined in config";

			// Check 2: Structure
			if (!projectDir.child("src/main/java").exists()) return "Missing src/main/java structure";

			// Check 3: Compilation (The Acid Test)
			if (Gd.compiler != null) {
				try {
					// 注入资源上下文，防止因资源缺失导致 Start 方法报错 (虽然只是编译检查，但有些静态块可能会跑)
					GameWorld.projectAssetsRoot = projectDir.child("assets");
					Class<?> clazz = Gd.compiler.compile(entryClass, projectDir.file().getAbsolutePath());
					if (clazz == null) return "Compilation failed (See log)";
					if (!com.goldsprite.gdengine.core.scripting.IGameScriptEntry.class.isAssignableFrom(clazz)) {
						return "Entry class must implement IGameScriptEntry";
					}
				} catch (Exception e) {
					return "Compiler error: " + e.getMessage();
				}
			} else {
				Debug.logT("Exporter", "⚠️ Compiler not available, skipping compilation check.");
			}

			return null;
		}
	}

	public static class ExportTemplateDialog extends BaseDialog {
		private final VisTextField idField, nameField, versionField;
		private final VisTextArea descArea;
		private final VisLabel errorLabel;
		private final FileHandle projectDir;

		public ExportTemplateDialog(FileHandle projectDir) {
			super("Export Template (Dev Only)");
			this.projectDir = projectDir;

			VisTable content = new VisTable();
			content.defaults().pad(5).left();

			// Auto-fill ID from folder name
			content.add(new VisLabel("Template ID (Folder Name):"));
			idField = new VisTextField(projectDir.name());
			content.add(idField).width(300).row();

			content.add(new VisLabel("Display Name:"));
			nameField = new VisTextField(projectDir.name());
			content.add(nameField).width(300).row();

			content.add(new VisLabel("Version:"));
			versionField = new VisTextField("1.0");
			content.add(versionField).width(100).row();

			content.add(new VisLabel("Description:")).top();
			descArea = new VisTextArea("Auto-exported template.");
			descArea.setPrefRows(3);
			content.add(descArea).width(300).row();

			add(content).padBottom(10).row();

			errorLabel = new VisLabel("");
			errorLabel.setColor(Color.RED);
			errorLabel.setWrap(true);
			add(errorLabel).width(400).padBottom(10).row();

			VisTextButton btnExport = new VisTextButton("Review 和 Export");
			btnExport.setColor(Color.ORANGE);
			btnExport.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					doExport();
				}
			});

			add(btnExport).growX().height(40);

			pack();
			centerWindow();
		}

		private void doExport() {
			String id = idField.getText().trim();
			if (id.isEmpty() || !id.matches("[a-zA-Z0-9_]+")) {
				errorLabel.setText("Invalid Template ID (Alphanumeric only)");
				pack(); return;
			}

			ProjectManager.TemplateInfo meta = new ProjectManager.TemplateInfo();
			meta.id = id;
			meta.displayName = nameField.getText();
			meta.description = descArea.getText();
			meta.version = versionField.getText();

			errorLabel.setText("Reviewing...");
			errorLabel.setColor(Color.YELLOW);

			// 异步执行防止卡顿 UI
			new Thread(() -> {
				String err = TemplateExporter.exportProject(projectDir, meta);

				Gdx.app.postRunnable(() -> {
					if (err == null) {
						fadeOut();
						// 可选：显示一个成功提示 Toast
						com.goldsprite.gdengine.log.Debug.logT("Exporter", "Export Completed!");
					} else {
						errorLabel.setText(err);
						errorLabel.setColor(Color.RED);
						pack();
					}
				});
			}).start();
		}
	}
}
