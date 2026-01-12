package com.goldsprite.solofight.screens.tests.iconeditor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.adapter.GObjectAdapter;
import com.goldsprite.gdengine.ui.input.SmartBooleanInput;
import com.goldsprite.gdengine.ui.input.SmartInput;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.EditorUIProvider;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class Inspector {
	private VisTable container;
	private final EditorUIProvider screen;
	private final SceneManager sceneManager;
	private final CommandManager commandManager;

	public Inspector(EditorUIProvider screen, SceneManager sm, CommandManager cm) {
		this.screen = screen;
		this.sceneManager = sm;
		this.commandManager = cm;
	}

	// [新增] 实时刷新逻辑
	public void update() {
		if (container == null) return;

		recursiveUpdate(container);
	}

	private void recursiveUpdate(Table table) {
		// 遍历所有子控件，如果是 SmartInput，则尝试刷新
		// 这样 WorldInfo 字符串变了，界面也会跟着变
		for (Actor child : table.getChildren()) {
			if (child instanceof SmartInput<?> smartInput) {
				// SmartInput.updateUI 是 protected，需要通过反射或者改访问权限
				// 建议：直接在 SmartInput 里加一个 public void refresh() 方法调用 updateUI()
				// 或者在这里强转调用 (如果我们把 updateUI 改成 public)

				// 为了不改动 SmartInput 太多，我们可以简单地这样：
				// 暂时假设 SmartInput 能够自我刷新，或者我们去改 SmartInput
				// [方案] 我们去把 SmartInput.updateUI 改成 public
				smartInput.updateUI();
			} else if (child instanceof Table table1) {
				// 递归查找 (针对 Vector2Drawer 那种嵌套 Table)
				recursiveUpdate(table1);
			}
		}
	}

	public void build(VisTable table, EditorTarget target) {
		this.container = table;
		table.clearChildren();
		table.top();

		if (target == null) {
			table.add(new VisLabel("No Selection")).pad(10);
			return;
		}

		// Special handling for GObjectAdapter
		if (target instanceof GObjectAdapter) {
			buildGObjectInspector((GObjectAdapter) target);
			return;
		}

		// Basic Name field for non-GObject targets (like Root)
		SmartTextInput nameInput = new SmartTextInput("Name", target.getName(), newName -> {
			target.setName(newName);
			updateHierarchyLabel(target);
		});
		container.add(nameInput).growX().padBottom(2).row();
	}

	private void updateHierarchyLabel(EditorTarget target) {
		// This is a simplified way to update the label if possible,
		// or we rely on the Hierarchy system to refresh itself.
		// For now, we assume the hierarchy might need a full refresh or the user accepts delay.
		if (screen instanceof com.goldsprite.gdengine.screens.ecs.editor.EditorController) {
			// ((com.goldsprite.gdengine.screens.ecs.editor.EditorController) screen).updateSceneHierarchy();
			// Calling updateSceneHierarchy is expensive, maybe just ignore for now or add a specific method.
		}
	}

	private void buildGObjectInspector(GObjectAdapter adapter) {
		GObject gobj = adapter.getRealObject();

		// Header
		container.add(new VisLabel("GObject")).colspan(2).left().pad(5).row();

		// Name
		SmartTextInput nameInput = new SmartTextInput("Name", gobj.getName(), v -> {
			gobj.setName(v);
			// updateHierarchyLabel(adapter);
		});
		container.add(nameInput).growX().colspan(2).row();

		// Components
		for (List<Component> comps : gobj.getComponentsMap().values()) {
			for (Component c : comps) {
				buildComponentInspector(c, gobj);
			}
		}

		// Add Component Button
		VisTextButton addBtn = new VisTextButton("Add Component");
		addBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				showAddComponentDialog(gobj, event.getStageX(), event.getStageY());
			}
		});
		container.add(addBtn).fillX().padTop(10).colspan(2).row();
	}

	private void buildComponentInspector(Component c, GObject gobj) {
		VisTable header = new VisTable();
		header.add(new VisLabel(c.getClass().getSimpleName())).expandX().left();

		VisTextButton removeBtn = new VisTextButton("X");
		removeBtn.setColor(Color.RED);
		removeBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				gobj.removeComponent(c);
				refreshValues();
			}
		});
		header.add(removeBtn).size(20, 20);

		container.add(header).growX().colspan(2).padTop(10).padBottom(5).row();

		// Reflection
		Field[] fields = c.getClass().getFields();
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;

			try {
				Class<?> type = f.getType();
				String name = f.getName();
				Object value = f.get(c);

				if (type == float.class || type == Float.class) {
					container.add(new SmartNumInput(name, (float)value, 0.1f, v -> {
						try { f.setFloat(c, v); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == int.class || type == Integer.class) {
					container.add(new SmartNumInput(name, (float)(int)value, 1f, v -> {
						try { f.setInt(c, v.intValue()); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == boolean.class || type == Boolean.class) {
					container.add(new SmartBooleanInput(name, (boolean)value, v -> {
						try { f.setBoolean(c, v); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == String.class) {
					container.add(new SmartTextInput(name, (String)value, v -> {
						try { f.set(c, v); } catch(Exception e) {}
					})).growX().colspan(2).row();
				} else if (type == Vector2.class) {
					Vector2 v = (Vector2) value;
					container.add(new VisLabel(name)).left();
					Table row = new Table();
					row.add(new SmartNumInput("X", v.x, 0.1f, val -> v.x = val)).growX().padRight(5);
					row.add(new SmartNumInput("Y", v.y, 0.1f, val -> v.y = val)).growX();
					container.add(row).growX().row();
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	Vector2 tmpVec2 = new Vector2();
	private void showAddComponentDialog(GObject gobj, float x, float y) {
		PopupMenu menu = new PopupMenu();

		// Add known components
		addComponentMenuItem(menu, gobj, SpriteComponent.class);
		addComponentMenuItem(menu, gobj, TransformComponent.class);
		// Add more components as needed

		menu.showMenu(container.getStage(), x, y);
	}

	private void addComponentMenuItem(PopupMenu menu, GObject gobj, Class<? extends Component> clazz) {
		menu.addItem(new MenuItem(clazz.getSimpleName(), new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				gobj.addComponent(clazz);
				refreshValues();
			}
		}));
	}

	public void refreshValues() {
		if (sceneManager.getSelection() != null && container != null) {
			build(container, sceneManager.getSelection());
		}
	}
}
