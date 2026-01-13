package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.core.scripting.ScriptResourceTracker;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

// [新增] 命中检测逻辑 (OBB)
public class SpriteComponent extends RenderComponent {

	// 记录资源路径，用于序列化保存
	public String assetPath = "";

	// [忽略序列化] 运行时对象，不要存进 JSON
	public transient TextureRegion region;

	public Color color = new Color(Color.WHITE);
	public boolean flipX = false;
	public boolean flipY = false;
	public float offsetX = 0;
	public float offsetY = 0;
	public float width = 0;
	public float height = 0;

	public SpriteComponent() {}

	public SpriteComponent(String fileName) {
		setPath(fileName);
	}

	// 核心修复：组件苏醒时，如果发现有路径但没图片，说明是刚加载出来的，立即恢复
	@Override
	protected void onAwake() {
		if (region == null && assetPath != null && !assetPath.isEmpty()) {
			reloadRegion();
		}
	}

	/** 设置路径并尝试加载 */
	public void setPath(String path) {
		this.assetPath = path;
		reloadRegion();
	}

	public void reloadRegion() {
		if (assetPath != null && !assetPath.isEmpty()) {
			TextureRegion reg = ScriptResourceTracker.loadRegion(assetPath);
			if (reg != null) {
				setRegion(reg);
			}
		}
	}

	public void setRegion(TextureRegion region) {
		this.region = region;
		// 只有当尺寸未初始化时才自动设置，避免覆盖用户调整过的大小
		if (width == 0 && height == 0 && region != null) {
			width = region.getRegionWidth();
			height = region.getRegionHeight();
		}
	}
	
	// [新增] 迁移自 SpriteSystem 的绘制逻辑
	@Override
	public void render(NeonBatch batch, Camera camera) {
		if (!isEnable() || region == null || transform == null) return;

		float x = transform.worldPosition.x;
		float y = transform.worldPosition.y;
		float rotation = transform.worldRotation;
		float sx = transform.worldScale.x;
		float sy = transform.worldScale.y;

		// Offset 逻辑
		float ox = offsetX * sx;
		float oy = offsetY * sy;
		if (rotation != 0) {
			float cos = MathUtils.cosDeg(rotation);
			float sin = MathUtils.sinDeg(rotation);
			x += ox * cos - oy * sin;
			y += ox * sin + oy * cos;
		} else {
			x += ox; y += oy;
		}

		if (flipX) sx = -sx;
		if (flipY) sy = -sy;

		Color oldColor = batch.getColor();
		batch.setColor(this.color);
		batch.draw(region, x - width / 2f, y - height / 2f, width / 2f, height / 2f, width, height, sx, sy, rotation);
		batch.setColor(oldColor);
	}

	// [新增] 命中检测逻辑 (OBB)
	@Override
	public boolean contains(float worldX, float worldY) {
		if (transform == null) return false;

		// 将世界点转换到本地空间进行 AABB 检测
		// 1. 平移
		float lx = worldX - transform.worldPosition.x;
		float ly = worldY - transform.worldPosition.y;

		// 2. 旋转 (逆旋转)
		float rot = -transform.worldRotation;
		float cos = MathUtils.cosDeg(rot);
		float sin = MathUtils.sinDeg(rot);
		float rx = lx * cos - ly * sin;
		float ry = lx * sin + ly * cos;

		// 3. 缩放
		// 注意: width/height 是原始大小，包含了 scale 影响的显示大小是 width*scale
		// 所以如果不除以 scale，我们要比较 rx vs width*scale/2
		// 这里选择将点除以 scale，比较 rx' vs width/2
		float sx = Math.abs(transform.worldScale.x);
		float sy = Math.abs(transform.worldScale.y);
		if (sx == 0 || sy == 0) return false;

		rx /= sx;
		ry /= sy;

		// 4. AABB 判定 (中心点为 0,0)
		float halfW = width / 2f;
		float halfH = height / 2f;

		return rx >= -halfW && rx <= halfW && ry >= -halfH && ry <= halfH;
	}
}
