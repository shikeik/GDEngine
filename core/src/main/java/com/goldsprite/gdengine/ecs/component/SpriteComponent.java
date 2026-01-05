package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.core.scripting.ScriptResourceTracker;

/**
 * 精灵组件 (帧动画的基础)
 * 职责：持有一个 TextureRegion，供 Animator 切换，供 SpriteSystem 渲染。
 */
public class SpriteComponent extends Component {

	public TextureRegion region;
	public Color color = new Color(Color.WHITE);
	public boolean flipX = false;
	public boolean flipY = false;

	// 偏移量 (相对于 Entity Transform)
	public float offsetX = 0;
	public float offsetY = 0;

	public float width = 0;
	public float height = 0;

	public SpriteComponent() {}

	/**
	 * [新增] 脚本专用快捷构造
	 * 自动从项目 assets 目录加载图片
	 * @param fileName 例如 "hero.png"
	 */
	public SpriteComponent(String fileName) {
		TextureRegion reg = ScriptResourceTracker.loadRegion(fileName);
		if (reg != null) {
			setRegion(reg);
		}
	}

	public void setRegion(TextureRegion region) {
		this.region = region;
		// 如果未设置宽高，自动应用图片原始比例
		if (width == 0 && height == 0 && region != null) {
			width = region.getRegionWidth();
			height = region.getRegionHeight();
		}
	}
}
