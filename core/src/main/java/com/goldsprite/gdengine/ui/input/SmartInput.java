package com.goldsprite.gdengine.ui.input;

// [核心修复] 补全所有缺失的 Import
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.gdengine.ui.widget.ToastUI; // 现在引用的是 Core 里的 ToastUI

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class SmartInput<T> extends VisTable {

    protected T value;
    protected Consumer<T> onChange;
    protected BiConsumer<T, T> onCommand;

    // [修复] 确保这个成员变量存在
    protected VisLabel labelActor;

    public SmartInput(String labelText, T initValue, Consumer<T> onChange) {
		debugAll();
        this.value = initValue;
        this.onChange = onChange;

        if (labelText != null) {
            labelActor = new VisLabel(labelText);
            float prefW = labelActor.getPrefWidth();
//            float finalW = Math.max(60, prefW);
//            add(labelActor).align(Align.center).width(finalW).left().padRight(5);
            add(labelActor).minWidth(0).left().padRight(5);
        }
    }

    // 设置提示信息
    public void setTooltip(String text) {
        if (labelActor != null) {
            labelActor.addListener(new ClickListener() {
					@Override public void clicked(InputEvent event, float x, float y) {
						if (ToastUI.inst() != null) ToastUI.inst().show("💡 " + text);
					}
				});
        }
    }

    // 标记为 Static (金色)
    public void markAsStatic() {
        if (labelActor != null) {
            labelActor.setColor(Color.GOLD);
            labelActor.setText(labelActor.getText() + " [S]");
        }
    }

    // 标记为 ReadOnly (灰色)
    public void markAsReadOnly() {
        setReadOnly(true);
        if (labelActor != null) {
            labelActor.setColor(Color.GRAY);
        }
    }

    public abstract void setReadOnly(boolean readOnly);

    public void setOnCommand(BiConsumer<T, T> onCommand) {
        this.onCommand = onCommand;
    }

    protected void addContent(Actor actor) {
        add(actor).growX().minWidth(50);
    }

    protected void notifyValueChanged(T newValue) {
        this.value = newValue;
        updateUI();
        if (onChange != null) onChange.accept(newValue);
    }

    protected void notifyCommand(T oldVal, T newVal) {
        if (onCommand != null && oldVal != null && !oldVal.equals(newVal)) {
            onCommand.accept(oldVal, newVal);
        }
    }

    public abstract void updateUI();
}
