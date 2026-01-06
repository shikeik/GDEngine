package com.goldsprite.gdengine.core.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.log.Debug;

/**
 * 引擎全局偏好设置 (Editor Preferences)
 * 存储在本地设备，不随项目同步。
 */
public class GDEngineConfig {

	private static final String CONFIG_FILE_PATH = "GDEngine/engine_config.json";
	private static final Json json = new Json();

	static {
		// [修复] 明确设置输出类型为 JSON (这会启用换行)
		json.setOutputType(JsonWriter.OutputType.json);
		json.setIgnoreUnknownFields(true);
		// [可选] 如果想强制不用引号包围Key可以设为javascript，但json兼容性最好
	}

	public String projectsRootPath = "";
	public float uiScale = 1.0f;
	public String lastOpenProjectPath = "";

	public static GDEngineConfig load() {
		FileHandle file = getConfigFile();
		GDEngineConfig config = null;

		if (file.exists()) {
			try {
				config = json.fromJson(GDEngineConfig.class, file);
			} catch (Exception e) {
				Debug.logT("Config", "配置读取失败，重置为默认: " + e.getMessage());
			}
		}

		if (config == null) {
			config = new GDEngineConfig();
			config.resetToDefault();
			config.save(); // 生成默认文件
		}

		// 校验路径有效性，无效则回退默认
		if (!checkPathValid(config.projectsRootPath)) {
			Debug.logT("Config", "配置路径无效，回退默认: " + config.projectsRootPath);
			config.resetToDefault();
		}

		Debug.logT("Config", "Loaded. Root: " + config.projectsRootPath);
		return config;
	}

	public void save() {
		try {
			FileHandle file = getConfigFile();
			// [修复] 使用 toJson 直接序列化，它会遵循 static 块里的 OutputType.json 配置
			file.writeString(json.prettyPrint(this), false, "UTF-8");
			Debug.logT("Config", "Saved to " + file.path());
		} catch (Exception e) {
			Debug.logT("Config", "保存失败: " + e.getMessage());
		}
	}

	public void resetToDefault() {
		this.projectsRootPath = getDefaultProjectsPath();
		this.uiScale = 1.0f;
	}

	/** 获取配置文件的物理句柄 */
	private static FileHandle getConfigFile() {
		if (PlatformImpl.isAndroidUser()) {
			// Android: /storage/emulated/0/GDEngine/engine_config.json
			String externalPath = PlatformImpl.AndroidExternalStoragePath;
			return Gdx.files.absolute(externalPath).child(CONFIG_FILE_PATH);
		} else {
			// PC: ./GDEngine/engine_config.json (项目根目录下)
			return Gdx.files.local(CONFIG_FILE_PATH);
		}
	}

	/** 获取默认的项目存放路径 */
	public static String getDefaultProjectsPath() {
		if (PlatformImpl.isAndroidUser()) {
			// [修复] Android 默认: SD卡/GDEngine/Projects
			return PlatformImpl.AndroidExternalStoragePath + "/GDEngine/Projects";
		} else {
			// PC 默认: ./GDEngine/Projects
			return Gdx.files.local("GDEngine/Projects").file().getAbsolutePath();
		}
	}

	private static boolean checkPathValid(String path) {
		try{
			FileHandle handle = Gdx.files.absolute(path);
			if (!handle.exists()) handle.mkdirs();
			return true;
		}catch (Exception e){
			return false; // 创建失败则表示路径不合法
		}
	}
}
