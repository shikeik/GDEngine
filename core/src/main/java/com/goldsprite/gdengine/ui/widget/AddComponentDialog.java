package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.ComponentRegistry;
import com.goldsprite.gdengine.core.utils.ComponentScanner;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.function.Consumer;

public class AddComponentDialog extends BaseDialog {

	private final GObject targetObject;
	private final Runnable onAdded;

	private VisTextField searchField;
	private VisTable listTable;
	private Array<Class<? extends Component>> allComponents;

	public AddComponentDialog(GObject target, Runnable onAdded) {
		super("Add Component");
		this.targetObject = target;
		this.onAdded = onAdded;

		// [修改] 直接从注册表获取，极快，且包含用户脚本
		allComponents = ComponentRegistry.getAll();

		// 2. 搜索框
		Table searchTable = new Table();
		searchTable.add(new VisLabel("Search: ")).padRight(5);
		searchField = new VisTextField();
		searchField.setMessageText("Component Name...");
		searchTable.add(searchField).growX();

		getContentTable().add(searchTable).growX().pad(5).row();

		// 3. 列表区域
		listTable = new VisTable();
		listTable.top().left();

		VisScrollPane scrollPane = new VisScrollPane(listTable);
		scrollPane.setFadeScrollBars(false);

		getContentTable().add(scrollPane).width(300).height(400).pad(5).row();

		// 4. 事件监听
		searchField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				refreshList(searchField.getText());
			}
		});

		// 初始显示全部
		refreshList("");

		pack();
		centerWindow();

		// 自动聚焦搜索框
		if (getStage() != null) getStage().setKeyboardFocus(searchField);
	}

	private void refreshList(String query) {
		listTable.clearChildren();
		String q = query.toLowerCase().trim();

		for (Class<? extends Component> clazz : allComponents) {
			String name = clazz.getSimpleName();

			// 过滤逻辑
			if (q.isEmpty() || name.toLowerCase().contains(q)) {
				addItem(clazz);
			}
		}
	}

	private void addItem(Class<? extends Component> clazz) {
		VisTextButton btn = new VisTextButton(clazz.getSimpleName(), "toggle"); // 使用 toggle 样式看起来像列表项
		btn.getLabel().setAlignment(Align.left);

		// 区分内置和自定义 (简单的颜色区分)
		if (!clazz.getName().startsWith("com.goldsprite.gdengine")) {
			btn.setColor(Color.ORANGE); // 用户脚本标橙
		}

		btn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				try {
					targetObject.addComponent(clazz);
					if (onAdded != null) onAdded.run();
					fadeOut();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		listTable.add(btn).growX().height(30).padBottom(2).row();
	}
}
