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
		collectRenderables();
		Collections.sort(renderList, comparator);

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		for (RenderComponent rc : renderList) {
			rc.render(batch, camera);
		}
		batch.end();
	}

	private void collectRenderables() {
		renderList.clear();
		List<GObject> entities = getInterestEntities();
		
		for (GObject obj : entities) {
			if (!obj.isActive() || obj.isDestroyed()) continue;
			
			List<RenderComponent> comps = obj.getComponents(RenderComponent.class);
			for (RenderComponent c : comps) {
				if (c.isEnable() && !c.isDestroyed()) {
					if (RenderLayerManager.isLayerWorldSpace(c.sortingLayer)) {
						renderList.add(c);
					}
				}
			}
		}
	}

	public List<RenderComponent> getSortedRenderables() {
		return renderList;
	}
}