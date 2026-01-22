package com.goldsprite.gdengine.screens.ecs.editor.inspector;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.drawers.*;

public class DrawerRegistry {
	private static final Array<IPropertyDrawer> drawers = new Array<>();
	private static final IPropertyDrawer defaultDrawer = new DefaultObjectDrawer();

	static {
		// 注册顺序很重要，特殊的在前面
		drawers.add(new PrimitiveDrawer());
		drawers.add(new StringDrawer());
		drawers.add(new EnumDrawer());
		drawers.add(new Vector2Drawer());
		drawers.add(new ColorDrawer());
		// ... 后续增加更多
	}

	public static IPropertyDrawer getDrawer(Class<?> type) {
		for (IPropertyDrawer d : drawers) {
			if (d.accept(type)) return d;
		}
		return defaultDrawer; // 兜底
	}
}