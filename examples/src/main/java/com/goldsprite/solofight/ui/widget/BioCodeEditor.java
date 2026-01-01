// 文件: ./core/src/main/java/com/goldsprite/solofight/ui/widget/BioCodeEditor.java
package com.goldsprite.solofight.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextField;

/**
 * 封装好的代码编辑器控件
 * 修复：通过暴露 calculateOffsets 完美解决滚动条刷新延迟问题
 */
public class BioCodeEditor extends VisTable {

    // [修改] 使用自定义内部类
    private CodeTextArea textArea; 
    private VisScrollPane scrollPane;

    private boolean isSelectionMode = false;
    private float autoScrollSpeed = 0f;

    public BioCodeEditor() {
        super();
        build();
    }

    private void build() {
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(VisUI.getSkin().get(VisTextField.VisTextFieldStyle.class));
        style.font = FontUtils.generateAutoClarity(35);
        style.font.getData().markupEnabled = true;
        style.font.getData().setScale(style.font.getData().scaleX*0.5f);

        // [修改] 实例化自定义类
        textArea = new CodeTextArea("");
        textArea.setStyle(style);

        VisTable container = new VisTable();
        container.add(textArea).grow().pad(5);

        scrollPane = new VisScrollPane(container);
        scrollPane.setScrollBarPositions(false, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setFlickScroll(false); 
        scrollPane.setOverscroll(false, false);
        scrollPane.setCancelTouchFocus(false);

        setupInteraction();

        this.add(scrollPane).grow();
    }

    private void setupInteraction() {
        textArea.addListener(new ActorGestureListener() {
                @Override
                public boolean longPress(Actor actor, float x, float y) {
                    isSelectionMode = true;
                    scrollPane.setFlickScroll(false);
                    Gdx.input.vibrate(500);
                    return true;
                }
            });

        textArea.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    if (isSelectionMode) {
                        checkAutoScroll(event.getStageY());
                    }
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    if (isSelectionMode) {
                        isSelectionMode = false;
                        autoScrollSpeed = 0f;
                    }
                }
            });
    }

    private void checkAutoScroll(float stageY) {
        float topEdge = scrollPane.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, scrollPane.getHeight())).y;
        float bottomEdge = scrollPane.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0)).y;

        float threshold = 60f; 
        float maxSpeed = 15f; 

        if (stageY > topEdge - threshold) {
            float ratio = (stageY - (topEdge - threshold)) / threshold;
            autoScrollSpeed = -maxSpeed * ratio;
        } else if (stageY < bottomEdge + threshold) {
            float ratio = ((bottomEdge + threshold) - stageY) / threshold;
            autoScrollSpeed = maxSpeed * ratio;
        } else {
            autoScrollSpeed = 0f;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (isSelectionMode && Math.abs(autoScrollSpeed) > 0.1f) {
            float currentY = scrollPane.getScrollY();
            float maxY = scrollPane.getMaxY();
            float nextY = currentY + autoScrollSpeed;
            if (nextY < 0) nextY = 0;
            if (nextY > maxY) nextY = maxY;
            scrollPane.setScrollY(nextY);
        }
    }

    public void setText(String text) {
        Gdx.app.postRunnable(() -> {
            textArea.setText(text);

            // [核心修复连招]
            // 1. 第一次握手：让 ScrollPane 布局，把宽度传给 TextArea
            scrollPane.validate(); 

            // 2. 强制计算：TextArea 立即根据新宽度计算行数 (Wrap Text)
            textArea.forceCalc(); 

            // 3. 第二次握手：通知 ScrollPane 读新高度
            textArea.invalidateHierarchy();
            scrollPane.layout(); 

            // 4. 归位
            scrollPane.setScrollY(0);
            scrollPane.updateVisualScroll();
        });
    }

    public String getText() {
        return textArea.getText();
    }

    // ==========================================
    // [核心内部类] 把 protected 方法挖出来
    // ==========================================
    private class CodeTextArea extends VisTextArea {
        public CodeTextArea(String text) {
            super(text);
        }

        @Override
        public float getPrefHeight() {
            // 防 NPE 保护
            if (getText() == null || getStyle() == null || getStyle().font == null) {
                return 15 * 20;
            }
            float rows = getLines();
            if (rows < 15) rows = 15;
            return rows * getStyle().font.getLineHeight() + 20;
        }

        // 将 protected 的 calculateOffsets 暴露为 public
        public void forceCalc() {
            super.calculateOffsets();
        }
    }
}
