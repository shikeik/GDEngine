package com.flappy;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 7段数码管风格的矢量数字绘制器
 * 不依赖任何图片资源
 */
public class NeonDigitDrawer {

	// 7段定义:
	//   --0--
	//  3     4
	//   --1--
	//  5     6
	//   --2--

	// 数字映射表 (true表示该段亮起)
	private static final boolean[][] DIGITS = {
		{true,  false, true,  true,  true,  true,  true},  // 0
		{false, false, false, false, true,  false, true},  // 1
		{true,  true,  true,  false, true,  true,  false}, // 2
		{true,  true,  true,  false, true,  false, true},  // 3
		{false, true,  false, true,  true,  false, true},  // 4
		{true,  true,  true,  true,  false, false, true},  // 5
		{true,  true,  true,  true,  false, true,  true},  // 6
		{true,  false, false, false, true,  false, true},  // 7
		{true,  true,  true,  true,  true,  true,  true},  // 8
		{true,  true,  true,  true,  true,  false, true}   // 9
	};

	public static void drawNumber(NeonBatch batch, int number, float x, float y, float size, float lineWidth, Color color) {
		String numStr = String.valueOf(number);
		float width = size * 0.6f; // 字宽
		float spacing = size * 0.8f; // 字间距

		// 居中计算起始X
		float startX = x - (numStr.length() * spacing) / 2f + (spacing - width)/2f;

		for (int i = 0; i < numStr.length(); i++) {
			int digit = Character.getNumericValue(numStr.charAt(i));
			if (digit >= 0 && digit <= 9) {
				drawDigit(batch, digit, startX + i * spacing, y, width, size, lineWidth, color);
			}
		}
	}

	private static void drawDigit(NeonBatch batch, int digit, float x, float y, float w, float h, float lw, Color color) {
		boolean[] segs = DIGITS[digit];
		float h2 = h / 2f;

		// 0: Top
		if (segs[0]) batch.drawLine(x, y + h, x + w, y + h, lw, color);
		// 1: Mid
		if (segs[1]) batch.drawLine(x, y + h2, x + w, y + h2, lw, color);
		// 2: Bot
		if (segs[2]) batch.drawLine(x, y, x + w, y, lw, color);
		// 3: TopLeft
		if (segs[3]) batch.drawLine(x, y + h2, x, y + h, lw, color);
		// 4: TopRight
		if (segs[4]) batch.drawLine(x + w, y + h2, x + w, y + h, lw, color);
		// 5: BotLeft
		if (segs[5]) batch.drawLine(x, y, x, y + h2, lw, color);
		// 6: BotRight
		if (segs[6]) batch.drawLine(x + w, y, x + w, y + h2, lw, color);
	}
}
