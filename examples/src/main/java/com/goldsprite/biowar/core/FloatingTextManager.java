package com.goldsprite.biowar.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.goldsprite.gameframeworks.assets.FontUtils;

/**
 * 漂浮文字管理器 (高性能对象池版)
 * 职责：
 * 1. 管理伤害飘字 (World Space)
 * 2. 管理连击提示 (UI Space)
 */
public class FloatingTextManager {

	private static FloatingTextManager instance;
	private BitmapFont font;

	// --- 1. 伤害飘字系统 (World Space) ---
	private final Array<FloatingText> activeTexts = new Array<>();
	private final Pool<FloatingText> textPool = new Pool<FloatingText>() {
		@Override
		protected FloatingText newObject() {
			return new FloatingText();
		}
	};

	// --- 2. 连击系统 (UI Space) ---
	private int comboCount = 0;
	private float comboTimer = 0f;
	private float comboScale = 1.0f; // 缩放动画
	private float comboAlpha = 0f;   // 透明度
	private final Vector2 comboPos = new Vector2(); // UI 坐标
	private GlyphLayout layout = new GlyphLayout(); // 用于计算文字宽度

	// --- 样式配置 ---
	private final Color COLOR_DMG_NORMAL = Color.WHITE;
	private final Color COLOR_DMG_CRIT = Color.valueOf("ffeb3b"); // 金色
	private final Color COLOR_COMBO = Color.valueOf("ffeb3b");

	private FloatingTextManager() {
		try {
			// 加载一个较大的字体，通过 scaling 缩小绘制，保证清晰度
			font = FontUtils.generate(40); 
			font.setUseIntegerPositions(false); // 允许浮点坐标，动画更平滑
		} catch (Exception e) {
			font = new BitmapFont();
		}
	}

	public static void init() {
		if (instance == null) instance = new FloatingTextManager();
	}

	public static FloatingTextManager getInstance() {
		if (instance == null) init();
		return instance;
	}

	/**
	 * 添加伤害飘字 (World Space)
	 * @param x 世界坐标 X
	 * @param y 世界坐标 Y (头顶)
	 * @param damage 伤害值
	 * @param isCrit 是否暴击
	 */
	public static void addDamage(float x, float y, int damage, boolean isCrit) {
		getInstance().spawn(x, y, String.valueOf(damage), isCrit);
	}

	private void spawn(float x, float y, String text, boolean isCrit) {
		FloatingText ft = textPool.obtain();
		ft.init(x, y, text, isCrit ? COLOR_DMG_CRIT : COLOR_DMG_NORMAL, isCrit ? 1.5f : 1.0f);
		activeTexts.add(ft);
	}

	/**
	 * 设置连击数 (UI Space)
	 */
	public static void addCombo() {
		getInstance().comboCount++;
		getInstance().triggerComboAnim();
	}

	public static void resetCombo() {
		getInstance().comboCount = 0;
		getInstance().comboAlpha = 0;
	}

	private void triggerComboAnim() {
		comboTimer = 2.0f; // 2秒后消失
		comboScale = 1.5f; // 瞬间变大
		comboAlpha = 1.0f;
	}

	/**
	 * 核心更新循环 (物理/动画)
	 */
	public void update(float delta) {
		// 1. 更新伤害飘字
		for (int i = activeTexts.size - 1; i >= 0; i--) {
			FloatingText ft = activeTexts.get(i);
			ft.update(delta);
			if (!ft.alive) {
				activeTexts.removeIndex(i);
				textPool.free(ft);
			}
		}

		// 2. 更新连击 UI
		if (comboTimer > 0) {
			comboTimer -= delta;
			if (comboTimer <= 0) {
				comboCount = 0;
				comboAlpha = 0;
			}
		}
		// 弹性缩放恢复
		if (comboScale > 1.0f) {
			comboScale += (1.0f - comboScale) * 10f * delta;
		}
	}

	/**
	 * 渲染世界层 (请配合 World Camera 使用)
	 */
	/**
	 * 渲染世界层 (请配合 World Camera 使用)
	 */
	public void renderWorld(SpriteBatch batch) {
		if (font == null) return;

		// 批量绘制
		for (FloatingText ft : activeTexts) {
			font.setColor(ft.color.r, ft.color.g, ft.color.b, ft.alpha);

			// [修复] 调整缩放比例
			// 原来的 0.025f 导致字只有 1px 大小
			// 现在改为 0.8f，字高约 32 单位 (角色高90)，比较合理
			float baseScale = 0.8f; 
			float finalScale = baseScale * ft.scale;

			font.getData().setScale(finalScale);

			// 居中绘制
			layout.setText(font, ft.text);
			// 注意：BitmapFont 绘制位置通常是左上角或基线，这取决于配置
			// 这里假设 x,y 是中心点，往左偏一半宽度，往上抬一点(避免遮住脚)
			font.draw(batch, ft.text, ft.x - layout.width / 2, ft.y + layout.height);
		}
	}

	/**
	 * 渲染 UI 层 (请配合 UI Camera / Stage 使用)
	 */
	public void renderUI(SpriteBatch batch, float uiWidth, float uiHeight) {
		if (comboCount <= 0 || font == null) return;

		// 1. 设置位置 (右上角)
		float padRight = 50f;
		float padTop = 100f;
		float x = uiWidth - padRight;
		float y = uiHeight - padTop;

		// 2. 设置样式
		font.setColor(COLOR_COMBO.r, COLOR_COMBO.g, COLOR_COMBO.b, comboAlpha);
		// UI 层不需要缩太小，直接用像素大小
		float uiScale = 1.0f * comboScale; 
		font.getData().setScale(uiScale);

		// 3. 绘制
		String text = comboCount + " HITS";
		layout.setText(font, text);

		// 稍微倾斜震动 (模拟 H5 的 rotate)
		// Batch 不支持直接 rotate text，除非改 Matrix，或者用 Cache。
		// 这里简单处理：不做旋转，只做缩放。
		font.draw(batch, text, x - layout.width, y);
	}

	public void dispose() {
		if (font != null) font.dispose();
	}

	// --- 内部粒子类 ---
	public static class FloatingText implements Pool.Poolable {
		float x, y;
		float vx, vy;
		String text;
		Color color;
		float scale;
		float alpha;
		float life;
		boolean alive;

		public void init(float x, float y, String text, Color color, float startScale) {
			this.x = x;
			this.y = y;
			this.text = text;
			this.color = color;
			this.scale = startScale;

			// 物理参数 (复刻 H5: vy = -3 ~ -5, vx = -1 ~ 1)
			// 注意：H5 Canvas Y轴向下，LibGDX Y轴向上。
			// H5: vy = -3 (向上飘). GDX: vy = 3 (向上飘).
			this.vx = (MathUtils.random() - 0.5f) * 40f; 
			this.vy = 80f + MathUtils.random() * 40f; // 初始向上速度

			this.alpha = 1.0f;
			this.life = 1.0f; // 1秒寿命
			this.alive = true;
		}

		public void update(float delta) {
			life -= delta;
			if (life <= 0) {
				alive = false;
				return;
			}

			// 物理模拟
			x += vx * delta;
			y += vy * delta;
			vy -= 200f * delta; // 重力下坠 (gravity)

			// 透明度渐变
			alpha = Math.max(0, life);
		}

		@Override
		public void reset() {
			alive = false;
			text = null;
		}
	}
}
