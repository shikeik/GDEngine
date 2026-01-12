package com.goldsprite.gdengine.core;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.PlatformImpl; // 引入平台判断
import com.goldsprite.gdengine.ecs.component.*;
import com.goldsprite.gdengine.log.Debug;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class ComponentRegistry {

	private static final Set<Class<? extends Component>> components = new HashSet<>();

	static {
		// =========================================================
		// 1. 核心组件显式注册 (Mobile First 策略)
		// =========================================================
		// 在 Android 上，反射扫描包极其困难且慢。
		// 我们直接把“出厂自带”的组件列在这里。这是最稳健的做法。
		register(TransformComponent.class);
		register(SpriteComponent.class);
		register(NeonAnimatorComponent.class);
		register(SkeletonComponent.class);
		register(FsmComponent.class);
		// register(BoxCollider.class); // 未来添加...

		// =========================================================
		// 2. Desktop 开发环境增强
		// =========================================================
		// 如果是在 PC 上开发引擎本身，我们希望能自动扫到新写的组件，不用每次来改这里。
		if (!PlatformImpl.isAndroidUser()) {
			scanBuiltInPackages("com.goldsprite.gdengine.ecs.component");
		}
	}

	@SuppressWarnings("unchecked")
	public static void register(Class<?> clazz) {
		try {
			// 严格过滤：必须是 Component 子类，非抽象，非匿名，非接口
			if (Component.class.isAssignableFrom(clazz)
				&& !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())
				&& !clazz.isAnonymousClass()
				&& !clazz.isInterface()) {

				components.add((Class<? extends Component>) clazz);
			}
		} catch (Throwable e) {
			// 忽略 (例如类加载错误)
		}
	}

	/** Android 不需要跑这个，直接跳过 */
	public static void scanBuiltInPackages(String packageName) {
		if (PlatformImpl.isAndroidUser()) return;

		try {
			// ... (原有的 PC 扫描逻辑保持不变) ...
			String path = packageName.replace('.', '/');
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Enumeration<URL> resources = classLoader.getResources(path);
			while (resources.hasMoreElements()) {
				URL resource = resources.nextElement();
				if ("file".equals(resource.getProtocol())) {
					scanDir(new File(resource.toURI()), packageName);
				}
			}
		} catch (Exception e) {
			Debug.logT("Registry", "PC Scan skip: " + e.getMessage());
		}
	}

	private static void scanDir(File dir, String packageName) {
		if (!dir.exists()) return;
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File file : files) {
			if (file.isDirectory()) {
				scanDir(file, packageName + "." + file.getName());
			} else if (file.getName().endsWith(".class")) {
				String className = packageName + "." + file.getName().replace(".class", "");
				try {
					Class<?> clazz = Class.forName(className);
					register(clazz);
				} catch (Throwable e) {}
			}
		}
	}

	public static void clearUserComponents() {
		components.removeIf(c -> !c.getName().startsWith("com.goldsprite.gdengine"));
	}

	public static Array<Class<? extends Component>> getAll() {
		Array<Class<? extends Component>> list = new Array<>();
		for (Class<? extends Component> c : components) list.add(c);
		list.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
		return list;
	}
}
