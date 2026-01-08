package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;

import java.util.function.Consumer;

public class SmartColorInput extends SmartInput<Color> {

	private final VisTextButton previewBtn;
	private final TextureRegionDrawable drawable;

	// 全局共享 Picker
	private static ColorPicker sharedPicker;

	public SmartColorInput(String label, Color initValue, Consumer<Color> onChange) {
		super(label, new Color(initValue), onChange);

		// 创建一个纯白 Texture 作为 drawable 基础，通过 setColor 染色
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		drawable = new TextureRegionDrawable(new Texture(p));
		p.dispose();

		previewBtn = new VisTextButton(value.toString());

		// 必须创建一个样式的副本 (Copy Style)，只作用于当前按钮
		TextButton.TextButtonStyle originalStyle = previewBtn.getStyle();
		TextButton.TextButtonStyle uniqueStyle = new TextButton.TextButtonStyle(originalStyle);

		// 修改副本的背景图
		uniqueStyle.up = drawable;
		uniqueStyle.down = drawable; 

		// 应用新样式
		previewBtn.setStyle(uniqueStyle);

		previewBtn.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					openPicker();
				}
			});

		updateUI();
		
		// 颜色选择器不需要太宽
		addContent(previewBtn);
	}

	private void openPicker() {
		if (sharedPicker == null) {
			sharedPicker = new ColorPicker();
		}

		final Color restoreColor = new Color(value);

		sharedPicker.setListener(new ColorPickerAdapter() {
				@Override
				public void changed(Color newColor) {
					notifyValueChanged(new Color(newColor));
				}

				@Override
				public void canceled(Color oldColor) {
					notifyValueChanged(restoreColor);
				}

				@Override
				public void finished(Color newColor) {
					Color finalColor = new Color(newColor);
					notifyValueChanged(finalColor);
					
					// 触发基类的命令回调
					notifyCommand(restoreColor, finalColor);
				}
			});
			
		sharedPicker.setColor(value);

		if (getStage() != null) {
			if (sharedPicker.getStage() != getStage()) {
				getStage().addActor(sharedPicker.fadeIn());
			} else {
				sharedPicker.fadeIn();
			}
		}
	}

	@Override
	protected void updateUI() {
		// 更新按钮颜色 (Actor Color 会与 drawable 颜色相乘)
		previewBtn.setColor(value);
		previewBtn.setText(value.toString().toUpperCase());
	}
}