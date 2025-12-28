package com.goldsprite.solofight.core.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.solofight.core.NeonBatch;

public class EffectManager {
	private static EffectManager instance;

	private final Array<Afterimage> afterimages = new Array<>();
	private final Array<LightningTrail> lightnings = new Array<>();

	public static EffectManager inst() {
		if (instance == null) instance = new EffectManager();
		return instance;
	}

	public void clear() {
		afterimages.clear();
		lightnings.clear();
	}

	public void update(float delta) {
		// 更新残影
		for (int i = afterimages.size - 1; i >= 0; i--) {
			if (!afterimages.get(i).update(delta)) afterimages.removeIndex(i);
		}
		// 更新闪电
		for (int i = lightnings.size - 1; i >= 0; i--) {
			if (!lightnings.get(i).update(delta)) lightnings.removeIndex(i);
		}
	}

	public void draw(NeonBatch batch) {
		for (Afterimage a : afterimages) a.draw(batch);
		for (LightningTrail l : lightnings) l.draw(batch);
	}

	// --- API ---
	public void addAfterimage(float x, float y, int dir, String state, float animTimer, Color color) {
		afterimages.add(new Afterimage(x, y, dir, state, animTimer, color));
	}

	public void addLightning(float x1, float y1, float x2, float y2) {
		lightnings.add(new LightningTrail(x1, y1, x2, y2));
	}

	// --- Inner Classes ---

	public static class Afterimage {
		float x, y, animTimer;
		int dir;
		String state;
		Color color;
		float life = 1.0f;

		public Afterimage(float x, float y, int dir, String state, float animTimer, Color color) {
			this.x = x; this.y = y; this.dir = dir;
			this.state = state; this.animTimer = animTimer; this.color = new Color(color);
		}

		public boolean update(float delta) {
			life -= 2.0f * delta; // 0.5秒消失
			return life > 0;
		}

		public void draw(NeonBatch batch) {
			float alpha = life * 0.5f;
			Color c = new Color(color);
			c.a = alpha;
			// 调用 Fighter 的静态绘制方法 (需要 Fighter 配合修改)
			Fighter.drawStickmanFigureStatic(batch, x + 20, y, dir, state, animTimer, c);
		}
	}

	public static class LightningTrail {
		Array<Vector2> path = new Array<>();
		Array<Vector2> origPath = new Array<>();
		float timer = 15; // 帧数计时 (H5逻辑)

		public LightningTrail(float x1, float y1, float x2, float y2) {
			int steps = 10;
			float dx = (x2 - x1) / steps;
			float dy = (y2 - y1) / steps;

			origPath.add(new Vector2(x1, y1));
			path.add(new Vector2(x1, y1));

			for (int i = 1; i < steps; i++) {
				float px = x1 + dx * i;
				float py = y1 + dy * i + (MathUtils.random() - 0.5f) * 40;
				origPath.add(new Vector2(px, py));
				path.add(new Vector2(px, py));
			}

			origPath.add(new Vector2(x2, y2));
			path.add(new Vector2(x2, y2));
		}

		public boolean update(float delta) {
			// H5: timer-- inside draw loop (60fps based)
			timer -= 60 * delta;

			// Jitter effect
			if (timer > 0) {
				for (int i = 1; i < path.size - 1; i++) {
					Vector2 orig = origPath.get(i);
					Vector2 curr = path.get(i);
					curr.set(orig.x + (MathUtils.random()-0.5f)*10, orig.y + (MathUtils.random()-0.5f)*10);
				}
			}
			return timer > 0;
		}

		public void draw(NeonBatch batch) {
			boolean bloom = timer < 5 || ((int)timer % 3 == 0);
			float width = bloom ? 5f : 2f;
			Color c = bloom ? Color.WHITE : Color.CYAN;
			c.a = MathUtils.clamp(timer / 15f, 0, 1);

			for (int i = 0; i < path.size - 1; i++) {
				Vector2 p1 = path.get(i);
				Vector2 p2 = path.get(i+1);
				batch.drawLine(p1.x, p1.y, p2.x, p2.y, width, c);
			}
		}
	}
}
