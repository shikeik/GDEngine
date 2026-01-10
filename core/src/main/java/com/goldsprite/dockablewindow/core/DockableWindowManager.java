package com.goldsprite.dockablewindow.core;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.goldsprite.gdengine.ui.widget.GSplitPane;

import java.util.*;
import java.util.function.Supplier;

public class DockableWindowManager {
	private final Stage stage;
	private final List<DockableWindow> windows = new ArrayList<>();
	private static final Map<DockingEdge, DockableWindow> dockingEdgeStatus = new LinkedHashMap<>();
	private final Map<DockableWindow, SplitedStatuDatas> beSplitedWindows = new HashMap<>();

	public DockableWindowManager(Stage stage) {
		this.stage = stage;
	}

	static {
		dockingEdgeStatus.put(DockingEdge.Top, null);
		dockingEdgeStatus.put(DockingEdge.Bottom, null);
		dockingEdgeStatus.put(DockingEdge.Left, null);
		dockingEdgeStatus.put(DockingEdge.Right, null);
	}

	float[] validArea = new float[4];
	public void addWindow(DockableWindow window) {
		if (windows.contains(window)) return;

		windows.add(window);
		window.dockingEdgeChangeListener.add((o) -> {
			onDockingEdgeChange(o);
		});
		window.isCancelDockingEdge = (e) -> {
			if (e.equals(DockingEdge.None)) return false;
			return dockingEdgeStatus.get(e) != null;
		};
		window.onTitleDraggingFinish.add((o) -> {
			Object[] objs = (Object[]) o;
			DockableWindow self = (DockableWindow) objs[0];
			Vector2 stageCursorPos = (Vector2) objs[1];
			handleEmbeding(self, stageCursorPos);
		});
		Supplier<float[]> getValidArea = () -> {
			DockableWindow left = dockingEdgeStatus.get(DockingEdge.Left);
			DockableWindow right = dockingEdgeStatus.get(DockingEdge.Right);
			DockableWindow bottom = dockingEdgeStatus.get(DockingEdge.Bottom);
			DockableWindow top = dockingEdgeStatus.get(DockingEdge.Top);
			float leftDockingWindowWidth = left == null ? 0 : left.getWidth();
			float rightDockingWindowWidth = right == null ? 0 : right.getWidth();
			float bottomDockingWindowWidth = bottom == null ? 0 : bottom.getHeight();
			float topDockingWindowWidth = top == null ? 0 : top.getHeight();
			validArea[0] = window.dockingShreshold + leftDockingWindowWidth;
			validArea[1] = window.dockingShreshold + bottomDockingWindowWidth;
			validArea[2] = window.getValidParentSize().x - window.dockingShreshold - rightDockingWindowWidth;
			validArea[3] = window.getValidParentSize().y - window.dockingShreshold - topDockingWindowWidth;
			return validArea;
		};
		window.getValidArea = getValidArea;
	}

	public void update(float delta) {
	}

	public void onDockingEdgeChange(Object o) {
		Object[] objs = (Object[]) o;
		DockableWindow window = (DockableWindow) objs[0];
		DockingEdge edge = (DockingEdge) objs[1];
		boolean done = (Boolean) objs[2];
		if (done){
			dockingEdgeStatus.put(edge, window);
		}
		else{
			dockingEdgeStatus.put(edge, null);
		}
	}

	Vector2 localPos = new Vector2();

	private void handleEmbeding(DockableWindow self, Vector2 stageCursorPos) {
		//从嵌入状态分离
		if (isSplitedWindow(self)) {
			SplitedStatuDatas selfOldData = beSplitedWindows.get(self);
			SplitedStatuDatas otherOldData = beSplitedWindows.get(selfOldData.other);
			GSplitPane gSplitPane = selfOldData.gSplitPane;
			DockableWindow gSplitContainer = selfOldData.container;
			Group parent = selfOldData.parent;
			DockableWindow other = selfOldData.other;

			//移出已分割窗口列表
			beSplitedWindows.remove(self);
			beSplitedWindows.remove(other);

			if (otherOldData.isDeep) {
				//从舞台移除容器
				parent.removeActor(gSplitContainer);

				gSplitPane.clear();

				gSplitContainer.clear();

				gSplitContainer.add(otherOldData.gSplitPane).grow();
				otherOldData.gSplitPane.setAbsoluteSplitAmount(otherOldData.gSplitPane.getSplitAmount());

				parent.addActor(gSplitContainer);
			}else {
				//从splitPane移出两窗口
				gSplitPane.clear();

				//从舞台移除容器
				parent.removeActor(gSplitContainer);

				//恢复other之前状态
				parent.addActor(other);
				other.setBanDocking(otherOldData.oldBanDocking);
				other.setEnableEdgeDrag(otherOldData.oldEnableEdgeDrag);
				other.setDockingEdge(otherOldData.oldDockingEdge);
				other.setBounds(otherOldData.oldBounds[0], otherOldData.oldBounds[1], otherOldData.oldBounds[2], otherOldData.oldBounds[3]);
			}
			//恢复self之前状态
			parent.addActor(self);
			self.setBanDocking(false);
			self.setEnableEdgeDrag(true);
			self.setDockingEdge(selfOldData.oldDockingEdge);
			self.setBounds(selfOldData.oldBounds[0], selfOldData.oldBounds[1], selfOldData.oldBounds[2], selfOldData.oldBounds[3]);

			dockingEdgeStatus.put(other.getDockingEdge(), otherOldData.isDeep ? gSplitContainer : other);
			return;
		}

		for (DockableWindow window : windows) {
			boolean isDeep = isSplitedWindow(window);
			SplitedStatuDatas deepWindowData = beSplitedWindows.get(window);
			DockableWindow other = isDeep ? deepWindowData.container : window;
			if (!other.isDocking() || self.isDocking()) continue;
			Group parent = self.getParent();

			other.stageToLocalCoordinates(localPos.set(stageCursorPos));
			Actor beSplitActor = other.hit(localPos.x, localPos.y, true);

//			String txt = "title: " + window.getTitleText()
//				+ ", hitActor: " + (beSplitActor == null ? "null" : beSplitActor.getClass().getName())
//				+ ", stageCursorPos: " + stageCursorPos
//				+ ", localPos: " + localPos
//				+ ", isHittable: " + window.isHittableByStage(stageCursorPos.x, stageCursorPos.y);
//			LogViewerService.log(txt);

			//拖动到已停靠窗口上进行嵌入
			if (!isSplitedWindow(self) && beSplitActor != null && window != self) {
				float xDiff = localPos.x - other.getWidth()/2f;
				float yDiff = localPos.y - other.getHeight()/2f;
				boolean isVerti = Math.abs(xDiff) < Math.abs(yDiff);
				boolean isFirst = isVerti ? yDiff > 0 : xDiff < 0;

				Skin skin = other.getSkin();
				DockingEdge dir = other.getDockingEdge();

				//创建容器
				DockableWindow gSplitContainer = null;
				if (isDeep) {
					gSplitContainer = deepWindowData.container;
					gSplitContainer.clear();
				}else{
					gSplitContainer = new DockableWindow("", skin);
					gSplitContainer.clear();
					gSplitContainer.registerEdgeDraggingListener(stage);
					gSplitContainer.setBackground((Drawable) null);
					gSplitContainer.setBounds(other.getX(), other.getY(), other.getWidth(), other.getHeight());
					gSplitContainer.setDockingEdge(dir);
				}

				//从父系移除两窗口
				parent.removeActor(!isDeep ? other : deepWindowData.container);
				parent.removeActor(self);

				//加入分割面板
				if (isDeep) {
					if (isVerti == deepWindowData.gSplitPane.isVertical()) deepWindowData.gSplitPane.setAbsoluteSplitAmount(deepWindowData.gSplitPane.getAbsoluteSplitAmount()/2f);
				}
				Actor otherPanel = !isDeep ? other : deepWindowData.gSplitPane;
				Actor first = isFirst ? self : otherPanel;
				Actor second = isFirst ? otherPanel : self;
				GSplitPane gSplitPane = new GSplitPane(first, second, isVerti, skin);

				//加入容器
				gSplitContainer.add(gSplitPane).grow();

				//添加回父系
				parent.addActor(gSplitContainer);

				SplitedStatuDatas otherOldData = new SplitedStatuDatas();
				otherOldData.container = gSplitContainer;
				otherOldData.gSplitPane = !isDeep ? gSplitPane : deepWindowData.gSplitPane;
				otherOldData.self = other;
				otherOldData.other = self;
				otherOldData.isDeep = isDeep;
				otherOldData.oldDockingEdge = other.getDockingEdge();
				otherOldData.oldBounds = new float[]{other.getX(), other.getY(), other.getWidth(), other.getHeight()};
				otherOldData.oldBanDocking = other.isBanDocking();
				otherOldData.oldEnableEdgeDrag = other.enableEdgeDrag;
				otherOldData.zIndex = isFirst? 1 : 0;
				otherOldData.parent = parent;
				otherOldData.isFirst = !isFirst;

				SplitedStatuDatas selfOldData = new SplitedStatuDatas();
				selfOldData.container = gSplitContainer;
				selfOldData.gSplitPane = gSplitPane;
				selfOldData.self = self;
				selfOldData.other = other;
				selfOldData.oldDockingEdge = self.getDockingEdge();
				selfOldData.oldBounds = new float[]{self.getX(), self.getY(), self.getWidth(), self.getHeight()};
				selfOldData.zIndex = isFirst ? 0 : 1;
				selfOldData.parent = parent;
				selfOldData.isFirst = isFirst;

				//禁用两窗口拖动拉伸
				other.setDockingEdge(dir);
				other.setBanDocking(true);
				if(!isDeep) other.setEnableEdgeDrag(false);
				//
				self.setDockingEdge(dir);
				self.setBanDocking(true);
				self.setEnableEdgeDrag(false);

				//加入已分割窗口列表
				beSplitedWindows.put(other, otherOldData);
				beSplitedWindows.put(self, selfOldData);

				dockingEdgeStatus.put(dir, gSplitContainer);
				return;
			}
		}
	}

	private boolean isSplitedWindow(DockableWindow window) {
		return beSplitedWindows.containsKey(window);
	}

	public static class SplitedStatuDatas {
		public Group parent;
		public DockableWindow container;
		public GSplitPane gSplitPane;
		public DockableWindow self, other;
		public float[] oldBounds;
		public DockingEdge oldDockingEdge;
		public boolean oldBanDocking;
		public boolean oldEnableEdgeDrag;
		public int zIndex;
		public boolean isDeep;
		public boolean isFirst;
	}
}
