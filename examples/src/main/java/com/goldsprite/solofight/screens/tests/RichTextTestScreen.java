package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.ui.widget.richtext.RichText;
import com.goldsprite.gdengine.ui.widget.richtext.RichTextEvent;
import com.goldsprite.solofight.ui.widget.ToastUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class RichTextTestScreen extends GScreen {
    
    private Stage uiStage;

    @Override
    public void create() {
        super.create();
        
        uiStage = new Stage(getUIViewport());
        getImp().addProcessor(uiStage);
        
        VisTable container = new VisTable();
        container.defaults().pad(10).left();
        
        container.add(new VisLabel("Rich Text Test")).row();
        
        // Test 1: Basic colors and sizes
        String text1 = "Hello [color=red]Red[/color] and [size=40]Big[/size] world!";
        container.add(new VisLabel("Test 1: " + text1)).row();
        RichText rt1 = new RichText(text1, 500);
        container.add(rt1).row();
        
        container.add(new VisLabel("----------------")).fillX().pad(5).row();
        
        // Test 2: Nested and Images
        String text2 = "Items: [img=gd_icon.png|32x32] x [size=40][color=gold]5[/color][/size]";
        container.add(new VisLabel("Test 2: Images")).row();
        RichText rt2 = new RichText(text2, 500);
        container.add(rt2).row();

        container.add(new VisLabel("----------------")).fillX().pad(5).row();
        
        // Test 3: Wrapping
        String text3 = "This is a long text to test automatic wrapping functionality. " +
                       "We have some [color=green]green text[/color] in the middle and some " +
                       "[size=20]small text[/size] as well. Let's see how it behaves when the " +
                       "width limit is reached. [img=gd_icon.png|20x20] Icon here.";
        container.add(new VisLabel("Test 3: Wrap (Width=300)")).row();
        RichText rt3 = new RichText(text3, 300);
        rt3.debug(); // Show bounds
        container.add(rt3).row();

        container.add(new VisLabel("----------------")).fillX().pad(5).row();
        
        // Test 4: Events
        String text4 = "Click [event=click_me][color=cyan]HERE[/color][/event] to trigger event.";
        container.add(new VisLabel("Test 4: Events")).row();
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
        container.add(rt4).row();
        
        // [New] Test 5: New Icons (RavenFantasyIcons16x16.png)
        // 这是一个 SpriteSheet，目前 RichText 还不支持 TextureRegion 切片，先测试直接加载看看效果，或者用 [img] 加载单个文件。
        // 假设用户只是想看这个图能不能加载。
        // 如果要支持 SpriteSheet 切片，RichTextParser 需要升级，暂时先显示整图或不测试切片。
        // 既然用户提到了 "sprites/icons/RavenFantasyIcons16x16.png"，我们加一个测试项。
        container.add(new VisLabel("----------------")).fillX().pad(5).row();
        String text5 = "New Icon: [img=sprites/icons/RavenFantasyIcons16x16.png]";
        container.add(new VisLabel("Test 5: Icons")).row();
        RichText rt5 = new RichText(text5, 500);
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
