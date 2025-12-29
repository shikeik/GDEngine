package com.goldsprite.gameframeworks.screens.basics;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

public abstract class ExampleGScreen extends GScreen {
	private SpriteBatch batch;
	private BitmapFont introFnt;
	private GlyphLayout glyphLayout;

	private final Vector2 introPos = new Vector2();

	public ExampleGScreen() {
		super(); // GScreen 构造

		batch = new SpriteBatch();
		glyphLayout = new GlyphLayout();
		introFnt = FontUtils.generateAutoClarity(30);
		introFnt.getData().setScale(0.5f);
	}

	public abstract String getIntroduction();

	public Vector2 getIntroductionPos() {
		// 适配 Viewport 坐标
		return introPos.set(20, getViewSize().y - 60);
	}

	@Override
	public void render(float delta) {
		super.render(delta);
		drawIntros();
	}

	protected void drawIntros() {
		// 确保 Batch 使用相机的投影矩阵
		batch.setProjectionMatrix(getUICamera().combined);
		batch.begin();
		glyphLayout.setText(introFnt, getIntroduction());
		introFnt.draw(batch, getIntroduction(), getIntroductionPos().x, getIntroductionPos().y);
		batch.end();
	}

	@Override
	public void dispose() {
		super.dispose();
		if(introFnt != null) introFnt.dispose();
		if(batch != null) batch.dispose();
	}
}
