package com.goldsprite.gdengine.neonbatch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/**
 * 通用矢量图形绘制基类
 * 职责：
 * 1. 管理 1x1 白点纹理
 * 2. 提供通用的描边 (Stroke) 和 填充 (Fill) 算法
 * 3. 所有的几何计算（Miter Join）都在这里完成
 */
public class BaseShapeBatch extends SpriteBatch{
	protected final TextureRegion blankRegion;
	// 缓存 UV，避免每次绘制调用方法
	protected final float whiteU, whiteV;

	// 顶点缓存 (避免 new)，最大支持 1024 个顶点的路径
	// [x, y, x, y, ...]
	private final float[] tempVerts = new float[2048];
	// Miter 计算用的临时变量
	private final Vector2 tmpV1 = new Vector2();
	private final Vector2 tmpV2 = new Vector2();
	private final Vector2 miterTmp = new Vector2();

	public BaseShapeBatch() {
		// 生成 1x1 纯白纹理
		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.fill();
		Texture texture = new Texture(pixmap);
		texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		blankRegion = new TextureRegion(texture);
		whiteU = blankRegion.getU();
		whiteV = blankRegion.getV();
		pixmap.dispose();
	}


	// [新增] 暴露白纹理供高级自定义绘制使用
	public TextureRegion getBlankRegion() {
		return blankRegion;
	}

	// --- 核心算法 1: 填充 (Fill) ---

	/**
	 * 填充凸多边形 (Convex Polygon) 或星形
	 * 使用 Triangle Fan (三角扇) 原理：以第一个点为中心，连接其余点
	 * @param vertices 顶点数组 [x0, y0, x1, y1, ...]
	 * @param count 顶点数量 (不是数组长度)
	 */
	protected void pathFill(float[] vertices, int count, Color color) {
		if (count < 3) return;

		// 三角扇中心点 (默认取第一个点)
		float cX = vertices[0];
		float cY = vertices[1];

		// 预先打包颜色 float bits
		float colorBits = color.toFloatBits();

		for (int i = 1; i < count - 1; i++) {
			float p1x = vertices[i * 2];
			float p1y = vertices[i * 2 + 1];
			float p2x = vertices[(i + 1) * 2];
			float p2y = vertices[(i + 1) * 2 + 1];

			// 绘制三角形: Center -> P1 -> P2
			drawSolidTriangle(cX, cY, p1x, p1y, p2x, p2y, colorBits);
		}
	}

	/**
	 * 填充扇形/圆弧 (Sector)
	 * 特殊逻辑：需要显式指定中心点
	 */
	protected void sectorFill(float centerX, float centerY, float[] vertices, int count, Color color) {
		if (count < 2) return;
		float colorBits = color.toFloatBits();
		for (int i = 0; i < count - 1; i++) {
			drawSolidTriangle(centerX, centerY,
				vertices[i * 2], vertices[i * 2 + 1],
				vertices[(i + 1) * 2], vertices[(i + 1) * 2 + 1],
				colorBits);
		}
	}

	// --- 核心算法 2: 描边 (Stroke) ---

	/**
	 * 通用路径描边 (Miter Join)
	 * @param vertices 顶点数组 [x0, y0, x1, y1, ...]
	 * @param count 顶点数量
	 * @param width 线宽
	 * @param isClosed 是否闭合 (首尾相连)
	 */
	protected void pathStroke(float[] vertices, int count, float width, boolean isClosed, Color color) {
		if (count < 2) return;

		float halfWidth = width * 0.5f;
		float colorBits = color.toFloatBits();

		// 缓存上一个断面的内外点
		float prevOuterX = 0, prevOuterY = 0, prevInnerX = 0, prevInnerY = 0;
		// 记录第一个断面的点，用于闭合
		float firstOuterX = 0, firstOuterY = 0, firstInnerX = 0, firstInnerY = 0;

		// 遍历所有顶点
		for (int i = 0; i < count; i++) {
			float curX = vertices[i * 2];
			float curY = vertices[i * 2 + 1];

			// 1. 确定 Prev 和 Next 坐标
			float prevX, prevY, nextX, nextY;

			if (i == 0) {
				if (isClosed) {
					// 闭合：Prev 是最后一个点
					prevX = vertices[(count - 1) * 2];
					prevY = vertices[(count - 1) * 2 + 1];
				} else {
					// 不闭合：Prev 是根据 Curr->Next 的反向延长
					// P_prev = P_curr - (P_next - P_curr)
					nextX = vertices[2]; nextY = vertices[3];
					prevX = curX - (nextX - curX);
					prevY = curY - (nextY - curY);
				}
			} else {
				prevX = vertices[(i - 1) * 2];
				prevY = vertices[(i - 1) * 2 + 1];
			}

			if (i == count - 1) {
				if (isClosed) {
					// 闭合：Next 是第一个点
					nextX = vertices[0];
					nextY = vertices[1];
				} else {
					// 不闭合：Next 是 Prev->Curr 的延长
					nextX = curX + (curX - prevX);
					nextY = curY + (curY - prevY);
				}
			} else {
				// 中间点
				int nextIdx = (i + 1) % count; // 防越界
				nextX = vertices[nextIdx * 2];
				nextY = vertices[nextIdx * 2 + 1];
			}

			// 2. 计算 Miter Offset
			computeMiterOffset(prevX, prevY, curX, curY, nextX, nextY, halfWidth);

			// 当前断面的内外点
			float currOuterX = curX + miterTmp.x;
			float currOuterY = curY + miterTmp.y;
			float currInnerX = curX - miterTmp.x;
			float currInnerY = curY - miterTmp.y;

			// 保存第一个断面以便闭合
			if (i == 0) {
				firstOuterX = currOuterX; firstOuterY = currOuterY;
				firstInnerX = currInnerX; firstInnerY = currInnerY;
			}

			// 3. 缝合 (Strip Generation)
			if (i > 0) {
				drawQuadStrip(prevOuterX, prevOuterY, currOuterX, currOuterY, currInnerX, currInnerY, prevInnerX, prevInnerY, colorBits);
			}

			// 更新缓存
			prevOuterX = currOuterX; prevOuterY = currOuterY;
			prevInnerX = currInnerX; prevInnerY = currInnerY;
		}

		// 4. 处理最后的闭合缝隙 (Last -> First)
		if (isClosed) {
			drawQuadStrip(prevOuterX, prevOuterY, firstOuterX, firstOuterY, firstInnerX, firstInnerY, prevInnerX, prevInnerY, colorBits);
		}
	}

	// --- 辅助计算 ---

	private void computeMiterOffset(float xPrev, float yPrev, float xCurr, float yCurr, float xNext, float yNext, float halfWidth) {
		// V1: Prev -> Curr
		tmpV1.set(xCurr - xPrev, yCurr - yPrev).nor();
		// V2: Curr -> Next
		tmpV2.set(xNext - xCurr, yNext - yCurr).nor();

		// Tangent (切线) = V1 + V2
		float tx = tmpV1.x + tmpV2.x;
		float ty = tmpV1.y + tmpV2.y;
		float tLen = (float) Math.sqrt(tx * tx + ty * ty);

		// 法线 (Normal) = (-ty, tx)
		float nx, ny;

		if (tLen < 0.0001f) {
			// 共线或尖角折返，退化处理：取 V1 的法线
			nx = -tmpV1.y;
			ny = tmpV1.x;
			miterTmp.set(nx, ny).scl(halfWidth);
			return;
		}

		tx /= tLen; ty /= tLen;
		nx = -ty; ny = tx;

		// Miter Length = halfWidth / dot(normal, v1_normal)
		float dot = nx * (-tmpV1.y) + ny * tmpV1.x;

		// 防止除零和过度尖角
		float miterLen = halfWidth / Math.max(0.1f, dot);
		float limit = halfWidth * 3f; // 限制尖角长度
		if (miterLen > limit) miterLen = limit;

		miterTmp.set(nx, ny).scl(miterLen);
	}

	// --- 底层 Vertex 提交 (画四边形和三角形) ---

	// 画两个三角形组成的四边形 (Strip)
	private void drawQuadStrip(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float colorBits) {
		// Tri 1: 1-2-3 (TopRight Triangle)
		drawSolidTriangle(x1, y1, x2, y2, x3, y3, colorBits);
		// Tri 2: 1-3-4 (BottomLeft Triangle)
		drawSolidTriangle(x1, y1, x3, y3, x4, y4, colorBits);
	}

	// 直接向 Batch 提交三角形顶点
	private void drawSolidTriangle(float x1, float y1, float x2, float y2, float x3, float y3, float colorBits) {
		// 构造4个顶点 (SpriteBatch 需要 4 个顶点画两个三角，这里我们通过退化第4个点画一个三角)
		float[] vArr = tempVerts; // 复用数组

		// V1
		vArr[0] = x1; vArr[1] = y1; vArr[2] = colorBits; vArr[3] = whiteU; vArr[4] = whiteV;
		// V2
		vArr[5] = x2; vArr[6] = y2; vArr[7] = colorBits; vArr[8] = whiteU; vArr[9] = whiteV;
		// V3
		vArr[10] = x3; vArr[11] = y3; vArr[12] = colorBits; vArr[13] = whiteU; vArr[14] = whiteV;
		// V4 (Repeat V3) -> Degenerate
		vArr[15] = x3; vArr[16] = y3; vArr[17] = colorBits; vArr[18] = whiteU; vArr[19] = whiteV;

		draw(blankRegion.getTexture(), vArr, 0, 20);
	}

	// [新增] 支持独立颜色的三角形带绘制
	protected void drawTriangleStrip(float[] vertices, float[] colors, int count) {
		if (count < 3) return;
		for (int i = 0; i < count - 2; i++) {
			// 构成三角形: (i, i+1, i+2)
			int idx1 = i, idx2 = i + 1, idx3 = i + 2;
			// 简单的交替绕序处理（虽然 Batch 默认不开剔除，为了规范还是写一下）
			if (i % 2 != 0) { int tmp = idx2; idx2 = idx3; idx3 = tmp; } // Swap to maintain winding

			drawSolidTriangle(
				vertices[idx1*2], vertices[idx1*2+1], colors[idx1],
				vertices[idx2*2], vertices[idx2*2+1], colors[idx2],
				vertices[idx3*2], vertices[idx3*2+1], colors[idx3]
			);
		}
	}

	private void drawSolidTriangle(float x1, float y1, float c1, float x2, float y2, float c2, float x3, float y3, float c3) {
		float[] vArr = tempVerts;
		vArr[0] = x1; vArr[1] = y1; vArr[2] = c1; vArr[3] = whiteU; vArr[4] = whiteV;
		vArr[5] = x2; vArr[6] = y2; vArr[7] = c2; vArr[8] = whiteU; vArr[9] = whiteV;
		vArr[10]= x3; vArr[11]= y3; vArr[12]= c3; vArr[13]= whiteU; vArr[14]= whiteV;
		vArr[15]= x3; vArr[16]= y3; vArr[17]= c3; vArr[18]= whiteU; vArr[19]= whiteV;
		draw(blankRegion.getTexture(), vArr, 0, 20);
	}
}

