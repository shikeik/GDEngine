package com.goldsprite.solofight.core;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisLabel;

public class VisUIHelper {
	public static BitmapFont cnFont;

	/**
	 * 加载 VisUI 并注入中文字体
	 */
	public static void loadWithChineseFont() {
		if (VisUI.isLoaded()) return;

		// 1. 加载默认皮肤 (这一步会加载默认的英文像素字体)
		VisUI.load();

		try {
			DebugUI.log("Injecting Chinese Font into VisUI...");

			// 2. 生成支持中文的字体 (使用 FontUtils 现有的逻辑)
			// 大小设为 24，清晰度较高
			cnFont = FontUtils.generateAutoClarity(40);int k2;
			cnFont.getData().setScale(cnFont.getData().scaleX * 0.7f);
			BitmapFont smFont = FontUtils.generate(40);
			smFont.getData().setScale(smFont.getData().scaleX * 0.56f);

			// 3. 获取 VisUI 的皮肤
			Skin skin = VisUI.getSkin();

			// 4. 暴力替换常用组件样式中的字体引用

			// Label (最重要)
			skin.get(Label.LabelStyle.class).font = cnFont;
			skin.get("small", Label.LabelStyle.class).font = smFont;

			//VisScrollPane.ScrollPaneStyle 没有font

			// Button / TextButton
			skin.get(TextButton.TextButtonStyle.class).font = cnFont;
			skin.get(VisTextButton.VisTextButtonStyle.class).font = cnFont;

			// TextField (输入框)
			skin.get(VisTextField.VisTextFieldStyle.class).font = cnFont;

			DebugUI.log("VisUI 中文字体调整成功.");

		} catch (Exception e) {
			e.printStackTrace();
			DebugUI.log("VisUI Font Inject Failed: " + e.getMessage());
		}
	}
}
