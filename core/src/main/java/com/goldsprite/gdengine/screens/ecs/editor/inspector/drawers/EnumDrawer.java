package com.goldsprite.gdengine.screens.ecs.editor.inspector.drawers;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.IPropertyDrawer;
import com.goldsprite.gdengine.ui.input.SmartSelectInput;
import java.lang.reflect.Field;

public class EnumDrawer implements IPropertyDrawer {
	@Override public boolean accept(Class<?> type) { return type.isEnum(); }

	@Override
	public Actor draw(Object target, Field field, boolean isReadOnly) {
		try {
			Object current = field.get(target);
			Object[] constants = field.getType().getEnumConstants();
			Array<Object> items = new Array<>(constants);

			// 泛型擦除，这里用 Object 混过去，SmartSelectInput 内部处理
			SmartSelectInput input = new SmartSelectInput(field.getName(), current, items, v -> {
				try { field.set(target, v); } catch (Exception e) {}
			});
			// [New] Data Binding
			input.bind(() -> {
				try { return field.get(target); } catch (Exception e) { return null; }
			});
			input.setReadOnly(isReadOnly);
			return input;
		} catch (Exception e) { return null; }
	}
}