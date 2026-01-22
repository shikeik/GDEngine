package com.goldsprite.gdengine.screens.ecs.editor.inspector.drawers;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.IPropertyDrawer;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import java.lang.reflect.Field;

public class StringDrawer implements IPropertyDrawer {
	@Override public boolean accept(Class<?> type) { return type == String.class; }

	@Override
	public Actor draw(Object target, Field field, boolean isReadOnly) {
		try {
			String val = (String) field.get(target);
			SmartTextInput input = new SmartTextInput(field.getName(), val, v -> {
				try { 
					field.set(target, v);
					// 特殊逻辑：SpriteComponent 自动刷新 (暂时硬编码在这里，以后可以用注解优化)
					if(target instanceof SpriteComponent && field.getName().equals("assetPath")) {
						((SpriteComponent)target).reloadRegion();
					}
				} catch (Exception e) {}
			});
			// [New] Data Binding
			input.bind(() -> {
				try { return (String) field.get(target); } catch (Exception e) { return ""; }
			});
			input.setReadOnly(isReadOnly);
			return input;
		} catch (Exception e) { return null; }
	}
}