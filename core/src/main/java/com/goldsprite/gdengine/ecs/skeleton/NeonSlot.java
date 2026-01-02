package com.goldsprite.gdengine.ecs.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 渲染插槽 (渲染层)
 * 职责：连接骨骼与皮肤，承载渲染顺序
 */
public class NeonSlot {
	public final String name;
	public final NeonBone bone; // 依附的骨骼

	public BoneSkin skin;       // 画什么 (Attachment)
	public Color color = new Color(Color.WHITE); // 叠加颜色

	public NeonSlot(String name, NeonBone bone, BoneSkin skin) {
		this.name = name;
		this.bone = bone;
		this.skin = skin;
	}

	public void draw(NeonBatch batch) {
		if (bone == null || skin == null) return;
		// 调用 Skin 绘制
		skin.draw(batch, bone.worldTransform, bone.length, color);
	}
}
