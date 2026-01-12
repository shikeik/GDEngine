package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
    private final VisTextButton dragBtn; // 提升为成员变量
    private final float step;

    private static SimpleNumPad sharedNumPad;

    public SmartNumInput(String label, float initValue, float step, Consumer<Float> onChange) {
        super(label, initValue, onChange);
        this.step = step;

        this.textField = new VisTextField(fmt(initValue));
        this.textField.setAlignment(Align.center);
        this.textField.setDisabled(true); // 默认只能通过点击弹出键盘修改

        this.textField.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					if (!textField.isDisabled()) showNumPad();
				}
			});

        // 初始化拖拽按钮
        dragBtn = new VisTextButton("<>");
        setupDragListener();

        VisTable controls = new VisTable();
        controls.add(dragBtn).width(24).padRight(2);
        controls.add(textField).growX();

        addContent(controls);
    }

    private void setupDragListener() {
        dragBtn.addListener(new InputListener() {
				float lastStageX;
				float startDragValue;
				ScrollPane parentScroll;

				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					if (dragBtn.isDisabled()) return false;
					lastStageX = event.getStageX();
					startDragValue = value;
					parentScroll = findParentScrollPane(SmartNumInput.this);
					if (parentScroll != null) parentScroll.setCancelTouchFocus(false);
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
					notifyCommand(startDragValue, value);
				}
			});
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        dragBtn.setDisabled(readOnly);
        dragBtn.setTouchable(readOnly ? Touchable.disabled : Touchable.enabled);
        // textField 本来就是 disabled (靠点击弹窗)，这里不需要额外设，但可以改颜色
        textField.setColor(1, 1, 1, readOnly ? 0.5f : 1f);
    }

    private ScrollPane findParentScrollPane(Actor start) {
        Actor current = start;
        while (current != null) {
            if (current instanceof ScrollPane) return (ScrollPane) current;
            current = current.getParent();
        }
        return null;
    }

    @Override public void updateUI() {
        textField.setText(fmt(value));
    }

    private String fmt(float val) {
        if (val == (int) val) return String.valueOf((int) val);
        return String.format("%.2f", val);
    }

    private void showNumPad() {
        if (dragBtn.isDisabled()) return; // 只读时不弹窗
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
                notifyCommand(oldVal, newVal);
            } catch (Exception ignored) {}
        });
    }
}
