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
	// 配置文件 (存储在 EngineRoot 下)
	private static final String CONFIG_FILENAME = "engine_config.json";

	private static final Json json = new Json();
	private static GDEngineConfig instance;

	static {
		json.setOutputType(JsonWriter.OutputType.json);
		json.setIgnoreUnknownFields(true);
	}

	// ==========================================
	// 配置字段 (JSON Payload)
	// ==========================================

	/**
	 * 自定义项目根目录 (绝对路径)
	 * 如果为空，则默认使用 engineRoot/Projects
	 */
	public String customProjectsPath = "";

	/** 项目存放子目录名 (User Space) */
	public String projectsSubDir = "UserProjects"; // [修改] 改为 UserProjects

	public float uiScale = 1.0f;
	public String lastOpenProjectPath = "";

	// 运行时缓存：当前生效的引擎根目录 (不序列化)
	private transient String activeEngineRoot;

	// ==========================================
	// 逻辑方法
	// ==========================================

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
				Debug.logT("Config", "Load failed, using default.");
			}
		}

		if (instance == null) instance = new GDEngineConfig();

		instance.activeEngineRoot = rootPath;
		instance.getProjectsDir().mkdirs(); // 确保项目目录存在
		instance.save();


		// [新增] 打印首选项物理位置
		printPrefsLocation();
		Debug.logT("Config", "Engine Root: " + rootPath);
	}

	public static GDEngineConfig getInstance() { return instance; }

	public void save() {
		if (activeEngineRoot == null) return;
		try {
			FileHandle file = Gdx.files.absolute(activeEngineRoot).child(CONFIG_FILENAME);
			file.writeString(json.toJson(this), false, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** 获取当前生效的项目根目录 */
	public FileHandle getProjectsDir() {
		// [修复] 防御性检查
		if (activeEngineRoot == null) {
			// 如果没初始化，尝试返回 customProjectsPath (如果有)，否则返回空对象或抛出更有意义的异常
			if (customProjectsPath != null && !customProjectsPath.isEmpty()) {
				return Gdx.files.absolute(customProjectsPath);
			}
			throw new IllegalStateException("Engine not initialized: activeEngineRoot is null");
		}
		return Gdx.files.absolute(activeEngineRoot).child(projectsSubDir);
	}

	public String getActiveEngineRoot() { return activeEngineRoot; }

	public static String getRecommendedRoot() {
		if (PlatformImpl.isAndroidUser()) {
			return PlatformImpl.AndroidExternalStoragePath + "/GDEngine";
		} else {
			return Gdx.files.local("GDEngine").file().getAbsolutePath();
		}
	}

	/** 打印 Preferences 在硬盘上的实际藏身之处 */
	private static void printPrefsLocation() {
		String location;
		if (PlatformImpl.isAndroidUser()) {
			try {
				// Android: Gdx.files.local(".") 指向 /data/user/0/<pkg>/files/
				// Prefs 在 /data/user/0/<pkg>/shared_prefs/gd_boot.xml
				// 我们通过 local 目录往上找一级，再进入 shared_prefs
				FileHandle pkgRoot = Gdx.files.absolute(Gdx.files.local("").file().getParentFile().getAbsolutePath());
				FileHandle prefsFile = pkgRoot.child("shared_prefs").child(PREF_NAME + ".xml");
				location = prefsFile.file().getAbsolutePath();
			} catch (Exception e) {
				location = "Android Internal Storage (Calculation Failed)";
			}
		} else {
			// Desktop (Lwjgl3): 默认存放在用户主目录的 .prefs 文件夹下
			String userHome = System.getProperty("user.home");
			location = userHome + "\\.prefs\\" + PREF_NAME;
		}
		Debug.logT("Config", "⚓ Boot Prefs Location: " + location);
	}
}
