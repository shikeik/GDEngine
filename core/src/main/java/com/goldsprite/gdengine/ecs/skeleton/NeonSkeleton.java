package com.goldsprite.gdengine.ecs.skeleton;

import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * 骨架组件 (Data Container)
 * 职责：持有 NeonSkeleton 数据。
 * 注意：矩阵计算逻辑已移交至 SkeletonSystem，确保在所有逻辑更新后执行。
 */
public class NeonSkeleton {
	// 逻辑根
	public NeonBone rootBone;

	// 查找表 (name -> bone)
	private final Map<String, NeonBone> boneMap = new HashMap<>();

	// 渲染队列 (扁平列表，决定遮挡关系)
	public final Array<NeonSlot> drawOrder = new Array<>();

	public NeonSkeleton() {
		// 默认创建一个 Root 节点
		rootBone = new NeonBone("root", 0);
		boneMap.put("root", rootBone);
	}

	/** 核心工厂方法：创建一个骨骼并自动创建对应的 Slot */
	public NeonBone createBone(String name, String parentName, float length, BoneSkin defaultSkin) {
		NeonBone bone = new NeonBone(name, length);

		// 1. 构建树 (Logic Hierarchy)
		NeonBone parent = boneMap.get(parentName);
		if (parent != null) {
			parent.addChild(bone);
		} else if (!name.equals("root")) {
			// 如果找不到父级，默认挂在 root 下
			rootBone.addChild(bone);
		}
		boneMap.put(name, bone);

		// 2. 构建渲染 Slot (Render Node)
		// 默认名字和骨骼一样
		NeonSlot slot = new NeonSlot(name, bone, defaultSkin);

		// 3. 加入渲染队列 (默认加在最后，即最上层)
		drawOrder.add(slot);

		return bone;
	}

	public NeonBone getBone(String name) {
		return boneMap.get(name);
	}

	public NeonSlot getSlot(String name) {
		for(NeonSlot slot : drawOrder) {
			if(slot.name.equals(name)) return slot;
		}
		return null;
	}

	public void update() {
		if (rootBone != null) rootBone.updateWorldTransform();
	}

	public Array<NeonSlot> getDrawOrder() {
		return drawOrder;
	}

	/** 手动调整插槽渲染顺序 */
	public void setSlotOrder(String slotName, int newIndex) {
		NeonSlot target = null;
		for(NeonSlot s : drawOrder) {
			if(s.name.equals(slotName)) { target = s; break; }
		}
		if(target != null) {
			drawOrder.removeValue(target, true);
			drawOrder.insert(Math.min(newIndex, drawOrder.size), target);
		}
	}
}
