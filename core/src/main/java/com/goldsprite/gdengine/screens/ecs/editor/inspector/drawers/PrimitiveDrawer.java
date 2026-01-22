package com.goldsprite.gdengine.screens.ecs.editor.inspector.drawers;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.IPropertyDrawer;
import com.goldsprite.gdengine.ui.input.SmartBooleanInput;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import java.lang.reflect.Field;

public class PrimitiveDrawer implements IPropertyDrawer {
	@Override
	public boolean accept(Class<?> type) {
		return type == int.class || type == Integer.class ||
				type == float.class || type == Float.class ||
				type == boolean.class || type == Boolean.class;
	}

	@Override
	public Actor draw(Object target, Field field, boolean isReadOnly) {
		String name = field.getName();
		Class<?> type = field.getType();
		try {
			Object val = field.get(target);

			if (type == boolean.class || type == Boolean.class) {
				SmartBooleanInput input = new SmartBooleanInput(name, (Boolean) val, v -> {
					try { field.set(target, v); } catch (Exception e) {}
				});
				// [New] Data Binding
				input.bind(() -> {
					try { return (Boolean) field.get(target); } catch (Exception e) { return false; }
				});
				input.setReadOnly(isReadOnly);
				return input;
			} 
			else if (type == int.class || type == Integer.class) {
				SmartNumInput input = new SmartNumInput(name, ((Number)val).floatValue(), 1f, v -> {
					try { field.set(target, v.intValue()); } catch (Exception e) {}
				});
				// [New] Data Binding
				input.bind(() -> {
					try { return ((Number) field.get(target)).floatValue(); } catch (Exception e) { return 0f; }
				});
				input.setReadOnly(isReadOnly);
				return input;
			} 
			else { // float
				SmartNumInput input = new SmartNumInput(name, ((Number)val).floatValue(), 0.1f, v -> {
					try { field.set(target, v.floatValue()); } catch (Exception e) {}
				});
				// [New] Data Binding
				input.bind(() -> {
					try { return ((Number) field.get(target)).floatValue(); } catch (Exception e) { return 0f; }
				});
				input.setReadOnly(isReadOnly);
				return input;
			}
		} catch (Exception e) { return null; }
	}
}