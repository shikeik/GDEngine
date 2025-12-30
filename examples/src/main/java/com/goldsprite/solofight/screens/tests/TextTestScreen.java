package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.FloatingTextManager;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class TextTestScreen extends ExampleGScreen {

	private SpriteBatch batch;
	private ShapeRenderer shape;
	private Stage uiStage;

	@Override
	public String getIntroduction() {
		return "漂浮文字系统测试\n[点击屏幕] 生成伤害数字\n[UI按钮] 增加连击";
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Portrait;
	}

	@Override
	public void create() {
		FloatingTextManager.init();

		batch = new SpriteBatch();
		shape = new ShapeRenderer();

		initUI();

		// 点击生成伤害
		getImp().addProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				Vector2 worldPos = screenToWorldCoord(screenX, screenY);
				// 随机伤害 10-1000
				int dmg = MathUtils.random(10, 1000);
				// 20% 概率暴击
				boolean crit = MathUtils.randomBoolean(0.2f);
				FloatingTextManager.addDamage(worldPos.x, worldPos.y, dmg, crit);
				return true;
			}
		});
	}

	private void initUI() {
		uiStage = new Stage(getUIViewport());
		getImp().addProcessor(uiStage);

		Table root = new Table();
		root.setFillParent(true);
		root.bottom().pad(50);
		uiStage.addActor(root);

		VisTextButton btnCombo = new VisTextButton("HIT (Combo++)");
		btnCombo.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				FloatingTextManager.addCombo();
			}
		});
		root.add(btnCombo).width(200).height(60);
	}

	@Override
	public void render0(float delta) {
		// 1. 逻辑更新
		FloatingTextManager.getInstance().update(delta);

		// 2. 绘制参考线 (地平线)
		shape.setProjectionMatrix(getWorldCamera().combined);
		shape.begin(ShapeRenderer.ShapeType.Line);
		shape.setColor(Color.GRAY);
		shape.line(-1000, 0, 1000, 0);
		shape.end();

		// 3. 绘制 World Space 文字 (伤害)
		batch.setProjectionMatrix(getWorldCamera().combined);
		batch.begin();
		FloatingTextManager.getInstance().renderWorld(batch);
		batch.end();

		// 4. 绘制 UI Space 文字 (连击)
		// 注意：这里使用 UI Viewport 的矩阵
		batch.setProjectionMatrix(getUIViewport().getCamera().combined);
		batch.begin();
		FloatingTextManager.getInstance().renderUI(batch, getUIViewport().getWorldWidth(), getUIViewport().getWorldHeight());
		batch.end();

		uiStage.act(delta);
		uiStage.draw();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (batch != null) batch.dispose();
		if (shape != null) shape.dispose();
		if (uiStage != null) uiStage.dispose();
		// FloatingTextManager.getInstance().dispose(); // 全局单例通常不在此处销毁，除非退出游戏
	}
}
