// core/src/main/java/com/goldsprite/gdengine/screens/ecs/editor/mvp/code/CodePanel.java

package com.goldsprite.gdengine.screens.ecs.editor.mvp.code;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.widget.BioCodeEditor;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.goldsprite.gdengine.ui.widget.PathLabel;
import com.badlogic.gdx.scenes.scene2d.ui.Value;

public class CodePanel extends EditorPanel {

	private BioCodeEditor codeEditor;
	private FileHandle currentFile;
	private PathLabel fileInfoLabel;

	public CodePanel() {
		super("Code");

		// [Fix] 移除 EditorPanel 默认的背景和 Padding，让代码编辑器贴边充满
		setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable)null);
		pad(0);
		contentTable.pad(0); // 确保内容容器也没有 Padding
		top();
		defaults().top();

		// --- 顶部工具栏容器 (嵌入在编辑器顶部) ---
		VisTable toolbar = new VisTable();
		toolbar.setBackground("button"); // 给工具栏单独加个深色背景区分


        // 1. Path Label [修改]：只占 1/2 宽度
        fileInfoLabel = new PathLabel("No file open");
        fileInfoLabel.setColor(Color.GRAY);
        // 使用 Value.percentWidth 限制宽度
        toolbar.add(fileInfoLabel).width(Value.percentWidth(0.5f, toolbar)).left().padLeft(5);

        // 2. 占位符 (挤压右侧按钮)
        toolbar.add().expandX();

        // 3. [新增] Save 按钮
        VisTextButton btnSave = new VisTextButton("Save");
        btnSave.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					saveCurrentFile();
				}
			});
        toolbar.add(btnSave).padRight(5);

        // 4. Maximize 按钮
        VisTextButton btnMax = new VisTextButton("[ ]");
        btnMax.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					com.goldsprite.gdengine.log.Debug.log("click []扩展按钮");
					EditorEvents.inst().emitToggleMaximizeCode();
					if (btnMax.getText().toString().equals("[ ]")) btnMax.setText("><");
					else btnMax.setText("[ ]");
				}
			});
        toolbar.add(btnMax).width(40).height(btnMax.getPrefHeight()).padRight(40); // 微调
		
        contentTable.add(toolbar).growX().height(contentTable.getPrefHeight()).row();
		
		// 编辑器核心
		codeEditor = new BioCodeEditor();
		codeEditor.setOnSave(this::saveCurrentFile);

		addContent(codeEditor);
	}

	public void openFile(FileHandle file) {
		if (file == null || !file.exists() || file.isDirectory()) return;

		this.currentFile = file;
		fileInfoLabel.setText(file.path());
		fileInfoLabel.setColor(Color.CYAN);

		// 读取内容
		try {
			String content = file.readString("UTF-8");
			codeEditor.setText(content);
		} catch (Exception e) {
			com.goldsprite.gdengine.log.Debug.logT("Code", "Error reading file: " + e.getMessage());
		}
	}

	private void saveCurrentFile() {
		if (currentFile != null) {
			try {
				currentFile.writeString(codeEditor.getText(), false, "UTF-8");
				ToastUI.inst().show("Saved: " + currentFile.name());

				// [Fix] 解决 TODO: 触发脏状态，提示需要编译
				EditorEvents.inst().emitCodeDirty();

			} catch (Exception e) {
				ToastUI.inst().show("Save Failed!");
				e.printStackTrace();
			}
		}
	}

	// [新增] 公开保存接口
	public void save() {
		saveCurrentFile();
	}

	public FileHandle getCurrentFile() {
		return currentFile;
	}
}
