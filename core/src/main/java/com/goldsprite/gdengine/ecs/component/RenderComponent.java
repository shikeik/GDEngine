package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.Camera;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 渲染组件基类
 * 所有需要参与世界渲染排序的组件都应继承此类
 */
public abstract class RenderComponent extends Component {

	public RenderComponent() {}
	
	/** [修改] 排序层名称 (对应 RenderLayerManager) */
	public String sortingLayer = "Default";
	
	/** 排序层级 (数值越大越靠前/后覆盖) 层内排序 (微调) */
	public int orderInLayer = 0;

	// [新增] 实现基类钩子，桥接到抽象 render 方法
	@Override
	public void onRender(NeonBatch batch, Camera camera) {
		render(batch, camera);
	}
	
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
	public boolean contains(float x, float y) {
		return false; // 默认不响应点击
	}
}
