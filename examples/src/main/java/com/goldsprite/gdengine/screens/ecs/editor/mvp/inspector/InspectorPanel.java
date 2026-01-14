package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.inspector.InspectorBuilder;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.input.SmartInput;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.goldsprite.gdengine.ui.widget.AddComponentDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.List;

public class InspectorPanel extends EditorPanel implements IInspectorView {

	private InspectorPresenter presenter;
	private VisTable bodyTable; // 滚动区域内部容器

	public InspectorPanel() {
		super("Inspector");

		bodyTable = new VisTable();
		bodyTable.top().left();
		// bodyTable.setBackground("window-bg");

		VisScrollPane scrollPane = new VisScrollPane(bodyTable);
		scrollPane.setOverscroll(false, false);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false);

		addContent(scrollPane);

		// 初始状态
		showEmpty();
	}

	@Override
	public void setPresenter(InspectorPresenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void rebuild(GObject selection) {
		bodyTable.clearChildren();

		if (selection == null) {
			showEmpty();
			return;
		}

		float pad = 10;

		// 1. 物体元数据 (Name, Tag)
		VisTable metaContainer = new VisTable();
		metaContainer.setBackground("panel1"); // 确保 Skin 里有这个，或者用 button

		metaContainer.add(new VisLabel("Name:")).left().padLeft(5);
		metaContainer.add(new SmartTextInput(null, selection.getName(), v -> presenter.changeName(v)))
			.growX().padRight(5).row();

		metaContainer.add(new VisLabel("Tag:")).left().padLeft(5);
		metaContainer.add(new SmartTextInput(null, selection.getTag(), v -> presenter.changeTag(v)))
			.growX().padRight(5).row();

		bodyTable.add(metaContainer).growX().pad(pad).row();

		// 2. 组件列表
		for (List<Component> comps : selection.getComponentsMap().values()) {
			for (Component c : comps) {
				buildComponentUI(c);
			}
		}

		// 3. 添加组件按钮
		VisTextButton btnAdd = new VisTextButton("Add Component");
		btnAdd.setColor(Color.GREEN);
		btnAdd.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				presenter.onAddComponentClicked();
			}
		});
		bodyTable.add(btnAdd).growX().pad(pad).padBottom(20);
	}

	private void showEmpty() {
		bodyTable.clearChildren();
		VisLabel label = new VisLabel("No Selection");
		label.setColor(Color.GRAY);
		bodyTable.add(label).pad(20);
	}

	private void buildComponentUI(Component c) {
		VisTable container = new VisTable();
		container.setBackground("panel1"); // 稍深背景

		// Header
		VisTable header = new VisTable();
		header.setBackground("list"); // 稍亮背景
		header.add(new VisLabel(c.getClass().getSimpleName())).expandX().left().pad(5);

		// Remove Button (Transform 不可删)
		if (!(c instanceof TransformComponent)) {
			VisTextButton btnRemove = new VisTextButton("X");
			btnRemove.setColor(Color.RED);
			btnRemove.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					presenter.removeComponent(c);
				}
			});
			header.add(btnRemove).size(25, 25).padRight(5).right();
		}
		container.add(header).growX().pad(2).row();

		// Body (反射生成)
		VisTable body = new VisTable();
		body.pad(5);
		InspectorBuilder.build(body, c);

		container.add(body).growX().pad(2).row();
		bodyTable.add(container).growX().pad(10).padTop(0).row();
	}

	@Override
	public void showAddComponentDialog(GObject target) {
		new AddComponentDialog(target, () -> presenter.onComponentAdded()).show(getStage());
	}

	@Override
	public void updateValues() {
		// 递归刷新所有 SmartInput
		recursiveUpdate(bodyTable);
	}

	private void recursiveUpdate(Actor actor) {
		if (actor instanceof SmartInput) {
			((SmartInput<?>) actor).updateUI();
		} else if (actor instanceof Group) {
			for (Actor child : ((Group) actor).getChildren()) {
				recursiveUpdate(child);
			}
		}
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		// 每帧自动刷新数值 (保持与游戏逻辑同步)
		updateValues();
	}
}
