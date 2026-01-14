package com.goldsprite.screens.tests;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane.TabbedPaneStyle;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;

public class VisTabTestScreen2 extends GScreen {

	private Stage stage;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		// 创建两个窗口进行对比

		// 1. 默认样式 (Horizontal)
		TestTabbedPane winDefault = new TestTabbedPane(false);
		winDefault.setPosition(100, 200);
		winDefault.setSize(400, 300);
		stage.addActor(winDefault);

		// 2. 竖直样式 (Vertical)
		TestTabbedPane winVertical = new TestTabbedPane(true);
		winVertical.setPosition(550, 200);
		winVertical.setSize(400, 300);
		stage.addActor(winVertical);
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

	/**
	 * 这是直接从 VisUI 源码复刻过来的测试窗口类
	 * (稍微修改了 Style 获取逻辑以防 Crash)
	 */
	public static class TestTabbedPane extends VisWindow {

		public TestTabbedPane(boolean vertical) {
			super("tabbed pane (" + (vertical ? "Vertical" : "Horizontal") + ")");

			TableUtils.setSpacingDefaults(this);

			setResizable(true);
			addCloseButton();
			closeOnEscape();

			final VisTable container = new VisTable();

			// [修改] 原源码是 get(vertical ? "vertical" : "default")
			// 但很多 Skin 没有 "vertical" 这个名字的定义，所以我们手动构造一下，原理是一样的
			TabbedPaneStyle style;
			if (vertical) {
				// 强制构造一个 vertical = true 的样式
				TabbedPaneStyle defaultStyle = VisUI.getSkin().get(TabbedPaneStyle.class);
				style = new TabbedPaneStyle(defaultStyle);
				style.vertical = true;
			} else {
				// 使用默认样式
				style = VisUI.getSkin().get(TabbedPaneStyle.class);
				// 确保默认是横向的 (有些 Skin 默认 vertical=true，这里强制复位一下)
				if (style.vertical) {
					style = new TabbedPaneStyle(style);
					style.vertical = false;
				}
			}

			TabbedPane tabbedPane = new TabbedPane(style);
			tabbedPane.addListener(new TabbedPaneAdapter() {
				@Override
				public void switchedTab(Tab tab) {
					container.clearChildren();
					container.add(tab.getContentTable()).expand().fill();
				}
			});

			// --- 核心布局逻辑 (源码逻辑) ---
			if (style.vertical) {
				// 竖向逻辑：Tab条在左，内容在右
				top();
				defaults().top();
				add(tabbedPane.getTable()).growY(); // 这里的 growY 意味着条本身是竖长的
				add(container).expand().fill();
			} else {
				// 横向逻辑：Tab条在上，内容在下
				add(tabbedPane.getTable()).expandX().fillX();
				row();
				add(container).expand().fill();
			}
			// ---------------------------

			tabbedPane.add(new TestTab("tab1"));
			tabbedPane.add(new TestTab("tab2"));
			tabbedPane.add(new TestTab("tab3"));

			// 默认选中第一个
			if (tabbedPane.getTabs().size > 0) {
				tabbedPane.switchTab(0);
			}

			// pack(); // 源码没有 pack，但这里为了让布局生效最好 pack 或者 setSize
		}
	}

	private static class TestTab extends Tab {
		private String title;
		private Table content;

		public TestTab(String title) {
			super(false, true);
			this.title = title;
			content = new VisTable();
			content.add(new VisLabel("Content of " + title));
		}

		@Override
		public String getTabTitle() {
			return title;
		}

		@Override
		public Table getContentTable() {
			return content;
		}
	}
}
