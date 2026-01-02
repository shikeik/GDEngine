package com.goldsprite.gameframeworks.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane; // 必须引用这个基类
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gameframeworks.ui.widget.SimpleNumPad;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import java.util.function.Consumer;

public class SmartNumInput extends VisTable {

    private final VisTextField textField;
    private float currentValue;
    private final float step;
    private final Consumer<Float> onChange;

    private static SimpleNumPad sharedNumPad;

    // 带 Label 构造
    public SmartNumInput(String label, float initValue, float step, Consumer<Float> onChange) {
        this(initValue, step, onChange);
        // 重新布局：把 Label 插到最前面
        clearChildren();
        add(new VisLabel(label)).width(80).left();
        add(buildControls()).growX().minWidth(50);
    }

    // 无 Label 构造
    public SmartNumInput(float initValue, float step, Consumer<Float> onChange) {
        this.currentValue = initValue;
        this.step = step;
        this.onChange = onChange;

        this.textField = new VisTextField(fmt(initValue));
        this.textField.setAlignment(Align.center);
        this.textField.setDisabled(true);
        this.textField.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) { showNumPad(); }
			});

        add(buildControls()).growX().minWidth(50);
    }

    private VisTable buildControls() {
        VisTable controls = new VisTable();

        VisTextButton dragBtn = new VisTextButton("<>");
        dragBtn.addListener(new InputListener() {
				float lastStageX;
				ScrollPane parentScroll; // 缓存找到的父级滚动窗

				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					lastStageX = event.getStageX();

					// 【还原旧逻辑】向上查找 ScrollPane 并锁定
					// 就像以前调用 screen.getInspectorScroll().setCancelTouchFocus(false) 一样
					parentScroll = findParentScrollPane(SmartNumInput.this);
					if (parentScroll != null) {
						parentScroll.setCancelTouchFocus(false); // 禁止它抢走事件
					}

					return true;
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					float currentStageX = event.getStageX();
					float dx = currentStageX - lastStageX;
					if (dx == 0) return;

					updateValue(currentValue + dx * step);
					lastStageX = currentStageX;
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					// 【还原旧逻辑】松手后恢复，让它能正常滚动
					if (parentScroll != null) {
						parentScroll.setCancelTouchFocus(true);
						parentScroll = null;
					}
				}
			});

        controls.add(dragBtn).width(24).padRight(2);
        controls.add(textField).growX();
        return controls;
    }

    /**
     * [手写辅助函数] 递归向上查找父级 ScrollPane
     * 无论这个组件被塞在多深的 Table 里，都能找到最外层的滚动窗
     */
    private ScrollPane findParentScrollPane(Actor start) {
        Actor current = start;
        while (current != null) {
            if (current instanceof ScrollPane) {
                return (ScrollPane) current;
            }
            current = current.getParent();
        }
        return null;
    }

    private void updateValue(float newVal) {
        this.currentValue = newVal;
        textField.setText(fmt(newVal));
        if (onChange != null) onChange.accept(newVal);
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
        sharedNumPad.show(textField, (res) -> {
            try {
                if (res.isEmpty() || res.equals("-")) return;
                updateValue(Float.parseFloat(res));
            } catch (Exception ignored) {}
        });
    }
}
