package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gdengine.core.Gd;
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

//		debugAll();

		// --- 标题 ---
		getContentTable().add(new VisLabel("Engine Preferences")).padBottom(20).row();

		// --- 表单区域 ---
		VisTable form = new VisTable();
		form.defaults().padBottom(10).left();

		// 1. Engine Root (ReadOnly) - 告诉用户引擎安装在哪
		form.add(new VisLabel("Engine Root (Read-only):"));
		VisLabel rootLabel = new VisLabel(Gd.engineConfig.getActiveEngineRoot());
		rootLabel.setColor(Color.GRAY);
		form.add(rootLabel).row();

		// 2. Projects Path (Editable) - 允许用户改到 Git 目录
		form.add(new VisLabel("Projects Path:"));
		pathField = new VisTextField(Gd.engineConfig.customProjectsPath);
		pathField.setMessageText("Leave empty to use default");
		form.add(pathField).width(400).row();

		getContentTable().add(form).padBottom(20).row();

		// --- 错误提示 ---
		errorLabel = new VisLabel("");
		errorLabel.setColor(Color.RED);
		getContentTable().add(errorLabel).center().padBottom(10).row();

		// --- 按钮栏 ---
		getContentTable().add(createButtonPanel()).growX();

		pack();
		centerWindow();
	}

	private Actor createButtonPanel() {
		VisTable table = new VisTable();

		VisTextButton btnReset = new VisTextButton("Default");
		btnReset.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				pathField.setText(""); // 清空即恢复默认
				errorLabel.setText("");
			}
		});

		VisTextButton btnSave = new VisTextButton("Save 和 Apply");
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

		// 如果输入了路径，校验有效性
		if (!newPath.isEmpty()) {
			FileHandle handle = Gdx.files.absolute(newPath);
			if (!handle.exists()) {
				try {
					handle.mkdirs();
				} catch (Exception e) {
					errorLabel.setText("Cannot create directory!");
					pack();
					return;
				}
			}
			if (!handle.isDirectory()) {
				errorLabel.setText("Path is not a directory!");
				pack();
				return;
			}
		}

		// 更新 Config
		Gd.engineConfig.customProjectsPath = newPath;
		Gd.engineConfig.save();

		// 刷新回调
		if (onConfigChanged != null) onConfigChanged.run();

		fadeOut();
	}
}
