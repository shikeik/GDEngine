package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.ui.widget.SimpleNumPad;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import java.util.function.Consumer;

public class SmartNumInput extends SmartInput<Float> {

	private final VisTextField textField;
	private final float step;

	private static SimpleNumPad sharedNumPad;

	public SmartNumInput(String label, float initValue, float step, Consumer<Float> onChange) {
		super(label, initValue, onChange);
		this.step = step;

		this.textField = new VisTextField(fmt(initValue));
		this.textField.setAlignment(Align.center);
		this.textField.setDisabled(true);
		this.textField.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) { showNumPad(); }
			});

		addContent(buildControls());
	}

	private VisTable buildControls() {
		VisTable controls = new VisTable();

		VisTextButton dragBtn = new VisTextButton("<>");
		dragBtn.addListener(new InputListener() {
				float lastStageX;
				float startDragValue; 
				ScrollPane parentScroll;

				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					lastStageX = event.getStageX();
					startDragValue = value; 

					parentScroll = findParentScrollPane(SmartNumInput.this);
					if (parentScroll != null) {
						parentScroll.setCancelTouchFocus(false);
					}

					return true;
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					float currentStageX = event.getStageX();
					float dx = currentStageX - lastStageX;
					if (dx == 0) return;

					notifyValueChanged(value + dx * step);
					lastStageX = currentStageX;
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					if (parentScroll != null) {
						parentScroll.setCancelTouchFocus(true);
						parentScroll = null;
					}
					
					// 触发基类的命令回调
					if (Math.abs(value - startDragValue) > 0.0001f) {
						notifyCommand(startDragValue, value);
					}
				}
			});

		controls.add(dragBtn).width(24).padRight(2);
		controls.add(textField).growX();
		return controls;
	}

	private ScrollPane findParentScrollPane(Actor start) {
		Actor current = start;
		while (current != null) {
			if (current instanceof ScrollPane) return (ScrollPane) current;
			current = current.getParent();
		}
		return null;
	}

	@Override
	protected void updateUI() {
		textField.setText(fmt(value));
	}

	private String fmt(float val) {
		if (val == (int) val) return String.valueOf((int) val);
		return String.format("%.2f", val);
	}

	private void showNumPad() {
		if (sharedNumPad == null) sharedNumPad = new SimpleNumPad();
		if (getStage() != null && sharedNumPad.getStage() != getStage()) {
			getStage().addActor(sharedNumPad);
		}
		sharedNumPad.toFront();
		
		float oldVal = value;
		sharedNumPad.show(textField, (res) -> {
			try {
				if (res.isEmpty() || res.equals("-")) return;
				float newVal = Float.parseFloat(res);
				
				notifyValueChanged(newVal);
				
				// 触发命令回调
				if (Math.abs(newVal - oldVal) > 0.0001f) {
					notifyCommand(oldVal, newVal);
				}
			} catch (Exception ignored) {}
		});
	}
}