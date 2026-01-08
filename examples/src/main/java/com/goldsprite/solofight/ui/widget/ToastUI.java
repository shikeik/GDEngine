package com.goldsprite.solofight.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.goldsprite.gdengine.assets.FontUtils;

public class ToastUI extends Label {

	private static ToastUI instance;

	public ToastUI() {
		super("", new Label.LabelStyle(FontUtils.generate(24), Color.valueOf("ffeb3b"))); // Yellow 900 weight equivalent
		instance = this;
		// Shadow effect via Style or manually drawing twice?
		// VisUI Label supports font shadow if configured, let's keep simple for now.
	}

	public static ToastUI inst() { return instance; }

	public void show(String msg) {
		setText(msg);
		pack(); // 重新计算尺寸
		
		if (getStage() != null) {
			// 居中显示在屏幕下方 20% 处
			float stageW = getStage().getWidth();
			float stageH = getStage().getHeight();
			setPosition((stageW - getWidth()) / 2, stageH * 0.2f);
		}
		
		clearActions();
		
		// Reset state
		getColor().a = 0;

		// H5: transition: opacity 0.5s.
		// Logic: Set text -> Opacity 1 (fast) -> Wait -> Opacity 0 (0.5s)
		addAction(Actions.sequence(
			Actions.fadeIn(0.05f),
			Actions.delay(0.5f),
			Actions.fadeOut(0.5f)
		));
	}
}
