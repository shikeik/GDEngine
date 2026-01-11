package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.richtext.RichText;
import com.goldsprite.solofight.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;

/**
 * RichText 布局与自适应测试场景
 * <p>
 * 提供一个交互式编辑器，用于编写富文本代码，并在一个受限的布局环境中实时预览。
 * 用于验证 RichText 在被其他控件包围时的尺寸计算和换行行为。
 * </p>
 */
public class RichTextLayoutTestScreen extends GScreen {

	private Stage uiStage;
	private RichText richText;
	private VisLabel sizeInfoLabel;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}
	
	@Override
	public void create() {
		super.create();

		uiStage = new Stage(getUIViewport());
		getImp().addProcessor(uiStage);

		VisTable root = new VisTable();
		root.setFillParent(true);
		root.defaults().pad(5);

		// --- 顶部：标题 ---
		root.add(new VisLabel("RichText 布局自适应测试")).colspan(2).row();

		// --- 中间：主要内容区 (SplitPane) ---
		// 左侧：编辑器
		VisTable editorTable = new VisTable();
		editorTable.add(new VisLabel("源码编辑器:")).left().row();
		
		String defaultCode = "欢迎使用 [color=gold]富文本编辑器[/color]！\n" +
							 "这里可以测试 [size=20]布局[/size] 和 铁剑 [img=sprites/icons/RavenFantasyIcons16x16.png#sword_iron] 图标。\n" +
							 "[color=green]尝试输入更多文字来测试自动换行...[/color]";
		
		final VisTextArea textArea = new VisTextArea(defaultCode);
		textArea.setPrefRows(15);
		VisScrollPane editorScroll = new VisScrollPane(textArea);
		editorTable.add(editorScroll).expand().fill();

		// 右侧：预览区 (模拟拥挤布局)
		VisTable previewContainer = new VisTable();
		previewContainer.debug(); // 显示容器边界
		previewContainer.add(new VisLabel("上方控件 (Header)")).row();

		VisTable crowdedRow = new VisTable();
		crowdedRow.add(new VisTextButton("左侧按钮")).top();
		
		// RichText 放在中间，限制宽度
		richText = new RichText(defaultCode, 300); // 初始限宽 300
		richText.debug(); // 显示 RichText 自身边界
		
		// 使用一个可变宽度的 Cell 包裹 RichText
		final com.badlogic.gdx.scenes.scene2d.ui.Cell<?> rtCell = crowdedRow.add(richText).top().pad(5);
		
		crowdedRow.add(new VisTextButton("右侧按钮")).top();
		
		previewContainer.add(crowdedRow).expand().fill().row();
		previewContainer.add(new VisLabel("下方控件 (Footer)")).row();

		// SplitPane 组合
		VisSplitPane splitPane = new VisSplitPane(editorTable, previewContainer, false);
		splitPane.setSplitAmount(0.4f); // 左侧占 40%
		
		root.add(splitPane).expand().fill().row();

		// --- 底部：控制栏 ---
		VisTable controls = new VisTable();
		
		VisLabel widthLabel = new VisLabel("RichText 限宽: 300");
		VisSlider widthSlider = new VisSlider(100, 800, 10, false);
		widthSlider.setValue(300);
		
		widthSlider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				float val = widthSlider.getValue();
				widthLabel.setText("RichText 限宽: " + (int)val);
				
				// 更新 RichText 限宽
				richText.setWidth(val);
				// 同时更新 Cell 宽度（如果是 Table 布局，通常 Cell 宽度由内容决定，或者限制内容）
				// 这里我们限制 Cell 的最大宽度，或者让 RichText 自己决定
				// rtCell.width(val); 
				
				// 强制重绘
				richText.layout();
				richText.invalidateHierarchy();
				updateSizeInfo();
			}
		});
		
		sizeInfoLabel = new VisLabel("Size: ?");
		
		controls.add(widthLabel).padRight(10);
		controls.add(widthSlider).width(300).padRight(20);
		controls.add(sizeInfoLabel);
		
		root.add(controls).bottom().fillX().pad(10);

		uiStage.addActor(root);
		
		// --- 事件监听 ---
		// 文本变更监听
		textArea.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				richText.setText(textArea.getText());
				richText.invalidateHierarchy();
				updateSizeInfo();
			}
		});

		// Toast
		if (ToastUI.inst() == null) new ToastUI();
		if (ToastUI.inst().getParent() == null) {
			uiStage.addActor(ToastUI.inst());
		} else {
			ToastUI.inst().toFront();
		}
		
		updateSizeInfo();
	}
	
	private void updateSizeInfo() {
		if (richText != null && sizeInfoLabel != null) {
			sizeInfoLabel.setText(String.format(
				"实际尺寸: %.0fx%.0f | 首选尺寸: %.0fx%.0f", 
				richText.getWidth(), richText.getHeight(),
				richText.getPrefWidth(), richText.getPrefHeight()
			));
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (getUIViewport() != null) {
			 getUIViewport().update(width, height, true);
		}
	}

	@Override
	public void render0(float delta) {
		if (uiStage != null) {
			uiStage.act(delta);
			uiStage.draw();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (uiStage != null) uiStage.dispose();
	}
}
