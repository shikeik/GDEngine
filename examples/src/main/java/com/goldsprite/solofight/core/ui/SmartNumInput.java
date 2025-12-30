package com.goldsprite.solofight.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.function.Consumer;

/**
 * 智能数值输入组件 (复刻自 BioInspector)
 * 特性：
 * 1. 显示数值文本，点击弹出数字小键盘 (SimpleNumPad)。
 * 2. 左侧附带 "<>" 按钮，按住左右拖动可快速调整数值。
 */
public class SmartNumInput extends VisTable {

	private final VisTextField textField;
	private float currentValue;
	private final float step;
	private final Consumer<Float> onChange;

	// 全局共享的一个数字键盘实例
	private static SimpleNumPad sharedNumPad;

	public SmartNumInput(String label, float initValue, float step, Consumer<Float> onChange) {
		this.currentValue = initValue;
		this.step = step;
		this.onChange = onChange;

		// 左侧 Label
		add(new VisLabel(label)).width(80).left(); // 固定宽度确保对齐

		// 中间 拖拽按钮
		VisTextButton dragBtn = new VisTextButton("<>");
		dragBtn.addListener(new InputListener() {
			float lastStageX;

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				lastStageX = event.getStageX();
				return true;
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				float currentStageX = event.getStageX();
				float dx = currentStageX - lastStageX;
				if (dx == 0) return;

				// 拖拽更新数值
				updateValue(currentValue + dx * step);
				lastStageX = currentStageX;
			}
		});
		add(dragBtn).width(24).padRight(2);

		// 右侧 输入框
		String valStr = fmt(initValue);
		textField = new VisTextField(valStr);
		textField.setAlignment(Align.center);
		textField.setDisabled(true); // 禁用原生键盘，使用自定义数字键盘

		textField.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				showNumPad();
			}
		});

		add(textField).growX().minWidth(50).row();
	}

	private void updateValue(float newVal) {
		this.currentValue = newVal;
		textField.setText(fmt(newVal));
		if (onChange != null) {
			onChange.accept(newVal);
		}
	}

	private String fmt(float val) {
		String s = String.format("%.2f", val);
		if (s.endsWith(".00")) return s.substring(0, s.length() - 3);
		if (s.endsWith("0")) return s.substring(0, s.length() - 1);
		return s;
	}

	private void showNumPad() {
		if (sharedNumPad == null) {
			sharedNumPad = new SimpleNumPad();
		}
		// 如果键盘还没添加到当前 Stage，加进去
		if (sharedNumPad.getStage() != getStage()) {
			if (getStage() != null) getStage().addActor(sharedNumPad);
		}
		
		// 确保在最上层
		sharedNumPad.toFront();

		sharedNumPad.show(textField, (res) -> {
			try {
				if (res.isEmpty() || res.equals("-")) return;
				updateValue(Float.parseFloat(res));
			} catch (Exception ignored) {}
		});
	}

	// ==========================================
	// 内部类：SimpleNumPad (移植并简化)
	// ==========================================
	public static class SimpleNumPad extends Table {
		private VisTextField targetField;
		private String buffer = "";
		private boolean isFirstInput = true;
		private Consumer<String> onConfirm;
		private final float btnW = 40, btnH = 25;

		private final InputListener stageListener = new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				Actor target = event.getTarget();
				// 点击键盘外部且不是目标输入框时关闭
				if (target != SimpleNumPad.this && !SimpleNumPad.this.isAscendantOf(target) && target != targetField) {
					setVisible(false);
				}
				return false;
			}
		};

		public SimpleNumPad() {
			super(VisUI.getSkin());
			setBackground("window-bg");
			pad(5);

			int[] nums = {7, 8, 9, 4, 5, 6, 1, 2, 3};
			for (int i = 0; i < nums.length; i++) {
				final int num = nums[i];
				addButton(String.valueOf(num), () -> append(String.valueOf(num)));
				if ((i + 1) % 3 == 0) row();
			}
			addButton(".", () -> append("."));
			addButton("0", () -> append("0"));
			addButton("C", this::clearBuffer);
			row();

			addButton("-", this::toggleNegative);
			VisTextButton okBtn = new VisTextButton("OK");
			okBtn.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					confirm();
				}
			});
			add(okBtn).colspan(2).fillX().height(btnH).pad(1);
			pack();
			setVisible(false);
		}

		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (getStage() == null) return;
			if (visible) {
				getStage().addCaptureListener(stageListener);
				toFront();
			} else {
				getStage().removeListener(stageListener);
			}
		}

		private void addButton(String text, Runnable action) {
			VisTextButton btn = new VisTextButton(text);
			btn.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) { action.run(); }
			});
			add(btn).size(btnW, btnH).pad(1);
		}

		public void show(VisTextField target, Consumer<String> onConfirmCallback) {
			this.targetField = target;
			this.onConfirm = onConfirmCallback;
			this.buffer = target.getText();
			this.isFirstInput = true;

			// 智能定位：显示在目标下方
			Vector2 pos = target.localToStageCoordinates(new Vector2(0, 0));
			float x = pos.x + target.getWidth() / 2 - getWidth() / 2;
			float y = pos.y - getHeight();

			// 边界检查
			if (y < 0) y = pos.y + target.getHeight(); // 翻转到上方
			if (x < 0) x = 0;

			setPosition(x, y);
			setVisible(true);
		}

		private void append(String str) {
			if (isFirstInput) { buffer = ""; isFirstInput = false; }
			buffer += str;
			if (targetField != null) targetField.setText(buffer);
		}

		private void clearBuffer() {
			buffer = "";
			if (targetField != null) targetField.setText("");
			isFirstInput = false;
		}

		private void toggleNegative() {
			if (isFirstInput) { buffer = ""; isFirstInput = false; }
			if (buffer.startsWith("-")) buffer = buffer.substring(1);
			else buffer = "-" + buffer;
			if (targetField != null) targetField.setText(buffer);
		}

		private void confirm() {
			if (buffer.isEmpty()) buffer = "0";
			if (onConfirm != null) onConfirm.accept(buffer);
			setVisible(false);
		}
	}
}
