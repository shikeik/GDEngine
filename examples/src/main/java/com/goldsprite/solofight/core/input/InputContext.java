package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.math.Vector2;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class InputContext {
	private static InputContext instance;

	// 虚拟状态 (Virtual State) - 用于连续移动
	public float moveX = 0;
	public boolean crouch = false;

	// 原始状态
	public final Set<Integer> heldKeys = new HashSet<>();
	public final Vector2 stickRaw = new Vector2(); // 摇杆归一化向量 (-1~1)
	public int stickZone = -1; // 当前摇杆扇区 (0-7)

	// 事件回调 (当产生离散指令时触发，如 TAP, SWIPE, JUMP键按下)
	// 参数: CommandID, SourceInfo (e.g. "KEY:J", "GESTURE:TAP")
	public BiConsumer<String, String> commandListener;

	public static InputContext inst() {
		if (instance == null) instance = new InputContext();
		return instance;
	}

	/**
	 * 更新虚拟状态 (每帧调用或输入变化时调用)
	 * 复刻 H5 的 InputSystem.updateVirtualState()
	 */
	public void updateVirtualState() {
		float vx = 0;
		boolean down = false;

		// 1. Keyboard
		if (heldKeys.contains(com.badlogic.gdx.Input.Keys.A)) vx -= 1;
		if (heldKeys.contains(com.badlogic.gdx.Input.Keys.D)) vx += 1;
		if (heldKeys.contains(com.badlogic.gdx.Input.Keys.S)) down = true;

		// 2. Stick
		// H5: if(TouchInput.stick.x < -0.3) vx = -1;
		if (stickRaw.x < -0.3f) vx = -1;
		if (stickRaw.x > 0.3f) vx = 1;
		// H5: if(TouchInput.currentZone === 6) down = true; (6 is DOWN)
		if (stickZone == 6) down = true;

		this.moveX = vx;
		this.crouch = down;
	}

	/**
	 * 触发指令 (供 ComboEngine 使用)
	 */
	public void emitCommand(String cmdId, String source) {
		if (commandListener != null) {
			commandListener.accept(cmdId, source);
		}
	}
}
