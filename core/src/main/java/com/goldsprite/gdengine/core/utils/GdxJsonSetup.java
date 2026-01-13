package com.goldsprite.gdengine.core.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.log.Debug;

import java.util.List;

/**
 * 引擎专用的 JSON 配置中心
 * 负责处理 GObject、Component 以及特殊类型的序列化逻辑
 */
public class GdxJsonSetup {

	public static ScriptJson create() {
		ScriptJson json = new ScriptJson();
		json.setOutputType(JsonWriter.OutputType.json);
		json.setUsePrototypes(false); // 禁止引用复用，保证每个物体数据独立
		json.setIgnoreUnknownFields(true);

		// [新增] TransformComponent 专用兼容性序列化器
		json.setSerializer(TransformComponent.class, new Json.Serializer<>() {
			@Override
			public void write(Json json, TransformComponent object, Class knownType) {
				json.writeObjectStart();
				json.writeValue("class", TransformComponent.class.getName());
				// 写入基本属性
				json.writeValue("position", object.position);
				json.writeValue("rotation", object.rotation);
				json.writeValue("scale", object.scale); // 写入 Vector2
				// 注意：world系列属性是缓存，不需要保存
				json.writeObjectEnd();
			}

			@Override
			public TransformComponent read(Json json, JsonValue jsonData, Class type) {
				TransformComponent t = new TransformComponent();

				// 1. 读取 Position
				if (jsonData.has("position")) {
					t.position.set(json.readValue(Vector2.class, jsonData.get("position")));
				}

				// 2. 读取 Rotation
				t.rotation = jsonData.getFloat("rotation", 0f);

				// 3. [核心兼容] 读取 Scale
				if (jsonData.has("scale")) {
					JsonValue scaleVal = jsonData.get("scale");
					if (scaleVal.isNumber()) {
						// 兼容旧档：如果是数字 (e.g. 1.0)，应用到 X 和 Y
						float s = scaleVal.asFloat();
						t.scale.set(s, s);
					} else {
						// 新档：如果是对象 (e.g. {x:1, y:1})，正常读取
						t.scale.set(json.readValue(Vector2.class, scaleVal));
					}
				}

				// 强制更新一次矩阵，防止初始数据不同步
				t.updateWorldTransform(null);

				return t;
			}
		});

		// 1. GObject 序列化器
		json.setSerializer(GObject.class, new Json.Serializer<>() {
			@Override
			public void write(Json json, GObject object, Class knownType) {
				json.writeObjectStart();
				json.writeValue("name", object.getName());
				json.writeValue("tag", object.getTag());
				json.writeValue("layer", object.getLayer());

				// [新增] 保存 transform 数据 (虽然它是组件，但它是特殊的)
				// 为了 JSON 可读性，我们可以把 Transform 扁平化，或者依然作为组件列表的一部分
				// 这里选择标准做法：Transform 也是组件，走组件列表逻辑

				// --- Components ---
				json.writeArrayStart("components");
				for (List<Component> list : object.getComponentsMap().values()) {
					for (Component c : list) {
						// [核心修复] 过滤匿名内部类
						// 匿名类通常包含 logic 代码，无法被正确反序列化，且不应作为数据资产保存
						if (c.getClass().isAnonymousClass()) {
							Debug.log("Json: Skipping anonymous component on " + object.getName());
							continue;
						}

						// 写入组件，包含 class 信息以便反序列化
						json.writeValue(c, null);
					}
				}
				json.writeArrayEnd();

				// --- Children (递归) ---
				if (!object.getChildren().isEmpty()) {
					json.writeArrayStart("children");
					for (GObject child : object.getChildren()) {
						// [优化] 不要保存临时生成的物体 (比如编辑器 Gizmo 或特效)
						// 可以增加一个 isTransient 标记，或者默认只保存 GObject
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

				if (jsonData.has("tag")) obj.setTag(jsonData.getString("tag"));
				if (jsonData.has("layer")) obj.setLayer(jsonData.getInt("layer"));

				// components
				if (jsonData.has("components")) {
					for (JsonValue compVal : jsonData.get("components")) {
						try {
							// 1. 这里调用专用序列化器，处理兼容性，得到一个临时的 t
							Component c = json.readValue(Component.class, compVal);

							if (c != null) {
								// 2. 特殊处理 Transform：做属性拷贝
								if (c instanceof TransformComponent) {
									TransformComponent t = (TransformComponent) c;
									// 拷贝 Local 属性
									obj.transform.position.set(t.position);
									obj.transform.rotation = t.rotation;
									// [修正] 必须拷贝 scale (Local)，而不是 worldScale
									// 因为专用序列化器是把数据读进 scale 里的
									obj.transform.scale.set(t.scale);

									// 顺手更新一下矩阵，让 world 属性生效
									obj.transform.updateWorldTransform(null);
								} else {
									// 3. 普通组件：直接挂载
									obj.addComponent(c);
								}
							}
						} catch (Exception e) {
							Debug.log("Json: Component load failed: " + e.getMessage());
						}
					}
				}
				// ... (children 处理保持不变) ...
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

		return json;
	}

	public static class ScriptJson extends Json {
		private ClassLoader customClassLoader;

		/**
		 * 这就是你想要的 setClassLoader 方法
		 */
		public void setClassLoader(ClassLoader classLoader) {
			this.customClassLoader = classLoader;
		}

		@Override
		public Class<?> getClass(String className) {
			// 1. 如果设置了自定义 ClassLoader，优先尝试使用它加载
			if (customClassLoader != null) {
				try {
					return Class.forName(className, true, customClassLoader);
				} catch (ClassNotFoundException e) {
					// 忽略异常，继续尝试默认加载器
				}
			}

			// 2. 如果自定义加载器没找到，或者没设置，使用 LibGDX 默认逻辑
			return super.getClass(className);
		}
	}
}
