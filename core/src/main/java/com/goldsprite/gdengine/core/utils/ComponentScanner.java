package com.goldsprite.gdengine.core.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.FsmComponent;
import com.goldsprite.gdengine.ecs.component.NeonAnimatorComponent;
import com.goldsprite.gdengine.ecs.component.SkeletonComponent;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.log.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ComponentScanner {

	// 1. 内置组件白名单 (手动注册最稳健，防止反射扫出一堆垃圾)
	private static final Array<Class<? extends Component>> builtInComponents = new Array<>();

	static {
		builtInComponents.add(TransformComponent.class);
		builtInComponents.add(SpriteComponent.class);
		builtInComponents.add(NeonAnimatorComponent.class);
		builtInComponents.add(SkeletonComponent.class);
		builtInComponents.add(FsmComponent.class);
		// 后续添加更多...
	}

	/**
	 * 获取所有可用组件 (内置 + 用户)
	 */
	public static Array<Class<? extends Component>> scanAll() {
		Array<Class<? extends Component>> result = new Array<>();

		// 1. 添加内置
		result.addAll(builtInComponents);

		// 2. 扫描用户组件 (需要项目上下文)
		// 假设编译后的 class 文件位于项目的 build/classes 目录下
		// 或者使用之前编译器定义的缓存目录

		// 这里做一个简单的文件系统扫描示例
		// 实际路径可能需要根据您的 DesktopScriptCompiler 配置来定
		// 假设我们去扫 "build/script_cache/classes"
		File scriptOutDir = new File("build/script_cache/classes");
		if (scriptOutDir.exists()) {
			scanDir(scriptOutDir, scriptOutDir, result);
		}

		return result;
	}

	private static void scanDir(File root, File curr, Array<Class<? extends Component>> result) {
		File[] files = curr.listFiles();
		if (files == null) return;

		for (File f : files) {
			if (f.isDirectory()) {
				scanDir(root, f, result);
			} else if (f.getName().endsWith(".class")) {
				// 计算类名: path/to/MyClass.class -> path.to.MyClass
				String path = f.getAbsolutePath().replace(root.getAbsolutePath(), "");
				// 去掉开头的斜杠
				if (path.startsWith(File.separator)) path = path.substring(1);

				String className = path.replace(File.separator, ".").replace(".class", "");

				try {
					// 使用 Gd.scriptClassLoader 加载，确保能找到用户类
					Class<?> clazz = Class.forName(className, false, Gd.scriptClassLoader);

					// 必须是 Component 的子类，且不是抽象类，且不是匿名类
					if (Component.class.isAssignableFrom(clazz)
						&& !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())
						&& !clazz.isAnonymousClass()) {

						// 避免重复添加内置组件 (如果用户不小心把引擎源码也编译了)
						if (!builtInComponents.contains((Class<? extends Component>) clazz, true)) {
							result.add((Class<? extends Component>) clazz);
						}
					}
				} catch (Throwable e) {
					// 忽略加载失败的类
				}
			}
		}
	}
}
