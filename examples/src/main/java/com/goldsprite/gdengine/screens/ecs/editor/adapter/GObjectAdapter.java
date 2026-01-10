package com.goldsprite.gdengine.screens.ecs.editor.adapter;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ecs.editor.adapter.GObjectWrapperCache;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

/**
 * [逻辑复刻版] GObject 适配器
 * 完美复刻 IconEditor 的 BaseNode 亲缘管理逻辑，解决 Root 节点残留问题。
 */
public class GObjectAdapter implements EditorTarget {

	private final GObject gobj;

	// [新增] 专门记录“非GObject”的父节点 (即编辑器的虚拟 Root)
	// 如果 gobj.parent 不为空，以此为准；否则使用 rootParent。
	private EditorTarget rootParent;

	// 缓存子节点列表，用于保持引用稳定
	private final Array<EditorTarget> cachedChildren = new Array<>();

	public GObjectAdapter(GObject gobj) {
		if (gobj == null) throw new IllegalArgumentException("GObject cannot be null");
		this.gobj = gobj;
	}

	public GObject getGObject() {
		return gobj;
	}

	// --- 基础属性 ---
	@Override public String getName() { return gobj.getName(); }
	@Override public void setName(String name) { gobj.setName(name); }
	@Override public String getTypeName() { return "Entity"; }

	// --- Transform ---
	@Override public float getX() { return gobj.transform.position.x; }
	@Override public void setX(float v) { gobj.transform.position.x = v; }
	@Override public float getY() { return gobj.transform.position.y; }
	@Override public void setY(float v) { gobj.transform.position.y = v; }
	@Override public float getRotation() { return gobj.transform.rotation; }
	@Override public void setRotation(float v) { gobj.transform.rotation = v; }
	@Override public float getScaleX() { return gobj.transform.scale; }
	@Override public void setScaleX(float v) { gobj.transform.scale = v; }
	@Override public float getScaleY() { return gobj.transform.scale; }
	@Override public void setScaleY(float v) { gobj.transform.scale = v; }

	// ==========================================
	// 💀 核心复刻区：亲缘关系 (Hierarchy)
	// ==========================================

	@Override
	public EditorTarget getParent() {
		// 优先返回 ECS 的真实父级
		if (gobj.getParent() != null) {
			return GObjectWrapperCache.get(gobj.getParent());
		}
		// 如果 ECS 没父级，返回我们记录的虚拟 Root
		return rootParent;
	}

	@Override
	public void setParent(EditorTarget newParent) {
		EditorTarget oldParent = getParent();

		// 1. [复刻] 从旧父级移除 (Remove from old parent)
		if (oldParent != null) {
			// 如果旧父级是 Root (非 Adapter)，必须手动从它的列表中移除！
			// 如果旧父级是 Adapter，gobj.setParent 会自动处理，但为了保险/统一，调用 removeValue 也没坏处
			// (虽然 Adapter.getChildren 是动态生成的，调用 removeValue 无效但安全)
			// 关键点：这一步清除了 Root 里的“分身”
			oldParent.getChildren().removeValue(this, true);
		}

		// 2. [复刻] 设置新父级 (Set new parent)
		if (newParent instanceof GObjectAdapter) {
			// 情况A: 认贼作父 (变成子物体)
			this.rootParent = null; // 清空虚拟父引用
			gobj.setParent(((GObjectAdapter) newParent).gobj);
		} else {
			// 情况B: 认祖归宗 (变成顶层物体)
			this.rootParent = newParent; // 记录虚拟父 (Root)
			gobj.setParent(null); // ECS 层面断开连接
		}

		// 3. [复刻] 添加到新父级 (Add to new parent)
		if (newParent != null) {
			// 如果新父级是 Root，必须手动加进去
			// 如果新父级是 Adapter，gobj.setParent 已经加了，但这里再加一次也不会错(List.contains检查)
			if (!newParent.getChildren().contains(this, true)) {
				newParent.addChild(this);
			}
		}
	}

	@Override
	public void removeFromParent() {
		setParent(null);
	}

	@Override
	public Array<EditorTarget> getChildren() {
		// 动态同步：始终反映 ECS 的真实层级
		cachedChildren.clear();
		for (GObject child : gobj.getChildren()) {
			cachedChildren.add(GObjectWrapperCache.get(child));
		}
		return cachedChildren;
	}

	@Override
	public void addChild(EditorTarget child) {
		// 主要是给 SceneManager 调用的入口
		// 实际逻辑由 child.setParent(this) 闭环处理
		if (child != null) {
			child.setParent(this);
		}
	}

	// ==========================================
	// 交互与渲染
	// ==========================================

	@Override
	public boolean hitTest(float wx, float wy) {
		float tx = gobj.transform.worldPosition.x;
		float ty = gobj.transform.worldPosition.y;
		float width = 60; // 稍微加大点击区域
		float height = 60;

		SpriteComponent sprite = gobj.getComponent(SpriteComponent.class);
		if (sprite != null && sprite.region != null) {
			width = sprite.width * Math.abs(gobj.transform.scale);
			height = sprite.height * Math.abs(gobj.transform.scale);
		}

		return wx >= tx - width/2 && wx <= tx + width/2 &&
			wy >= ty - height/2 && wy <= ty + height/2;
	}

	@Override
	public void render(NeonBatch batch) {
		float x = gobj.transform.worldPosition.x;
		float y = gobj.transform.worldPosition.y;

		// 绘制选中高亮框
		float s = 1.0f; // 这里的缩放可以根据 Camera zoom 调整，暂且固定
		float len = 20 * s;
		batch.drawLine(x - len, y, x + len, y, 2, Color.YELLOW);
		batch.drawLine(x, y - len, x, y + len, 2, Color.YELLOW);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof GObjectAdapter) {
			return this.gobj == ((GObjectAdapter) obj).gobj;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return gobj.hashCode();
	}

	public GObject getRealObject() {
		return gobj;
	}
}
