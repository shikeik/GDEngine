package com.goldsprite.gdengine.screens.ecs.editor.mvp.project;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.event.ContextListener;
import com.goldsprite.gdengine.ui.widget.FileTreeWidget;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

public class ProjectPanel extends EditorPanel {

	private ProjectPresenter presenter;
	private final FileTreeWidget fileTree;
	private final VisTable gridTable;
	private final VisLabel pathLabel;

	public ProjectPanel() {
		super("Project");

		// 1. 左侧：文件树
		fileTree = new FileTreeWidget();
		VisScrollPane treeScroll = new VisScrollPane(fileTree);
		treeScroll.setFadeScrollBars(false);
		VisTable treeScrollContainer = new VisTable();
		treeScrollContainer.add(treeScroll).grow();

		// 2. 右侧：网格视图
		VisTable rightContainer = new VisTable();

		pathLabel = new VisLabel("Assets/");
		pathLabel.setColor(Color.GRAY);
		rightContainer.add(pathLabel).growX().pad(5).row();

		gridTable = new VisTable();
		gridTable.top().left();

		VisScrollPane gridScroll = new VisScrollPane(gridTable);
		gridScroll.setFadeScrollBars(false);
		// 给 ScrollPane 的监听器，捕捉空白处点击
		gridScroll.addListener(new ContextListener() {
			@Override
			public void onShowMenu(float stageX, float stageY) {
				// 如果鼠标下面没有 Item (通过 hit 检测)，则显示当前文件夹菜单
				Actor hit = gridTable.hit(stageX, stageY, true);
				if (hit == null || hit == gridTable) {
					// 获取当前目录 (需要 Presenter 支持，这里回调 null 表示当前目录)
					presenter.onShowContextMenu(null, stageX, stageY);
				}
			}

			@Override
			public void onLeftClick(InputEvent event, float x, float y, int count) {
				// 点击空白处：取消选中 (可选)
				Actor hit = gridTable.hit(gridTable.stageToLocalCoordinates(new com.badlogic.gdx.math.Vector2(event.getStageX(), event.getStageY())).x,
					gridTable.stageToLocalCoordinates(new com.badlogic.gdx.math.Vector2(event.getStageX(), event.getStageY())).y, true);
				if (hit == null || hit == gridTable) {
					presenter.onGridSelected(null);
				}
			}
		});
		rightContainer.add(gridScroll).grow();

		// 3. 分割
		VisSplitPane split = new VisSplitPane(treeScrollContainer, rightContainer, false);
		split.setSplitAmount(0.25f);

		addContent(split);
	}

	public void setPresenter(ProjectPresenter presenter) {
		this.presenter = presenter;
		fileTree.setCallbacks(
			presenter::onTreeSelected,
			presenter::onGridDoubleClicked, // [修改] 复用 Grid 的双击逻辑即可，逻辑是一样的
			presenter::onShowContextMenu
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
		
		// [核心修复] 右键菜单：添加 event.stop() 防止冒泡到背景
		btn.addListener(new ContextListener() {
				@Override public void onShowMenu(float stageX, float stageY) {
					presenter.onGridSelected(file);
					presenter.onShowContextMenu(file, stageX, stageY);
					// 【关键】阻止事件冒泡，否则 Grid 背景也会收到右键事件，导致弹出两个菜单
					// ContextListener 内部捕获了 InputEvent，但我们要确保它停止传播
					// 由于 onShowMenu 是回调，我们无法直接访问 event。
					// 必须在 ContextListener 的实现里处理，或者这里用 hack。
					// 更好的方式：ContextListener 的 touchDown 返回 true 就会消耗事件。
					// 但右键通常是在 touchUp 或者专门的 rightClick 中触发。
				}

				// 重写 touchDown 确保消耗事件，防止背景响应
				@Override
				public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
					// 调用 super 让它处理长按/点击检测
					super.touchDown(event, x, y, pointer, button);
					event.stop(); // 消费事件！
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
