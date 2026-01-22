package com.goldsprite.gdengine.screens.ecs.editor.inspector.drawers;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.IPropertyDrawer;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import java.lang.reflect.Field;

public class Vector2Drawer implements IPropertyDrawer {
	@Override
	public boolean accept(Class<?> type) {
		return type == Vector2.class;
	}

	@Override
	public Actor draw(Object target, Field field, boolean isReadOnly) {
		try {
			Vector2 val = (Vector2) field.get(target);

			// 外层容器
			VisTable container = new VisTable();
			VisLabel nameLbl = new VisLabel(field.getName() + ":");
			container.add(nameLbl).minWidth(20).left().padRight(10);

			// X 分量
			SmartNumInput inputX = new SmartNumInput("X", val.x, 0.1f, v -> {
				val.x = v;
			});
			inputX.bind(() -> val.x); // [New] Data Binding
			inputX.setReadOnly(isReadOnly);

			// Y 分量
			SmartNumInput inputY = new SmartNumInput("Y", val.y, 0.1f, v -> {
				val.y = v;
			});
			inputY.bind(() -> val.y); // [New] Data Binding
			inputY.setReadOnly(isReadOnly);

			// 布局：一行显示
			Table row = new Table();
			row.add().growX();
			row.add(inputX);
			row.add(inputY).width(inputY.getPrefWidth());

			container.add(row).growX();
			return container;

		} catch (Exception e) { return null; }
	}
}
