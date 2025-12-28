package com.goldsprite.solofight.core;

import java.util.HashMap;
import java.util.Map;

public class TextDB {
	public enum Lang { CN, EN }

	private static Lang currentLang = Lang.CN;
	private static final Map<String, String> zh = new HashMap<>();
	private static final Map<String, String> en = new HashMap<>();

	static {
		// --- ZH ---
		zh.put("tab_ctrl", "操作说明"); zh.put("tab_moves", "出招表");
		zh.put("th_action", "动作"); zh.put("th_key", "键盘"); zh.put("th_touch", "触屏"); zh.put("th_skill", "招式");
		zh.put("act_move", "移动"); zh.put("act_jump", "跳跃"); zh.put("act_atk", "攻击"); zh.put("act_ult", "大招");
		zh.put("in_stick", "摇杆"); zh.put("in_swipe_u", "上滑"); zh.put("in_swipe_d", "下滑"); zh.put("in_tap", "点击");
		zh.put("skl_dash_l", "左冲刺"); zh.put("skl_dash_r", "右冲刺"); zh.put("skl_dash_a", "冲刺 (自动)"); zh.put("skl_flash", "闪 刀");
		zh.put("key_dash_l", "按住 A + L"); zh.put("tch_dash_l", "左滑");
		zh.put("key_dash_r", "按住 D + L"); zh.put("tch_dash_r", "右滑");
		zh.put("key_dash_a", "单按 L"); zh.put("tch_dash_a", "-");
		zh.put("key_flash", "按住 S + L"); zh.put("tch_flash", "摇杆下 + 划屏");
		zh.put("btn_close", "点击任意处关闭");
		zh.put("CMD_MOVE_LEFT", "左移"); zh.put("CMD_MOVE_RIGHT", "右移"); zh.put("CMD_CROUCH", "下蹲");
		zh.put("CMD_JUMP", "跳跃"); zh.put("CMD_ATK", "攻击"); zh.put("CMD_ULT", "大招");
		zh.put("CMD_DASH_L", "左冲"); zh.put("CMD_DASH_R", "右冲"); zh.put("CMD_DASH_AUTO", "冲刺");
		zh.put("FLASH SLASH", "闪 刀"); zh.put("DASH", "冲 刺"); zh.put("ATTACK", "攻 击"); zh.put("JUMP", "跳 跃"); zh.put("ULTIMATE", "大 招");
		zh.put("KEY", "按键"); zh.put("STICK", "摇杆"); zh.put("GESTURE", "手势");

		// --- EN ---
		en.put("tab_ctrl", "CONTROLS"); en.put("tab_moves", "MOVES LIST");
		en.put("th_action", "ACTION"); en.put("th_key", "KEYBOARD"); en.put("th_touch", "TOUCH"); en.put("th_skill", "SKILL");
		en.put("act_move", "Move"); en.put("act_jump", "Jump"); en.put("act_atk", "Attack"); en.put("act_ult", "Ult");
		en.put("in_stick", "Joystick"); en.put("in_swipe_u", "Swipe Up"); en.put("in_swipe_d", "Swipe Down"); en.put("in_tap", "Tap");
		en.put("skl_dash_l", "Dash (Left)"); en.put("skl_dash_r", "Dash (Right)"); en.put("skl_dash_a", "Dash (Auto)"); en.put("skl_flash", "Flash Slash");
		en.put("key_dash_l", "Hold A + L"); en.put("tch_dash_l", "Swipe Left");
		en.put("key_dash_r", "Hold D + L"); en.put("tch_dash_r", "Swipe Right");
		en.put("key_dash_a", "Press L"); en.put("tch_dash_a", "-");
		en.put("key_flash", "Hold S + L"); en.put("tch_flash", "Stick Down + Swipe");
		en.put("btn_close", "TAP ANYWHERE TO CLOSE");
		en.put("CMD_MOVE_LEFT", "LEFT"); en.put("CMD_MOVE_RIGHT", "RIGHT"); en.put("CMD_CROUCH", "CROUCH");
		en.put("CMD_JUMP", "JUMP"); en.put("CMD_ATK", "ATK"); en.put("CMD_ULT", "ULT");
		en.put("CMD_DASH_L", "DASH L"); en.put("CMD_DASH_R", "DASH R"); en.put("CMD_DASH_AUTO", "DASH");
		en.put("FLASH SLASH", "FLASH SLASH"); en.put("DASH", "DASH"); en.put("ATTACK", "ATTACK"); en.put("JUMP", "JUMP"); en.put("ULTIMATE", "ULTIMATE");
		en.put("KEY", "Key"); en.put("STICK", "Stick"); en.put("GESTURE", "Gesture");
	}

	public static String get(String key) {
		Map<String, String> map = (currentLang == Lang.CN) ? zh : en;
		return map.getOrDefault(key, key);
	}

	public static void toggle() {
		currentLang = (currentLang == Lang.CN) ? Lang.EN : Lang.CN;
	}

	public static String getLangName() {
		return currentLang.name();
	}
}
