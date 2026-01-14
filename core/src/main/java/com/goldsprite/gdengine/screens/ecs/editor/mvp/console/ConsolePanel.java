package com.goldsprite.gdengine.screens.ecs.editor.mvp.console;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.List;

public class ConsolePanel extends EditorPanel {

	private VisLabel logLabel;
	private VisScrollPane scrollPane;
	private boolean autoScroll = true;

	public ConsolePanel() {
		super("Console");

		// 1. 工具栏 (Clear, AutoScroll)
		VisTable toolbar = new VisTable();
		toolbar.left();

		VisTextButton btnClear = new VisTextButton("Clear");
		btnClear.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				com.goldsprite.gdengine.log.Debug.getLogs().clear();
				updateLog();
			}
		});

		VisTextButton btnTest = new VisTextButton("Test Log");
		btnTest.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) { com.goldsprite.gdengine.log.Debug.log("Test log entry..."); }
		});

		toolbar.add(btnClear).padRight(5);
		toolbar.add(btnTest).padRight(5);

		// 添加到 Content 的顶部
		contentTable.add(toolbar).growX().pad(5).row();

		// 2. 日志内容区
		logLabel = new VisLabel();
		logLabel.setAlignment(Align.topLeft);
		logLabel.setWrap(true); // 允许换行

		scrollPane = new VisScrollPane(logLabel);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false); // 只允许纵向滚动

		VisTable logContainer = new VisTable();
		logContainer.setBackground("list"); //稍微深一点的背景
		logContainer.add(scrollPane).grow().pad(5);

		contentTable.add(logContainer).grow();
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		// 简单轮询刷新 (生产环境可以用事件驱动)
		updateLog();
	}

	private int lastLogSize = 0;

	private void updateLog() {
		List<String> logs = com.goldsprite.gdengine.log.Debug.getLogs();
		if (logs.size() == lastLogSize) return; // 无变化

		lastLogSize = logs.size();

		if (logs.isEmpty()) {
			logLabel.setText("");
			return;
		}

		// 性能优化：只显示最后 100 行，避免 Text 爆炸
		StringBuilder sb = new StringBuilder();
		int start = Math.max(0, logs.size() - 100);
		for (int i = start; i < logs.size(); i++) {
			sb.append(logs.get(i)).append("\n");
		}

		logLabel.setText(sb.toString());

		// 自动滚动到底部
		if (autoScroll) {
			scrollPane.layout();
			scrollPane.setScrollY(scrollPane.getMaxY());
		}
	}
}
