package com.goldsprite.gdengine.screens.ecs.editor.inspector.drawers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.IPropertyDrawer;
import com.goldsprite.gdengine.ui.input.SmartColorInput;
import java.lang.reflect.Field;

public class ColorDrawer implements IPropertyDrawer {
	@Override
	public boolean accept(Class<?> type) {
		return type == Color.class;
	}

	@Override
	public Actor draw(Object target, Field field, boolean isReadOnly) {
		try {
			Color val = (Color) field.get(target);
			SmartColorInput input = new SmartColorInput(field.getName(), val, v -> {
				try { ((Color)field.get(target)).set(v); } catch (Exception e) {}
			});
			// [New] Data Binding (注意：Color 是对象，需要返回新对象或拷贝，或者 SmartColorInput 内部处理了比较)
			// SmartColorInput 通常依赖 value.equals，这里 Color.equals 比较的是 RGBA 值，所以直接返回对象引用即可，
			// 只要 SmartInput.sync 里做了 equals 检查，且 Color 还是那个对象但内容变了...
			// 等等，如果对象引用没变，Objects.equals 会返回 true，导致不更新！
			// 所以这里我们需要返回一个新的 Color 副本，或者让 SmartColorInput 内部能检测内容变化。
			// 考虑到性能，Color 最好是值传递。但这里 field.get 返回的是引用。
			// 简便起见，每次 bind 返回一个新的 Color 副本以触发更新（会有 GC 开销，但在 Editor 模式下 30FPS 可接受）。
			input.bind(() -> {
				try { return new Color((Color)field.get(target)); } catch (Exception e) { return Color.WHITE; }
			});
			
			input.setReadOnly(isReadOnly);
			return input;
		} catch (Exception e) { return null; }
	}
}