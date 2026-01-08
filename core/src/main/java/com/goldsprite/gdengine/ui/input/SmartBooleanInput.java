package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import java.util.function.Consumer;

public class SmartBooleanInput extends SmartInput<Boolean> {
    
    private final VisCheckBox checkBox;
    
    public SmartBooleanInput(String label, boolean initValue, Consumer<Boolean> onChange) {
        super(label, initValue, onChange);
        
        // Checkbox 自带 label，但为了统一对齐，我们使用 SmartInput 的 label 机制
        // 所以这里的 text 为空
        checkBox = new VisCheckBox("", initValue);
        
        checkBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean newVal = checkBox.isChecked();
                boolean oldVal = value;
                
                value = newVal;
                if (onChange != null) onChange.accept(newVal);
                
                if (oldVal != newVal) {
                    notifyCommand(oldVal, newVal);
                }
            }
        });
        
        addContent(checkBox);
    }

    @Override
    protected void updateUI() {
        if (checkBox.isChecked() != value) {
            checkBox.setChecked(value);
        }
    }
}