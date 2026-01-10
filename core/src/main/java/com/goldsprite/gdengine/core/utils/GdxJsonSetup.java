package com.goldsprite.gdengine.core.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.core.scripting.ScriptResourceTracker;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎专用的 JSON 配置中心
 * 负责处理 GObject、Component 以及特殊类型的序列化逻辑
 */
public class GdxJsonSetup {

	public static Json create() {
		Json json = new Json();
		json.setOutputType(JsonWriter.OutputType.json);
		json.setUsePrototypes(false); // 禁止引用复用，保证每个物体数据独立
		json.setIgnoreUnknownFields(true);

		// 1. GObject 序列化器
		json.setSerializer(GObject.class, new Json.Serializer<GObject>() {
			@Override
			public void write(Json json, GObject object, Class knownType) {
				json.writeObjectStart();
				json.writeValue("name", object.getName());
				// json.writeValue("tag", object.getTag()); // 暂时没用到

				// --- Components ---
				json.writeArrayStart("components");
				// 遍历所有组件 (扁平化列表)
				for (List<Component> list : object.getComponentsMap().values()) {
					for (Component c : list) {
						// 写入组件，包含 class 信息以便反序列化
						json.writeValue(c, null);
					}
				}
				json.writeArrayEnd();

				// --- Children (递归) ---
				if (!object.getChildren().isEmpty()) {
					json.writeArrayStart("children");
					for (GObject child : object.getChildren()) {
						json.writeValue(child, GObject.class);
					}
					json.writeArrayEnd();
				}

				json.writeObjectEnd();
			}

			@Override
			public GObject read(Json json, JsonValue jsonData, Class type) {
				String name = jsonData.getString("name", "GObject");
				GObject obj = new GObject(name);

				// components
				if (jsonData.has("components")) {
					for (JsonValue compVal : jsonData.get("components")) {
						// 自动根据 class 字段实例化组件
						Component c = json.readValue(Component.class, compVal);
						if (c != null) {
							// 特殊处理 Transform (GObject 自带一个，不要重复添加，而是覆盖数据)
							if (c instanceof TransformComponent) {
								TransformComponent t = (TransformComponent) c;
								obj.transform.position.set(t.position);
								obj.transform.scale = t.scale;
								obj.transform.rotation = t.rotation;
							} else {
								obj.addComponent(c);
							}
						}
					}
				}

				// children
				if (jsonData.has("children")) {
					for (JsonValue childVal : jsonData.get("children")) {
						GObject child = json.readValue(GObject.class, childVal);
						if (child != null) {
							child.setParent(obj);
						}
					}
				}

				return obj;
			}
		});

		// 2. TextureRegion 特殊处理 (只存资源路径，不存像素数据)
		// 注意：这需要 SpriteComponent 里记录了资源路径。
		// 目前 SpriteComponent 只有 region 对象，没有 path 字符串。
		// **我们需要先修改 SpriteComponent 增加 path 字段**，否则无法保存图片引用。

		return json;
	}
}
