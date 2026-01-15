package com.goldsprite.gdengine.screens.basics;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import java.util.LinkedHashMap;
import java.util.Map;
import com.goldsprite.gdengine.screens.ScreenManager;

public abstract class BaseSelectionScreen extends ExampleGScreen {
	protected final Map<String, Class<? extends GScreen>> screenMapping = new LinkedHashMap<>();
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
		return "选择屏";
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	protected void initViewport() {
		uiViewportScale = 2f;
		super.initViewport();
	}

	// 子类只需实现这个方法来填充列表
	protected abstract void initScreenMapping(Map<String, Class<? extends GScreen>> map);

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
			Class<? extends GScreen> key = screenMapping.get(title);

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
					onScreenSelected(key);
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

	/**
	 * 屏幕选择回调 (子类可重写此方法进行拦截)
	 */
	protected void onScreenSelected(Class<? extends GScreen> screenClass) {
		// 自动创建并跳转
		getScreenManager().turnNewScreen(screenClass);
	}

	@Override
	public void dispose() {
		if (stage != null) stage.dispose();
	}
}
