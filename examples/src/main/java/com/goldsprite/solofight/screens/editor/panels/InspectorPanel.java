package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.solofight.screens.editor.EditorContext;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.goldsprite.gdengine.ui.input.SmartBooleanInput;
import com.badlogic.gdx.math.Vector2;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class InspectorPanel extends BaseEditorPanel {
	private VisTable contentTable;

	public InspectorPanel(Skin skin, EditorContext context) {
		super("Inspector", skin, context);
	}

	@Override
	protected void initContent() {
		contentTable = new VisTable();
		contentTable.top().left();
		getContent().add(new VisScrollPane(contentTable)).grow();

		context.onSelectionChanged.add(this::buildInspector);
	}

	private void buildInspector(GObject selection) {
		contentTable.clearChildren();
		if (selection == null) {
			contentTable.add(new VisLabel("No Selection")).pad(10);
			return;
		}

		// Header (Name, Tag, Layer)
		contentTable.add(new VisLabel("GObject")).colspan(2).left().pad(5).row();
		contentTable.add(new SmartTextInput("Name", selection.getName(), selection::setName)).growX().colspan(2).row();
		
		// Components
		for (List<Component> comps : selection.getComponentsMap().values()) {
			for (Component c : comps) {
				buildComponentInspector(c);
			}
		}
	}

	private void buildComponentInspector(Component c) {
		contentTable.add(new VisLabel(c.getClass().getSimpleName())).colspan(2).left().padTop(10).padBottom(5).row();
		
		// Reflection
		Field[] fields = c.getClass().getFields(); // Public fields
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;
			
			try {
				Class<?> type = f.getType();
				String name = f.getName();
				Object value = f.get(c);
				
				if (type == float.class || type == Float.class) {
					contentTable.add(new SmartNumInput(name, (float)value, 0.1f, v -> {
						try { f.setFloat(c, v); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == int.class || type == Integer.class) {
					contentTable.add(new SmartNumInput(name, (float)(int)value, 1f, v -> {
						try { f.setInt(c, v.intValue()); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == boolean.class || type == Boolean.class) {
					contentTable.add(new SmartBooleanInput(name, (boolean)value, v -> {
						try { f.setBoolean(c, v); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == String.class) {
					contentTable.add(new SmartTextInput(name, (String)value, v -> {
						try { f.set(c, v); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == Vector2.class) {
					Vector2 v = (Vector2) value;
					contentTable.add(new VisLabel(name)).left();
					Table row = new Table();
					row.add(new SmartNumInput("X", v.x, 0.1f, val -> v.x = val)).growX().padRight(5);
					row.add(new SmartNumInput("Y", v.y, 0.1f, val -> v.y = val)).growX();
					contentTable.add(row).growX().row();
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
