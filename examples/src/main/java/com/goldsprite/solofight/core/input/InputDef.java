package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * 输入系统定义：包含硬件类型枚举、指令定义、以及配置表
 */
public class InputDef {

	public enum HwType {
		KEY, STICK, GESTURE
	}

	public static class Trigger {
		public HwType hw;
		public int code;       // 键盘 KeyCode
		public String id;      // 手势 ID (TAP, SWIPE_UP...)
		public String dir;     // 摇杆方向 (LEFT, RIGHT...)
		public Integer modKey; // 组合键 (nullable)

		// Keyboard Trigger
		public Trigger(int code, Integer modKey) {
			this.hw = HwType.KEY;
			this.code = code;
			this.modKey = modKey;
		}

		// [修复] 合并 Stick 和 Gesture 的构造函数，通过 HwType 区分
		public Trigger(HwType hw, String value) {
			this.hw = hw;
			if (hw == HwType.STICK) {
				this.dir = value;
			} else if (hw == HwType.GESTURE) {
				this.id = value;
			}
		}
	}

	public static class Command {
		public String id;
		public String icon;
		public List<Trigger> triggers = new ArrayList<>();

		public Command(String id, String icon) {
			this.id = id;
			this.icon = icon;
		}

		public Command addKey(int code) { return addKey(code, null); }
		public Command addKey(int code, Integer mod) {
			triggers.add(new Trigger(code, mod));
			return this;
		}

		// [修复] 调用新的构造函数，传入 HwType
		public Command addStick(String dir) {
			triggers.add(new Trigger(HwType.STICK, dir));
			return this;
		}

		// [修复] 调用新的构造函数，传入 HwType
		public Command addGesture(String id) {
			triggers.add(new Trigger(HwType.GESTURE, id));
			return this;
		}
	}

	// --- 复刻 H5 的配置表 ---
	public static final List<Command> COMMANDS = new ArrayList<>();

	static {
		// { id: 'CMD_MOVE_LEFT', triggers: [{hw:'KEY', code:'KeyA'}, {hw:'STICK', dir:'LEFT'}] }
		COMMANDS.add(new Command("CMD_MOVE_LEFT", "⬅").addKey(Input.Keys.A).addStick("LEFT"));

		// { id: 'CMD_MOVE_RIGHT', triggers: [{hw:'KEY', code:'KeyD'}, {hw:'STICK', dir:'RIGHT'}] }
		COMMANDS.add(new Command("CMD_MOVE_RIGHT", "➡").addKey(Input.Keys.D).addStick("RIGHT"));

		// { id: 'CMD_CROUCH', triggers: [{hw:'KEY', code:'KeyS'}, {hw:'STICK', dir:'DOWN'}] }
		COMMANDS.add(new Command("CMD_CROUCH", "⬇").addKey(Input.Keys.S).addStick("DOWN"));

		// { id: 'CMD_JUMP', triggers: [{hw:'KEY', code:'KeyK'}, {hw:'GESTURE', id:'SWIPE_UP'}] }
		COMMANDS.add(new Command("CMD_JUMP", "⬆").addKey(Input.Keys.K).addGesture("SWIPE_UP"));

		// { id: 'CMD_ATK', triggers: [{hw:'KEY', code:'KeyJ'}, {hw:'GESTURE', id:'TAP'}] }
		COMMANDS.add(new Command("CMD_ATK", "⚔️").addKey(Input.Keys.J).addGesture("TAP"));

		// { id: 'CMD_ULT', triggers: [{hw:'KEY', code:'KeyO'}, {hw:'GESTURE', id:'SWIPE_DOWN'}] }
		COMMANDS.add(new Command("CMD_ULT", "⭕").addKey(Input.Keys.O).addGesture("SWIPE_DOWN"));

		// { id: 'CMD_DASH_L', triggers: [{hw:'GESTURE', id:'SWIPE_LEFT'}, {hw:'KEY', code:'KeyL', mod:'KeyA'}] }
		COMMANDS.add(new Command("CMD_DASH_L", "⏪").addGesture("SWIPE_LEFT").addKey(Input.Keys.L, Input.Keys.A));

		// { id: 'CMD_DASH_R', triggers: [{hw:'GESTURE', id:'SWIPE_RIGHT'}, {hw:'KEY', code:'KeyL', mod:'KeyD'}] }
		COMMANDS.add(new Command("CMD_DASH_R", "⏩").addGesture("SWIPE_RIGHT").addKey(Input.Keys.L, Input.Keys.D));

		// { id: 'CMD_DASH_AUTO', triggers: [{hw:'KEY', code:'KeyL'}] }
		COMMANDS.add(new Command("CMD_DASH_AUTO", "🚀").addKey(Input.Keys.L));
	}
}
