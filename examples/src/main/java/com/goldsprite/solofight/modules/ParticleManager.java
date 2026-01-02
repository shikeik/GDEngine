package com.goldsprite.solofight.modules;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class ParticleManager {
	private static ParticleManager instance;
	private final Array<Particle> particles = new Array<>();

	public static ParticleManager inst() {
		if (instance == null) instance = new ParticleManager();
		return instance;
	}

	public enum Type { BOX, SHARD }

	public static class Particle {
		float x, y, vx, vy;
		float size, life, maxLife;
		Color color;
		Type type;
		float rotation, vRot; // For Shard

		public boolean update(float delta) {
			// H5: update runs 60 times/sec roughly.
			// x += vx * timeScale; life -= 0.02 * timeScale;
			float dtScale = 60 * delta; // Normalize to frames

			x += vx * dtScale;
			y += vy * dtScale;
			life -= 0.02f * dtScale;

			if (type == Type.SHARD) rotation += vRot * dtScale;

			return life > 0;
		}

		public void draw(NeonBatch batch) {
			float alpha = Math.max(0, life / maxLife);
			Color c = new Color(color);
			c.a = alpha;

			if (type == Type.BOX) {
				batch.drawRect(x-size/2f, y-size/2f, size, size, 0, 0, c, true); // Solid Box
			} else if (type == Type.SHARD) {
				// Draw Triangle
				// H5: moveTo(-size, size), lineTo(0, -size), lineTo(size, size)
				// Need to rotate vertices
				batch.drawRegularPolygon(x, y, size, 3, rotation, 0, c, true);
			}
		}
	}

	public void spawn(float x, float y, Color color, float speed, float size, float lifeScale, Type type) {
		Particle p = new Particle();
		p.x = x; p.y = y; p.color = new Color(color);
		p.vx = (MathUtils.random() - 0.5f) * speed;
		p.vy = (MathUtils.random() - 0.5f) * speed;
		p.life = lifeScale; p.maxLife = lifeScale;
		p.size = size;
		p.type = type;

		if (type == Type.SHARD) {
			p.rotation = MathUtils.random(360f);
			p.vRot = (MathUtils.random() - 0.5f) * 10f; // Spin speed
		}
		particles.add(p);
	}

	// 辅助: 批量生成
	public void burst(float x, float y, Color color, int count, Type type, float speedBase) {
		for (int i = 0; i < count; i++) {
			float s = (type == Type.SHARD) ? 8 : 4;
			float l = (type == Type.SHARD) ? 1.5f : 1.0f; // H5 life is around 1.0
			spawn(x, y, color, speedBase, s, l, type);
		}
	}

	public void update(float delta) {
		for (int i = particles.size - 1; i >= 0; i--) {
			if (!particles.get(i).update(delta)) {
				particles.removeIndex(i);
			}
		}
	}

	public void draw(NeonBatch batch) {
		for (Particle p : particles) {
			p.draw(batch);
		}
	}

	public void clear() { particles.clear(); }
}
