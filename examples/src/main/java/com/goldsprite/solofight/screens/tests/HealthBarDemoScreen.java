package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.ui.H5SkewBar;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.goldsprite.solofight.core.NeonStage;

public class HealthBarDemoScreen extends ExampleGScreen {

	private Stage stage;
	private H5SkewBar p1Bar; // P1: 左倾, 蓝, 左->右
	private H5SkewBar p2Bar; // P2: 右倾, 红, 右->左

	@Override
	public String getIntroduction() {
		return "H5 渐变血条复刻 (v2.0 封装版)";
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Portrait;
	}

	@Override
	public void create() {
		stage = new NeonStage(getUIViewport());
		getImp().addProcessor(stage);

		Table root = new Table();
		root.setFillParent(true);
		stage.addActor(root);

		// --- 配置 P1 样式 (Neon Blue) ---
		H5SkewBar.BarStyle p1Style = new H5SkewBar.BarStyle();
		p1Style.gradientStart = Color.valueOf("00eaff"); // 亮蓝
		p1Style.gradientEnd = Color.valueOf("0088aa");   // 暗蓝
		p1Style.skewDeg = -20f; // 负值向左歪 (\)

		p1Bar = new H5SkewBar(0, 500, p1Style);
		p1Bar.setSize(400, 30); // 显式设置尺寸

		// --- 配置 P2 样式 (Shadow Red) ---
		H5SkewBar.BarStyle p2Style = new H5SkewBar.BarStyle();
		p2Style.gradientStart = Color.valueOf("ff0055"); // 亮红
		p2Style.gradientEnd = Color.valueOf("aa0033");   // 暗红
		p2Style.skewDeg = 20f; // 正值向右歪 (/) 对称

		p2Bar = new H5SkewBar(0, 500, p2Style);
		p2Bar.setSize(400, 30);
		p2Bar.setFillFromRight(true); // P2 从右往左扣血

		// --- 布局 ---
		root.add(new VisLabel("P1 Player (HP: 500)")).padBottom(5).row();
		root.add(p1Bar).width(400).height(30).padBottom(40).row();

		root.add(new VisLabel("P2 Enemy (HP: 500)")).padBottom(5).row();
		root.add(p2Bar).width(400).height(30).padBottom(40).row();

		// --- 控制器 ---
		VisSlider slider = new VisSlider(0, 500, 10, false);
		slider.setValue(500);
		slider.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// 直接设置数值
					p1Bar.setValue(slider.getValue());
					p2Bar.setValue(slider.getValue());
				}
			});

		root.add(new VisLabel("拖动控制血量:")).padBottom(5).row();
		root.add(slider).width(300).padBottom(20).row();

		VisTextButton btnHit = new VisTextButton("重击 (-150)");
		btnHit.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					slider.setValue(slider.getValue() - 150);
				}
			});
		root.add(btnHit);
	}

	@Override
	public void render0(float delta) {
		getUIViewport().apply();
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (stage != null) stage.dispose();
	}
}
