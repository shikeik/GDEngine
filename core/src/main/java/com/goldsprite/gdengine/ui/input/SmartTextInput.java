package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
                // 实时更新值但不触发 Command (防止每打一个字都记录一次撤销)
                // 实际的 Command 触发应该在失去焦点或回车时，但目前 VisTextField 默认没有 FocusListener 
                // 简单起见，我们这里暂不触发 Command，或者可以考虑加个 FocusListener
                value = textField.getText();
                if (onChange != null) onChange.accept(value);
            }
        });

        // 监听回车键或失去焦点来触发 Command (简化版: 仅监听 FocusLost 模拟)
        textField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.FocusListener() {
            String startValue = initValue;

            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (focused) {
                    startValue = value;
                } else {
                    if (startValue != null && !startValue.equals(value)) {
                        notifyCommand(startValue, value);
                    }
                }
            }
        });
        
        addContent(textField);
    }

    @Override
    protected void updateUI() {
        if (!textField.getText().equals(value)) {
            int cursorPosition = textField.getCursorPosition();
            textField.setText(value);
            textField.setCursorPosition(Math.min(cursorPosition, value.length()));
        }
    }
}