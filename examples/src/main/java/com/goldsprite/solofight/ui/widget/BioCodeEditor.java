// 文件: ./core/src/main/java/com/goldsprite/solofight/ui/widget/BioCodeEditor.java
package com.goldsprite.solofight.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

/**
 * 封装好的代码编辑器控件
 * v3.3 Fix: 优化交互逻辑 -> 长按拖动选区，松手后才显示菜单
 */
public class BioCodeEditor extends VisTable {

    private CodeTextArea textArea; 
    private VisScrollPane scrollPane;
    private VisTable popupMenu; 

    // 交互状态标记
    private boolean isSelectionMode = false; // 是否处于拖拽选区模式
    private boolean hasLongPressed = false;  // 本次触摸是否触发了长按
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

        createPopupMenu();
        setupInteraction();

        this.add(scrollPane).grow();
    }

    private void createPopupMenu() {
        popupMenu = new VisTable();
        try {
            popupMenu.setBackground(VisUI.getSkin().getDrawable("window-bg"));
        } catch (Exception e) {
            popupMenu.setBackground(VisUI.getSkin().getDrawable("button"));
        }

        String btnStyle = "default"; 

        VisTextButton btnCopy = new VisTextButton("复制", btnStyle);
        btnCopy.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					textArea.copy();
					hidePopupMenu();
				}
			});

        VisTextButton btnPaste = new VisTextButton("粘贴", btnStyle);
        btnPaste.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					String content = Gdx.app.getClipboard().getContents();
					if (content != null) {
						// 手动拼接
						String oldText = textArea.getText();
						int cursor = textArea.getCursorPosition();
						int selectionStart = textArea.getSelectionStart();
						int start = Math.min(cursor, selectionStart);
						int end = Math.max(cursor, selectionStart);

						StringBuilder sb = new StringBuilder(oldText);
						if (end > start) {
							sb.delete(start, end);
							cursor = start;
						}
						sb.insert(cursor, content);
						textArea.setText(sb.toString());
						textArea.setCursorPosition(cursor + content.length());
						textArea.clearSelection();
						refreshLayout();
					}
					hidePopupMenu();
				}
			});

        VisTextButton btnCut = new VisTextButton("剪切", btnStyle);
        btnCut.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					textArea.cut();
					refreshLayout();
					hidePopupMenu();
				}
			});

        VisTextButton btnAll = new VisTextButton("全选", btnStyle);
        btnAll.addListener(new ClickListener() {
				@Override public void clicked(InputEvent event, float x, float y) {
					textArea.selectAll();
					hidePopupMenu();
					// 全选后允许直接拖动选区
					isSelectionMode = true; 
				}
			});

        popupMenu.add(btnCopy).pad(5);
        popupMenu.add(btnPaste).pad(5);
        popupMenu.add(btnCut).pad(5);
        popupMenu.add(btnAll).pad(5);
        popupMenu.pack(); 
        popupMenu.setVisible(false);
    }

    private void showPopupMenu(float x, float y) {
        if (getStage() == null) return;

        if (popupMenu.getStage() == null) {
            getStage().addActor(popupMenu);
        }
        popupMenu.toFront();
        popupMenu.setVisible(true);

        Vector2 stagePos = textArea.localToStageCoordinates(new Vector2(x, y));

        // 显示在手指上方
        float menuX = stagePos.x - popupMenu.getWidth() / 2;
        float menuY = stagePos.y + 50;

        if (menuX < 0) menuX = 0;
        if (menuX + popupMenu.getWidth() > getStage().getWidth()) {
            menuX = getStage().getWidth() - popupMenu.getWidth();
        }
        if (menuY + popupMenu.getHeight() > getStage().getHeight()) {
            menuY = stagePos.y - 50; 
        }

        popupMenu.setPosition(menuX, menuY);
    }

    private void hidePopupMenu() {
        if (popupMenu != null) {
            popupMenu.setVisible(false);
            popupMenu.remove(); 
        }
        // 注意：关闭菜单时不要重置 isSelectionMode，
        // 否则用户想再次拖动选区时会变成滚动页面
        // isSelectionMode 应该在点击空白处时重置
    }

    private void setupInteraction() {
        // 配置手势：缩短长按时间，增加防抖范围
        textArea.addListener(new ActorGestureListener(20, 0.4f, 0.5f, 0.15f) {
				@Override
				public boolean longPress(Actor actor, float x, float y) {
					// 1. 触发长按
					hasLongPressed = true;
					isSelectionMode = true;
					scrollPane.setFlickScroll(false); // 禁止页面滚动，允许文字选区拖动
					Gdx.input.vibrate(50);

					// [修改] 这里不弹菜单！
					// 让用户继续拖动手指去调整选区，或者直接松手
					return true;
				}
			});

        textArea.addListener(new InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					// 重置长按标记
					hasLongPressed = false;

					// 点击时如果菜单开着，先关掉
					if (popupMenu.isVisible()) {
						hidePopupMenu();
					}
					return true;
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					// 只有进入选择模式（长按后）才检测自动滚动
					if (isSelectionMode) {
						checkAutoScroll(event.getStageY());
					}
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					autoScrollSpeed = 0f;

					// [核心修改] 松手时，如果是长按触发的，才显示菜单
					if (hasLongPressed) {
						showPopupMenu(x, y);
						hasLongPressed = false; // 消费掉这个标记
					}
				}
			});
    }

    private void checkAutoScroll(float stageY) {
        float topEdge = scrollPane.localToStageCoordinates(new Vector2(0, scrollPane.getHeight())).y;
        float bottomEdge = scrollPane.localToStageCoordinates(new Vector2(0, 0)).y;

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

    private void refreshLayout() {
        Gdx.app.postRunnable(() -> {
            scrollPane.validate(); 
            textArea.forceCalc(); 
            textArea.invalidateHierarchy();
            scrollPane.layout(); 
        });
    }

    public void setText(String text) {
        textArea.setText(text);
        refreshLayout();
        Gdx.app.postRunnable(() -> {
            scrollPane.setScrollY(0);
            scrollPane.updateVisualScroll();
        });
    }

    public String getText() {
        return textArea.getText();
    }

    private class CodeTextArea extends VisTextArea {
        public CodeTextArea(String text) {
            super(text);
        }

        @Override
        public float getPrefHeight() {
            if (getText() == null || getStyle() == null || getStyle().font == null) {
                return 15 * 20;
            }
            float rows = getLines();
            if (rows < 15) rows = 15;
            return rows * getStyle().font.getLineHeight() + 20;
        }

        public void forceCalc() {
            super.calculateOffsets();
        }
    }
}
