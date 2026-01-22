package com.goldsprite.gdengine.screens.ecs.editor.inspector;

import com.badlogic.gdx.scenes.scene2d.Actor;
import java.lang.reflect.Field;

public interface IPropertyDrawer {
	/** 检查是否支持该字段类型 */
	boolean accept(Class<?> type);
	
	/** 绘制 UI 组件 */
	Actor draw(Object target, Field field, boolean isReadOnly);
}