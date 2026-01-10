package com.goldsprite.dockablewindow.core;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.goldsprite.gdengine.input.Event;

import java.util.function.Function;
import java.util.function.Supplier;

public class DockableWindow extends FloatingWindow {
	private final WindowStatus windowStatus = WindowStatus.Floating;
	private DockingEdge dockingEdge = DockingEdge.None;
	public final float dockingShreshold = 0;

	private final float[] floatingRect = new float[4];

	private boolean leaveDockingEdge = true;
	private boolean banDocking;

	public final Event<Object> dockingEdgeChangeListener = new Event<Object>();
	public Function<DockingEdge, Boolean> isCancelDockingEdge;

	public DockableWindow(String titleText, Skin skin) {
		super(titleText, skin);

		registerDockingEventListener();
	}

	public void registerDockingEventListener() {
		addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (banDocking) return false;

				if (isDocking() && enterDraggingTitleBar) {
					cancelDocking(x, y);
				}
				return true;
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if (banDocking) return;
				docking(x, y);
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				enableTitleDrag = true;
			}
		});
	}

	Vector2 parentCursor = new Vector2();

	public void cancelDocking(float localX, float localY) {
		localToParentCoordinates(parentCursor.set(localX, localY));

		setDockingEdge(DockingEdge.None);
		float xRatio = localX / getWidth();
		float yRatio = getHeight() - localY;//计算取消停靠前cursorY与topY的距离并在取消停靠后还原cursorY位置
		setSize(floatingRect[2], floatingRect[3]);
		setPosition(parentCursor.x - xRatio * getWidth(), parentCursor.y - (getHeight() - yRatio));
		clampPos();
		startX = getX();
	}

	@Override
	protected void windowEdgeResize(Vector2 hoverEdge, float deltaX, float deltaY, float startWidth, float startHeight) {
		boolean done = dockingEdge == DockingEdge.Left && hoverEdge.x > 0;
		if (dockingEdge == DockingEdge.Right && hoverEdge.x < 0) done = true;
		if (dockingEdge == DockingEdge.Top && hoverEdge.y < 0) done = true;
		if (dockingEdge == DockingEdge.Bottom && hoverEdge.y > 0) done = true;
		if (dockingEdge == DockingEdge.None) done = true;
		if (done) super.windowEdgeResize(hoverEdge, deltaX, deltaY, startWidth, startHeight);
	}

	Vector2 parentPos = new Vector2();
	float[] validArea = new float[4];
	public Supplier<float[]> getValidArea = () -> {
		validArea[0] = dockingShreshold;
		validArea[1] = dockingShreshold + 0;
		validArea[2] = getValidParentSize().x - dockingShreshold;
		validArea[3] = getValidParentSize().y - dockingShreshold;
		return validArea;
	};
	private void docking(float localX, float localY) {
		if (dockingEdge != DockingEdge.None) return;

		localToParentCoordinates(parentPos.set(localX, localY));
		float x = getX(), newX = x;
		float y = getY(), newY = y;
		float width = getWidth(), newWidth = width;
		float height = getHeight(), newHeight = height;
		floatingRect[0] = x;
		floatingRect[1] = y;
		floatingRect[2] = width;
		floatingRect[3] = height;

		float[] validArea = getValidArea.get();
		float validWidth = validArea[2] - validArea[0];
		float validHeight = validArea[3] - validArea[1];

		float left = x;
		float bottom = y;
		float right = x + width;
		float top = y + height;
		float edgeLeft = validArea[0];
		float edgeBottom = validArea[1];
		float edgeRight = validArea[2];
		float edgeTop = validArea[3];

		float diffLeftX = left - edgeLeft;
		float diffRightX = edgeRight - right;
		float diffTopY = edgeTop - top;
		float diffBottomY = bottom - edgeBottom;

		float shreshold = 15;
		boolean isLeft = parentPos.x < edgeLeft + shreshold;
		boolean isRight = parentPos.x > edgeRight - shreshold;
		boolean isTop = parentPos.y > edgeTop - shreshold;
		boolean isBottom = parentPos.y < edgeBottom + shreshold;

		float disX = isLeft ? diffLeftX : (isRight ? diffRightX : 0);
		float disY = isBottom ? diffBottomY : (isTop ? diffTopY : 0);

		if (disX == 0 && disY == 0) {
			leaveDockingEdge = true;
			return;
		} else if (!leaveDockingEdge) {
			return;
		}

		DockingEdge dockingEdge = DockingEdge.None;
		//横向
		if (Math.abs(disX) > Math.abs(disY)) {
			newX = isLeft ? edgeLeft : edgeRight - width;
			newY = validArea[1];
			newHeight = validHeight;
			dockingEdge = isLeft ? DockingEdge.Left : DockingEdge.Right;
		}
		//纵向
		else {
			newY = isBottom ? edgeBottom : edgeTop - height;
			newX = validArea[0];
			newWidth = validWidth;
			dockingEdge = isBottom ? DockingEdge.Bottom : DockingEdge.Top;
		}

		if (!setDockingEdge(dockingEdge)) return;
		enableTitleDrag = false;
		leaveDockingEdge = false;
		setSize(newWidth, newHeight);
		setPosition(newX, newY);
	}

	Object[] data = new Object[3];

	public DockingEdge getDockingEdge() {
		return this.dockingEdge;
	}

	public boolean setDockingEdge(DockingEdge dockingEdge) {
		if (isCancelDockingEdge != null && isCancelDockingEdge.apply(dockingEdge)) return false;

		DockingEdge oldDockingEdge = this.dockingEdge;
		this.dockingEdge = dockingEdge;

		data[0] = this;
		if (dockingEdge.equals(DockingEdge.None)) {
			data[1] = oldDockingEdge;
			data[2] = false;
			dockingEdgeChangeListener.invoke(data);
		} else {
			data[1] = dockingEdge;
			data[2] = true;
			dockingEdgeChangeListener.invoke(data);
		}

		return true;
	}

	public boolean isFloating() {
		return dockingEdge == DockingEdge.None;
	}

	public boolean isDocking() {
		return dockingEdge != DockingEdge.None;
	}

	public boolean isBanDocking() {
		return this.banDocking;
	}

	public void setBanDocking(boolean banDocking) {
		this.banDocking = banDocking;
	}
}
