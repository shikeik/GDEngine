package com.goldsprite.gdengine.core.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.core.Gd;
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
			GdxJsonSetup.ScriptJson json = GdxJsonSetup.create();

			// [核心修改] 注入 ClassLoader
			// 这样 json.fromJson 遇到用户自定义类时，就能找到了！
			json.setClassLoader(Gd.scriptClassLoader);


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
			List<GObject> roots = GameWorld.inst().getRootEntities();

			// [核心修复] 过滤掉 DDOL 物体，只保存纯场景数据
			List<GObject> sceneObjects = new ArrayList<>();
			for (GObject obj : roots) {
				if (!obj.isDontDestroyOnLoad()) { // 只存没“免死金牌”的
					sceneObjects.add(obj);
				}
			}

			// 保存过滤后的列表
			String text = json.prettyPrint(sceneObjects);
			file.writeString(text, false);

			Debug.logT("SceneLoader", "💾 场景已保存: " + file.name() + " (ObjCount: " + sceneObjects.size() + ")");
		} catch (Exception e) {
			Debug.logT("SceneLoader", "❌ 保存异常: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
