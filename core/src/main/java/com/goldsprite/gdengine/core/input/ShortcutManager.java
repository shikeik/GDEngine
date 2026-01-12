package com.goldsprite.gdengine.core.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.log.Debug;

import java.util.HashMap;
import java.util.Map;

public class ShortcutManager extends InputAdapter {

	private Array<KeyBind> bindings = new Array<>();
	private final Map<String, Runnable> actions = new HashMap<>();
	private final FileHandle configFile;

	// [可选] 注入 Stage 以便在输入框聚焦时禁用快捷键
	private Stage uiStage;

	public ShortcutManager(Stage uiStage) {
		this.uiStage = uiStage;
		this.configFile = Gdx.files.local("shortcuts.json");
		load();
	}

	public void register(String actionId, Runnable action) {
		actions.put(actionId, action);
	}

	public void load() {
		if (!configFile.exists()) {
			loadDefaults();
			save();
			return;
		}

		try {
			Json json = new Json();
			bindings = json.fromJson(Array.class, KeyBind.class, configFile);

			// 确保加载进来的数据被解析过 (虽然 Json.Serializable.read 会调用，但双重保险)
			for(KeyBind kb : bindings) {
				if(kb.keyCode == -1) kb.parseTrigger();
			}

			Debug.logT("Shortcut", "Loaded %d shortcuts", bindings.size);
		} catch (Exception e) {
			Debug.logT("Shortcut", "Load error: " + e.getMessage());
			loadDefaults();
		}
	}

	public void save() {
		try {
			Json json = new Json();
			json.setOutputType(com.badlogic.gdx.utils.JsonWriter.OutputType.json);
			configFile.writeString(json.prettyPrint(bindings), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadDefaults() {
		bindings.clear();

		// --- 所见即所得的配置 ---
		addDefault("TOOL_MOVE",   "W");
		addDefault("TOOL_ROTATE", "E");
		addDefault("TOOL_SCALE",  "R");

		addDefault("ACTION_UNDO", "Ctrl+Z");
		addDefault("ACTION_REDO", "Ctrl+Shift+Z"); // 或者 "Ctrl+Y"

		addDefault("ACTION_SAVE", "Ctrl+S");
		addDefault("ACTION_DELETE", Input.Keys.toString(Input.Keys.FORWARD_DEL));
		addDefault("ACTION_FOCUS",  "F");
	}

	private void addDefault(String id, String trigger) {
		bindings.add(new KeyBind(id, trigger));
	}

	@Override
	public boolean keyDown(int keycode) {
		// [优化] 如果 UI 输入框正在输入，屏蔽快捷键
		if (uiStage != null && uiStage.getKeyboardFocus() != null) {
			// 这里可以细化：如果是 TextField 且按下的不是功能键(F1-F12)，则屏蔽
			// 简单起见，只要有焦点就屏蔽 W/E/R 等，但 Ctrl+S 可能想保留？
			// 目前的策略：输入框优先，屏蔽所有。
			return false;
		}

		boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
		boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
		boolean alt = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);

		for (KeyBind bind : bindings) {
			if (bind.keyCode == keycode) {
				// 严格匹配
				if (bind.ctrl == ctrl && bind.shift == shift && bind.alt == alt) {
					Runnable action = actions.get(bind.actionId);
					if (action != null) {
						action.run();
						// Debug.logT("Shortcut", bind.actionId);
						return true;
					}
				}
			}
		}
		return false;
	}
}
