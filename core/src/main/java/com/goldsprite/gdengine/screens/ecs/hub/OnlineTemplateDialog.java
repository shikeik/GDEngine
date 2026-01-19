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
        // [修改] addTemplateItem 增加 id 参数 (BigDemo)
        // 参数: 显示名称, 文件夹ID, Manifest URL
        addTemplateItem("BigDemo (70MB Test)", "BigDemo", "https://cdn.jsdelivr.net/gh/shikeik/GDEngine@main/dist/templates/BigDemo/manifest.json");;

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

	// [修改] 增加 String id 参数
    private void addTemplateItem(String name, String id, String manifestUrl) {
        VisTable row = new VisTable();
        row.setBackground("button");
        row.pad(10);

        row.add(new VisLabel(name)).expandX().left();

        VisTextButton btnDownload = new VisTextButton("Download");
        btnDownload.setColor(Color.CYAN);
        btnDownload.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					startDownload(name, id, manifestUrl); // 传递 ID
				}
			});
        row.add(btnDownload);

        listTable.add(row).growX().padBottom(5).row();
    }

    // [修改] 核心下载路径修正
    private void startDownload(String name, String id, String url) {
        // 1. 获取引擎根目录 (例如 /sdcard/GDEngine)
        String engineRoot = com.goldsprite.gdengine.core.config.GDEngineConfig.getInstance().getActiveEngineRoot();
        if (engineRoot == null) {
            statusLabel.setText("Error: Engine not initialized");
            return;
        }

        // 2. 构建目标路径: <Root>/LocalTemplates/<ID>/
        // 这样解压出来的 project.json 就会在 /LocalTemplates/BigDemo/project.json，结构就对了
        FileHandle targetDir = Gdx.files.absolute(engineRoot)
			.child("LocalTemplates")
			.child(id); // 关键：建立子文件夹

        // 这里的 saveDir 传给下载器，下载器会解压到这里
        String saveDir = targetDir.file().getAbsolutePath();

        statusLabel.setText("Initializing download...");
        statusLabel.setColor(Color.YELLOW);

        // 如果目录存在，建议先清理（可选），防止旧文件残留
        if (targetDir.exists()) {
            targetDir.deleteDirectory();
        }
        targetDir.mkdirs();

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
			});
		}
        );
    }
}
