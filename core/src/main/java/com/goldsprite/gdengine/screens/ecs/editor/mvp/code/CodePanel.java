package com.goldsprite.gdengine.screens.ecs.editor.mvp.code;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.widget.BioCodeEditor;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.VisLabel;

public class CodePanel extends EditorPanel {

	private BioCodeEditor codeEditor;
	private FileHandle currentFile;
	private VisLabel fileInfoLabel;

	public CodePanel() {
		super("Code");

		// 信息栏
		fileInfoLabel = new VisLabel("No file open");
		fileInfoLabel.setColor(Color.GRAY);
		contentTable.add(fileInfoLabel).growX().pad(5).row();

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
				// TODO: 触发编译请求 (EditorState.setDirty)
			} catch (Exception e) {
				ToastUI.inst().show("Save Failed!");
				e.printStackTrace();
			}
		}
	}

	public FileHandle getCurrentFile() {
		return currentFile;
	}
}
