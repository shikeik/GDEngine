package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.core.scripting.ScriptResourceTracker;

public class SpriteComponent extends Component {

	// [新增] 记录资源路径，用于序列化保存
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
}
