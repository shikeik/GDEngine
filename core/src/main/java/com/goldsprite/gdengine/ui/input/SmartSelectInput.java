package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import java.util.function.Consumer;

public class SmartSelectInput<T> extends SmartInput<T> {
	
	private final VisSelectBox<T> selectBox;
	
	public SmartSelectInput(String label, T initValue, Array<T> items, Consumer<T> onChange) {
		super(label, initValue, onChange);
		
		selectBox = new VisSelectBox<>();
		selectBox.setItems(items);
		selectBox.setSelected(initValue);
		
		selectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				T newVal = selectBox.getSelected();
				T oldVal = value;
				
				// 更新内部值
				value = newVal;
				if (onChange != null) onChange.accept(newVal);
				
				// 触发 Command
				if (oldVal != newVal) {
					notifyCommand(oldVal, newVal);
				}
			}
		});
		
		addContent(selectBox);
	}

	@Override
	protected void updateUI() {
		if (selectBox.getSelected() != value) {
			selectBox.setSelected(value);
		}
	}
}