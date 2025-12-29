package com.goldsprite.gameframeworks.screens.basics;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.goldsprite.gameframeworks.screens.IGScreen;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import java.util.LinkedHashMap;
import java.util.Map;
import com.goldsprite.gameframeworks.screens.ScreenManager;

public abstract class BaseSelectionScreen extends ExampleGScreen {
	protected final Map<String, Class<? extends IGScreen>> screenMapping = new LinkedHashMap<>();
	private final float layout_padding = 40;
	private final float cell_margin = 20;
	private final float cell_height = 80;
	protected Stage stage;
	protected VisTable rootTable;

	public BaseSelectionScreen() {
		super();
		initScreenMapping(screenMapping);
	}

	@Override
	public String getIntroduction() {
		return "";
	}
	
	// [新增] 菜单默认竖屏
    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Portrait;
    }

	// 子类只需实现这个方法来填充列表
	protected abstract void initScreenMapping(Map<String, Class<? extends IGScreen>> map);

	@Override
	public void create() {
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		rootTable = new VisTable();
		rootTable.setFillParent(true);
		stage.addActor(rootTable);

		VisTable buttonList = getButtonTable();
		VisScrollPane scrollPane = new VisScrollPane(buttonList);
		scrollPane.setScrollingDisabled(true, false);
		scrollPane.setFadeScrollBars(false);
		rootTable.add(scrollPane).expand().fill().pad(20);
	}

	protected VisTable getButtonTable() {
		VisTable buttonTable = new VisTable();
		buttonTable.pad(layout_padding).defaults().space(cell_margin);

		ButtonGroup<Button> btnGroup = new ButtonGroup<>();
		btnGroup.setMaxCheckCount(1);
		btnGroup.setUncheckLast(true);

		for (String title : screenMapping.keySet()) {
			Class<? extends IGScreen> key = screenMapping.get(title);

			if (key == null) {
				VisLabel lbl = new VisLabel(title);
				lbl.setColor(0, 1, 1, 1);
				buttonTable.add(lbl).padTop(20).padBottom(10).row();
				continue;
			}

			VisTextButton button = new VisTextButton(title);
			btnGroup.add(button);
			Cell<VisTextButton> cell = buttonTable.add(button);
			cell.expandX().fillX();
			cell.height(cell_height);
			buttonTable.row();
			
			button.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					// 自动创建并跳转
					getScreenManager().setCurScreen(key, true);
				}
			});
		}
		return buttonTable;
	}

	@Override
	public void render0(float delta) {
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void dispose() {
		if (stage != null) stage.dispose();
	}
}
