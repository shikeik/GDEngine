package com.goldsprite.gdengine.screens.ecs.hub.mvp;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Timer;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.core.project.model.ProjectConfig;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.core.web.DocServer;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.ui.event.ContextListener;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.ChangeLogDialog;
import com.goldsprite.gdengine.ui.widget.IDEConsole;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.gdengine.screens.ecs.hub.SettingsWindow;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

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

		topBar.add(titleLabel).expandX().left();
		topBar.add(btnSettings).right().padRight(10).height(50);
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

		VisTextButton btnLog = new VisTextButton("📅 更新日志");
		btnLog.setColor(Color.SKY);
		btnLog.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
//				new ChangeLogDialog().show(getStage()); // 注释原内置解析式日志查看器

				// 使用新WebView接口方式
				{
					// 1. 启动本地服务器
					DocServer.startServer();

					// 2. 调用浏览器打开 http://localhost:8899/index.html
					if (Gd.browser != null) {
						Gd.browser.openUrl(DocServer.getIndexUrl(), "GDEngine Docs");
					}
				}
			}
		});

		bottomBar.add(btnLog).pad(5).left();
		add(bottomBar).growX().left();
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
			add(errorLabel).growX().padBottom(10).row();

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
