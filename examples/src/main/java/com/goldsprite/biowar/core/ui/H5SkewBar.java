package com.goldsprite.biowar.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.goldsprite.biowar.core.NeonBatch;

/**
 * 复刻 H5 样式的倾斜渐变血条
 */
public class H5SkewBar extends Actor {

	private NeonBatch neonRenderer;

	// 样式属性 (默认复刻 H5: border #555, bg rgba(30,30,30,0.8))
	private Color borderColor = Color.valueOf("555555");
	private Color bgColor = new Color(0.12f, 0.12f, 0.12f, 0.8f); 
	private Color colorLeft, colorRight;

	private float borderThickness = 2f;
	private float skewAngleDeg = -20f;

	private float percent = 1.0f; // 0.0 ~ 1.0

	// 伤害缓冲动画用
	private float damagePercent = 1.0f;
	private Color dmgColor = Color.WHITE;

	public H5SkewBar(float width, float height, Color cLeft, Color cRight) {
		setSize(width, height);
		this.colorLeft = cLeft;
		this.colorRight = cRight;
	}

	public void setPercent(float p) {
		this.percent = MathUtils.clamp(p, 0, 1);
		// 如果是回血，瞬间跟上；如果是扣血，damagePercent 滞后
		if (this.percent > this.damagePercent) {
			this.damagePercent = this.percent;
		}
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		// 简单的插值动画模拟 CSS transition (缓慢扣血效果)
		if (damagePercent > percent) {
			damagePercent -= delta * 0.3f; // 速度可调
			if (damagePercent < percent) damagePercent = percent;
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		// 1. 初始化/获取 NeonBatch 包装器
		if (neonRenderer == null || neonRenderer.getBatch() != batch) {
			if (batch instanceof SpriteBatch) {
				neonRenderer = new NeonBatch((SpriteBatch) batch);
			} else {
				return; 
			}
		}

		float x = getX();
		float y = getY();
		float w = getWidth();
		float h = getHeight();

		// 计算 skew 偏移量: offset = h * tan(angle)
		// CSS skewX(-20deg) 让顶部向左偏移
		float skewOffset = h * MathUtils.tanDeg(skewAngleDeg);

		// 2. 绘制边框 (描边)
		// 顶点顺序: BL, BR, TR, TL
		float[] borderVerts = new float[] {
			x, y,                      
			x + w, y,                  
			x + w + skewOffset, y + h, 
			x + skewOffset, y + h      
		};
		// 注意：drawPolygon 在 NeonBatch 中不闭合，我们需要手动指定闭合参数或者传入闭合点
		// NeonBatch.drawPolygon 内部调用的 pathStroke(..., true, ...) 是闭合的，所以没问题
		neonRenderer.drawPolygon(borderVerts, 4, borderThickness, borderColor, false);

		// 为了不让内容盖住边框，稍微收缩一点内容区域 (padding)
		float pad = borderThickness;
		// 简单收缩算法 (完美算法需要向量计算，这里近似处理)
		float contentX = x + pad; 
		float contentY = y + pad;
		float contentW = w - pad * 2;
		float contentH = h - pad * 2;
		float contentSkew = contentH * MathUtils.tanDeg(skewAngleDeg);

		// 3. 绘制背景底色 (半透明黑)
		neonRenderer.drawSkewGradientRect(contentX, contentY, contentW, contentH, contentSkew, bgColor, bgColor);

		// 4. 绘制白色缓冲层 (Damage Layer)
		if (damagePercent > 0) {
			float dmgW = contentW * damagePercent;
			neonRenderer.drawSkewGradientRect(contentX, contentY, dmgW, contentH, contentSkew, dmgColor, dmgColor);
		}

		// 5. 绘制实际血条 (渐变)
		if (percent > 0) {
			float barW = contentW * percent;
			// 颜色透明度跟随 Actor
			Color c1 = tmpColor(colorLeft, parentAlpha);
			Color c2 = tmpColor(colorRight, parentAlpha);

			neonRenderer.drawSkewGradientRect(contentX, contentY, barW, contentH, contentSkew, c1, c2);
		}
	}

	// 辅助：应用 Alpha
	private Color tmpColor(Color c, float alpha) {
		Color out = new Color(c);
		out.a *= alpha;
		return out;
	}
}
