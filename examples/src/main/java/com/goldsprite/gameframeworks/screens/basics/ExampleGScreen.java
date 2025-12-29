package com.goldsprite.gameframeworks.screens.basics;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public abstract class ExampleGScreen extends GScreen {
	private SpriteBatch batch;
	private BitmapFont introFnt;
	private GlyphLayout glyphLayout;

	private final Vector2 introPos = new Vector2();

	// 1. 定义基准尺寸 (540p)
	protected static final float BASE_SHORT = 540f;
	protected static final float BASE_LONG = 960f;
	protected static final float VIEWPORT_SCALE = 1.0f; // 保持原本的缩放系数
	
	public ExampleGScreen() {
		super(); // GScreen 构造

		batch = new SpriteBatch();
		glyphLayout = new GlyphLayout();
		introFnt = FontUtils.generateAutoClarity(30);
		introFnt.getData().setScale(0.5f);
	}

	public String getIntroduction() { return ""; }
	// 2. 强制子类指定方向
	public abstract ScreenManager.Orientation getOrientation();

	// 3. 智能初始化视口 (接管 GScreen 的 initViewport)
	@Override
	protected void initViewport() {
		float w, h;
		if (getOrientation() == ScreenManager.Orientation.Landscape) {
			w = BASE_LONG;
			h = BASE_SHORT;
		} else {
			w = BASE_SHORT;
			h = BASE_LONG;
		}

		// 自动应用缩放系数
		uiViewport = new ExtendViewport(w * VIEWPORT_SCALE, h * VIEWPORT_SCALE);
	}

	// 4. 自动处理转屏逻辑
	@Override
	public void show() {
		super.show(); // GScreen.show 处理输入和 resize
		getScreenManager().setOrientation(getOrientation());
	}

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
