package com.goldsprite.solofight.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.assets.FontUtils;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class BioCodeEditor extends VisTable {

	private CodeTextArea textArea;
	private VisLabel lineNumbers; // 行号显示
	private VisScrollPane scrollPane;
	private VisTable popupMenu;
	private VisTable contentTable; // 内部容器，包含行号和文本

	// 交互状态
	private boolean isSelectionMode = false;
	private boolean hasLongPressed = false;
	private float autoScrollSpeed = 0f;

	// 回调
	private Runnable onSaveCallback;

	public BioCodeEditor() {
		super();
		build();
	}

	public void setOnSave(Runnable onSave) {
		this.onSaveCallback = onSave;
	}

	private void build() {
		setBackground("window-bg");

		// 1. 样式定制 (光标高亮)
		VisTextField.VisTextFieldStyle baseStyle = VisUI.getSkin().get(VisTextField.VisTextFieldStyle.class);
		VisTextField.VisTextFieldStyle customStyle = new VisTextField.VisTextFieldStyle(baseStyle);

		// 字体设置
		customStyle.font = FontUtils.generateAutoClarity(35);
		customStyle.font.getData().markupEnabled = true;
		customStyle.font.getData().setScale(customStyle.font.getData().scaleX * 0.5f);

		// [修复3] 光标定制：亮黄、加粗
		Pixmap p = new Pixmap(3, (int)customStyle.font.getLineHeight(), Pixmap.Format.RGBA8888);
		p.setColor(Color.YELLOW);
		p.fill();
		customStyle.cursor = new TextureRegionDrawable(new Texture(p));
		customStyle.selection = new TextureRegionDrawable(new Texture(createColorPixmap(new Color(0, 0.5f, 1f, 0.3f)))); // 选区颜色优化

		// 2. 初始化组件
		textArea = new CodeTextArea("");
		textArea.setStyle(customStyle);
		// [修复4] 允许输入 Tab (禁用焦点遍历)
		textArea.setFocusTraversal(false);

		// [修复1] 行号组件
		lineNumbers = new VisLabel("1");
		lineNumbers.setStyle(new VisLabel.LabelStyle(customStyle.font, Color.GRAY));
		lineNumbers.setAlignment(Align.topRight);

		// 3. 布局组装
		// contentTable 放入 ScrollPane，保证行号和文本一起滚动
		contentTable = new VisTable();
		contentTable.add(lineNumbers).top().right().padRight(10).padTop(3); // 行号略微下移对齐
		contentTable.add(textArea).grow().top();

		scrollPane = new VisScrollPane(contentTable);
		scrollPane.setScrollBarPositions(false, false);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setFlickScroll(false);
		scrollPane.setOverscroll(false, false);
		scrollPane.setCancelTouchFocus(false);

		createPopupMenu();
		setupInteraction();

		this.add(scrollPane).grow();
	}

	private Pixmap createColorPixmap(Color c) {
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(c); p.fill(); return p;
	}

	private void setupInteraction() {
		// [修复2] 监听文本变化，实时更新高度和行号
		textArea.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				updateLayoutAndLineNumbers();
			}
		});

		// [修复5] 监听 Ctrl+S
		textArea.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if (keycode == Input.Keys.S && isCtrlPressed()) {
					if (onSaveCallback != null) {
						onSaveCallback.run();
						// 简单的视觉反馈
						textArea.setColor(Color.GREEN);
						textArea.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.color(Color.WHITE, 0.3f));
					}
					return true;
				}
				return false;
			}

			private boolean isCtrlPressed() {
				return Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
			}
		});

		// 手势逻辑 (保持原有逻辑)
		textArea.addListener(new ActorGestureListener(20, 0.4f, 0.5f, 0.15f) {
			@Override
			public boolean longPress(Actor actor, float x, float y) {
				hasLongPressed = true;
				isSelectionMode = true;
				scrollPane.setFlickScroll(false);
				Gdx.input.vibrate(50);
				return true;
			}
		});

		textArea.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				hasLongPressed = false;
				if (popupMenu.isVisible()) hidePopupMenu();
				return true;
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if (isSelectionMode) checkAutoScroll(event.getStageY());
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				autoScrollSpeed = 0f;
				if (hasLongPressed) {
					showPopupMenu(x, y);
					hasLongPressed = false;
				}
			}
		});
	}

	// [核心] 刷新布局与行号
	private void updateLayoutAndLineNumbers() {
		// 1. 生成行号文本
		int lines = textArea.getLines();
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= lines; i++) {
			sb.append(i).append('\n');
		}
		// 补一行防止甚至滚动不到最后
		sb.append("~");

		// 只有行数变了才刷新文本 (优化)
		String newNums = sb.toString();
		if (!newNums.equals(lineNumbers.getText().toString())) {
			lineNumbers.setText(newNums);
		}

		// 2. 强制刷新高度
		// 关键：Scene2D 的 TextArea 不会自动通知 ScrollPane 内容变大了
		// 我们需要 invalidate 内部容器，让 ScrollPane 重新测量
		Gdx.app.postRunnable(() -> {
			textArea.invalidateHierarchy();
			contentTable.invalidateHierarchy(); // 强制 Table 重新布局
			scrollPane.layout(); // 强制 ScrollPane 计算滚动条

			// 如果光标在最底下，自动跟随滚动? (可选)
			// scrollPane.scrollTo(0, textArea.getCursorY(), 0, 0);
		});
	}

	// ... (createPopupMenu, showPopupMenu, hidePopupMenu, checkAutoScroll, act 保持不变) ...
	// 为了节省篇幅，这里复用之前的菜单逻辑，请保留原有的这些方法实现

	private void createPopupMenu() {
		popupMenu = new VisTable();
		try { popupMenu.setBackground(VisUI.getSkin().getDrawable("window-bg")); }
		catch (Exception e) { popupMenu.setBackground(VisUI.getSkin().getDrawable("button")); }

		String btnStyle = "default";

		// 简化：复用原有的按钮逻辑
		VisTextButton btnCopy = new VisTextButton("复制", btnStyle);
		btnCopy.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { textArea.copy(); hidePopupMenu(); }});

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
					updateLayoutAndLineNumbers(); // 粘贴后必须刷新布局
				}
				hidePopupMenu();
			}
		});

		VisTextButton btnCut = new VisTextButton("剪切", btnStyle);
		btnCut.addListener(new ClickListener() {
			@Override public void clicked(InputEvent event, float x, float y) {
				textArea.cut();
				updateLayoutAndLineNumbers(); // 粘贴后必须刷新布局
				hidePopupMenu();
			}
		});

		VisTextButton btnAll = new VisTextButton("全选", btnStyle);
		btnAll.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { textArea.selectAll(); hidePopupMenu(); isSelectionMode = true; }});

		popupMenu.add(btnCopy).pad(5);
		popupMenu.add(btnPaste).pad(5);
		popupMenu.add(btnAll).pad(5);
		popupMenu.pack();
		popupMenu.setVisible(false);
	}

	private void showPopupMenu(float x, float y) {
		if (getStage() == null) return;
		if (popupMenu.getStage() == null) getStage().addActor(popupMenu);
		popupMenu.toFront(); popupMenu.setVisible(true);
		Vector2 stagePos = textArea.localToStageCoordinates(new Vector2(x, y));
		float menuX = stagePos.x - popupMenu.getWidth() / 2;
		float menuY = stagePos.y + 50;
		// 边界检查省略，可复用之前的
		popupMenu.setPosition(menuX, menuY);
	}

	private void hidePopupMenu() {
		if (popupMenu != null) popupMenu.setVisible(false);
	}

	private void checkAutoScroll(float stageY) {
		float topEdge = scrollPane.localToStageCoordinates(new Vector2(0, scrollPane.getHeight())).y;
		float bottomEdge = scrollPane.localToStageCoordinates(new Vector2(0, 0)).y;
		float threshold = 60f; float maxSpeed = 15f;
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
			if (nextY < 0) nextY = 0; if (nextY > maxY) nextY = maxY;
			scrollPane.setScrollY(nextY);
		}
	}

	public void setText(String text) {
		textArea.setText(text);
		updateLayoutAndLineNumbers(); // 初始化时也要刷新
		Gdx.app.postRunnable(() -> {
			scrollPane.setScrollY(0);
			scrollPane.updateVisualScroll();
		});
	}

	public String getText() {
		return textArea.getText();
	}

	// 内部类 CodeTextArea
	private class CodeTextArea extends VisTextArea {
		public CodeTextArea(String text) { super(text); }

		@Override
		public float getPrefHeight() {
			// [核心修复] 强制高度计算逻辑
			// 行数 * 行高 + Padding
			float rows = Math.max(getLines(), 15); // 至少 15 行
			return rows * getStyle().font.getLineHeight() + 20;
		}
	}
}
