package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Cullable;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.kotcrab.vis.ui.widget.VisLabel;

/**
 * 自由拖拽查看器 (仿地图/CAD操作)
 * 特性：
 * 1. 按住左键自由拖拽 (360度)
 * 2. Ctrl+滚轮缩放内容
 * 3. 限制边界，防止内容拖丢
 */
public class FreePanViewer extends WidgetGroup implements Cullable {
	private Actor content;
	private final Rectangle cullingArea = new Rectangle();

	// 缩放设置
	private float scale = 1.0f;
	private float minScale = 0.5f; // 最小 0.5 (之前太小了)
	private float maxScale = 1.5f; // 最大 1.5 (之前太大了)

	// 拖拽状态
	private float lastX, lastY;

	public FreePanViewer(Actor content) {
		this.content = content;
		addActor(content);

		// 核心交互监听
		addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				// 只响应左键
				if (button == Input.Buttons.LEFT) {
					lastX = x;
					lastY = y;
					// 请求焦点，确保能接收 scroll 事件
					getStage().setScrollFocus(FreePanViewer.this);
					return true;
				}
				return false;
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				// 计算增量 (注意：content 移动后，x/y 坐标系也会变，所以要用 stage 坐标或者相对计算)
				// 这里简单处理：直接累加偏移量
				float dx = x - lastX;
				float dy = y - lastY;

				moveContent(dx, dy);

				// 更新 lastX/Y (因为内容移动了，本地坐标系下的触点也变了，这里逻辑需要小心)
				// 其实最简单的做法是：touchDragged 的 x,y 是相对于 Listener Actor (this) 的
				// 只要 this 不动，x,y 就是准的。
				// 我们移动的是 content (子 Actor)，FreePanViewer (this) 本身不动。
				// 所以可以直接用。
				lastX = x;
				lastY = y;
			}

			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				// 仅当按住 Ctrl 时缩放
				if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
					doZoom(amountY, x, y);
					return true; // 【关键】吃掉事件，防止冒泡给外层 ScrollPane
				}
				return false; // 没按 Ctrl，放行（允许外层滚动）
			}
		});
	}

	private void moveContent(float dx, float dy) {
		content.moveBy(dx, dy);
		clampPosition();
	}

	private void doZoom(float amountY, float pivotX, float pivotY) {
		// amountY: 1 (缩小), -1 (放大)
		float zoomSpeed = 0.1f;
		float newScale = scale - amountY * zoomSpeed;

		newScale = MathUtils.clamp(newScale, minScale, maxScale);

		if (newScale != scale) {
			// 如果是 Label，优先使用 setFontScale 保证清晰度
			// 但 setFontScale 会改变 Label 的 PrefSize，导致排版变化
			// 对于代码预览，直接整体 setScale 性能更好且手感更平滑

			// 简单的中心缩放逻辑 (或者基于鼠标位置)
			// 这里简化为：直接改 scale，然后 clamp 位置
			scale = newScale;
			content.setScale(scale);

			// 如果是 Label，还需要通知它重新计算大小（如果用了 fontScale）
			// 如果是 setScale，则是视觉缩放

			// 这里针对 CodeViewer 特化：如果是 VisLabel，我们改 FontScale 吗？
			// 改 FontScale 会导致换行变化，代码编辑器通常不希望换行变化。
			// 建议：直接用 Actor Scale。

			if (content instanceof VisLabel conTable) {
				conTable.setFontScale(scale);
				conTable.invalidate(); // 触发布局重算
				conTable.pack();       // 重新计算宽高
			} else {
				content.setScale(scale);
			}

			clampPosition();
		}
	}

	/** 限制内容不跑出视野太远 */
	private void clampPosition() {
		float viewW = getWidth();
		float viewH = getHeight();
		float contentW = content.getWidth() * content.getScaleX();
		float contentH = content.getHeight() * content.getScaleY();

		// 策略：允许拖拽出边界，但至少保留 50px 在视野内
		float margin = 50f;

		float x = content.getX();
		float y = content.getY();

		if (x > viewW - margin) x = viewW - margin;
		if (x + contentW < margin) x = margin - contentW;

		if (y > viewH - margin) y = viewH - margin;
		if (y + contentH < margin) y = margin - contentH;

		content.setPosition(x, y);
	}

	@Override
	public void layout() {
		// 第一次布局时，让内容对齐左上角
		if (content.getWidth() == 0) {
			if (content instanceof VisLabel conTable) {
				conTable.pack(); // 计算内容实际大小
			}
			content.setPosition(0, getHeight() - content.getHeight());
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		// 开启裁剪 (Scissor)
		if (clipBegin()) {
			super.draw(batch, parentAlpha);
			batch.flush();
			clipEnd();
		}
	}

	// --- Cullable 实现 (配合裁剪) ---
	@Override
	public void setCullingArea(Rectangle cullingArea) {
		this.cullingArea.set(cullingArea);
	}
}
