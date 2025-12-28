package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.biowar.core.ui.H5SkewBar;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class HealthBarDemoScreen extends ExampleGScreen {

	private Stage stage;
	private H5SkewBar p1Bar;
	private H5SkewBar p2Bar;

	@Override
	public String getIntroduction() {
		return "H5 渐变血条复刻演示";
	}

	@Override
	public void create() {
		stage = new Stage(getViewport());
		getImp().addProcessor(stage);

		Table root = new Table();
		root.setFillParent(true);
		stage.addActor(root);

		// --- 1. 创建 P1 血条 (Neon Blue) ---
		Color c1Left = Color.valueOf("00eaff");
		Color c1Right = Color.valueOf("0088aa");
		p1Bar = new H5SkewBar(400, 30, c1Left, c1Right);

		// --- 2. 创建 P2 血条 (Red, 反向) ---
		// 注意：H5里 P2 是反向的 (flex-direction: row-reverse)
		// 在 LibGDX 里我们可以通过负的 width 或者 scaleX(-1) 来实现，
		// 或者简单的修改 H5SkewBar 支持 skew 方向，这里简单演示正向的
		Color c2Left = Color.valueOf("ff0055");
		Color c2Right = Color.valueOf("aa0033");
		p2Bar = new H5SkewBar(400, 30, c2Left, c2Right);

		// 布局
		root.add(new VisLabel("P1 Player (Neon)")).padBottom(10).row();
		root.add(p1Bar).padBottom(40).row();

		root.add(new VisLabel("P2 Enemy (Shadow)")).padBottom(10).row();
		root.add(p2Bar).padBottom(40).row();

		// 控制器
		VisSlider slider = new VisSlider(0, 1, 0.01f, false);
		slider.setValue(1.0f);
		slider.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					p1Bar.setPercent(slider.getValue());
					p2Bar.setPercent(slider.getValue());
				}
			});

		root.add(new VisLabel("拖动滑块模拟扣血:")).padBottom(5).row();
		root.add(slider).width(300).padBottom(20).row();

		VisTextButton btnHit = new VisTextButton("模拟受击 (-10%)");
		btnHit.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					slider.setValue(slider.getValue() - 0.1f);
				}
			});
		root.add(btnHit);
	}

	@Override
	public void render0(float delta) {
		getViewport().apply();
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (stage != null) stage.dispose();
	}
}
