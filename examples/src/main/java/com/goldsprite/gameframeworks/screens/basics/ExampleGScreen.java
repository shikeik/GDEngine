package com.goldsprite.gameframeworks.screens.basics;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

public abstract class ExampleGScreen extends GScreen {
	private static SpriteBatch batch;
	private static BitmapFont introFnt;
	private static GlyphLayout glyphLayout;
	
	private final Vector2 introPos = new Vector2();

	public ExampleGScreen() {
		super(); // GScreen 构造
		if(batch==null){
			batch = new SpriteBatch();
			// 防止字体加载失败
			BitmapFont temp;
			try { temp = FontUtils.generateAutoClarity(16); }
			catch (Exception e) { temp = new BitmapFont(); }
			introFnt = temp;
			glyphLayout = new GlyphLayout();
		}
	}

	public abstract String getIntroduction();

	public Vector2 getIntroductionPos() {
		// 适配 Viewport 坐标
		return introPos.set(getViewSize().x - 20 - glyphLayout.width, getViewSize().y - 60);
	}

	@Override
	public void render(float delta) {
		super.render(delta);
		drawIntros();
	}

	protected void drawIntros() {
		// 确保 Batch 使用相机的投影矩阵
		batch.setProjectionMatrix(getCamera().combined);
		batch.begin();
		glyphLayout.setText(introFnt, getIntroduction());
		introFnt.draw(batch, getIntroduction(), getIntroductionPos().x, getIntroductionPos().y);
		batch.end();
	}

	@Override
	public void dispose() {
		super.dispose();
		if(introFnt != null) introFnt.dispose();
	}
}
