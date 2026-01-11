package com.goldsprite.gdengine.core.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.log.Debug;

import java.util.ArrayList;
import java.util.List;

public class SceneLoader {

	/**
	 * 加载场景 (覆盖模式)
	 * 会清空当前场景中除 DDOL 以外的所有物体，然后加载新物体。
	 */
	public static void load(FileHandle file) {
		load(file, true);
	}

	/**
	 * 加载场景
	 * @param file 场景文件
	 * @param clearWorld true=切换场景(清空旧的), false=叠加加载(Addtive)
	 */
	public static void load(FileHandle file, boolean clearWorld) {
		if (file == null || !file.exists()) {
			Debug.logT("SceneLoader", "❌ 场景文件不存在: " + (file == null ? "null" : file.path()));
			return;
		}

		try {
			// 1. 清理 (如果需要)
			if (clearWorld) {
				GameWorld.inst().clear();
			}

			// 2. 反序列化
			Json json = GdxJsonSetup.create();

			// 读取列表。Json 内部会调用 GObject 的反序列化逻辑
			// GObject 构造时会自动注册到 GameWorld，所以这里不需要我们手动 add。
			@SuppressWarnings("unchecked")
			ArrayList<GObject> newRoots = json.fromJson(ArrayList.class, GObject.class, file);

			Debug.logT("SceneLoader", "✅ 场景加载完毕: " + file.name() + " (Objects: " + (newRoots != null ? newRoots.size() : 0) + ")");

		} catch (Exception e) {
			Debug.logT("SceneLoader", "❌ 加载异常: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 保存当前场景 (只保存根节点)
	 */
	public static void saveCurrentScene(FileHandle file) {
		if (file == null) return;
		try {
			Json json = GdxJsonSetup.create();
			// 获取所有根物体
			List<GObject> roots = GameWorld.inst().getRootEntities();
			// 过滤掉不应该保存的 (比如一些临时的 Editor Gizmo 辅助物体，如果有的话)
			// 目前假设 rootEntities 里的都要存

			String text = json.prettyPrint(roots);
			file.writeString(text, false);

			Debug.logT("SceneLoader", "💾 场景已保存: " + file.name());
		} catch (Exception e) {
			Debug.logT("SceneLoader", "❌ 保存异常: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
