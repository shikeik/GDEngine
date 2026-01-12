package com.goldsprite.gdengine.core.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * 快捷键绑定 (重构版)
 * 支持 "Ctrl+Shift+Z" 格式的直观配置
 */
public class KeyBind implements Json.Serializable {
	public String actionId;
	public String trigger; // 存盘字段: "Ctrl+S"

	// --- 运行时缓存 (不存盘) ---
	public transient int keyCode = -1;
	public transient boolean ctrl;
	public transient boolean shift;
	public transient boolean alt;

	public KeyBind() {}

	public KeyBind(String actionId, String trigger) {
		this.actionId = actionId;
		this.trigger = trigger;
		parseTrigger();
	}

	/**
	 * 核心解析逻辑
	 * 将 "Ctrl+Alt+Del" 解析为 keyCode 和 boolean 标记
	 */
	public void parseTrigger() {
		if (trigger == null || trigger.isEmpty()) return;

		// 重置状态
		ctrl = false; shift = false; alt = false; keyCode = -1;

		// 支持加号分割，不区分大小写
		// 例如: "Ctrl+S", "ctrl shift z", "Alt+F4"
		String[] parts = trigger.split("\\+");

		for (String part : parts) {
			switch (part) {
				case "Ctrl":
				case "Control":
					ctrl = true;
					break;
				case "Shift":
					shift = true;
					break;
				case "Alt":
					alt = true;
					break;
				default:
					// 尝试解析按键名 (LibGDX 内置映射)
					int code = Input.Keys.valueOf(part);
					if (code != -1) {
						this.keyCode = code;
					} else {
						System.err.println("KeyBind Parse Error: Unknown key '" + part + "' in " + trigger);
					}
					break;
			}
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("actionId", actionId);
		json.writeValue("trigger", trigger);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		actionId = jsonData.getString("actionId");
		trigger = jsonData.getString("trigger");
		parseTrigger(); // 读取后立即解析
	}
}
