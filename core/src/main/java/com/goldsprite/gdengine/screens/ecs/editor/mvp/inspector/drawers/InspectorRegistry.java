package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers.*;

public class InspectorRegistry {
	private static final Array<IInspectorDrawer> drawers = new Array<>();

	// 默认兜底绘制器 (反射)
	private static final IInspectorDrawer defaultDrawer = new DefaultInspectorDrawer();

	static {
		// 注册顺序：具体的在前，通用的在后
		drawers.add(new GObjectInspectorDrawer());
		drawers.add(new FileInspectorDrawer());
	}

	@SuppressWarnings("unchecked")
	public static IInspectorDrawer<Object> getDrawer(Object target) {
		if (target == null) return null;
		for (IInspectorDrawer d : drawers) {
			if (d.accept(target)) return d;
		}
		return defaultDrawer;
	}
}
