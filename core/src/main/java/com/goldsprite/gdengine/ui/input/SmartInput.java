package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

public abstract class SmartInput<T> extends VisTable {
    
    protected T value;
    protected Consumer<T> onChange;
    protected BiConsumer<T, T> onCommand;

    public SmartInput(String labelText, T initValue, Consumer<T> onChange) {
        this.value = initValue;
        this.onChange = onChange;

        if (labelText != null) {
            VisLabel label = new VisLabel(labelText);
            // [自适应宽度] 优先使用文本测量宽度，但保留最小对齐宽度(80)
            // 如果文本很长，让输入条适应剩余空间；如果文本短，保持对齐
            float prefW = label.getPrefWidth();
            float finalW = Math.max(80, prefW);
            
            add(label).width(finalW).left().padRight(5);
        }
    }

    public void setOnCommand(BiConsumer<T, T> onCommand) {
        this.onCommand = onCommand;
    }

    /**
     * 子类必须调用此方法来添加主要内容控件
     * 自动处理 growX 等布局属性
     */
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
    
    protected abstract void updateUI();
}