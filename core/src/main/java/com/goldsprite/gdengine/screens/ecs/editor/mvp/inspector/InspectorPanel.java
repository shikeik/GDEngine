package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector;

// 保持 imports，增加 Registry 的引用
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.goldsprite.gdengine.ui.input.SmartInput;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel; 
import com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers.InspectorRegistry; 

public class InspectorPanel extends EditorPanel implements IInspectorView { // IInspectorView 接口可能还得改一下泛型

	private InspectorPresenter presenter;
	private VisTable bodyTable;

	private static final float REFRESH_RATE = 1f / 30f;
	private float timer = 0f;

	public InspectorPanel() {
		super("Inspector");
		bodyTable = new VisTable();
		bodyTable.top().left();
		VisScrollPane scrollPane = new VisScrollPane(bodyTable);
		scrollPane.setOverscroll(false, false);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false);
		addContent(scrollPane);
		showEmpty();
	}

	@Override
	public void setPresenter(InspectorPresenter presenter) {
		this.presenter = presenter;
	}

	// [修改] 参数改为 Object
	@Override
	public void rebuild(Object selection) {
		bodyTable.clearChildren();

		if (selection == null) {
			showEmpty();
			return;
		}

		// 1. 查找绘制器
		var drawer = InspectorRegistry.getDrawer(selection);

		// 2. 绘制
		if (drawer != null) {
			// Unchecked cast is safe due to registry logic
			drawer.draw(selection, bodyTable);
		} else {
			showEmpty();
		}
	}

	private void showEmpty() {
		bodyTable.clearChildren();
		VisLabel label = new VisLabel("No Selection");
		label.setColor(Color.GRAY);
		bodyTable.add(label).pad(20);
	}

	// [删除] buildComponentUI, showAddComponentDialog 等具体方法 (已移至 Drawer)

	@Override
	public void updateValues() {
		recursiveUpdate(bodyTable);
	}

	private void recursiveUpdate(Actor actor) {
		if (actor instanceof SmartInput) {
			// [Fix] 调用新的 sync 方法，从数据源拉取最新值
			((SmartInput<?>) actor).sync();
		} else if (actor instanceof Group) {
			for (Actor child : ((Group) actor).getChildren()) {
				recursiveUpdate(child);
			}
		}
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		
		// [New] 30 FPS Throttling
		timer += delta;
		if (timer >= REFRESH_RATE) {
			updateValues();
			timer = 0f;
		}
	}
}
