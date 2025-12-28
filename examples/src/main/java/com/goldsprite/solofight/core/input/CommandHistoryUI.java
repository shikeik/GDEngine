package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gameframeworks.assets.FontUtils;

public class CommandHistoryUI extends Table {

	private final int MAX_ITEMS = 8;
	private final float ITEM_WIDTH = 160;
	private final float ITEM_HEIGHT = 22;

	// 静态资源 (只加载一次)
	private static Texture bgRaw, bgMove;
	private static Label.LabelStyle styleIcon, styleNameMove, styleNameRaw, styleSrc;
	private static boolean isInit = false;

	public CommandHistoryUI() {
		top().left(); // 顶部对齐，新消息往下堆叠
		initResources();
	}

	private void initResources() {
		if (isInit) return;

		// 1. Raw 背景: 深黑 + 灰色边框
		Pixmap p1 = new Pixmap((int)ITEM_WIDTH, (int)ITEM_HEIGHT, Pixmap.Format.RGBA8888);
		p1.setColor(0, 0, 0, 0.8f);
		p1.fill();
		p1.setColor(0.33f, 0.33f, 0.33f, 1f);
		p1.fillRectangle(0, 0, 3, (int)ITEM_HEIGHT);
		bgRaw = new Texture(p1);
		p1.dispose();

		// 2. Move 背景: 淡青 + 青色边框
		Pixmap p2 = new Pixmap((int)ITEM_WIDTH, (int)ITEM_HEIGHT, Pixmap.Format.RGBA8888);
		p2.setColor(0/255f, 234/255f, 255/255f, 0.15f);
		p2.fill();
		p2.setColor(0/255f, 234/255f, 255/255f, 1f);
		p2.fillRectangle(0, 0, 3, (int)ITEM_HEIGHT);
		bgMove = new Texture(p2);
		p2.dispose();

		// 3. 字体样式 (复用)
		BitmapFont font12 = FontUtils.generate(12);
		BitmapFont font9 = FontUtils.generate(9);

		styleIcon = new Label.LabelStyle(font12, Color.WHITE);
		styleNameMove = new Label.LabelStyle(font12, new Color(0, 234/255f, 255/255f, 1f));
		styleNameRaw = new Label.LabelStyle(font12, new Color(0.8f, 0.8f, 0.8f, 1f));
		styleSrc = new Label.LabelStyle(font9, Color.GRAY);

		isInit = true;
	}

	public void addHistory(String cmdId, String src, String type, String icon) {
		// [修复逻辑] 如果满了，移除最上面的项 (Oldest)，而不是最下面的 (Newest)
		if (getCells().size >= MAX_ITEMS) {
			Cell c = getCells().first(); // 获取顶部 Cell
			c.getActor().remove();       // 移除 Actor
			getCells().removeIndex(0);   // 移除 Cell 定义
		}

		// 创建并添加到最底部 (Newest)
		HistoryItem item = new HistoryItem(cmdId, src, type, icon);
		add(item).width(ITEM_WIDTH).height(ITEM_HEIGHT).padTop(2).row();

		// [关键] 强制刷新布局，确保位置正确
		pack();

		// 动画: 从左侧滑入
		item.getColor().a = 0;
		item.addAction(Actions.parallel(
			Actions.fadeIn(0.1f),
			Actions.sequence(
				Actions.moveBy(-10, 0),
				Actions.moveBy(10, 0, 0.1f)
			)
		));
	}

	private static class HistoryItem extends Group {
		private final Texture bg;

		public HistoryItem(String cmdId, String src, String type, String icon) {
			// [关键] 显式设置 Group 尺寸，否则 draw 里的 getWidth() 可能为 0
			setSize(160, 22);

			boolean isMove = type.equals("move");
			this.bg = isMove ? bgMove : bgRaw;

			// Icon
			Label lIcon = new Label(icon, styleIcon);
			lIcon.setPosition(8, 4);
			addActor(lIcon);

			// Name
			String name = cmdId.replace("CMD_", "");
			Label lName = new Label(name, isMove ? styleNameMove : styleNameRaw);
			lName.setPosition(28, 4);
			addActor(lName);

			// Source
			Label lSrc = new Label(src, styleSrc);
			lSrc.setAlignment(Align.right);
			lSrc.setPosition(160 - lSrc.getPrefWidth() - 5, 5);
			addActor(lSrc);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			Color c = getColor();
			batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

			// 绘制背景
			if (bg != null) {
				batch.draw(bg, getX(), getY(), getWidth(), getHeight());
			}

			super.draw(batch, parentAlpha);
		}
	}
}
