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
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.badlogic.gdx.scenes.scene2d.Actor;

public abstract class EditorPanel extends VisTable {

    protected VisTable contentTable;
    protected VisLabel titleLabel;

	// [新增] 焦点状态
	protected boolean hasFocus = false;
	private final Drawable whitePixel;

    public EditorPanel(String title) {
        setBackground("window-bg");

		// 获取白点用于画线
		whitePixel = VisUI.getSkin().getDrawable("white");

        // 1. Title Bar
        VisTable titleBar = new VisTable();
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

	private void setupFocusListener() {
		addListener(new InputListener() {
			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				// 过滤点击，只处理移动
				if (pointer != -1) return;

				// 鼠标进入：视觉高亮
				hasFocus = true;

				if (getStage() != null) {
					// [核心修改] 自动查找肚子里的 ScrollPane
					ScrollPane target = findChildScrollPane(EditorPanel.this);
					if (target != null) {
						// 找到了才设置焦点
						getStage().setScrollFocus(target);
//						 com.goldsprite.gdengine.log.Debug.log("自动获取滚轮焦点: " + target);
					}
				}
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				// 1. 过滤点击
				if (pointer != -1) return;

				// 2. 过滤子组件：如果鼠标移到了子组件上（包括那个 ScrollPane），不要触发退出
				if (toActor != null && toActor.isDescendantOf(EditorPanel.this)) {
					return;
				}

				// 鼠标真正离开：取消高亮
				hasFocus = false;

				// [核心修改] 智能释放焦点
				// 如果当前的 scrollFocus 是我自己或者我的子孙，说明是我刚才抢的，现在要释放
				Actor currentFocus = getStage() != null ? getStage().getScrollFocus() : null;
				if (currentFocus != null && currentFocus.isDescendantOf(EditorPanel.this)) {
					getStage().setScrollFocus(null);
//					 com.goldsprite.gdengine.log.Debug.log("自动释放滚轮焦点");
				}
			}
		});
	}

	// [新增] 绘制高亮边框
	@Override
	public void draw(Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);

		if (hasFocus) {
			Color old = batch.getColor();
			// 绘制青色边框，表示活跃
			batch.setColor(Color.CYAN);
			float t = 2f; // 边框厚度

			// Top
			whitePixel.draw(batch, getX(), getY() + getHeight() - t, getWidth(), t);
			// Bottom
			whitePixel.draw(batch, getX(), getY(), getWidth(), t);
			// Left
			whitePixel.draw(batch, getX(), getY(), t, getHeight());
			// Right
			whitePixel.draw(batch, getX() + getWidth() - t, getY(), t, getHeight());

			batch.setColor(old);
		}
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
