package com.goldsprite.gdengine.ui.event;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.goldsprite.gdengine.log.Debug;

/**
 * 统一上下文菜单监听器
 * <p>
 * 封装了 PC (右键单击) 和 Mobile (长按) 唤出菜单的统一逻辑。
 * 同时保留了左键点击的回调接口。
 * </p>
 */
public abstract class ContextListener extends ActorGestureListener {
	private boolean longPressed;

	// 参数: halfTapSquareSize, tapCountInterval, longPressDuration, maxFlingDelay
	public ContextListener() {
		// 长按时间设为 0.4s，比较跟手
		super(20, 0.4f, 0.4f, 0.15f);
	}

	@Override
	public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
		// TODO: Implement this method
		super.touchDown(event, x, y, pointer, button);
		longPressed = false;
	}

	/**
	 * 当需要显示菜单时触发 (右键 或 长按)
	 * @param stageX 舞台 X 坐标
	 * @param stageY 舞台 Y 坐标
	 */
	public abstract void onShowMenu(float stageX, float stageY);

	/**
	 * 当左键点击时触发 (可选覆盖)
	 * @param count 点击次数 (1=单击, 2=双击)
	 */
	public void onLeftClick(InputEvent event, float x, float y, int count) {}

	@Override
	public void tap(InputEvent event, float x, float y, int count, int button) {
		if (longPressed) return; // 触发长按则取消
		
		if (button == Input.Buttons.RIGHT) {int k;
			onShowMenu(event.getStageX(), event.getStageY());
		} else if (button == Input.Buttons.LEFT) {
			onLeftClick(event, x, y, count);
		}
	}

	@Override
	public boolean longPress(Actor actor, float x, float y) {
		longPressed = true;
		
		// 转换局部坐标到舞台坐标
		Vector2 stagePos = actor.localToStageCoordinates(new Vector2(x, y));

		onShowMenu(stagePos.x, stagePos.y);

		// 移动端震动反馈
		Gdx.input.vibrate(50);

		return true;
	}
}
