package com.goldsprite.gdengine.ecs.system;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.goldsprite.gdengine.ecs.ComponentManager;
import com.goldsprite.gdengine.ecs.GameSystemInfo;
import com.goldsprite.gdengine.ecs.component.Component;
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
@GameSystemInfo(interestComponents = {RenderComponent.class})
public class WorldRenderSystem extends BaseSystem {

	private final NeonBatch batch;
	private Camera camera;

	// 渲染队列
	private final List<RenderComponent> renderList = new ArrayList<>();

	// 排序器：Order(升) -> Y(降) -> GID(升)
	private final Comparator<RenderComponent> comparator = (c1, c2) -> {
		// 1. Sorting Order
		int orderComp = Integer.compare(c1.sortingOrder, c2.sortingOrder);
		if (orderComp != 0) return orderComp;

		// 2. Y Sort (Descending) - Y越大越在后(背景)，Y越小越在前(前景)
		// 注意: 2D 俯视视角下，脚下的物体(Y小)应该盖住头顶的物体(Y大)
		float y1 = c1.getTransform().worldPosition.y;
		float y2 = c2.getTransform().worldPosition.y;
		int yComp = Float.compare(y2, y1); 
		if (yComp != 0) return yComp;

		// 3. GID (Stable Sort)
		return Integer.compare(c1.getGid(), c2.getGid());
	};

	public WorldRenderSystem(NeonBatch batch, Camera camera) {
		this.batch = batch;
		this.camera = camera;
	}

	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	@Override
	public void update(float delta) {
		// 1. 收集
		collectRenderables();

		// 2. 排序
		Collections.sort(renderList, comparator);

		// 3. 绘制
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		for (RenderComponent rc : renderList) {
			rc.render(batch, camera);
		}
		batch.end();
	}

	private void collectRenderables() {
		renderList.clear();
		List<GObject> entities = getInterestEntities(); // 这个列表现在包含所有有 RenderComponent 的实体

		for (GObject obj : entities) {
			if (!obj.isActive() || obj.isDestroyed()) continue;

			// 获取所有渲染组件 (一个物体可能有多个)
			List<RenderComponent> comps = obj.getComponents(RenderComponent.class);
			for (RenderComponent c : comps) {
				if (c.isEnable() && !c.isDestroyed()) {
					renderList.add(c);
				}
			}
		}
	}

	/**
	 * 获取已排序的渲染列表 (供编辑器点击检测使用)
	 */
	public List<RenderComponent> getSortedRenderables() {
		return renderList;
	}
}
