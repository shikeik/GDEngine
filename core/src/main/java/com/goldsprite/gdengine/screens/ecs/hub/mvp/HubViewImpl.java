package com.goldsprite.gdengine.screens.ecs.hub.mvp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Timer;
import com.goldsprite.gdengine.BuildConfig;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.CloudConstants;
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.core.project.model.ProjectConfig;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.screens.ecs.hub.SettingsWindow;
import com.goldsprite.gdengine.ui.event.ContextListener;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.IDEConsole;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.goldsprite.gdengine.utils.MultiPartDownloader;
import com.goldsprite.gdengine.screens.ecs.hub.OnlineTemplateDialog;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.badlogic.gdx.Preferences;

import java.util.Locale;

/**
 * Hub 视图的具体实现 (View Implementation)
 * 职责：负责所有的 UI 布局、控件初始化和用户交互监听。
 * 纯粹的 UI 代码，不包含业务逻辑。
 */
public class HubViewImpl extends VisTable implements IHubView {

	private HubPresenter presenter;

	// UI Components
	private VisTable projectListTable;
	private IDEConsole console;

	public HubViewImpl() {
		setFillParent(true);
		top().pad(20);

		initMainLayout();
	}

	@Override
	public void setPresenter(HubPresenter presenter) {
		this.presenter = presenter;
	}

	private void initMainLayout() {
		// 1. Top Bar
		VisTable topBar = new VisTable();
		VisLabel titleLabel = new VisLabel("GDProject Hub");
		titleLabel.setColor(Color.CYAN);

		VisTextButton btnSettings = new VisTextButton("⚙ Settings");
		btnSettings.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// Settings 改变路径后，通知 Presenter 刷新
				new SettingsWindow(() -> presenter.refreshProjectList()).show(getStage());
			}
		});

		VisTextButton btnCreate = new VisTextButton("[ + New Project ]");
		btnCreate.setColor(Color.GREEN);
		btnCreate.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				new CreateProjectDialog().show(getStage());
			}
		});

		VisTextButton btnStore = new VisTextButton("☁ Store");
		btnStore.setColor(Color.ORANGE);
		btnStore.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					new OnlineTemplateDialog().show(getStage());
				}
			});

		topBar.add(titleLabel).expandX().left();
		topBar.add(btnSettings).right().padRight(10).height(50);
		topBar.add(btnStore).right().padRight(10).height(50); // 新增
		topBar.add(btnCreate).right().height(50);
		add(topBar).growX().height(60).padBottom(10).row();

		// 2. Project List
		projectListTable = new VisTable();
		projectListTable.top();

		VisScrollPane scrollPane = new VisScrollPane(projectListTable);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false);

		VisTable container = new VisTable();
		container.setBackground("window-bg");
		container.add(scrollPane).grow().pad(20);

		add(container).grow().row();

		// 3. Console
		console = new IDEConsole();
		add(console).growX().row();

		// 4. Bottom Bar
		VisTable bottomBar = new VisTable();
		bottomBar.left();

		VisTextButton btnLog = new VisTextButton("📅 引擎文档(下载查看)");
		btnLog.setColor(Color.SKY);
		btnLog.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				openLocalDocs();
			}
		});

		bottomBar.add(btnLog).pad(5).left();
		add(bottomBar).growX().left();
	}

	// 定义常量
	private static final String PREF_DOCS = "gd_docs_config";
	private static final String KEY_DOC_TIME = "local_doc_updated_at";
	// [修改] 清单走镜像 (秒更)，文件走 CDN (在清单里写死了)
	private static final String DOC_MANIFEST_URL = "https://gh-proxy.com/https://raw.githubusercontent.com/shikeik/GDEngine/refs/heads/main/dist/docs_manifest.json";

	private void openLocalDocs() {
		String activeRoot = GDEngineConfig.getInstance().getActiveEngineRoot();
		if (activeRoot == null) activeRoot = GDEngineConfig.getRecommendedRoot();

		final String finalRoot = activeRoot;
		FileHandle docEntry = Gdx.files.absolute(activeRoot).child("engine_docs/index.html");

		// 1. 如果本地没有，直接开始下载流程
		if (!docEntry.exists()) {
			startDocsUpdateFlow(finalRoot, true); // true = 强制下载
			return;
		}

		// 2. 如果有，检查更新
		ToastUI.inst().show("正在检查文档更新...");
		startDocsUpdateFlow(finalRoot, false);
	}

	// [重构] 统一的更新流程
	private void startDocsUpdateFlow(String rootPath, boolean forceDownload) {
		// Step 1: 获取 SHA
		MultiPartDownloader.fetchLatestSha(new MultiPartDownloader.ShaCallback() {
			@Override
			public void onSuccess(String sha) {
				// Step 2: 组装 Proxy URL 获取清单
				String manifestUrl = CloudConstants.getManifestUrl(sha, CloudConstants.PATH_DOCS_DIST + "docs_manifest.json");

				MultiPartDownloader.fetchManifest(manifestUrl, new MultiPartDownloader.ManifestCallback() {
					@Override
					public void onSuccess(MultiPartDownloader.Manifest manifest) {
						if (forceDownload) {
							// 强制下载
							performDocsDownload(rootPath, sha, manifest.updatedAt);
						} else {
							// 检查版本
							checkDocVersion(rootPath, sha, manifest);
						}
					}

					@Override
					public void onError(String err) {
						if (forceDownload) {
							showError("清单获取失败: " + err);
						} else {
							ToastUI.inst().show("无法连接更新服务器，打开本地缓存...");
							launchDocServer();
						}
					}
				});
			}

			@Override
			public void onError(String err) {
				if (forceDownload) {
					showError("版本检查失败(API): " + err);
				} else {
					ToastUI.inst().show("离线模式: 无法获取版本信息");
					launchDocServer();
				}
			}
		});
	}

	private void checkDocVersion(String rootPath, String sha, MultiPartDownloader.Manifest cloudManifest) {
		Preferences prefs = Gdx.app.getPreferences(PREF_DOCS);
		String localTime = prefs.getString(KEY_DOC_TIME, "");

		if (!localTime.equals(cloudManifest.updatedAt)) {
			String sizeStr = String.format(Locale.CHINESE, "%.2f MB", cloudManifest.totalSize / 1024f / 1024f);
			new BaseDialog("文档更新") {
				@Override
				protected void result(Object object) {
					if ((boolean) object) {
						performDocsDownload(rootPath, sha, cloudManifest.updatedAt);
					} else {
						launchDocServer();
					}
				}
			}
				.text("发现新版本 (" + cloudManifest.updatedAt + ")\n大小: " + sizeStr + "\n是否更新？")
				.button("更新", true).button("暂不", false).show(getStage());
		} else {
			ToastUI.inst().show("文档已是最新");
			launchDocServer();
		}
	}

	private void performDocsDownload(String rootPath, String sha, String updateTime) {
		ToastUI.inst().show("开始下载文档...");

		// 组装 URL
		String manifestUrl = CloudConstants.getManifestUrl(sha, CloudConstants.PATH_DOCS_DIST + "docs_manifest.json");
		String cdnBaseUrl = CloudConstants.getAssetCdnBaseUrl(sha, CloudConstants.PATH_DOCS_DIST);

		MultiPartDownloader.download(
			manifestUrl,
			cdnBaseUrl,
			rootPath,
			(progress, msg) -> {
				Gdx.app.postRunnable(() -> {
					if (progress < 0) showError("下载失败: " + msg);
					else if (progress % 10 == 0) ToastUI.inst().show(msg);
				});
			},
			() -> {
				Gdx.app.postRunnable(() -> {
					ToastUI.inst().show("更新完毕！");
					// 保存版本
					Preferences prefs = Gdx.app.getPreferences(PREF_DOCS);
					prefs.putString(KEY_DOC_TIME, updateTime);
					prefs.flush();

					launchDocServer();
				});
			}
		);
	}

	private void launchDocServer() {
		try {
			com.goldsprite.gdengine.core.web.DocServer.startServer(
				Gdx.files.absolute(GDEngineConfig.getInstance().getActiveEngineRoot())
				.child("engine_docs").file().getAbsolutePath()
			);

			String url = com.goldsprite.gdengine.core.web.DocServer.getIndexUrl() + "?v=" + BuildConfig.DEV_VERSION;
			ToastUI.inst().show("文档服务已启动");

			if (Gd.browser != null) {
				Gd.browser.openUrl(url, "GDEngine Docs");
			}
		} catch (Exception e) {
			showError("Server Start Failed: " + e.getMessage());
		}
	}

	@Override
	public void showProjects(Array<FileHandle> projects) {
		projectListTable.clearChildren();

		if (projects.size == 0) {
			VisLabel emptyLabel = new VisLabel("No projects found.\nClick [+ New Project] to start.", Align.center);
			emptyLabel.setColor(Color.GRAY);
			projectListTable.add(emptyLabel).padTop(100);
			return;
		}

		Json json = new Json();
		json.setIgnoreUnknownFields(true);

		for (FileHandle projDir : projects) {
			VisTable item = new VisTable();
			item.setBackground("button");
			item.setTouchable(Touchable.enabled);
			item.pad(10);

			VisLabel nameLbl = new VisLabel(projDir.name());
			item.add(new VisLabel("📁 ")).padRight(10);
			item.add(nameLbl).expandX().left();

			// 读取项目配置
			String projEngineVer = "?";
			FileHandle conf = projDir.child("project.json");
			if (conf.exists()) {
				try {
					ProjectConfig cfg = json.fromJson(ProjectConfig.class, conf);
					if (cfg.engineVersion != null) projEngineVer = cfg.engineVersion;
				} catch(Exception e) {}
			}

			VisLabel pathLabel = new VisLabel("Engine: " + projDir.path() + " | " + projEngineVer);
			pathLabel.setColor(Color.GRAY);
			item.add(pathLabel).right().padRight(20);

			// 交互事件
			item.addListener(new ContextListener() {
				private Timer.Task tapTask;

				@Override
				public void onShowMenu(float stageX, float stageY) {
					showProjectMenu(projDir, stageX, stageY);
				}

				@Override
				public boolean longPress(Actor actor, float x, float y) {
					if (tapTask != null) tapTask.cancel();
					return super.longPress(actor, x, y);
				}

				@Override
				public void onLeftClick(InputEvent event, float x, float y, int count) {
					if (count == 2) {
						if (tapTask != null) tapTask.cancel();
						presenter.onProjectOpenRequest(projDir);
					} else if (count == 1) {
						tapTask = Timer.schedule(new Timer.Task() {
							@Override
							public void run() {
								new ConfirmOpenDialog(projDir.name(), () -> {
									presenter.onProjectOpenRequest(projDir);
								}).show(getStage());
							}
						}, 0.2f);
					}
				}
			});

			projectListTable.add(item).growX().height(80).padBottom(10).row();
		}
	}

	private void showProjectMenu(FileHandle projDir, float x, float y) {
		PopupMenu menu = new PopupMenu();
		// [新增] 升级按钮
		MenuItem itemUpgrade = new MenuItem("Upgrade Engine");
		itemUpgrade.getLabel().setColor(Color.GOLD);
		itemUpgrade.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				new BaseDialog("Upgrade Project") {
					@Override protected void result(Object object) {
						if ((boolean) object) {
							presenter.onProjectUpgradeRequest(projDir);
						}
					}
				}
					.text("Update [" + projDir.name() + "] libs to v" + BuildConfig.DEV_VERSION + "?\n(This will overwrite jars in libs folder)")
					.button("Upgrade", true)
					.button("Cancel", false)
					.show(getStage());
			}
		});
		menu.addItem(itemUpgrade);

		menu.addSeparator(); // 分隔线

		MenuItem itemDelete = new MenuItem("Delete Project");
		itemDelete.getLabel().setColor(Color.RED);
		itemDelete.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				showDeleteProjectConfirm(projDir);
			}
		});
		menu.addItem(itemDelete);

		menu.showMenu(getStage(), x, y);
	}

	private void showDeleteProjectConfirm(FileHandle projDir) {
		new BaseDialog("Delete Project") {
			@Override
			protected void result(Object object) {
				if ((boolean) object) {
					presenter.onProjectDeleteRequest(projDir);
				}
			}
		}
			.text("Warning: This will PERMANENTLY delete project:\n" + projDir.name() + "\n\nCannot be undone!")
			.button("Delete", true)
			.button("Cancel", false)
			.show(getStage());
	}

	@Override
	public void showToast(String msg) {
		ToastUI.inst().show(msg);
	}

	@Override
	public void showError(String msg) {
		new BaseDialog("Error").text(msg).button("OK").show(getStage());
	}

	// =========================================================
	// Dialogs (Moved from Screen to View)
	// =========================================================

	public class CreateProjectDialog extends BaseDialog {
		private final VisTextField nameField;
		private final VisTextField pkgField;
		private final VisLabel errorLabel;
		private final VisSelectBox<String> templateBox;
		private final VisImage previewImage;
		private final VisLabel descLabel, versionLabel, enginVersionLabel;
		private final Array<TemplateInfo> templates;

		public CreateProjectDialog() {
			super("New Project");
			templates = ProjectService.inst().listTemplates();

			VisTable content = new VisTable();
			content.defaults().padBottom(10).left();

			// 1. Template
			float labelWidth = 220;
			VisTable tplRow = new VisTable();
			tplRow.add(new VisLabel("Template:")).width(labelWidth).left();
			templateBox = new VisSelectBox<>();
			Array<String> names = new Array<>();
			for(TemplateInfo t : templates) names.add(t.displayName);
			templateBox.setItems(names);
			tplRow.add(templateBox).width(labelWidth*3);
			content.add(tplRow).growX().row();

			// 2. Info
			VisTable infoTable = new VisTable();
			infoTable.setBackground(VisUI.getSkin().getDrawable("button"));
			infoTable.pad(15);
			previewImage = new VisImage();
			infoTable.add(previewImage).size(100).center().left().padRight(20);

			VisTable detailsTable = new VisTable();
			detailsTable.top().left();
			descLabel = new VisLabel("Description...");
			descLabel.setWrap(true);
			descLabel.setColor(Color.LIGHT_GRAY);
			descLabel.setAlignment(Align.center);
			detailsTable.add(descLabel).growX().center().top().row();

			versionLabel = new VisLabel("v1.0");
			versionLabel.setColor(Color.CYAN);
			versionLabel.setAlignment(Align.right);
			detailsTable.add(versionLabel).growX().right().padBottom(5).row();

			enginVersionLabel = new VisLabel("v1.0");
			enginVersionLabel.setColor(Color.GOLDENROD);
			enginVersionLabel.setAlignment(Align.right);
			detailsTable.add(enginVersionLabel).growX().right().padBottom(5);

			infoTable.add(detailsTable).grow();
			content.add(infoTable).growX().minHeight(120).padBottom(15).row();

			// 3. Inputs
			String baseName = "MyGame";
			String finalName = baseName;
			// 简单的名字查重逻辑
			FileHandle projectsRoot = Gd.engineConfig.getProjectsDir();
			if (projectsRoot != null && projectsRoot.exists()) {
				int counter = 1;
				while (projectsRoot.child(finalName).exists()) {
					finalName = baseName + counter;
					counter++;
				}
			}

			VisTable nameRow = new VisTable();
			nameRow.add(new VisLabel("Project Name:")).width(labelWidth).left();
			nameField = new VisTextField(finalName);
			nameRow.add(nameField).growX();
			content.add(nameRow).growX().row();

			VisTable pkgRow = new VisTable();
			pkgRow.add(new VisLabel("Package:")).width(labelWidth).left();
			pkgField = new VisTextField("com." + finalName.toLowerCase());
			pkgRow.add(pkgField).growX();
			content.add(pkgRow).growX().row();


			// [新增] 实时监听包名输入
			pkgField.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						String currentPkg = pkgField.getText();
						if (!com.goldsprite.gdengine.core.project.ProjectService.isValidPackageName(currentPkg)) {
							pkgField.setColor(Color.PINK); // 非法变红
							errorLabel.setText("Invalid Java Package Name");
						} else {
							pkgField.setColor(Color.WHITE);
							errorLabel.setText("");
						}
					}
			});
			nameField.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					pkgField.setText("com." + nameField.getText().toLowerCase());
				}
			});

			add(content).minWidth(600).pad(10).row();

			errorLabel = new VisLabel("");
			errorLabel.setColor(Color.RED);
			errorLabel.setWrap(true);
			errorLabel.setAlignment(Align.center);
			content.add(errorLabel).minWidth(Value.percentWidth(0.8f)).growX().padBottom(10).row();

			VisTextButton createBtn = new VisTextButton("Create Project");
			createBtn.setColor(Color.GREEN);
			createBtn.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					doCreate();
				}
			});
			content.add(createBtn).colspan(2).bottom().center().width(200).height(45);

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

			descLabel.setText(tmpl.description != null ? tmpl.description : "No description.");
			versionLabel.setText("template: v" + (tmpl.version != null ? tmpl.version : "1.0"));
			enginVersionLabel.setText("engine: v" + (tmpl.engineVersion != null ? tmpl.engineVersion : "1.0"));

			FileHandle imgFile = tmpl.dirHandle.child("preview.png");
			if(imgFile.exists()) {
				try {
					Texture tex = new Texture(imgFile);
					previewImage.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
				} catch(Exception e) { e.printStackTrace(); }
			} else {
				previewImage.setDrawable(new TextureRegionDrawable(new TextureRegion(new Texture(Gd.files.internal("gd_icon.png")))));
			}
			pack();
			centerWindow();
		}

		private void doCreate() {
			int idx = templateBox.getSelectedIndex();
			if(idx < 0) { errorLabel.setText("Please select a template"); return; }

			TemplateInfo tmpl = templates.get(idx);
			String name = nameField.getText().trim();
			String pkg = pkgField.getText().trim();

			// 调用 Presenter
			presenter.onProjectCreateRequest(tmpl, name, pkg);
			fadeOut(); // 无论成功失败，Presenter 会处理 UI 反馈，这里先关窗
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
		@Override protected void result(Object object) {
			if ((boolean) object) onYes.run();
		}
	}
}
