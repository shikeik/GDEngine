package com.goldsprite.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane.TabbedPaneStyle;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;

public class VisTabTestScreen extends GScreen {

    private Stage stage;

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

	@Override
	protected void initViewport() {
		uiViewportScale = 2f;
		super.initViewport();
	}

    @Override
    public void create() {
        stage = new Stage(getUIViewport());
        getImp().addProcessor(stage);

        VisTable root = new VisTable();
		root.debugAll();
        root.setFillParent(true);
        root.defaults().pad(20);

        // --- Group 1: Default Style ---
        root.add(createTabGroup("1. Default Style (new TabbedPane())", null)).grow();

        // --- Group 2: Forced Horizontal ---
        TabbedPaneStyle hStyle = new TabbedPaneStyle(VisUI.getSkin().get(TabbedPaneStyle.class));
        hStyle.vertical = false;
        root.add(createTabGroup("2. Forced Horizontal (vertical=false)", hStyle)).grow();

        // --- Group 3: Forced Vertical ---
        TabbedPaneStyle vStyle = new TabbedPaneStyle(VisUI.getSkin().get(TabbedPaneStyle.class));
        vStyle.vertical = true;
        root.add(createTabGroup("3. Forced Vertical (vertical=true)", vStyle)).grow();

        stage.addActor(root);
    }

    private VisTable createTabGroup(String title, TabbedPaneStyle style) {
        VisTable container = new VisTable();
        container.setBackground("window-bg"); // 给个背景看边界

        container.add(new VisLabel(title)).colspan(2).row();

        // 创建 Pane
        TabbedPane pane;
        if (style == null) {
            pane = new TabbedPane(); // 使用默认
        } else {
            pane = new TabbedPane(style); // 使用自定义
        }

        // 内容容器
        VisTable contentContainer = new VisTable();
        contentContainer.setBackground("button"); // 给内容区一个背景

        pane.addListener(new TabbedPaneAdapter() {
				@Override
				public void switchedTab(Tab tab) {
					contentContainer.clearChildren();
					contentContainer.add(tab.getContentTable()).center();
				}
			});

        // 添加 Tab
        pane.add(new TestTab("Tab A"));
        pane.add(new TestTab("Tab B"));
        pane.add(new TestTab("Long Name Tab C"));

        // 布局：根据是否是竖向来决定怎么摆放
        // 这里的逻辑是：如果 Pane 本身是竖向的，按钮条应该在左边；如果是横向，按钮条在上面。
        // 但为了测试 Button Strip 本身的排列，我们统一把 Table 放在上面。

        container.add(pane.getTable()).left().padBottom(5).row();
        container.add(contentContainer).grow();

        return container;
    }

    private static class TestTab extends Tab {
        private String title;
        private VisTable content;

        public TestTab(String title) {
            super(false, false);
            this.title = title;
            content = new VisTable();
            content.add(new VisLabel("Content of " + title));
        }

        @Override public String getTabTitle() { return title; }
        @Override public Table getContentTable() { return content; }
    }

    @Override
    public void render0(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
