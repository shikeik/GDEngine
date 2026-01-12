package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.kotcrab.vis.ui.widget.VisTextField;
import java.util.function.Consumer;

public class SmartTextInput extends SmartInput<String> {

    private final VisTextField textField;

    public SmartTextInput(String label, String initValue, Consumer<String> onChange) {
        super(label, initValue, onChange);

        textField = new VisTextField(initValue);
        textField.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					value = textField.getText();
					if (onChange != null) onChange.accept(value);
				}
			});

        // 监听焦点丢失触发 Command (可选)
        textField.addListener(new FocusListener() {
				String startValue = initValue;
				@Override
				public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
					if (focused) startValue = value;
					else notifyCommand(startValue, value);
				}
			});

        addContent(textField);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        textField.setDisabled(readOnly);
        // 如果只读，透明度降低一点
        textField.setColor(1, 1, 1, readOnly ? 0.5f : 1f);
    }

    @Override public void updateUI() {
        if (!textField.getText().equals(value)) {
            int cursorPosition = textField.getCursorPosition();
            textField.setText(value);
            textField.setCursorPosition(Math.min(cursorPosition, value.length()));
        }
    }
}
