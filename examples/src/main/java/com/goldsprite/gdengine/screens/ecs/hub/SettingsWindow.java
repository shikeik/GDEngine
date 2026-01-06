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
		pathField.setText(Gd.engineConfig.projectsRootPath);
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
				pathField.setText(GDEngineConfig.getDefaultProjectsPath());
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

	private void saveSettings() {
		String newPath = pathField.getText().trim();

		// 简单的有效性校验
		FileHandle handle = Gdx.files.absolute(newPath);
		if (!handle.exists()) {
			// 尝试创建
			try {
				handle.mkdirs();
			} catch (Exception e) {
				errorLabel.setText("Invalid path: Cannot create directory.");
				pack();
				return;
			}
		}

		if (!handle.isDirectory()) {
			errorLabel.setText("Path exists but is not a directory.");
			pack();
			return;
		}

		// 更新 Config 并保存
		Gd.engineConfig.projectsRootPath = newPath;
		Gd.engineConfig.save();

		// 回调刷新
		if (onConfigChanged != null) onConfigChanged.run();

		fadeOut();
	}
}
