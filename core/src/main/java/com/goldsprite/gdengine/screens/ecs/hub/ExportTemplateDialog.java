// 文件: core/src/main/java/com/goldsprite/gdengine/screens/ecs/hub/ExportTemplateDialog.java
package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.core.project.model.TemplateInfo;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class ExportTemplateDialog extends BaseDialog {
	private final VisTextField idField, nameField, versionField;
	private final VisTextArea descArea;
	private final VisLabel errorLabel;
	private final FileHandle projectDir;

	public ExportTemplateDialog(FileHandle projectDir) {
		super("Export Template"); // 移除了 Dev Only 标记，现在是正式功能
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
		descArea = new VisTextArea("User exported template.");
		descArea.setPrefRows(3);
		content.add(descArea).width(300).row();

		add(content).padBottom(10).row();

		errorLabel = new VisLabel("");
		errorLabel.setColor(Color.RED);
		errorLabel.setWrap(true);
		add(errorLabel).width(400).padBottom(10).row();

		VisTextButton btnExport = new VisTextButton("Export to Local");
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
		// 自动注入引擎版本
		meta.engineVersion = com.goldsprite.gdengine.BuildConfig.DEV_VERSION;

		errorLabel.setText("Exporting...");
		errorLabel.setColor(Color.YELLOW);

		// 异步执行
		new Thread(() -> {
			// [修复] 使用 Service 接口
			String err = ProjectService.inst().exportProjectAsTemplate(projectDir, meta);

			Gdx.app.postRunnable(() -> {
				if (err == null) {
					fadeOut();
					com.goldsprite.gdengine.log.Debug.logT("Exporter", "✅ Export Completed: " + id);
					// 提示用户
					com.goldsprite.gdengine.ui.widget.ToastUI.inst().show("Template Exported!");
				} else {
					errorLabel.setText(err);
					errorLabel.setColor(Color.RED);
					pack();
				}
			});
		}).start();
	}
}
