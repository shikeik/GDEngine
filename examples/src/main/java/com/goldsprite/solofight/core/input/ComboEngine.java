package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import java.util.function.BiConsumer;

public class ComboEngine {
	private static ComboEngine instance;

	// 指令缓冲 (ID, Source)
	private static class CmdEntry {
		String id;
		String source;
		public CmdEntry(String id, String source) { this.id = id; this.source = source; }
	}

	private final Array<CmdEntry> buffer = new Array<>();
	private final float TIMEOUT = 0.4f; // 400ms 判定窗
	private Timer.Task resetTask;

	// 回调：当判定出最终招式时触发
	public BiConsumer<String, String> onCommandExecuted;

	public static ComboEngine inst() {
		if (instance == null) instance = new ComboEngine();
		return instance;
	}

	public void push(String cmdId, String source) {
		// 过滤持续性状态指令 (如 MOVE_LEFT/RIGHT/CROUCH 状态本身不进Buffer，但触发瞬间可以进?
		// H5逻辑: 按下瞬间触发判定。我们这里简化：所有指令都进Buffer进行模式匹配
		// 但要注意 INPUT_STATE 不应该由 ComboEngine 处理移动，ComboEngine 只处理 Trigger。
		// 不过 H5 Manifest 里 CROUCH 是 STATE。
		// 修正：InputContext 应该区分 "State Update" 和 "Discrete Command Event"。
		// 现在的架构 commandListener 只在 Trigger 时触发，或者 State 变化时触发。

		buffer.add(new CmdEntry(cmdId, source));

		// 尝试匹配连招
		boolean matched = checkMoves();

		// 如果没匹配出特殊招式，且是单发指令，则直接执行原始指令
		if (!matched) {
			execute(cmdId, source, false); // false = 不是组合技
		}

		// 重置计时器
		if (resetTask != null) resetTask.cancel();
		resetTask = Timer.schedule(new Timer.Task() {
			@Override
			public void run() {
				buffer.clear();
			}
		}, TIMEOUT);
	}

	private boolean checkMoves() {
		if (buffer.size < 2) return false;

		CmdEntry last = buffer.get(buffer.size - 1);
		CmdEntry prev = buffer.get(buffer.size - 2);

		// 1. 闪刀: [下蹲] + [冲刺]
		// H5: prev.id === 'CMD_CROUCH' && last.id.startsWith('CMD_DASH')
		if (prev.id.equals("CMD_CROUCH") && last.id.startsWith("CMD_DASH")) {
			execute("FLASH SLASH", last.source, true);
			buffer.clear(); // 清空缓冲，防止重复触发
			return true;
		}

		// 2. 冲刺: [冲刺] (其实是单键，但在 H5 里 DASH 是个 Trigger)
		// 这里的逻辑：如果只是普通 DASH，上面 !matched 已经执行了。
		// 主要是拦截组合技。

		return false;
	}

	private void execute(String finalCmd, String source, boolean isCombo) {
		// 如果是组合技组件（如由下蹲变成闪刀），我们需要"撤销"或者忽略之前的下蹲吗？
		// 动作游戏中通常是：下蹲已经执行了，闪刀会打断下蹲。

		// 特殊处理：如果是组合技成分的后半段（如Dash），且被判定为组合技，
		// 那么在 push() 的 !matched 分支里，它已经被拦截了？
		// 不，checkMoves 返回 true，!matched 为 false，所以原始 DASH 不会执行，只执行 FLASH SLASH。

		if (onCommandExecuted != null) {
			onCommandExecuted.accept(finalCmd, source);
		}
	}
}
