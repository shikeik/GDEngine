package com.goldsprite.gdengine.screens.ecs.editor.inspector.drawers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.IPropertyDrawer;
import java.lang.reflect.Field;

public class DefaultObjectDrawer implements IPropertyDrawer {
	@Override public boolean accept(Class<?> type) { return true; } // 接受一切

	@Override
	public Actor draw(Object target, Field field, boolean isReadOnly) {
		try {
			Object val = field.get(target);
			VisTable table = new VisTable();
			table.left();
			table.add(new VisLabel(field.getName())).padRight(5);

			String text = (val == null) ? "null" : val.toString();
			VisLabel valLabel = new VisLabel(text);
			valLabel.setColor(Color.GRAY); // 默认灰色，表示只读/不可编辑
			table.add(valLabel).growX();

			// [修复] 支持多行文本
			valLabel.setWrap(true);
			// 这里 growX 会让 label 占据剩余宽度，配合 wrap 实现换行
			table.add(valLabel).growX().left();

			return table;
		} catch (Exception e) { return null; }
	}
}
