package com.goldsprite.gdengine.core.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.log.Debug;

/**
 * 引擎全局配置管理器
 * 机制：利用 Preferences (gd_boot) 存储引擎根目录路径，实现位置解耦。
 */
public class GDEngineConfig {

	// 引导首选项 Key
	private static final String PREF_NAME = "gd_boot";
	private static final String KEY_ENGINE_ROOT = "engine_root";

	// 配置文件名 (存储在 EngineRoot 下)
	private static final String CONFIG_FILENAME = "engine_config.json";

	private static final Json json = new Json();
	private static GDEngineConfig instance;

	static {
		json.setOutputType(JsonWriter.OutputType.json);
		json.setIgnoreUnknownFields(true);
	}

	// ==========================================
	// 实例字段 (JSON Payload)
	// ==========================================

	/** 项目存放子目录名 (默认 Projects) */
	public String projectsSubDir = "Projects";

	public float uiScale = 1.0f;
	public String lastOpenProjectPath = "";

	// 运行时缓存：当前生效的引擎根目录 (不序列化)
	private transient String activeEngineRoot;

	// ==========================================
	// 静态生命周期
	// ==========================================

	/**
	 * 尝试加载配置
	 * @return 如果成功加载(或已初始化)返回 true；如果未配置引导路径返回 false (需要弹窗)
	 */
	public static boolean tryLoad() {
		if (instance != null) return true;

		Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
		String savedRoot = prefs.getString(KEY_ENGINE_ROOT, null);

		if (savedRoot == null || savedRoot.trim().isEmpty()) {
			return false; // 未引导
		}

		// 校验目录是否存在
		FileHandle rootHandle = Gdx.files.absolute(savedRoot);
		if (!rootHandle.exists() || !rootHandle.isDirectory()) {
			Debug.logT("Config", "引导路径失效: " + savedRoot);
			return false;
		}

		loadFromRoot(savedRoot);
		return true;
	}

	/**
	 * 初始化/重置引擎根目录 (由 SetupDialog 调用)
	 */
	public static void initialize(String engineRootPath) {
		FileHandle root = Gdx.files.absolute(engineRootPath);
		if (!root.exists()) root.mkdirs();

		// 1. 保存引导
		Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
		prefs.putString(KEY_ENGINE_ROOT, engineRootPath);
		prefs.flush();

		// 2. 加载/生成配置
		loadFromRoot(engineRootPath);
	}

	private static void loadFromRoot(String rootPath) {
		FileHandle configFile = Gdx.files.absolute(rootPath).child(CONFIG_FILENAME);

		if (configFile.exists()) {
			try {
				instance = json.fromJson(GDEngineConfig.class, configFile);
			} catch (Exception e) {
				Debug.logT("Config", "JSON损坏，重置默认: " + e.getMessage());
			}
		}

		if (instance == null) {
			instance = new GDEngineConfig();
		}

		instance.activeEngineRoot = rootPath;

		// 确保 Projects 目录存在
		instance.getProjectsDir().mkdirs();

		instance.save(); // 确保文件存在
		Debug.logT("Config", "Ready. Engine Root: " + rootPath);
	}

	public static GDEngineConfig getInstance() {
		return instance;
	}

	// ==========================================
	// 实例方法
	// ==========================================

	public void save() {
		if (activeEngineRoot == null) return;
		try {
			FileHandle file = Gdx.files.absolute(activeEngineRoot).child(CONFIG_FILENAME);
			file.writeString(json.toJson(this), false, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FileHandle getProjectsDir() {
		return Gdx.files.absolute(activeEngineRoot).child(projectsSubDir);
	}

	/** 获取推荐的默认安装位置 (供 SetupDialog 使用) */
	public static String getRecommendedRoot() {
		if (PlatformImpl.isAndroidUser()) {
			return PlatformImpl.AndroidExternalStoragePath + "/GDEngine";
		} else {
			// PC 默认: 当前工作目录下的 GDEngine (便携)
			return Gdx.files.local("GDEngine").file().getAbsolutePath();
		}
	}
}
