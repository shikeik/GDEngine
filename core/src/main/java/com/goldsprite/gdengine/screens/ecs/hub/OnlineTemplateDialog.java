package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.core.Gd;
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
        // 模拟数据：实际这里应该先去下 templates_index.json
        addTemplateItem("BigDemo (70MB Test)", "https://cdn.jsdelivr.net/gh/shikeik/GDEngine@main/dist/templates/BigDemo/manifest.json");

        getContentTable().add(listTable).grow().width(500).height(300).pad(10).row();

        // 2. 状态区
        statusLabel = new VisLabel("Ready");
        statusLabel.setColor(Color.GRAY);
        getContentTable().add(statusLabel).growX().pad(5).row();

        progressBar = new VisProgressBar(0, 100, 1, false);
        getContentTable().add(progressBar).growX().pad(5).row();

        addCloseButton();
        pack();
        centerWindow();
    }

    private void addTemplateItem(String name, String manifestUrl) {
        VisTable row = new VisTable();
        row.setBackground("button");
        row.pad(10);

        row.add(new VisLabel(name)).expandX().left();

        VisTextButton btnDownload = new VisTextButton("Download");
        btnDownload.setColor(Color.CYAN);
        btnDownload.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					startDownload(name, manifestUrl);
				}
			});
        row.add(btnDownload);

        listTable.add(row).growX().padBottom(5).row();
    }

    private void startDownload(String name, String url) {
        // 目标：LocalTemplates 目录 (ProjectService 会扫描这里)
        FileHandle localTemplates = Gd.files.local("LocalTemplates");

        // 这里的 saveDir 应该是存放解压后文件夹的父目录
        // MultiPartDownloader 会在里面创建 "download_cache"，合并完解压到 saveDir
        // 我们的 zip 包结构是: BigDemo/project.json...
        // 所以直接解压到 LocalTemplates 即可
        String saveDir = localTemplates.file().getAbsolutePath();

        statusLabel.setText("Initializing download...");
        statusLabel.setColor(Color.YELLOW);

        MultiPartDownloader.download(
            url,
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
				// 还可以通知 Hub 刷新创建项目的列表
			});
		}
        );
    }
}
