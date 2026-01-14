package com.goldsprite.gdengine.ui.input;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.ui.widget.SimpleNumPad;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.function.Consumer;

public class SmartVector2Input extends SmartInput<Vector2> {

	private final AxisEditor editorX;
	private final AxisEditor editorY;

	private static SimpleNumPad sharedNumPad;

	public SmartVector2Input(String label, Vector2 initValue, Consumer<Vector2> onChange) {
		super(label, initValue, onChange);

		VisTable container = new VisTable();
		// [核心修复] 1. 强制右对齐
		container.right();

		// 2. 占位符 (让内容尽量靠右)
		container.add().growX();

		// 3. X轴编辑器 (给 minWidth 防止被压扁)
		editorX = new AxisEditor("X", initValue.x, val -> {
			value.x = val;
			notifyChange();
		});
		// [核心修复] width 改为 prefWidth, 并增加 minWidth(80) 铁布衫
		container.add(editorX.root).width(120).minWidth(80).padRight(5);

		// 4. Y轴编辑器
		editorY = new AxisEditor("Y", initValue.y, val -> {
			value.y = val;
			notifyChange();
		});
		container.add(editorY.root).width(120).minWidth(80);

		addContent(container);
	}

	private void notifyChange() {
		if (onChange != null) onChange.accept(value);
		// Vector2 是引用类型，Command 需要记录深拷贝，这里简化处理，
		// 实际 Command 系统可能需要在这里 snapshot 一个新 Vector2
		// notifyCommand(oldVec, new Vector2(value));
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		editorX.setReadOnly(readOnly);
		editorY.setReadOnly(readOnly);
	}

	@Override
	public void updateUI() {
		// 只有数值变化时才更新 UI，防止正在输入时被覆盖
		if (editorX != null) editorX.setValue(value.x);
		if (editorY != null) editorY.setValue(value.y);
	}

	// =================================================================================
	// 内部类：单轴编辑器 (封装了 Label + DragButton + TextField)
	// =================================================================================
	private class AxisEditor {
		VisTable root;
		VisTextField textField;
		VisTextButton dragBtn;
		Consumer<Float> onAxisChange;
		float curVal;
		final float step = 0.1f; // 拖拽步进

		public AxisEditor(String axisLabel, float initVal, Consumer<Float> callback) {
			this.curVal = initVal;
			this.onAxisChange = callback;

			root = new VisTable();

			// 轴标签 (X/Y)
			VisLabel lbl = new VisLabel(axisLabel);
			lbl.setColor(axisLabel.equals("X") ? 0.8f : 0.4f, axisLabel.equals("Y") ? 0.8f : 0.4f, 0.4f, 1f); // 简单着色区分
			root.add(lbl).padRight(4);

			// 拖拽按钮
			dragBtn = new VisTextButton("<>");
			setupDragListener();
			root.add(dragBtn).width(28).padRight(2);

			// 输入框
			textField = new VisTextField(fmt(initVal));
			textField.setAlignment(Align.left);
			textField.setDisabled(true); // 禁用键盘输入，改为点击弹窗

			textField.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					if (!textField.isDisabled()) showNumPad();
				}
			});

			root.add(textField).growX();
		}

		public void setValue(float val) {
			// 简单的防抖，避免重绘文本
			if (Math.abs(curVal - val) > 0.0001f) {
				curVal = val;
				textField.setText(fmt(val));
			}
		}

		public void setReadOnly(boolean readOnly) {
			dragBtn.setDisabled(readOnly);
			dragBtn.setTouchable(readOnly ? Touchable.disabled : Touchable.enabled);
			textField.setColor(1, 1, 1, readOnly ? 0.5f : 1f);
		}

		private void setupDragListener() {
			dragBtn.addListener(new InputListener() {
				float lastStageX;
				float startDragValue;
				ScrollPane parentScroll;

				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					if (dragBtn.isDisabled()) return false;
					lastStageX = event.getStageX();
					startDragValue = curVal;

					// 禁用父级滚动
					parentScroll = findParentScrollPane(dragBtn);
					if (parentScroll != null) {
						parentScroll.setCancelTouchFocus(false);
						event.getStage().setScrollFocus(null);
					}
					return true;
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					float currentStageX = event.getStageX();
					float dx = currentStageX - lastStageX;
					if (dx == 0) return;

					float newVal = curVal + dx * step;
					// 更新内部状态
					setValue(newVal);
					// 回调外部更新 Vector2
					if (onAxisChange != null) onAxisChange.accept(newVal);

					lastStageX = currentStageX;
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					if (parentScroll != null) {
						parentScroll.setCancelTouchFocus(true);
						parentScroll = null;
					}
					// notifyCommand(startDragValue, curVal); // 如果需要撤销重做支持，这里需要透传出去
				}
			});
		}

		private void showNumPad() {
			if (dragBtn.isDisabled()) return;
			if (sharedNumPad == null) sharedNumPad = new SimpleNumPad();
			if (getStage() != null && sharedNumPad.getStage() != getStage()) {
				getStage().addActor(sharedNumPad);
			}
			sharedNumPad.toFront();

			sharedNumPad.show(textField, (res) -> {
				try {
					if (res.isEmpty() || res.equals("-")) return;
					float newVal = Float.parseFloat(res);
					setValue(newVal);
					if (onAxisChange != null) onAxisChange.accept(newVal);
				} catch (Exception ignored) {}
			});
		}
	}

	// --- 辅助方法 ---

	private ScrollPane findParentScrollPane(Actor start) {
		Actor current = start;
		while (current != null) {
			if (current instanceof ScrollPane) return (ScrollPane) current;
			current = current.getParent();
		}
		return null;
	}

	private String fmt(float val) {
		if (val == (int) val) return String.valueOf((int) val);
		return String.format("%.2f", val);
	}
}
