package com.goldsprite.gdengine.screens.ecs.editor.mvp;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.badlogic.gdx.scenes.scene2d.Actor;

public abstract class EditorPanel extends VisTable {

	// --- 静态焦点管理器 ---
	private static EditorPanel currentActivePanel = null;
	private final Drawable whitePixel;
	protected VisTable contentTable;
	protected VisLabel titleLabel;
	// [新增] 引用标题栏 Table，以便控制显示隐藏
	protected VisTable titleBar;
	// [新增] 焦点状态
	protected boolean hasFocus = false;

	public EditorPanel(String title) {
		setBackground("window-bg");

		// 获取白点用于画线
		whitePixel = VisUI.getSkin().getDrawable("white");

		// 1. Title Bar
		titleBar = new VisTable(); // [修改] 赋值给成员变量
		titleBar.setBackground("button");

		titleLabel = new VisLabel(title);
		titleLabel.setAlignment(Align.left);
		titleLabel.setColor(Color.LIGHT_GRAY);

		titleBar.add(titleLabel).expandX().fillX().pad(2, 5, 2, 5);
		addTitleButtons(titleBar);

		// [修复] 增加 minHeight(26)，防止被压缩消失
		add(titleBar).growX().height(26).minHeight(26).row();

		// 2. Content
		contentTable = new VisTable();
		add(contentTable).grow();

		// [核心修复] 焦点管理与滚轮隔离
		setupFocusListener();
	}

	// 请求成为焦点（抢占逻辑）
	public static void requestFocus(EditorPanel panel) {
		// 如果已经是自己，啥都不做 (或者根据需求刷新一下)
		if (currentActivePanel == panel) return;

		// 1. 让旧的失去焦点
		if (currentActivePanel != null) {
			currentActivePanel.setFocusState(false);
		}

		// 2. 更新记录
		currentActivePanel = panel;

		// 3. 让新的获取焦点
		if (currentActivePanel != null) {
			currentActivePanel.setFocusState(true);
		}
	}

	// 供外部调用：清空所有焦点（比如点到了背景空白处）
	public static void clearAllFocus() {
		requestFocus(null);
	}

	// [新增] 控制标题栏显隐
	public void setHeaderVisible(boolean visible) {
		titleBar.setVisible(visible);
		// 重新触发布局，如果隐藏了，高度应为0
		getCell(titleBar).height(visible ? 26 : 0).minHeight(visible ? 26 : 0);
		invalidateHierarchy();
	}

	private void setupFocusListener() {
		addListener(new InputListener() {
			// [适配 PC]: 鼠标移入自动获取焦点
			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				// pointer == -1 表示纯鼠标移动
				if (pointer == -1) {
					requestFocus(EditorPanel.this);
				}
			}

			// [适配 手机/PC]: 点击/触摸按下瞬间获取焦点
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				// 只要点到了这个 Panel 范围内，就请求焦点
				requestFocus(EditorPanel.this);

				// 返回 false，表示我们只监听“被点到了”这件事，
				// 不消耗事件，让事件继续传递给子控件（比如里面的按钮、列表项）
				return false;
			}

			// [修改]: exit 方法现在主要用于 PC 移出时自动取消（可选）
			// 手机端通常没有 "移出" 的概念，除非是滑出去了。
			// 策略：手机端不通过 exit 取消，只通过“点击别的”来取消。
			// PC端：如果你希望鼠标移开就取消焦点，保留下面逻辑；
			// 如果希望 PC 端也是“点击锁定焦点”，则删掉 exit 逻辑。
			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				if (pointer == -1) { // PC 鼠标移出
					// 只有当移出的目标不是自己的子控件时，才放弃焦点
					if (toActor == null || !toActor.isDescendantOf(EditorPanel.this)) {
						// PC端行为选择：
						// 选项A (保持现状)：移出即失去焦点 -> requestFocus(null);
						// 选项B (像手机一样)：移出不失去，直到点别的 -> 什么都不写

						// 这里保留你原本的逻辑 (选项A):
						requestFocus(null);
					}
				}
			}
		});
	}

	// --- 实例方法：执行具体的视觉/逻辑变化 ---
	private void setFocusState(boolean active) {
		this.hasFocus = active;

		if (active) {
			// 获得焦点：视觉高亮 + 滚轮接管
			if (getStage() != null) {
				ScrollPane target = findChildScrollPane(this);
				if (target != null) {
					getStage().setScrollFocus(target);
				}
			}
		} else {
			// 失去焦点：取消高亮 + 释放滚轮
			// 注意：只有当 Stage 的 ScrollFocus 确实是自己内部那个 ScrollPane 时才清空
			// 防止把别的地方抢过去的焦点给清了
			if (getStage() != null) {
				Actor currentScroll = getStage().getScrollFocus();
				if (currentScroll != null && currentScroll.isDescendantOf(this)) {
					getStage().setScrollFocus(null);
				}
			}
		}
	}

	// [新增] 绘制高亮边框
	@Override
	public void draw(Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);

		if(batch instanceof NeonBatch neonBatch){
			if (hasFocus) {
				// 绘制黄色边框，表示活跃
				Color c = Color.YELLOW;
				float t = 2f; // 边框厚度
				float padding = 4;

				neonBatch.drawRect(
					getX() + padding,
					getY() + padding,
					getWidth() - padding * 2,
					getHeight() - padding * 2,
					0, t, c, false);
			}
		}else com.goldsprite.gdengine.log.Debug.logErrT("EditorPanel", "draw", "Batch is not NeonBatch");
	}

	protected void addTitleButtons(Table titleBar) {}

	protected void addContent(Actor content) {
		contentTable.add(content).grow();
	}

	public void setTitle(String title) {
		titleLabel.setText(title);
	}


	/**
	 * 递归查找当前 Actor 下的第一个 ScrollPane
	 */
	private ScrollPane findChildScrollPane(Actor parent) {
		if (parent instanceof ScrollPane) {
			return (ScrollPane) parent;
		}
		if (parent instanceof Group group) {
			for (Actor child : group.getChildren()) {
				ScrollPane found = findChildScrollPane(child);
				if (found != null) return found;
			}
		}
		return null;
	}
}
