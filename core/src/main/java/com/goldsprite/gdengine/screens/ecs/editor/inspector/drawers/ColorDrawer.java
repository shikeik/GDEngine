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
            input.setReadOnly(isReadOnly);
            return input;
        } catch (Exception e) { return null; }
    }
}