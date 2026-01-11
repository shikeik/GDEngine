package com.goldsprite.solofight.screens.editor.utils;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;

public class GObjectSerializer {
	public static String serialize(GObject gobject) {
		Json json = new Json();
		json.setOutputType(JsonWriter.OutputType.json);
		
		json.setSerializer(GObject.class, new Json.Serializer<GObject>() {
			@Override
			public void write(Json json, GObject object, Class knownType) {
				json.writeObjectStart();
				json.writeValue("name", object.getName());
				json.writeValue("tag", object.getTag());
				json.writeValue("layer", object.getLayer());
				
				// Components
				json.writeArrayStart("components");
				for (java.util.List<Component> list : object.getComponentsMap().values()) {
					for (Component c : list) {
						json.writeValue(c, null);
					}
				}
				json.writeArrayEnd();
				
				// Children
				json.writeArrayStart("children");
				for (GObject child : object.getChildren()) {
					json.writeValue(child, GObject.class);
				}
				json.writeArrayEnd();
				
				json.writeObjectEnd();
			}

			@Override
			public GObject read(Json json, com.badlogic.gdx.utils.JsonValue jsonData, Class type) {
				String name = jsonData.getString("name", "GObject");
				GObject gobject = new GObject(name);
				gobject.setTag(jsonData.getString("tag", "Untagged"));
				gobject.setLayer(jsonData.getInt("layer", 0));
				
				// Components
				if (jsonData.has("components")) {
					for (com.badlogic.gdx.utils.JsonValue compValue : jsonData.get("components")) {
						Component c = json.readValue(Component.class, compValue);
						if (c != null) {
							if (c instanceof TransformComponent) {
								TransformComponent existing = gobject.transform;
								TransformComponent newTrans = (TransformComponent) c;
								existing.position.x = newTrans.position.x;
								existing.position.y = newTrans.position.y;
								existing.rotation = newTrans.rotation;
								existing.scale = newTrans.scale;
							} else {
								gobject.addComponent(c);
							}
						}
					}
				}
				
				// Children
				if (jsonData.has("children")) {
					for (com.badlogic.gdx.utils.JsonValue childValue : jsonData.get("children")) {
						GObject child = json.readValue(GObject.class, childValue);
						if (child != null) {
							child.setParent(gobject);
						}
					}
				}
				
				return gobject;
			}
		});
		
		return json.prettyPrint(gobject);
	}
}
