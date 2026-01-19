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
    private static final String DOC_MANIFEST_URL = "https://cdn.jsdelivr.net/gh/shikeik/GDEngine@main/dist/docs_manifest.json";
	
	private void openLocalDocs() {
        String activeRoot = com.goldsprite.gdengine.core.config.GDEngineConfig.getInstance().getActiveEngineRoot();
        if (activeRoot == null) activeRoot = com.goldsprite.gdengine.core.config.GDEngineConfig.getRecommendedRoot();

        FileHandle docEntry = com.badlogic.gdx.Gdx.files.absolute(activeRoot).child("engine_docs/index.html");
        boolean localExists = docEntry.exists();

        // 1. 如果本地完全没有，直接下载
        if (!localExists) {
            startDocsDownload(activeRoot, null); // null 表示强制下载
            return;
        }

        // 2. 如果本地有，先提示“正在检查更新...”，同时异步去取云端清单
        ToastUI.inst().show("正在检查文档更新...");

        String finalRoot = activeRoot;
        com.goldsprite.gdengine.utils.MultiPartDownloader.fetchManifest(DOC_MANIFEST_URL, new com.goldsprite.gdengine.utils.MultiPartDownloader.ManifestCallback() {
				@Override
				public void onSuccess(com.goldsprite.gdengine.utils.MultiPartDownloader.Manifest cloudManifest) {
					checkDocVersion(finalRoot, cloudManifest);
				}

				@Override
				public void onError(String err) {
					// 网络失败，但本地有文件，直接打开旧版
					ToastUI.inst().show("无法连接更新服务器，打开本地缓存...");
					launchDocServer();
				}
			});
    }
	
	private void checkDocVersion(String rootPath, com.goldsprite.gdengine.utils.MultiPartDownloader.Manifest cloudManifest) {
        com.badlogic.gdx.Preferences prefs = com.badlogic.gdx.Gdx.app.getPreferences(PREF_DOCS);
        String localTime = prefs.getString(KEY_DOC_TIME, "");

        // 对比时间戳
        if (!localTime.equals(cloudManifest.updatedAt)) {
            // 版本不一致 (有更新)
            String sizeStr = String.format("%.2f MB", cloudManifest.totalSize / 1024f / 1024f);

            new BaseDialog("文档更新") {
                @Override
                protected void result(Object object) {
                    if ((boolean) object) {
                        // 用户选择更新
                        startDocsDownload(rootPath, cloudManifest.updatedAt);
                    } else {
                        // 用户选择跳过，打开旧版
                        launchDocServer();
                    }
                }
            }
				.text("发现新版本文档 (" + cloudManifest.updatedAt + ")\n大小: " + sizeStr + "\n是否更新？")
				.button("更新 (Update)", true)
				.button("暂不 (Skip)", false)
				.show(getStage());

        } else {
            // 版本一致，直接打开
            ToastUI.inst().show("文档已是最新");
            launchDocServer();
        }
    }
	
	// 复用下载逻辑，增加 updateTime 参数用于更新 Prefs
    private void startDocsDownload(String rootPath, String newUpdateTime) {
        String SAVE_DIR = rootPath; 

        ToastUI.inst().show("开始下载文档...");

        com.goldsprite.gdengine.utils.MultiPartDownloader.download(
            DOC_MANIFEST_URL, 
            SAVE_DIR,
            (progress, msg) -> {
			com.badlogic.gdx.Gdx.app.postRunnable(() -> {
				if (progress < 0) showError("下载失败: " + msg);
				else if (progress % 10 == 0) ToastUI.inst().show(msg);
			});
		},
		() -> {
			com.badlogic.gdx.Gdx.app.postRunnable(() -> {
				ToastUI.inst().show("文档更新完毕！");

				// [核心] 下载成功后，更新本地记录的时间戳
				// 如果 newUpdateTime 为 null (首次下载)，我们需要从刚才下载的 manifest 里拿
				// 但 MultiPartDownloader.download 内部没把 manifest 传出来。
				// 简单做法：我们再从云端拿一次？不，这太蠢了。
				// 优化做法：MultiPartDownloader.download 的 onFinish 回调如果能把 Manifest 传回来最好。
				// 既然现在不想改 Downloader 接口，我们可以在这里偷个懒：
				// 如果 newUpdateTime 是 null，说明是首次下载，我们假设它是最新的（或者可以在 download 内部存）。

				// 为了严谨，建议修改一下 MultiPartDownloader.download 的 onFinish 签名
				// 但为了不改动太大，我们这里如果是首次下载，就先不存 Prefs (下次打开会再次检查，然后存入)
				// 或者，我们可以再次 fetch 一次 manifest (有缓存，很快)

				if (newUpdateTime != null) {
					com.badlogic.gdx.Preferences prefs = com.badlogic.gdx.Gdx.app.getPreferences(PREF_DOCS);
					prefs.putString(KEY_DOC_TIME, newUpdateTime);
					prefs.flush();
				} else {
					// 首次下载完，为了防止下次误报更新，我们应该保存。
					// 这里再调一次 fetch 拿最新的时间存起来
					com.goldsprite.gdengine.utils.MultiPartDownloader.fetchManifest(DOC_MANIFEST_URL, new com.goldsprite.gdengine.utils.MultiPartDownloader.ManifestCallback() {
                            @Override public void onSuccess(com.goldsprite.gdengine.utils.MultiPartDownloader.Manifest m) {
                                com.badlogic.gdx.Preferences prefs = com.badlogic.gdx.Gdx.app.getPreferences(PREF_DOCS);
                                prefs.putString(KEY_DOC_TIME, m.updatedAt);
                                prefs.flush();
                            }
                            @Override public void onError(String e) {}
                        });
				}

				launchDocServer();
			});
		}
        );
    }
	
	private void launchDocServer() {
        try {
            com.goldsprite.gdengine.core.web.DocServer.startServer(
                Gdx.files.absolute(com.goldsprite.gdengine.core.config.GDEngineConfig.getInstance().getActiveEngineRoot())
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

    private void startMultiPartDownload(String rootPath) {
        // 定义云端清单地址 (假设我们稍后会上传到这里)
        // 使用 JsDelivr 加速 GitHub
        String MANIFEST_URL = "https://cdn.jsdelivr.net/gh/shikeik/GDEngine@main/dist/docs_manifest.json";
        String SAVE_DIR = rootPath; // 下载到根目录，解压出 engine_docs

        ToastUI.inst().show("未检测到本地文档，准备从云端获取...");

        // 调用分卷下载器 (MultiPartDownloader)
        // 注意：这里我们马上就要创建这个类
       	MultiPartDownloader.download(
            MANIFEST_URL, 
            SAVE_DIR,
            (progress, msg) -> {
			// UI 线程回调
			Gdx.app.postRunnable(() -> {
				if (progress < 0) showError("下载失败: " + msg);
				else if (progress % 10 == 0) ToastUI.inst().show(msg);
			});
		},
		() -> {
			// 完成回调
			Gdx.app.postRunnable(() -> {
				ToastUI.inst().show("文档下载解压完毕！");
				launchDocServer();
			});
		}
        );
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
