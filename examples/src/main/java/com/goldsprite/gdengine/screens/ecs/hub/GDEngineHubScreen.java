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
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.core.project.model.ProjectConfig;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.event.ContextListener;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.ChangeLogDialog;
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
		root.add(console).growX().row();

		// [新增] 底部栏 (包含日志按钮)
		VisTable bottomBar = new VisTable();
		bottomBar.left();

		VisTextButton btnLog = new VisTextButton("📅 更新日志");
		btnLog.setColor(Color.SKY);
		btnLog.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// 调用 show(stage) 使用全屏逻辑
				new ChangeLogDialog().show(stage);
			}
		});

		// 稍微加点 Padding 让它离底边有点距离
		bottomBar.add(btnLog).pad(5).left();

		// 将底部栏添加到 root 的最后
		root.add(bottomBar).growX().left();
	}

	public void refreshList() {
		projectListTable.clearChildren();
		Array<FileHandle> projects = ProjectService.inst().listProjects();

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
					ProjectConfig cfg = new Json().fromJson(ProjectConfig.class, conf);
					if (cfg.engineVersion != null) projEngineVer = cfg.engineVersion;
				} catch(Exception e) {}
			}

			// UI 展示
			VisLabel pathLabel = new VisLabel("Engine: " + projDir.path() + " | " + projEngineVer);
			pathLabel.setColor(Color.GRAY);
			//pathLabel.setFontScale(0.8f);
			item.add(pathLabel).right().padRight(20);

			// [修改] 使用 ContextListener 统一交互
			item.addListener(new ContextListener() {
				private Timer.Task tapTask;

				@Override
				public void onShowMenu(float stageX, float stageY) {
					showProjectMenu(projDir, stageX, stageY);
				}

				@Override
				public boolean longPress(Actor actor, float x, float y) {
					if (tapTask != null) tapTask.cancel(); // 取消延迟确认弹窗
					int k5;
					return super.longPress(actor, x, y);
				}

				@Override
				public void onLeftClick(InputEvent event, float x, float y, int count) {
					// 保持原有的 单击/双击 区分逻辑
					if (count == 2) {
						if (tapTask != null) tapTask.cancel();
						openProject(projDir);
					} else if (count == 1) {
						tapTask = Timer.schedule(new Timer.Task() {
							@Override
							public void run() {
								new ConfirmOpenDialog(projDir.name(), () -> {
									openProject(projDir);
								}).show(stage);
							}
						}, 0.2f);
					}
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
		ProjectService.inst().setCurrentProject(projectDir);
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
		private final Array<TemplateInfo> templates;

		public CreateProjectDialog(Runnable onSuccess) {
			super("New Project");
			this.onSuccess = onSuccess;

			templates = ProjectService.inst().listTemplates();

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
			for(TemplateInfo t : templates) names.add(t.displayName);
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
			TemplateInfo tmpl = templates.get(idx);

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

			TemplateInfo tmpl = templates.get(idx);
			String name = nameField.getText().trim();
			String pkg = pkgField.getText().trim();

			String err = ProjectService.inst().createProject(tmpl, name, pkg);
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
		public static String exportProject(FileHandle projectDir, TemplateInfo meta) {
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
					ProjectConfig cfg = new Json().fromJson(ProjectConfig.class, projJson);
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
				TemplateInfo finalMeta = new TemplateInfo();
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
				ProjectConfig cfg = new Json().fromJson(ProjectConfig.class, configFile);
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

			TemplateInfo meta = new TemplateInfo();
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
