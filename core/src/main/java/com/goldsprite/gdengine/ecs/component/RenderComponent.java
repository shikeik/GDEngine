package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.Camera;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 渲染组件基类
 * 所有需要参与世界渲染排序的组件都应继承此类
 */
public abstract class RenderComponent extends Component {

	public RenderComponent() {}int k;
	
	/** 排序层级 (数值越大越靠前/后覆盖) */
	public int sortingOrder = 0;

	/**
	 * 执行绘制
	 * @param batch 统一的渲染批处理
	 * @param camera 当前渲染使用的相机 (用于剔除或LOD等)
	 */
	public abstract void render(NeonBatch batch, Camera camera);

	/**
	 * 命中检测 (用于编辑器选中)
	 * @param x 世界坐标 X
	 * @param y 世界坐标 Y
	 * @return 是否包含该点
	 */
	public abstract boolean contains(float x, float y);
}
