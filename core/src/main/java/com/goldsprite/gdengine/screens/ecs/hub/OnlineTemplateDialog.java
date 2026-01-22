package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.CloudConstants;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.gdengine.utils.MultiPartDownloader;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisProgressBar;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class OnlineTemplateDialog extends BaseDialog {

	private final VisProgressBar progressBar;
	private final VisLabel statusLabel;
	private final VisTable listTable;

	public OnlineTemplateDialog() {
		super("Online Template Store");

		// 1. 列表区
		listTable = new VisTable();
		listTable.top().left();

		// [修改] 只传名字和ID，不再传 URL
		addTemplateItem("BigDemo (102MB Test)", "BigDemo");

		getContentTable().add(listTable).grow().width(500).height(300).pad(10).row();

		// 2. 状态区
		statusLabel = new VisLabel("Ready");
		statusLabel.setColor(Color.GRAY);
		getContentTable().add(statusLabel).growX().pad(5).row();

		progressBar = new VisProgressBar(0, 100, 1, false);
		getContentTable().add(progressBar).growX().pad(5).row();
	}

	// [修改] 移除 manifestUrl 参数
	private void addTemplateItem(String name, String id) {
		VisTable row = new VisTable();
		row.setBackground("button");
		row.pad(10);

		row.add(new VisLabel(name)).expandX().left();

		VisTextButton btnDownload = new VisTextButton("Download");
		btnDownload.setColor(Color.CYAN);
		btnDownload.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				startDownload(name, id); // 调用简化后的方法
			}
		});
		row.add(btnDownload);

		listTable.add(row).growX().padBottom(5).row();
	}

	// [重构] 下载逻辑
	private void startDownload(String name, String id) {
		String engineRoot = GDEngineConfig.getInstance().getActiveEngineRoot();
		if (engineRoot == null) {
			statusLabel.setText("Error: Engine not initialized");
			return;
		}

		FileHandle targetDir = Gdx.files.absolute(engineRoot).child("LocalTemplates").child(id);
		String saveDir = targetDir.file().getAbsolutePath();

		statusLabel.setText("Step 1: Checking Version...");
		statusLabel.setColor(Color.YELLOW);

		if (targetDir.exists()) targetDir.deleteDirectory();
		targetDir.mkdirs();

		// Step 1: 获取 SHA
		MultiPartDownloader.fetchLatestSha(new MultiPartDownloader.ShaCallback() {
			@Override
			public void onSuccess(String sha) {
				Gdx.app.postRunnable(() -> {
					statusLabel.setText("Step 2: Downloading...");

					// Step 2: 组装 URL 并下载
					String manifestPath = CloudConstants.PATH_TEMPLATES_DIST + id + "/manifest.json";
					String assetPath = CloudConstants.PATH_TEMPLATES_DIST + id + "/";

					String manifestUrl = CloudConstants.getManifestUrl(sha, manifestPath);
					String cdnBaseUrl = CloudConstants.getAssetCdnBaseUrl(sha, assetPath);

					MultiPartDownloader.download(
						manifestUrl,
						cdnBaseUrl,
						saveDir,
						(percent, msg) -> {
							Gdx.app.postRunnable(() -> {
								if (percent >= 0) {
									progressBar.setValue(percent);
									statusLabel.setText(msg);
								} else {
									statusLabel.setText("Error: " + msg);
									statusLabel.setColor(Color.RED);
								}
							});
						},
						() -> {
							Gdx.app.postRunnable(() -> {
								statusLabel.setText("Completed!");
								statusLabel.setColor(Color.GREEN);
								ToastUI.inst().show("Template " + name + " installed!");
							});
						}
					);
				});
			}

			@Override
			public void onError(String err) {
				Gdx.app.postRunnable(() -> {
					statusLabel.setText("API Error: " + err);
					statusLabel.setColor(Color.RED);
				});
			}
		});
	}
}
