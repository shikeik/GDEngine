// 文件: ./examples/src/main/java/com/goldsprite/solofight/screens/tests/TempTestScreen.java
package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.goldsprite.gameframeworks.log.Debug;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class TempTestScreen extends ExampleGScreen {

    private Stage stage;
    private DebugTextArea textArea; // 使用自定义类
    private VisScrollPane scrollPane;
    private VisTable container;

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

    @Override
    public void create() {
        Debug.showDebugUI = true;

        stage = new Stage(getUIViewport());
        getImp().addProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);
        stage.addActor(root);

        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(VisUI.getSkin().get(VisTextField.VisTextFieldStyle.class));
        style.font = FontUtils.generateAutoClarity(24);

        // 1. 使用暴露了 forceCalc 的自定义类
        textArea = new DebugTextArea("Init");
        textArea.setStyle(style);

        container = new VisTable();
        container.add(textArea).grow().top(); 

        scrollPane = new VisScrollPane(container);
        scrollPane.setFadeScrollBars(false); 
        scrollPane.setScrollingDisabled(true, false);

        root.add(scrollPane).width(600).height(400).padBottom(20).row();

        VisTextButton btnLoad = new VisTextButton("验证修复方案 (Verify Fix)");
        btnLoad.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					verifyFixSequence();
				}
			});
        root.add(btnLoad).height(50).width(300);
    }

    private void verifyFixSequence() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("Line ").append(i).append(" data...\n");
        String text = sb.toString();

        Debug.logT("VERIFY", "=== 点击开始 ===");

        Gdx.app.postRunnable(() -> {
            // A. 设置文本
            textArea.setText(text);
            Debug.logT("VERIFY", "1. SetText后: Lines=%d (此时应该是旧值 1)", textArea.getLines());

            // B. 第一次握手：让 ScrollPane 算出 TextArea 应该有多宽
            container.invalidate(); 
            scrollPane.validate(); 
            Debug.logT("VERIFY", "2. SP Validate后: W=%.1f (宽度已拿到)", textArea.getWidth());
            Debug.logT("VERIFY", "   此时 Lines=%d (依然没变，因为还没 draw)", textArea.getLines());

            // C. 【绝杀时刻】手动触发计算！
            textArea.forceCalc(); 
            Debug.logT("VERIFY", "3. ForceCalc后: Lines=%d (见证奇迹的时刻！应为 51)", textArea.getLines());

            // D. 第二次握手：通知 ScrollPane 读新高度
            textArea.invalidateHierarchy();
            scrollPane.layout(); 

            Debug.logT("VERIFY", "4. Final Check: PrefH=%.1f", textArea.getPrefHeight());
            Debug.logT("VERIFY", "5. Scroll MaxY: %.1f (大于0说明滚动条出来了)", scrollPane.getMaxY());

            scrollPane.setScrollY(0);
            scrollPane.updateVisualScroll();
        });
    }

    // ==========================================
    // 🛠️ 关键内部类：把 protected 方法挖出来
    // ==========================================
    private class DebugTextArea extends VisTextArea {
        public DebugTextArea(String text) {
            super(text);
        }

        @Override
        public float getPrefHeight() {
            float lines = getLines();
            float fontH = getStyle().font.getLineHeight();
            return lines * fontH + 20;
        }

        // 把父类的 protected void calculateOffsets() 暴露出来
        public void forceCalc() {
            super.calculateOffsets();
        }
    }

    @Override
    public void render0(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if(stage!=null)stage.dispose();
    }
}
