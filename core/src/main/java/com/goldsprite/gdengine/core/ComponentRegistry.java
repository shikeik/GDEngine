package com.goldsprite.gdengine.core;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.log.Debug;

import java.util.HashSet;
import java.util.Set;

/**
 * 全局组件注册表 (最终统一版)
 * 核心机制：基于索引文件 (Index File) 的惰性加载。
 * 无论是 PC 还是 Android，都只认 index 文件，不再进行低效的包扫描。
 */
public class ComponentRegistry {

	// 使用 Set 去重
	private static final Set<Class<? extends Component>> components = new HashSet<>();

	// 静态块：引擎启动时，自动尝试加载内置索引
	static {
		reloadEngineIndex();
	}

	/**
	 * 加载引擎内置组件索引
	 * (由 Gradle 任务 generateEngineIndex 生成于 src/main/resources/engine.index)
	 */
	public static void reloadEngineIndex() {
		try {
			// 尝试从 Classpath 读取 (打进 JAR 包里的资源)
			FileHandle engineIndexFile = Gd.files.internal("engine.index");
			if (engineIndexFile.exists()) {
				parseAndRegister(engineIndexFile);
			} else {
				// 如果是在 IDE 纯源码环境开发且没运行 Gradle 任务，可能会缺失
				Debug.logT("Registry", "⚠️ engine.index not found in classpath.");
			}
		} catch (Exception e) {
			Debug.logT("Registry", "Engine index load error: " + e.getMessage());
		}
	}

	/**
	 * 加载用户项目组件索引
	 * (通常在 打开项目 或 编译完成 后由 EditorController 调用)
	 *
	 * @param projectIndexFile 项目根目录下的 project.index 文件
	 */
	public static void reloadUserIndex(FileHandle projectIndexFile) {
		// 1. 清理旧的用户组件 (保留 com.goldsprite.gdengine 开头的内置组件)
		// 这一步很重要，防止重编译后旧的 Class 引用还留在这里
		components.removeIf(c -> !c.getName().startsWith("com.goldsprite.gdengine"));

		if (projectIndexFile != null && projectIndexFile.exists()) {
			Debug.logT("Registry", "🔄 Loading User Index: " + projectIndexFile.path());
			parseAndRegister(projectIndexFile);
		} else {
			Debug.logT("Registry", "⚠️ User Index not found.");
		}

		Debug.logT("Registry", "Registry Updated. Total Components: " + components.size());
	}

	/**
	 * 核心解析逻辑：读取文本 -> 反射加载 -> 注册
	 */
	private static void parseAndRegister(FileHandle file) {
		String content = file.readString("UTF-8");
		if (content == null || content.isEmpty()) {
			return;
		}

		String[] lines = content.split("\\r?\\n");
		int count = 0;

		for (String className : lines) {
			className = className.trim();
			if (className.isEmpty()) continue;

			// [过滤 1] 剔除内部类/匿名类 (带 $ 的)
			if (className.contains("$")) continue;

			try {
				// [关键] 使用 Gd.scriptClassLoader 加载
				// initialize = false : 只加载定义，不执行 static 块，性能极高且安全
				// 这允许我们在不触发副作用的情况下检查类信息
				Class<?> clazz = Class.forName(className, false, Gd.scriptClassLoader);

				// [过滤 2] 鉴权：必须是 Component 子类
				if (!register(clazz)) continue;

				count++;
			} catch (ClassNotFoundException e) {
				// 仅在调试模式下打印，避免日志刷屏
				// Debug.logT("Registry", "  ❌ ClassNotFound: " + className);
			} catch (Throwable e) {
				Debug.logT("Registry", "  ❌ Error loading " + className + ": " + e);
			}
		}
		Debug.logT("Registry", "Loaded " + count + " valid components from " + file.name());
	}

	@SuppressWarnings("unchecked")
	public static boolean register(Class<?> clazz) {
		// 严格过滤：必须是 Component 子类，非抽象，非接口
		if (Component.class.isAssignableFrom(clazz)
			&& !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())
			&& !clazz.isInterface()) {

			Class<? extends Component> compClz = (Class<? extends Component>) clazz;
			components.add(compClz);
			return true;
		}
		return false;
	}

	/**
	 * 获取列表 (UI 使用)
	 */
	public static Array<Class<? extends Component>> getAll() {
		Array<Class<? extends Component>> list = new Array<>();
		for (Class<? extends Component> c : components) {
			list.add(c);
		}
		// 字母排序，方便 UI 查找
		list.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
		return list;
	}

	// --- 兼容旧 API (虽然不再推荐使用，但为了防止报错保留空实现) ---
	public static void clearUserComponents() {
		components.removeIf(c -> !c.getName().startsWith("com.goldsprite.gdengine"));
	}

	public static void scanBuiltInPackages(String pkg) { /* Deprecated, use engine.index */ }
}
