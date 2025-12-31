package com.goldsprite.solofight.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;

public class ParallaxBackground {

	// H5 颜色常量
	private final Color COL_BG_MAIN = Color.valueOf("111111");
	private final Color COL_BG_DARK = Color.valueOf("0a0a0a");
	private final Color COL_COL_1 = Color.valueOf("222222");
	private final Color COL_COL_2 = Color.valueOf("301111");
	private final Color COL_COL_3 = Color.valueOf("113030");
	private final Color COL_GRID = new Color(0, 234/255f, 255/255f, 0.3f); // Neon line

	public void draw(NeonBatch batch, OrthographicCamera cam) {
		// 1. 地平线以下的深色背景
		// H5: ctx.fillRect(-1000, this.logicH-100, ...)
		// GDX Y=0 是地面。H5 logicH-100 是地面。
		// 我们在 Y < 0 画深色块
		batch.drawRect(cam.position.x - 2000, -500, 4000, 500, 0, 0, COL_BG_DARK, true);

		// 2. 地平线发光线
		batch.drawLine(-2000, 0, 3000, 0, 2f, COL_GRID);

		// 3. 视差柱子 (Parallax Pillars)
		// H5 Loop: for(let i=-5; i<15; i++)
		// X calculation: i*200 - (cam.x * 0.2)
		// LibGDX 相机原点在中心，cam.position.x 对应 H5 的 cam.x

		float parallaxFactor = 0.2f;
		float camOffset = cam.position.x * parallaxFactor;

		// 动态计算渲染范围，确保不穿帮
		// 这里的 i 对应 H5 的循环索引，我们需要覆盖足够宽的范围
		// 假设 startX = -1000, endX = 2000
		int startI = -15;
		int endI = 25;

		for (int i = startI; i < endI; i++) {
			// 计算柱子高度 (Sin wave)
			// H5: 300 + Math.sin(i*132)*100
			float h = 300 + MathUtils.sin(i * 132f) * 100;

			// 计算 X 坐标 (带视差)
			float x = i * 200 - camOffset;

			// 绘制主柱体 (Y轴向上，地面是0)
			// H5 rect: y = logicH - 100 - h.  So bottom is at logicH-100 (Ground).
			// GDX: draw from y=0 upwards.
			batch.drawRect(x, 0, 120, h, 0, 0, COL_BG_MAIN, true);

			// 绘制装饰小块 (Details)
			Color detailColor = (i % 3 == 0) ? COL_COL_1 : ((i % 2 == 0) ? COL_COL_2 : COL_COL_3);
			// H5: y = logicH-100-h+20. (Top offset 20)
			// GDX: y = h - 20 - 10 (height 10)
			batch.drawRect(x + 10, h - 30, 100, 10, 0, 0, detailColor, true);
		}
	}
}
