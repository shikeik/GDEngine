// core/src/main/java/com/goldsprite/gdengine/screens/ecs/editor/mvp/code/CodePanel.java

package com.goldsprite.gdengine.screens.ecs.editor.mvp.code;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.widget.BioCodeEditor;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class CodePanel extends EditorPanel {

	private BioCodeEditor codeEditor;
	private FileHandle currentFile;
	private VisLabel fileInfoLabel;

	public CodePanel() {
		super("Code");

		// [Fix] 移除 EditorPanel 默认的背景和 Padding，让代码编辑器贴边充满
		setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable)null);
		pad(0);
		contentTable.pad(0); // 确保内容容器也没有 Padding

		// --- 顶部工具栏容器 (嵌入在编辑器顶部) ---
		VisTable toolbar = new VisTable();
		toolbar.setBackground("button"); // 给工具栏单独加个深色背景区分

		// 1. 文件信息
		fileInfoLabel = new VisLabel("No file open");
		fileInfoLabel.setColor(Color.GRAY);
		toolbar.add(fileInfoLabel).expandX().left().padLeft(5); // 左侧留点空隙

		// 2. 全屏切换按钮
		VisTextButton btnMax = new VisTextButton("[ ]");
		btnMax.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				EditorEvents.inst().emitToggleMaximizeCode();
				if (btnMax.getText().toString().equals("[ ]")) btnMax.setText("><");
				else btnMax.setText("[ ]");
			}
		});
		toolbar.add(btnMax).width(30).height(25); // 紧贴右边，不要 padRight

		// 添加工具栏到顶部
		contentTable.add(toolbar).growX().height(26).row();

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

	// [新增] 公开保存接口
	public void save() {
		saveCurrentFile();
	}

	public FileHandle getCurrentFile() {
		return currentFile;
	}
}
