package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class KeyboardProcessor extends InputAdapter {

	@Override
	public boolean keyDown(int keycode) {
		InputContext ctx = InputContext.inst();

		// 1. 更新按键状态
		ctx.heldKeys.add(keycode);

		// 2. 匹配离散指令 (Trigger)
		for (InputDef.Command cmd : InputDef.COMMANDS) {
			for (InputDef.Trigger trig : cmd.triggers) {
				// 只处理键盘类型的触发器
				if (trig.hw == InputDef.HwType.KEY && trig.code == keycode) {
					boolean modMatch = true;

					// 组合键逻辑 (H5复刻)
					if (trig.modKey != null) {
						// 如果定义了修饰键 (如 A+L)，必须按住修饰键
						if (!ctx.heldKeys.contains(trig.modKey)) modMatch = false;
					} else {
						// 如果没有修饰键 (如单按 L)，必须确保没有按下冲突的方向键
						// H5逻辑: CMD_DASH_AUTO (L) 只有在 A 和 D 都没按的时候触发
						// 否则会误触 CMD_DASH_L/R
						if (cmd.id.equals("CMD_DASH_AUTO")) {
							if (ctx.heldKeys.contains(Input.Keys.A) || ctx.heldKeys.contains(Input.Keys.D)) {
								modMatch = false;
							}
						}
					}

					if (modMatch) {
						String keyName = Input.Keys.toString(keycode);
						// 触发指令，Source 格式: KEY:J
						ctx.emitCommand(cmd.id, "KEY:" + keyName);
						// 注意：这里不 return，允许一个键触发多个逻辑（虽罕见）
					}
				}
			}
		}

		// 3. 更新虚拟状态 (移动/下蹲)
		ctx.updateVirtualState();

		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		InputContext ctx = InputContext.inst();
		ctx.heldKeys.remove(keycode);
		ctx.updateVirtualState();
		return true;
	}
}
