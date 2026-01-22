package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.InspectorBuilder;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

public class DefaultInspectorDrawer implements IInspectorDrawer<Object> {
	@Override
	public boolean accept(Object target) {
		return true; // 接受一切
	}

	@Override
	public void draw(Object target, VisTable container) {
		VisLabel title = new VisLabel("Object: " + target.getClass().getSimpleName());
		title.setColor(Color.CYAN);
		container.add(title).pad(10).row();

		// 直接复用我们强大的反射构建器
		InspectorBuilder.build(container, target);
	}
}
