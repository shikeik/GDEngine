package com.goldsprite.gdengine.screens.ecs.editor.inspector;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.goldsprite.gdengine.core.annotations.*;
import com.goldsprite.gdengine.ui.input.SmartInput;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class InspectorBuilder {

    public static void build(VisTable container, Object target) {
        if (target == null) return;

        Class<?> clazz = target.getClass();
        Field[] fields = clazz.getDeclaredFields(); // 获取所有字段，包括 private

        for (Field field : fields) {
            // 1. 黑名单过滤
            if (field.isAnnotationPresent(Hide.class)) continue;
            
            // 2. 权限检查
            int mod = field.getModifiers();
            boolean isPublic = Modifier.isPublic(mod);
            boolean show = isPublic || field.isAnnotationPresent(Show.class);
            
            if (!show) continue; // 既不是 public 也没加 @Show

            // 允许访问 private
            field.setAccessible(true);

            // 3. 装饰器：Header
            if (field.isAnnotationPresent(Header.class)) {
                String title = field.getAnnotation(Header.class).value();
                VisLabel headerLbl = new VisLabel(title);
                headerLbl.setColor(Color.CYAN);
                container.add(headerLbl).colspan(2).left().padTop(10).padBottom(2).row();
            }

            // 4. 状态判定
            boolean isFinal = Modifier.isFinal(mod);
            boolean isStatic = Modifier.isStatic(mod);
            boolean isReadOnly = isFinal || field.isAnnotationPresent(ReadOnly.class);

            // 5. 查找绘制器并绘制
            IPropertyDrawer drawer = DrawerRegistry.getDrawer(field.getType());
            Actor widget = drawer.draw(target, field, isReadOnly);

            if (widget != null) {
                // 应用 SmartInput 的额外视觉效果
                if (widget instanceof SmartInput) {
                    SmartInput<?> input = (SmartInput<?>) widget;
                    if (isStatic) input.markAsStatic();
                    if (isReadOnly) input.markAsReadOnly();
                    
                    if (field.isAnnotationPresent(Tooltip.class)) {
                        input.setTooltip(field.getAnnotation(Tooltip.class).value());
                    }
                }
                
                container.add(widget).growX().colspan(2).row();
            }
        }
    }
}