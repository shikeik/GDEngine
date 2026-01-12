package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import java.util.function.Consumer;

public class SmartBooleanInput extends SmartInput<Boolean> {

    private final VisCheckBox checkBox;

    public SmartBooleanInput(String label, boolean initValue, Consumer<Boolean> onChange) {
        super(label, initValue, onChange);

        checkBox = new VisCheckBox("", initValue);
        checkBox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					boolean newVal = checkBox.isChecked();
					boolean oldVal = value;
					value = newVal;
					if (onChange != null) onChange.accept(newVal);
					notifyCommand(oldVal, newVal);
				}
			});

        addContent(checkBox);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        checkBox.setDisabled(readOnly);
        checkBox.setTouchable(readOnly ? Touchable.disabled : Touchable.enabled);
    }

    @Override
    protected void updateUI() {
        if (checkBox.isChecked() != value) {
            checkBox.setChecked(value);
        }
    }
}
