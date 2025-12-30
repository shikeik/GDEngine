package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.audio.SynthAudio;
import com.goldsprite.solofight.core.audio.SynthAudio.WaveType;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class AudioTestScreen extends ExampleGScreen {

	private Stage stage;

	@Override
	public String getIntroduction() {
		return "SynthAudio 合成器测试\n(无资源文件，数学生成)";
	}
	
	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Portrait;
	}

	@Override
	public void create() {
		// 确保音频引擎已启动
		SynthAudio.init();

		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		Table root = new Table();
		root.setFillParent(true);
		stage.addActor(root);

		// --- 按钮布局 ---

		root.defaults().width(250).height(60).pad(10);

		// 在最上方添加 BGM 开关
		final VisTextButton btnBgm = new VisTextButton("BGM: OFF");
		btnBgm.setColor(Color.RED);

		btnBgm.addListener(new ChangeListener() {
				boolean isPlaying = false;
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					isPlaying = !isPlaying;
					if (isPlaying) {
						SynthAudio.playBGM();
						btnBgm.setText("BGM: ON (Cyberpunk)");
						btnBgm.setColor(Color.GREEN);
					} else {
						SynthAudio.stopBGM();
						btnBgm.setText("BGM: OFF");
						btnBgm.setColor(Color.RED);
					}
				}
			});
		root.add(btnBgm).width(300).padBottom(20).row();

		// 1. 攻击 (Swing)
		VisTextButton btnSwing = new VisTextButton("Attack (Noise)");
		btnSwing.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// H5: noise(0.1, 0.1)
					SynthAudio.playTone(0, WaveType.NOISE, 0.1f, 0.1f);
				}
			});
		root.add(btnSwing).row();

		// 2. 命中 (Hit)
		VisTextButton btnHit = new VisTextButton("Hit (Saw + Noise)");
		btnHit.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// H5: noise(0.2, 0.3) + tone(100, saw, 0.2, 0.2, 50)
					SynthAudio.playTone(0, WaveType.NOISE, 0.2f, 0.3f);
					SynthAudio.playTone(100, WaveType.SAWTOOTH, 0.2f, 0.2f, 50);
				}
			});
		root.add(btnHit).row();

		// 3. 跳跃 (Jump)
		VisTextButton btnJump = new VisTextButton("Jump (Sine Slide)");
		btnJump.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// H5: tone(200, sine, 0.2, 0.1, 400)
					SynthAudio.playTone(200, WaveType.SINE, 0.2f, 0.1f, 400);
				}
			});
		root.add(btnJump).row();

		// 4. 拼刀 (Clash)
		VisTextButton btnClash = new VisTextButton("Clash (Square+Sine)");
		btnClash.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// H5: tone(800, square, 0.1, 0.3) + tone(1200, sine, 0.3, 0.3, 100)
					SynthAudio.playTone(800, WaveType.SQUARE, 0.1f, 0.3f);
					SynthAudio.playTone(1200, WaveType.SINE, 0.3f, 0.3f, 100);
				}
			});
		root.add(btnClash).row();

		// 5. 大招蓄力 (Ult Cast)
		VisTextButton btnUlt = new VisTextButton("Ult Cast (Low Square)");
		btnUlt.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// H5: tone(50, square, 1.0, 0.2) + tone(1000, saw, 1.0, 0.1, 100)
					SynthAudio.playTone(50, WaveType.SQUARE, 1.0f, 0.2f);
					SynthAudio.playTone(1000, WaveType.SAWTOOTH, 1.0f, 0.1f, 100);
				}
			});
		root.add(btnUlt).row();
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
		// 注意：SynthAudio 通常设计为全局单例，只有在游戏彻底退出时才 dispose
		// 这里为了演示安全，暂时不 dispose SynthAudio
	}
}
