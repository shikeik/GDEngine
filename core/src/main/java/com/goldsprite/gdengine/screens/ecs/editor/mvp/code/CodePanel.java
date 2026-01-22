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

	// [新增] 引用 Save 按钮以便改色
	private VisTextButton btnSave;
	// [新增] 本地脏状态标记
	private boolean isLocalDirty = false;

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

		// 1.1 占位符 (挤压右侧按钮)
		toolbar.add().expandX();

		// 2. Save 按钮 [修改: 赋值给成员变量]
		btnSave = new VisTextButton("Save");
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

		// [新增] 监听文字输入
		codeEditor.setOnTextChanged(this::onContentModified);

		addContent(codeEditor);
	}
	
	// [新增] 内容被修改时的逻辑
	private void onContentModified() {
		if (!isLocalDirty && currentFile != null) {
			isLocalDirty = true;
			updateUIState();
		}
	}

	// [新增] 统一更新 UI 状态
	private void updateUIState() {
		if (currentFile == null) return;

		String path = currentFile.path();

		if (isLocalDirty) {
			// 变脏：加星号，颜色变橙/黄
			fileInfoLabel.setText(path + " *"); // PathLabel 会自动处理截断，* 号会在末尾
			fileInfoLabel.setColor(Color.ORANGE);
			btnSave.setColor(Color.YELLOW);
		} else {
			// 干净：恢复原状
			fileInfoLabel.setText(path);
			fileInfoLabel.setColor(Color.CYAN);
			btnSave.setColor(Color.WHITE);
		}
	}

	public void openFile(FileHandle file) {
		if (file == null || !file.exists() || file.isDirectory()) return;

		this.currentFile = file;

		// 读取内容
		try {
			String content = file.readString("UTF-8");
			codeEditor.setText(content); // 这里会触发 ChangeListener

			// [关键] setText 会触发 changed 事件导致 isLocalDirty 变 true
			// 所以我们需要在这里强制重置为 false
			isLocalDirty = false;
			updateUIState();

		} catch (Exception e) {
			com.goldsprite.gdengine.log.Debug.logT("Code", "Error reading file: " + e.getMessage());
		}
	}

	private void saveCurrentFile() {
		if (currentFile != null) {
			try {
				currentFile.writeString(codeEditor.getText(), false, "UTF-8");
				ToastUI.inst().show("Saved: " + currentFile.name());

				// [修改] 保存成功，重置本地脏状态
				isLocalDirty = false;
				updateUIState();

				// 触发全局编译脏状态 (通知 Toolbar Build 按钮变红)
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
