package com.goldsprite.gdengine.ecs.system;

import com.badlogic.gdx.graphics.Camera;
import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.SystemType;
import com.goldsprite.gdengine.ecs.component.RenderComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 统一渲染系统
 * 职责：收集所有 RenderComponent，排序，并执行绘制。
 */
// [核心修复] 显式标记为 RENDER 类型
@GameSystemInfo(type = SystemType.RENDER, interestComponents = {RenderComponent.class})
public class WorldRenderSystem extends BaseSystem {

	private final List<RenderComponent> renderList = new ArrayList<>();

	// 排序器：LayerDepth -> OrderInLayer
	private final Comparator<RenderComponent> comparator = (c1, c2) -> {
		// 1. Layer Depth
		int depth1 = RenderLayerManager.getLayerDepth(c1.sortingLayer);
		int depth2 = RenderLayerManager.getLayerDepth(c2.sortingLayer);
		if (depth1 != depth2) return Integer.compare(depth1, depth2);

		// 2. Order in Layer
		return Integer.compare(c1.orderInLayer, c2.orderInLayer);
	};

	public WorldRenderSystem() {}
	public WorldRenderSystem(NeonBatch batch, Camera camera) {}

	@Override
	public void render(NeonBatch batch, Camera camera) {
		// 渲染循环专用：清空并复用成员变量，零GC
		renderList.clear();
		collectTo(renderList); // 复用逻辑

		Collections.sort(renderList, comparator);

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		for (RenderComponent rc : renderList) {
			rc.render(batch, camera);
		}
		batch.end();
	}

	/**
	 * [新增] 对外查询接口：获取当前所有需要渲染的组件（排序后）
	 * 专门给点击检测 (Picking) 使用，与渲染循环解耦，互不干扰。
	 */
	public List<RenderComponent> queryRenderables() {
		List<RenderComponent> result = new ArrayList<>();
		collectTo(result);
		result.sort(comparator);
		return result;
	}

	/** 提取出的公共逻辑 */
	private void collectTo(List<RenderComponent> targetList) {
		List<GObject> entities = getInterestEntities();
		for (GObject obj : entities) {
			if (!obj.isActive() || obj.isDestroyed()) continue;

			List<RenderComponent> comps = obj.getComponents(RenderComponent.class);
			for (RenderComponent c : comps) {
				if (c.isEnable() && !c.isDestroyed()) {
					if (RenderLayerManager.isLayerWorldSpace(c.sortingLayer)) {
						targetList.add(c);
					}
				}
			}
		}
	}

	// 这个方法之前返回的是成员变量，现在为了安全，建议废弃或仅供调试
	public List<RenderComponent> getSortedRenderables() {
		return renderList;
	}
}
