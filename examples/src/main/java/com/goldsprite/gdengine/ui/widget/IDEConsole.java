package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.List;

public class IDEConsole extends VisTable {

	private boolean expanded = false;
	private final VisLabel logContent;
	private final VisLabel lastLogLabel;
	private final VisScrollPane scrollPane;
	private final VisTextButton toggleBtn;

	private final float COLLAPSED_HEIGHT = 35f;
	private final float EXPANDED_HEIGHT = 250f;

	public IDEConsole() {
		setBackground("window-bg");

		// 1. 顶部栏 (始终显示)
		VisTable header = new VisTable();

		toggleBtn = new VisTextButton("▲");
		toggleBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				toggle();
			}
		});

		lastLogLabel = new VisLabel("Ready");
		lastLogLabel.setColor(Color.GRAY);
		lastLogLabel.setEllipsis(true); // 超长省略

		VisTextButton clearBtn = new VisTextButton("Clear");
		clearBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// 这里和Table.Debug冲突只能全限定名
				com.goldsprite.gdengine.log.Debug.getLogs().clear();
				updateLogText();
			}
		});

		header.add(toggleBtn).width(30).height(30).padRight(5);
		header.add(lastLogLabel).expandX().fillX().padRight(5);
		header.add(clearBtn).height(30);

		add(header).growX().height(COLLAPSED_HEIGHT).row();

		// 2. 内容区 (仅展开显示)
		logContent = new VisLabel();
		logContent.setAlignment(com.badlogic.gdx.utils.Align.topLeft);
		logContent.setWrap(true); // 自动换行

		scrollPane = new VisScrollPane(logContent);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false); // 仅垂直滚动

		add(scrollPane).grow(); // 占据剩余空间

		// 初始状态
		setExpanded(false);
	}

	private void toggle() {
		setExpanded(!expanded);
	}

	public void setExpanded(boolean expand) {
		this.expanded = expand;
		toggleBtn.setText(expand ? "▼" : "▲");

		// 控制内容区显隐
		Cell<?> scrollCell = getCell(scrollPane);
		if (expand) {
			scrollCell.height(EXPANDED_HEIGHT - COLLAPSED_HEIGHT); // 减去头部高度
			scrollPane.setVisible(true);
		} else {
			scrollCell.height(0);
			scrollPane.setVisible(false);
		}

		invalidateHierarchy(); // 通知父容器重新布局 (实现挤压效果)
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		updateLogText();
	}

	private void updateLogText() {
		List<String> logs = com.goldsprite.gdengine.log.Debug.getLogs();
		if (logs.isEmpty()) {
			lastLogLabel.setText("No logs.");
			logContent.setText("");
			return;
		}

		// 更新最后一条
		String last = logs.get(logs.size() - 1);
		// 去除颜色代码简单处理 (可选)
		lastLogLabel.setText(last);

		// 只有展开时才拼接所有 Log (省性能)
		if (expanded) {
			// 简单的防抖或优化：只有数量变了才重置 text
			// 这里为了简单每帧刷新，实际可以用 StringBuilder 优化
			StringBuilder sb = new StringBuilder();
			// 只显示最近 50 条防止卡顿
			int start = Math.max(0, logs.size() - 50);
			for (int i = start; i < logs.size(); i++) {
				sb.append(logs.get(i)).append("\n");
			}
			// 检查内容是否变化，避免重复 setText 导致 layout 计算
			String newText = sb.toString();
			if (!newText.equals(logContent.getText().toString())) {
				logContent.setText(newText);
				// 自动滚动到底部
				scrollPane.layout();
				scrollPane.setScrollY(scrollPane.getMaxY());
			}
		}
	}
}
