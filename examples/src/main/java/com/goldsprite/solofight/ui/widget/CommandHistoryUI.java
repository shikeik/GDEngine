package com.goldsprite.solofight.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gameframeworks.assets.FontUtils;

/**
 * 指令历史 UI (v3.0 视觉复刻版)
 * 改进：
 * 1. 抛弃静态纹理，使用 Vertex Color 实现真正的 Linear Gradient。
 * 2. 1:1 复刻 H5 CSS 样式 (Raw: Dark, Move: Gradient Cyan)。
 */
public class CommandHistoryUI extends Table {

	private final int MAX_ITEMS = 8;
	private final float ITEM_WIDTH = 180; // 稍微加宽一点以容纳长文本
	private final float ITEM_HEIGHT = 24; // 高度微调

	// 静态资源
	private static TextureRegion whitePixel;
	private static Label.LabelStyle styleIcon, styleNameMove, styleNameRaw, styleSrc;
	private static boolean isInit = false;

	// 颜色常量 (H5 1:1)
	private static final Color COL_BORDER_RAW = Color.valueOf("555555");
	private static final Color COL_BG_RAW = new Color(0, 0, 0, 0.6f); // 实色黑底

	private static final Color COL_BORDER_MOVE = Color.valueOf("00eaff");
	private static final Color COL_BG_MOVE_START = new Color(0/255f, 234/255f, 255/255f, 0.25f); // 渐变起始
	private static final Color COL_BG_MOVE_END = new Color(0/255f, 234/255f, 255/255f, 0f);      // 渐变结束(透明)

	public CommandHistoryUI() {
		top().left();
		initResources();
	}

	private void initResources() {
		if (isInit) return;

		// 1. 生成 1x1 白点用于绘制
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		whitePixel = new TextureRegion(new Texture(p));
		p.dispose();

		// 2. 字体样式
		BitmapFont font12 = FontUtils.generate(14); // 稍微加大字体
		BitmapFont font9 = FontUtils.generate(10);

		styleIcon = new Label.LabelStyle(font12, Color.WHITE);

		// Move: 亮青色文字
		styleNameMove = new Label.LabelStyle(font12, Color.valueOf("00eaff"));
		// Raw: 灰白色文字
		styleNameRaw = new Label.LabelStyle(font12, Color.valueOf("cccccc"));

		styleSrc = new Label.LabelStyle(font9, Color.valueOf("777777"));

		isInit = true;
	}

	public void addHistory(String cmdId, String src, String type, String icon) {
		if (getCells().size >= MAX_ITEMS) {
			Cell c = getCells().first();
			c.getActor().remove();
			getCells().removeIndex(0);
		}

		HistoryItem item = new HistoryItem(cmdId, src, type, icon);
		add(item).width(ITEM_WIDTH).height(ITEM_HEIGHT).padTop(3).row(); // 增加间距

		pack();

		// 动画: Slide In
		item.getColor().a = 0;
		item.addAction(Actions.parallel(
			Actions.fadeIn(0.1f),
			Actions.sequence(
				Actions.moveBy(-15, 0),
				Actions.moveBy(15, 0, 0.15f)
			)
		));
	}

	private class HistoryItem extends Group {
		private final boolean isMove;

		public HistoryItem(String cmdId, String src, String type, String icon) {
			setSize(ITEM_WIDTH, ITEM_HEIGHT);
			this.isMove = type.equals("move");

			// Icon (Bold)
			Label lIcon = new Label(icon, styleIcon);
			lIcon.setPosition(8, 4);
			addActor(lIcon);

			// Name
			String name = cmdId.replace("CMD_", "");
			Label lName = new Label(name, isMove ? styleNameMove : styleNameRaw);
			lName.setPosition(30, 4); // icon后移一点
			addActor(lName);

			// Source
			Label lSrc = new Label(src, styleSrc);
			lSrc.setAlignment(Align.right);
			lSrc.setPosition(180 - lSrc.getPrefWidth() - 6, 5);
			addActor(lSrc);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			Color c = getColor();
			float alpha = c.a * parentAlpha;
			if (alpha <= 0) return;

			float x = getX();
			float y = getY();
			float w = getWidth();
			float h = getHeight();

			// 1. 绘制左侧边框 (3px width)
			Color borderColor = isMove ? COL_BORDER_MOVE : COL_BORDER_RAW;
			batch.setColor(borderColor.r, borderColor.g, borderColor.b, alpha);
			batch.draw(whitePixel, x, y, 3, h);

			// 2. 绘制背景 (Raw: 纯色, Move: 渐变)
			if (isMove) {
				// 绘制水平渐变 (Left -> Right)
				drawGradientRect(batch, x + 3, y, w - 3, h, COL_BG_MOVE_START, COL_BG_MOVE_END, alpha);
			} else {
				// 绘制纯色背景
				batch.setColor(COL_BG_RAW.r, COL_BG_RAW.g, COL_BG_RAW.b, alpha);
				batch.draw(whitePixel, x + 3, y, w - 3, h);
			}

			// 3. 绘制子组件 (Label)
			super.draw(batch, parentAlpha);
		}

		/**
		 * 辅助方法：使用 Batch 绘制水平渐变矩形
		 */
		private void drawGradientRect(Batch batch, float x, float y, float w, float h, Color start, Color end, float alpha) {
			float c1 = start.toFloatBits(); // 预乘 Alpha 需要注意，这里假设 Color 自身 Alpha 已正确设置
			// 但 Batch 的 setColor 会影响 draw，这里我们需要手动构建顶点颜色
			// 简单的做法是利用 Batch.draw(texture, vertices...)

			// 重新计算带 Alpha 的颜色 Bits
			float startBits = tempColor(start, alpha);
			float endBits = tempColor(end, alpha);

			float[] v = tempVerts;
			float u = whitePixel.getU();
			float v2 = whitePixel.getV2(); // V2 is usually top in Y-up? No, standard UV.
			float v1 = whitePixel.getV();
			// LibGDX SpriteBatch vertices: x, y, color, u, v

			// BL (Start Color)
			v[0] = x; v[1] = y; v[2] = startBits; v[3] = u; v[4] = v2;
			// TL (Start Color)
			v[5] = x; v[6] = y+h; v[7] = startBits; v[8] = u; v[9] = v1;
			// TR (End Color)
			v[10] = x+w; v[11] = y+h; v[12] = endBits; v[13] = u; v[14] = v1;
			// BR (End Color)
			v[15] = x+w; v[16] = y; v[17] = endBits; v[18] = u; v[19] = v2;

			batch.draw(whitePixel.getTexture(), v, 0, 20);
		}
	}

	// 缓存数组避免 GC
	private static final float[] tempVerts = new float[20];
	private static final Color tmpC = new Color();

	private static float tempColor(Color c, float alpha) {
		return tmpC.set(c).mul(1, 1, 1, alpha).toFloatBits();
	}
}
