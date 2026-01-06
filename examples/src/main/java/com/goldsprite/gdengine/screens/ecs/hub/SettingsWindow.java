package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class SettingsWindow extends BaseDialog {

	private final VisTextField pathField;
	private final VisLabel errorLabel;
	private final Runnable onConfigChanged;

	public SettingsWindow(Runnable onConfigChanged) {
		super("Engine Settings");
		this.onConfigChanged = onConfigChanged;

		left();

		add(new VisLabel("Projects Root Path:")).left().padBottom(5).row();

		// 路径输入框
		pathField = new VisTextField();
		pathField.setText(Gd.engineConfig.projectsSubDir);
		add(pathField).growX().minWidth(500).padBottom(10).row();

		// 错误提示区域
		errorLabel = new VisLabel("");
		errorLabel.setColor(Color.RED);
		add(errorLabel).center().padBottom(10).row();

		// 按钮栏
		add(createButtonPanel()).growX();

		pack();
		centerWindow();
	}

	private Actor createButtonPanel() {
		VisTable table = new VisTable();

		VisTextButton btnReset = new VisTextButton("Reset Default");
		btnReset.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				pathField.setText(GDEngineConfig.getRecommendedRoot());
				errorLabel.setText("");
			}
		});

		VisTextButton btnSave = new VisTextButton("Save");
		btnSave.setColor(Color.GREEN);
		btnSave.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveSettings();
			}
		});

		VisTextButton btnCancel = new VisTextButton("Cancel");
		btnCancel.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				fadeOut();
			}
		});

		table.add(btnReset).left();
		table.add().expandX();
		table.add(btnCancel).padRight(10);
		table.add(btnSave);

		return table;
	}

	// [修改] saveSettings 方法逻辑
	private void saveSettings() {
		// 这里现在只修改 projectsSubDir，或者只做一些简单的 UI Scale 配置
		// 如果我们允许用户在设置里修改 Root 路径，逻辑会比较复杂(迁移文件等)
		// 暂时只允许修改 "子目录名" 或者 "UI Scale"

		// 既然我们现在的需求是 Projects 路径可配置，而 Config 已经把 Projects 锁定在 Root 下了
		// 那么这里的输入框如果依然是绝对路径，逻辑就会冲突。

		// 方案修正：
		// SettingsWindow 暂时只显示当前 Root 路径 (Read Only)，或者提供 "Reset Root" 按钮(清除 Prefs 并重启)。
		// 鉴于目前需求，我们先把这个窗口简化为 "信息展示 + UI配置"。

		// 如果您确实想改 Projects 路径，那是修改 GDEngineConfig.projectsSubDir

		// 示例：只保存 UI Scale (如果有输入框的话)
		// Gd.engineConfig.uiScale = ...

		Gd.engineConfig.save();
		if (onConfigChanged != null) onConfigChanged.run();
		fadeOut();
	}
}
