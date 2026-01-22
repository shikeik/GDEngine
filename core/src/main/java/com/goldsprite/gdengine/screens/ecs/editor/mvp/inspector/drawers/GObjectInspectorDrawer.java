package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.InspectorBuilder;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.goldsprite.gdengine.ui.widget.AddComponentDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.List;

public class GObjectInspectorDrawer implements IInspectorDrawer<GObject> {

	@Override
	public boolean accept(Object target) {
		return target instanceof GObject;
	}

	@Override
	public void draw(GObject selection, VisTable container) {
		float pad = 10;

		// 1. Meta Data
		VisTable metaContainer = new VisTable();
		metaContainer.setBackground("panel1");

		metaContainer.add(new VisLabel("Name:")).left().padLeft(5);
		SmartTextInput nameInput = new SmartTextInput(null, selection.getName(), v -> {
			selection.setName(v);
			// [Fix] 移除 emitStructureChanged()，防止焦点丢失。
			// 数据已绑定，Inspector 会自动刷新；Hierarchy 刷新由其自身逻辑控制。
		});
		nameInput.bind(selection::getName); // [New] Data Binding
		metaContainer.add(nameInput).growX().padRight(5).row();

		metaContainer.add(new VisLabel("Tag:")).left().padLeft(5);
		SmartTextInput tagInput = new SmartTextInput(null, selection.getTag(), selection::setTag);
		tagInput.bind(selection::getTag); // [New] Data Binding
		metaContainer.add(tagInput).growX().padRight(5).row();

		container.add(metaContainer).growX().pad(pad).row();

		// 2. Components
		for (List<Component> comps : selection.getComponentsMap().values()) {
			for (Component c : comps) {
				buildComponentUI(c, container);
			}
		}

		// 3. Add Component Button
		VisTextButton btnAdd = new VisTextButton("Add Component");
		btnAdd.setColor(Color.GREEN);
		btnAdd.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					// 弹出对话框，添加成功后通知刷新
					new AddComponentDialog(selection, () -> {
						EditorEvents.inst().emitStructureChanged();
						// 重新绘制自己 (Hack: 通过发送选中事件触发 Presenter 重绘)
						EditorEvents.inst().emitSelectionChanged(selection);
					}).show(container.getStage());
				}
			});
		container.add(btnAdd).growX().pad(pad).padBottom(20);
	}

	private void buildComponentUI(Component c, VisTable parent) {
		VisTable container = new VisTable();
		container.setBackground("panel1");

		// Header
		VisTable header = new VisTable();
		header.setBackground("list");
		header.add(new VisLabel(c.getClass().getSimpleName())).expandX().left().pad(5);

		// Remove Button
		if (!(c instanceof TransformComponent)) {
			VisTextButton btnRemove = new VisTextButton("X");
			btnRemove.setColor(Color.RED);
			btnRemove.addListener(new ClickListener() {
					@Override public void clicked(InputEvent event, float x, float y) {
						c.destroyImmediate();
						EditorEvents.inst().emitStructureChanged();
						// 重新绘制
						EditorEvents.inst().emitSelectionChanged(c.getGObject());
					}
				});
			header.add(btnRemove).size(25, 25).padRight(5).right();
		}
		container.add(header).growX().pad(2).row();

		// Body (Reflect)
		VisTable body = new VisTable();
		body.pad(5);
		InspectorBuilder.build(body, c);

		container.add(body).growX().pad(2).row();
		parent.add(container).growX().pad(10).padTop(0).row();
	}
}
