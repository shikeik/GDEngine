package com.goldsprite.gameframeworks.ui.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import java.util.function.Consumer;

/**
 * 参数控制组件基类
 * @param <T> 控制的数据类型 (Float, Color, etc.)
 */
public abstract class SmartInput<T> extends VisTable {

	protected T value;
	protected Consumer<T> onChange;

	public SmartInput(String labelText, T initValue, Consumer<T> onChange) {
		this.value = initValue;
		this.onChange = onChange;

		// 统一布局：左侧 Label (固定宽度)
		add(new VisLabel(labelText)).width(80).left(); // 80px 宽确保对齐
	}

	/**
	 * 子类调用此方法将内容控件添加到右侧
	 */
	protected void addContent(Actor actor) {
		add(actor).growX().minWidth(50).row();
	}

	/**
	 * 更新内部数值并触发回调
	 */
	protected void notifyValueChanged(T newValue) {
		this.value = newValue;
		updateUI(); // 同步 UI 显示
		if (onChange != null) {
			onChange.accept(newValue);
		}
	}

	/**
	 * 根据当前 value 更新 UI 显示 (子类实现)
	 */
	protected abstract void updateUI();
}
