package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.goldsprite.solofight.core.BloomRenderer;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class BloomDebugScreen extends ExampleGScreen {

	private BloomRenderer bloom;
	private NeonBatch neonBatch;
	private SpriteBatch batch;
	private ShapeRenderer bgRenderer;

	@Override
	public String getIntroduction() {
		return "Bloom Debug Stage 2:\nManual Composite (Transparent BG)";
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);
		bloom = new BloomRenderer();
		bgRenderer = new ShapeRenderer();
	}

	@Override
	public void render0(float delta) {
		// 1. 画背景 (不发光的部分)
		drawCheckerBoard();

		// 2. 捕获发光物体
		bloom.captureStart(batch);
		{
			batch.setProjectionMatrix(worldCamera.combined);
			batch.begin();
			{
				neonBatch.drawCircle(worldCamera.position.x, worldCamera.position.y, 100, 8, Color.CYAN, 32, false);
			}
			batch.end();
		}
		bloom.captureEnd();
		// 3. 计算模糊
		bloom.process();
		// 4. 合成并上屏 (Overlays on top of background)
		// 这一步会混合：原图 + 光晕，并且处理透明度
		bloom.render(neonBatch.getBatch());
	}

	private void drawCheckerBoard() {
		// [修改] 使用 worldCamera
		bgRenderer.setProjectionMatrix(worldCamera.combined);
		bgRenderer.begin(ShapeRenderer.ShapeType.Filled);
		float cellSize = 60;
		// 覆盖 Viewport 范围
		float w = getUIViewport().getWorldWidth();
		float h = getUIViewport().getWorldHeight();
		int cols = (int) (w / cellSize) + 1;
		int rows = (int) (h / cellSize) + 1;

		float startX = worldCamera.position.x - w/2;
		float startY = worldCamera.position.y - h/2;

		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				bgRenderer.setColor((x + y) % 2 == 0 ? 0.3f : 0.25f, (x + y) % 2 == 0 ? 0.3f : 0.25f, (x + y) % 2 == 0 ? 0.3f : 0.25f, 1);
				bgRenderer.rect(startX + x * cellSize, startY + y * cellSize, cellSize, cellSize);
			}
		}
		bgRenderer.end();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (bloom != null) bloom.resize(width, height);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (bloom != null) bloom.dispose();
		bgRenderer.dispose();
	}
}
