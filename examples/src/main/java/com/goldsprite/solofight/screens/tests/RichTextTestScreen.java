package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.richtext.RichText;
import com.goldsprite.gdengine.ui.widget.richtext.RichTextEvent;
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;

public class RichTextTestScreen extends GScreen {
	
	private Stage uiStage;
	
	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		super.create();
		
		uiStage = new Stage(getUIViewport());
		getImp().addProcessor(uiStage);
		
		VisTable container = new VisTable();
		container.defaults().pad(10).left();
		
		container.add(new VisLabel("富文本功能测试 (Rich Text)")).row();
		
		// Test 1: Basic colors and sizes
		String text1 = "你好 [color=red]红色[/color] 和 [size=40]大号[/size] 世界！";
		container.add(new VisLabel("测试 1: 基础样式")).row();
		RichText rt1 = new RichText(text1, 500);
		container.add(rt1).row();
		
		container.add(new VisLabel("----------------")).fillX().pad(5).row();
		
		// Test 2: Nested and Images
		String text2 = "获得物品: [img=gd_icon.png|32x32] x [size=40][color=gold]5[/color][/size]";
		container.add(new VisLabel("测试 2: 图文混排")).row();
		RichText rt2 = new RichText(text2, 500);
		container.add(rt2).row();

		container.add(new VisLabel("----------------")).fillX().pad(5).row();
		
		// Test 3: Wrapping
		String text3 = "这是一段很长的文本，用于测试自动换行功能。" +
						"中间包含一些 [color=green]绿色文本[/color] 和 " +
						"[size=20]小号字体[/size]。让我们看看当达到宽度限制时它如何表现。" +
						"[img=gd_icon.png|20x20] 图标结尾。";
		container.add(new VisLabel("测试 3: 自动换行 (宽=300)")).row();
		RichText rt3 = new RichText(text3, 300);
		rt3.debug(); // Show bounds
		container.add(rt3).row();

		container.add(new VisLabel("----------------")).fillX().pad(5).row();
		
		// Test 4: Events
		String text4 = "点击 [event=click_me][color=cyan]这里[/color][/event] 触发事件。";
		container.add(new VisLabel("测试 4: 点击事件")).row();
		RichText rt4 = new RichText(text4, 500);
		rt4.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
			@Override
			public boolean handle(com.badlogic.gdx.scenes.scene2d.Event e) {
				if (e instanceof RichTextEvent) {
					RichTextEvent re = (RichTextEvent)e;
					if (ToastUI.inst() != null) {
						ToastUI.inst().show("Event: " + re.eventId);
					}
					return true;
				}
				return false;
			}
		});
		container.add(rt4).row();int k2;
		
		// [New] Test 5: New Icons (RavenFantasyIcons16x16.png) using Custom Atlas
		// Syntax: [img=path/to/image.png#regionName]
		container.add(new VisLabel("----------------")).fillX().pad(5).row();
		
		container.add(new VisLabel("Test 5: 自定义资源图标 (pot_clay, helmet_iron, orbs_magic)")).row();
		String text5 = "Atlas Test: [img=sprites/icons/RavenFantasyIcons16x16.png#pot_clay] " +
						"[img=sprites/icons/RavenFantasyIcons16x16.png#helmet_iron] " +
						"[img=sprites/icons/RavenFantasyIcons16x16.png#orbs_magic]";
						
		RichText rt5 = new RichText(text5, 500);
		// 放大一点看清楚
		rt5.setScale(2); 
		rt5.setOrigin(0,0);
		container.add(rt5).row();
		
		VisScrollPane scroll = new VisScrollPane(container);
		uiStage.addActor(scroll);
		scroll.setFillParent(true);
		
		// Ensure ToastUI is on top
		if (ToastUI.inst() == null) new ToastUI();
		if (ToastUI.inst().getParent() == null) {
			uiStage.addActor(ToastUI.inst());
		} else {
			ToastUI.inst().toFront();
		}
	}
	
	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		// 强制 UI 视口以左下角为 (0,0)，不居中，避免坐标混乱
		if (getUIViewport() != null) {
			 getUIViewport().update(width, height, true); // GScreen 默认是 true (CenterCamera)
			 // 如果用户觉得 "trigger文本又在0,0" 是偏移问题，可能是因为 WorldCamera 和 UICamera 不一致。
			 // 这里 RichText 是 UI 组件，只受 UICamera 影响。
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
