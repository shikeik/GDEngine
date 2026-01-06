package com.goldsprite.gdengine.ecs.skeleton.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonKeyframe;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;

public class NeonJsonUtils {

	private static final Json json = new Json();

	static {
		json.setOutputType(JsonWriter.OutputType.json);
		json.setIgnoreUnknownFields(true);
	}

	/** 将 Runtime 对象转为 JSON 字符串 (用于保存) */
	public static String toJson(NeonAnimation anim) {
		NeonDataModels.AnimData data = new NeonDataModels.AnimData();
		data.name = anim.name;
		data.duration = anim.duration;
		data.looping = anim.looping;

		for (NeonTimeline tl : anim.timelines) {
			NeonDataModels.TimelineData tlData = new NeonDataModels.TimelineData();
			tlData.bone = tl.boneName;
			tlData.prop = tl.property.name();

			for (NeonKeyframe kf : tl.frames) {
				NeonDataModels.KeyframeData kData = new NeonDataModels.KeyframeData();
				kData.t = kf.time;
				kData.c = kf.curve.name();

				if (tl.property == NeonProperty.SPRITE) {
					// 暂时只支持 String 类型的 SpriteName (如果是 TextureRegion 对象没法直接存)
					// 约定：运行时如果是 Object，toString 必须返回资源名
					if (kf.objectValue != null) kData.s = kf.objectValue.toString();
				} else {
					kData.v = kf.floatValue;
				}
				tlData.keys.add(kData);
			}
			data.timelines.add(tlData);
		}
		return json.prettyPrint(data);
	}

	/** 将 JSON 字符串转为 Runtime 对象 */
	public static NeonAnimation fromJson(String jsonStr) {
		NeonDataModels.AnimData data = json.fromJson(NeonDataModels.AnimData.class, jsonStr);
		if (data == null) throw new IllegalArgumentException("Invalid JSON");

		NeonAnimation anim = new NeonAnimation(data.name, data.duration, data.looping);

		for (NeonDataModels.TimelineData tlData : data.timelines) {
			NeonProperty prop;
			try {
				prop = NeonProperty.valueOf(tlData.prop);
			} catch (Exception e) {
				System.err.println("Unknown property: " + tlData.prop);
				continue;
			}

			NeonTimeline tl = new NeonTimeline(tlData.bone, prop);
			for (NeonDataModels.KeyframeData kData : tlData.keys) {
				NeonCurve curve = NeonCurve.LINEAR;
				try { if(kData.c != null) curve = NeonCurve.valueOf(kData.c); } catch(Exception ignored){}

				if (prop == NeonProperty.SPRITE) {
					// 这里反序列化出来的是 String (图片名)
					// 实际 Animator 运行时需要根据这个 String 去 TextureAtlas 里找图
					// 我们暂且存 String
					tl.addKeyframe(kData.t, kData.s);
				} else {
					tl.addKeyframe(kData.t, kData.v, curve);
				}
			}
			anim.addTimeline(tl);
		}
		return anim;
	}
}
