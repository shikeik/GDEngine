package com.goldsprite.solofight.ui.window;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.goldsprite.gdengine.assets.FontUtils;

/**
 * 结算面板
 * 样式复刻 H5:
 * - 背景: rgba(0,0,0,0.9)
 * - 标题: 80px, Italic (LibGDX 模拟), Cyan/Red
 * - 按钮: 白底黑字
 */
public class GameOverUI extends Table {

	private final Label lblTitle;
	private final TextButton btnRestart;
	private Runnable onRestart;

	public GameOverUI() {
		setFillParent(true);
		setVisible(false); // 默认隐藏

		// 1. 背景 (纯黑 90% 透明度)
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(0, 0, 0, 0.9f);
		p.fill();
		setBackground(new TextureRegionDrawable(new Texture(p)));
		p.dispose();

		// 2. 标题 (VICTORY / DEFEAT)
		Label.LabelStyle titleStyle = new Label.LabelStyle();
		titleStyle.font = FontUtils.generate(80); // 巨大字体
		lblTitle = new Label("", titleStyle);
		add(lblTitle).padBottom(50).row();

		// 3. 重玩按钮 (白底黑字)
		TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
		// 按钮背景 (白色)
		Pixmap pBtn = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pBtn.setColor(Color.WHITE);
		pBtn.fill();
		TextureRegionDrawable whiteDrawable = new TextureRegionDrawable(new Texture(pBtn));
		pBtn.dispose();

		btnStyle.up = whiteDrawable;
		btnStyle.down = whiteDrawable.tint(Color.LIGHT_GRAY); // 按下变灰
		btnStyle.font = FontUtils.generate(30);
		btnStyle.fontColor = Color.BLACK; // 黑字

		btnRestart = new TextButton("RETRY MISSION", btnStyle);
		// H5 有 skewX(-10deg)，这里通过 Transform 模拟
		btnRestart.setTransform(true);
		btnRestart.setOrigin(com.badlogic.gdx.utils.Align.center);
		// LibGDX UI skew 比较麻烦，这里先仅做缩放动画，视觉上白底黑字已经很像了

		btnRestart.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (onRestart != null) onRestart.run();
				setVisible(false);
			}
		});

		add(btnRestart).width(300).height(60);
	}

	public void show(boolean isWin, Runnable restartCallback) {
		this.onRestart = restartCallback;

		if (isWin) {
			lblTitle.setText("VICTORY");
			lblTitle.setColor(Color.valueOf("00eaff")); // Cyan
		} else {
			lblTitle.setText("DEFEAT");
			lblTitle.setColor(Color.valueOf("ff0055")); // Red
		}

		setVisible(true);
		toFront(); // 确保在最上层
	}
}
