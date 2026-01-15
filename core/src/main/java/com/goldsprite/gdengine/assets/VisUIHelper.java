package com.goldsprite.gdengine.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.log.Debug;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

public class VisUIHelper {
	public static BitmapFont cnFont;
	public static BitmapFont cnFontSmall;

	/**
	 * 加载 VisUI 并注入中文字体
	 */
	public static void loadWithChineseFont() {
		if (VisUI.isLoaded()) return;

		// 1. 加载默认皮肤 (这一步会加载默认的英文像素字体)
		VisUI.load();

		try {
//			Debug.log("Injecting Chinese Font into VisUI...");

			// 2. 获取 VisUI 的皮肤
			Skin skin = VisUI.getSkin();

			// [新增] 合并 "shade" 皮肤资源
			try {
				// 加载 Atlas
				TextureAtlas shadeAtlas = new TextureAtlas(Gdx.files.internal("ui_skins/shade/skin/uiskin.atlas"));
				skin.addRegions(shadeAtlas);
				// 加载 JSON (合并样式)
				skin.load(Gdx.files.internal("ui_skins/shade/skin/uiskin.json"));
				Debug.log("已合并 Shade 皮肤资源.");
			} catch (Exception e) {
				Debug.log("合并 Shade 皮肤失败: " + e.getMessage());
			}

			// 3. 生成支持中文的字体 (使用 FontUtils 现有的逻辑)
			// 大小设为 24，清晰度较高
			cnFont = FontUtils.generateAutoClarity(40);
			cnFont.getData().setScale(cnFont.getData().scaleX * 0.7f);
			BitmapFont smFont = FontUtils.generate(40);
			smFont.getData().setScale(smFont.getData().scaleX * 0.56f);
			cnFontSmall = smFont;

			// 4. 暴力替换常用组件样式中的字体引用

			// Label (VisLabel也是它)
			skin.get(Label.LabelStyle.class).font = cnFont;

			// [新增] 兼容 GlobalAssets 的 "small" 样式
			if (skin.has("small", Label.LabelStyle.class)) {
				skin.get("small", Label.LabelStyle.class).font = smFont;
			} else {
				Label.LabelStyle smallLabelStyle = new Label.LabelStyle();
				smallLabelStyle.font = smFont;
				smallLabelStyle.fontColor = Color.WHITE;
				skin.add("small", smallLabelStyle);
			}

			skin.get(MenuItem.MenuItemStyle.class).font = smFont;
			skin.get(Window.WindowStyle.class).titleFont = smFont;
			skin.get(SelectBox.SelectBoxStyle.class).font = smFont;
			skin.get(List.ListStyle.class).font = smFont;

			//VisScrollPane.ScrollPaneStyle 没有font

			// Button / TextButton
			skin.get(TextButton.TextButtonStyle.class).font = cnFont;
			skin.get(VisTextButton.VisTextButtonStyle.class).font = cnFont;
			skin.get("toggle", VisTextButton.VisTextButtonStyle.class).font = smFont;

			// TextField (输入框)
			skin.get(VisTextField.VisTextFieldStyle.class).font = cnFont;

			// [新增] 移植 GlobalAssets 的其他样式定义
			addGlobalAssetsStyles(skin, cnFont, smFont);

			Debug.log("VisUI 中文字体调整成功.");

			fixHandleSize();
		} catch (Exception e) {
			e.printStackTrace();
			Debug.log("VisUI 中文字体调整失败: " + e.getMessage());
		}
	}

	private static void addGlobalAssetsStyles(Skin skin, BitmapFont font, BitmapFont smallFont) {
		// Logger Style
		Label.LabelStyle loggerLabelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
		loggerLabelStyle.font = font; // Or a specific logger font if needed
		skin.add("loggerLabelStyle", loggerLabelStyle);

		// Title
		if (!skin.has("title", Label.LabelStyle.class)) {
			Label.LabelStyle titleLabelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
			titleLabelStyle.font = font;
			skin.add("title", titleLabelStyle);
		} else {
			skin.get("title", Label.LabelStyle.class).font = font;
		}

		// Subtitle
		if (!skin.has("subtitle", Label.LabelStyle.class)) {
			Label.LabelStyle subtitleLabelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
			subtitleLabelStyle.font = smallFont;
			skin.add("subtitle", subtitleLabelStyle);
		} else {
			skin.get("subtitle", Label.LabelStyle.class).font = smallFont;
		}

		// No Background TextField
		TextField.TextFieldStyle textFieldStyle = skin.get(TextField.TextFieldStyle.class);
		TextField.TextFieldStyle noBackgroundTextFieldStyle = new TextField.TextFieldStyle(textFieldStyle);
		noBackgroundTextFieldStyle.background = null;
		skin.add("nobackground", noBackgroundTextFieldStyle);

		// No Background ScrollPane
		ScrollPane.ScrollPaneStyle defaultScrollStyle = skin.get(ScrollPane.ScrollPaneStyle.class);
		ScrollPane.ScrollPaneStyle noBackgroundScrollStyle = new ScrollPane.ScrollPaneStyle(defaultScrollStyle);
		noBackgroundScrollStyle.background = null;
		skin.add("nobackground", noBackgroundScrollStyle);
	}

	private static void fixHandleSize() {
		Skin skin = VisUI.getSkin();
		float size = 20f;
		float splitBarThickness = PlatformImpl.isAndroidUser() ? 15f : 8f;
		float g = 0.4f;
		TextureRegionDrawable cDrawable = ColorTextureUtils.createColorDrawable(new Color(g, g, g, 0.4f));

		SplitPane.SplitPaneStyle splitPaneStyle = skin.get("default-vertical", SplitPane.SplitPaneStyle.class);
		// 优先使用 shade 的 handle，如果不想用 shade 的可以覆盖
		// GlobalAssets 是覆盖了 handle 的
		splitPaneStyle.handle = cDrawable;
		splitPaneStyle.handle.setMinHeight(splitBarThickness);

		SplitPane.SplitPaneStyle splitPaneStyleHori = skin.get("default-horizontal", SplitPane.SplitPaneStyle.class);
		splitPaneStyleHori.handle = cDrawable;
		splitPaneStyleHori.handle.setMinWidth(splitBarThickness);

		VisSplitPane.VisSplitPaneStyle visSplitPaneStyle = skin.get("default-vertical", VisSplitPane.VisSplitPaneStyle.class);
		visSplitPaneStyle.handle = cDrawable;
		visSplitPaneStyle.handle.setMinWidth(splitBarThickness);

		VisSplitPane.VisSplitPaneStyle visSplitPaneStyleHori = skin.get("default-horizontal", VisSplitPane.VisSplitPaneStyle.class);
		visSplitPaneStyleHori.handle = cDrawable;
		visSplitPaneStyleHori.handle.setMinHeight(splitBarThickness);

		// VisTree fixes
		skin.get("default", VisTree.TreeStyle.class).plus.setMinWidth(size);
		skin.get("default", VisTree.TreeStyle.class).plus.setMinHeight(size);
		skin.get("default", VisTree.TreeStyle.class).minus.setMinWidth(size);
		skin.get("default", VisTree.TreeStyle.class).minus.setMinHeight(size);


		// VisCheckBox fixes
		size = 30;
		VisCheckBox.VisCheckBoxStyle checkBoxStyle = skin.get(VisCheckBox.VisCheckBoxStyle.class);
		checkBoxStyle.checkBackground.setMinWidth(size);
		checkBoxStyle.checkBackground.setMinHeight(size);
		checkBoxStyle.checkBackgroundDown.setMinWidth(size);
		checkBoxStyle.checkBackgroundDown.setMinHeight(size);
		checkBoxStyle.checkBackgroundOver.setMinWidth(size);
		checkBoxStyle.checkBackgroundOver.setMinHeight(size);
//		checkBoxStyle.checkedDown.setMinWidth(size);
//		checkBoxStyle.checkedDown.setMinHeight(size);
//		checkBoxStyle.checkedOver.setMinWidth(size);
//		checkBoxStyle.checkedOver.setMinHeight(size);
	}
}
