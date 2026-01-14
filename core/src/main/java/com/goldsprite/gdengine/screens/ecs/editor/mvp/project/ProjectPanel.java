package com.goldsprite.gdengine.screens.ecs.editor.mvp.project;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable; // 关键导入
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.event.ContextListener;
import com.goldsprite.gdengine.ui.widget.FileTreeWidget;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;

public class ProjectPanel extends EditorPanel {

    private ProjectPresenter presenter;
    private FileTreeWidget fileTree;
    private VisTable gridTable;
    private VisLabel pathLabel;

    public ProjectPanel() {
        super("Project");

        // 1. 左侧：文件树
        fileTree = new FileTreeWidget();
        VisScrollPane treeScroll = new VisScrollPane(fileTree);
        treeScroll.setFadeScrollBars(false);

        // 2. 右侧：网格视图
        VisTable rightContainer = new VisTable();

        pathLabel = new VisLabel("Assets/");
        pathLabel.setColor(Color.GRAY);
        rightContainer.add(pathLabel).growX().pad(5).row();

        gridTable = new VisTable();
        gridTable.top().left();

        VisScrollPane gridScroll = new VisScrollPane(gridTable);
        gridScroll.setFadeScrollBars(false);
        rightContainer.add(gridScroll).grow();

        // 3. 分割
        VisSplitPane split = new VisSplitPane(treeScroll, rightContainer, false);
        split.setSplitAmount(0.25f);

        addContent(split);
    }

    public void setPresenter(ProjectPresenter presenter) {
        this.presenter = presenter;
        fileTree.setCallbacks(
            file -> presenter.onTreeSelected(file),
		(file, x, y) -> presenter.onShowContextMenu(file, x, y)
        );
    }

    public void refreshTree(FileHandle root) {
        fileTree.build(root);
    }

    public void updatePathLabel(String path) {
        pathLabel.setText(path);
    }

    public void showFolderContent(Array<FileHandle> files) {
        gridTable.clearChildren();

        if (files.size == 0) {
            gridTable.add(new VisLabel("Empty Folder")).pad(20);
            return;
        }

        float itemSize = 80f;
        float padding = 10f;
        int columns = 8;
        int count = 0;

        for (FileHandle file : files) {
            if (file.name().startsWith(".")) continue;

            Actor item = createGridItem(file);
            gridTable.add(item).size(itemSize, itemSize + 20).pad(padding);

            count++;
            if (count % columns == 0) gridTable.row();
        }
    }

    // --- [修复点 1] ---
    private Actor createGridItem(FileHandle file) {
        String name = file.name();
        if (name.length() > 10) name = name.substring(0, 8) + "..";

        VisImageTextButton btn = new VisImageTextButton(name, "default");
        btn.getLabelCell().padTop(5);
        btn.getImageCell().expand().fill();
        btn.align(Align.center);

        // [修复] 使用 getImage().setDrawable(...)
        btn.getImage().setDrawable(getIconDrawable(file));

        btn.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (getTapCount() == 2) {
						presenter.onGridDoubleClicked(file);
					} else {
						presenter.onGridSelected(file);
					}
				}
			});

        btn.addListener(new ContextListener() {
				@Override public void onShowMenu(float stageX, float stageY) {
					presenter.onGridSelected(file);
					presenter.onShowContextMenu(file, stageX, stageY);
				}
			});

        return btn;
    }

    // --- [修复点 2] ---
    // 返回类型改为 Drawable (接口)，因为 tint 返回的不一定是 TextureRegionDrawable
    private Drawable getIconDrawable(FileHandle file) {
        String drawableName = "white"; 
        // 确保从 Skin 获取 Region 并包装
        TextureRegionDrawable drawable = new TextureRegionDrawable(VisUI.getSkin().getRegion(drawableName));

        if (file.isDirectory()) return drawable.tint(Color.GOLD);
        if (file.extension().equals("java")) return drawable.tint(Color.CYAN);
        if (file.extension().equals("scene")) return drawable.tint(Color.ORANGE);
        if (file.extension().equals("png")) return drawable.tint(Color.PINK);

        return drawable.tint(Color.LIGHT_GRAY);
    }

    public void showMenu(com.kotcrab.vis.ui.widget.PopupMenu menu, float x, float y) {
        menu.showMenu(getStage(), x, y);
    }
}
