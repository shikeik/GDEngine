package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.kotcrab.vis.ui.VisUI;

public class CommandHistoryUI extends Table {

	private final int MAX_ITEMS = 8;
	// 背景纹理单例
	private static Texture bgRaw, bgMove;

	public CommandHistoryUI() {
		// H5: bottom: 250px, left: 10px, width: 160px
		// Table 布局从下往上
		bottom().left();
		initTextures();
	}

	private void initTextures() {
		if (bgRaw != null) return;

		// Raw: Dark bg (0,0,0,0.8), Left Border #555 3px
		Pixmap p1 = new Pixmap(160, 22, Pixmap.Format.RGBA8888);
		p1.setColor(0, 0, 0, 0.8f);
		p1.fill();
		p1.setColor(0.33f, 0.33f, 0.33f, 1f); // #555
		p1.fillRectangle(0, 0, 3, 22);
		bgRaw = new Texture(p1); p1.dispose();

		// Move: Linear Gradient (Cyan 0.2 -> Transparent), Left Border #00eaff 3px
		// Pixmap 不支持渐变，用纯色模拟低透明度背景
		Pixmap p2 = new Pixmap(160, 22, Pixmap.Format.RGBA8888);
		p2.setColor(0/255f, 234/255f, 255/255f, 0.2f); // Cyan 0.2
		p2.fill(); // 暂用纯色代替渐变
		p2.setColor(0/255f, 234/255f, 255/255f, 1f); // #00eaff
		p2.fillRectangle(0, 0, 3, 22);
		bgMove = new Texture(p2); p2.dispose();
	}

	public void addHistory(String cmdId, String src, String type, String icon) {
		HistoryItem item = new HistoryItem(cmdId, src, type, icon);

		// 插入到最上方 (Table row logic: add() puts at bottom if not configured, but we want new on top?
		// H5 CSS: flex-direction: column-reverse. Newest at bottom visually?
		// No, H5: insertBefore(div, firstChild) -> Newest on TOP.
		// And CSS bottom:250px, column-reverse -> Newest at BOTTOM of container?
		// Wait, H5: `flex-direction: column-reverse` aligns items to bottom, but DOM order matters.
		// Let's stick to standard VisUI Table: add().row() puts at bottom.
		// We want newest at BOTTOM visually to match "stacking up" or TOP?
		// H5 Prototype visual: History grows upwards or stays fixed?
		// "bottom: 250px ... max-height: 200px ... overflow: hidden".
		// Usually fighting games history scrolls DOWN (newest on top) or UP (newest on bottom).
		// Let's implement: Newest on Top.

		// Clean old
		if (getChildren().size >= MAX_ITEMS) {
			getChildren().first().remove(); // Remove oldest
		}

		// Add new item
		add(item).width(160).height(22).padBottom(2).row();

		// Animation: Slide In
		item.getColor().a = 0;
		item.addAction(Actions.parallel(
			Actions.fadeIn(0.1f),
			Actions.moveBy(-10, 0), // Start pos adjustment
			Actions.moveBy(10, 0, 0.1f) // Slide right
		));
	}

	private static class HistoryItem extends Group {
		private Texture bg;

		public HistoryItem(String cmdId, String src, String type, String icon) {
			this.bg = type.equals("move") ? bgMove : bgRaw;

			// Layout (Manual positioning for performance)
			// Icon
			Label lIcon = new Label(icon, new Label.LabelStyle(FontUtils.generate(12), Color.WHITE));
			lIcon.setPosition(8, 4);
			addActor(lIcon);

			// Name
			String name = cmdId.replace("CMD_", ""); // 简易 i18n
			Label lName = new Label(name, new Label.LabelStyle(FontUtils.generate(12), Color.WHITE));
			if (type.equals("move")) lName.setColor(0, 234/255f, 255/255f, 1f);
			else lName.setColor(0.8f, 0.8f, 0.8f, 1f);
			lName.setPosition(28, 4);
			addActor(lName);

			// Source (Right aligned)
			Label lSrc = new Label(src, new Label.LabelStyle(FontUtils.generate(9), Color.GRAY));
			lSrc.setPosition(160 - lSrc.getPrefWidth() - 5, 5);
			addActor(lSrc);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			Color c = getColor();
			batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
			batch.draw(bg, getX(), getY(), getWidth(), getHeight());
			super.draw(batch, parentAlpha);
		}
	}
}
