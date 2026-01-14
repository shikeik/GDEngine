package com.goldsprite.gdengine.screens.ecs.editor.mvp.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.project.ProjectService;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;

public class ProjectPresenter {
    private final ProjectPanel view;
    private final FileHandle projectRoot;

    // 当前在右侧网格显示的目录
    private FileHandle currentDir;

    public ProjectPresenter(ProjectPanel view) {
        this.view = view;
        this.view.setPresenter(this);

        FileHandle proj = ProjectService.inst().getCurrentProject();
        this.projectRoot = (proj != null) ? proj : Gdx.files.local(".");

        // 默认显示根目录
        this.currentDir = projectRoot;

        refresh();
    }

    public void refresh() {
        view.refreshTree(projectRoot);
        loadDirContent(currentDir);
    }

    // --- Tree Interactions ---

    public void onTreeSelected(FileHandle file) {
        // 如果点的是文件夹，右边就显示它的内容
        if (file.isDirectory()) {
            loadDirContent(file);
        } else {
            // 如果点的是文件，右边显示它所在的文件夹，并选中它
            loadDirContent(file.parent());
        }
        // 同时通知 Inspector
        EditorEvents.inst().emitSelectionChanged(file);
    }

    // --- Grid Interactions ---

    public void onGridSelected(FileHandle file) {
        // 通知 Inspector 泛型绘制器工作
        EditorEvents.inst().emitSelectionChanged(file);
    }

    public void onGridDoubleClicked(FileHandle file) {
        if (file.isDirectory()) {
            // 进入文件夹
            loadDirContent(file);
        } else {
            // 打开文件 (打印日志)
            Gdx.app.log("Project", "Open File: " + file.name());
            // TODO: 这里可以发射 OpenScriptEvent 之类的
        }
    }

    private void loadDirContent(FileHandle dir) {
        this.currentDir = dir;
        view.updatePathLabel(dir.path().replace(projectRoot.path(), "")); // 相对路径

        FileHandle[] files = dir.list();
        Array<FileHandle> list = new Array<>(files);
        // 简单排序
        list.sort((a, b) -> {
            if(a.isDirectory() && !b.isDirectory()) return -1;
            if(!a.isDirectory() && b.isDirectory()) return 1;
            return a.name().compareTo(b.name());
        });

        view.showFolderContent(list);
    }

    // --- Menu ---

    public void onShowContextMenu(FileHandle file, float x, float y) {
        PopupMenu menu = new PopupMenu();

        if (file.isDirectory()) {
            menu.addItem(new MenuItem("Create Folder", new ChangeListener() {
								 @Override public void changed(ChangeEvent event, Actor actor) {
									 file.child("NewFolder").mkdirs();
									 refresh();
								 }
							 }));

            // 简单的创建脚本逻辑
            menu.addItem(new MenuItem("Create Script", new ChangeListener() {
								 @Override public void changed(ChangeEvent event, Actor actor) {
									 file.child("NewScript.java").writeString("public class NewScript {}", false);
									 refresh();
								 }
							 }));
        }

        MenuItem delItem = new MenuItem("Delete");
        delItem.getLabel().setColor(Color.RED);
        delItem.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					if(file.isDirectory()) file.deleteDirectory(); else file.delete();
					refresh();
				}
			});
        menu.addItem(delItem);

        view.showMenu(menu, x, y);
    }
}
