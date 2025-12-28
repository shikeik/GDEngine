package com.goldsprite.solofight.core.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.audio.SynthAudio;
import com.goldsprite.solofight.core.input.InputContext;

public class Fighter {
	// 基础属性
	public float x, y;
	public float vx, vy;
	public float w = 40, h = 90;
	public Color color;
	public int dir = 1;
	public boolean isAi;

	// 状态机
	public String state = "idle";
	public float animTimer = 0;
	public boolean inAir = false;

	// 战斗属性
	public float hp = 500, maxHp = 500;
	public float mp = 0;

	// 技能数据
	public float slashStartX, flashTargetX;

	// 引用
	private Fighter enemy;
	private final Color drawColor = new Color(); // 缓存绘制颜色
	private float hitFlashTimer = 0; // 受击闪白计时器

	public Fighter(float x, Color color, boolean isAi) {
		this.x = x;
		this.y = 0;
		this.color = color;
		this.isAi = isAi;
		this.dir = isAi ? -1 : 1;
	}

	public void setEnemy(Fighter enemy) { this.enemy = enemy; }

	public void handleCommand(String cmd) {
		if (state.equals("dead") || state.equals("hit") || state.equals("flash_slash")) return;

		switch (cmd) {
			case "CMD_JUMP":
				if (!inAir) {
					vy = 22; // 调高跳跃力度
					state = "jump";
					inAir = true; // 立即标记为滞空
					SynthAudio.playTone(200, SynthAudio.WaveType.SINE, 0.2f, 0.1f, 400);
				}
				break;
			case "CMD_ATK":
				attack();
				break;
			case "CMD_DASH_L": dash(-1); break;
			case "CMD_DASH_R": dash(1); break;
			case "FLASH SLASH": flashSlash(); break;
		}
	}

	public void update(float delta) {
		InputContext input = InputContext.inst();

		// --- 1. Control Logic ---
		if (!isAi) {
			// 只有在空闲、跑动或跳跃时允许移动
			if (state.equals("idle") || state.equals("run") || state.equals("jump")) {
				float moveX = input.moveX;
				this.vx = moveX * 6;

				if (moveX != 0) {
					this.dir = (int) Math.signum(moveX);
					if (!inAir) this.state = "run";
				} else {
					if (!inAir) this.state = "idle";
				}
			}
			// 下蹲状态覆盖 (但在 H5 逻辑里下蹲由 InputContext 直接处理 State)
			// 这里我们简化：如果 Input 说 Crouch 且在地表，那就是 idle 姿态下的 crouch 绘制
		}

		// --- 2. Physics ---
		vy -= 0.9f * (60 * delta); // 增加重力感
		x += vx * (60 * delta);
		y += vy * (60 * delta);

		// Ground Collision
		if (y < 0) {
			y = 0;
			vy = 0;
			inAir = false;
			// 落地重置状态
			if (state.equals("jump") || state.equals("hit")) state = "idle";
		} else {
			inAir = true;
			// 如果由跑动滑出平台，切换为跳跃/下落姿态
			if (state.equals("run")) state = "jump";
		}

		// Friction
		if (!state.equals("dash") && !state.equals("flash_slash") && vx != 0) {
			vx *= 0.8f;
			if (Math.abs(vx) < 0.1f) vx = 0;
		}

		// Boundaries
		if (x < 0) x = 0;
		if (x > 1000 - w) x = 1000 - w;

		// --- 3. Hit Logic (Attack Collision) ---
		if (state.equals("attack")) {
			// 在第 2-5 帧判定伤害
			if (animTimer >= 2 && animTimer <= 5) {
				// 计算攻击框: 面前 80px 范围
				float atkX = x + w/2 + (40 * dir);
				float atkRange = 80;

				// 简单的距离判定
				if (enemy != null && Math.abs(enemy.x + enemy.w/2 - atkX) < atkRange && Math.abs(enemy.y - y) < 50) {
					enemy.takeDamage(15, dir);
					// 命中反馈 (顿帧)
					// TODO: Global Hitstop
				}
			}
		}

		// --- 4. Timers ---
		animTimer += 1.0f * (60 * delta);
		if (hitFlashTimer > 0) hitFlashTimer -= delta;

		updateSkills(delta);
	}

	public void takeDamage(float dmg, int hitDir) {
		if (state.equals("dead") || state.equals("flash_slash")) return;

		hp -= dmg;
		if (hp <= 0) hp = 0;

		state = "hit";
		vx = 8 * hitDir; // 被击退
		vy = 5;          // 被打飞一点
		animTimer = 0;
		hitFlashTimer = 0.2f; // 闪白 0.2秒

		SynthAudio.playTone(100, SynthAudio.WaveType.SAWTOOTH, 0.2f, 0.2f, 50); // Hit Sound
	}

	private void attack() {
		state = "attack";
		animTimer = 0;
		vx = 0;
		SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f);
	}

	private void dash(int d) {
		if (d == 0) d = dir;
		this.dir = d;
		this.state = "dash";
		this.animTimer = 0;
		this.vx = 25 * d;
		SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f);
	}

	private void flashSlash() {
		this.state = "flash_slash";
		this.animTimer = 0;
		this.vx = 0;
		this.slashStartX = this.x;
		if (enemy != null) {
			this.flashTargetX = enemy.x + (enemy.x > this.x ? 100 : -100);
		} else {
			this.flashTargetX = this.x + (300 * dir);
		}
		SynthAudio.playTone(800, SynthAudio.WaveType.SINE, 0.1f, 0.1f, 50);
	}

	private void updateSkills(float delta) {
		if (state.equals("attack") && animTimer > 15) state = "idle";

		if (state.equals("dash")) {
			vy = 0;
			if (animTimer > 10) { state = "idle"; vx = 0; }
		}

		if (state.equals("flash_slash")) {
			vy = 0;
			if (animTimer >= 6 && animTimer < 7) {
				this.x = flashTargetX;
				// 瞬移命中判定
				if (enemy != null && Math.abs(x - enemy.x) < 150) {
					enemy.takeDamage(40, dir);
				}
				SynthAudio.playTone(600, SynthAudio.WaveType.SAWTOOTH, 0.1f, 0.2f, 2000);
			}
			if (animTimer > 25) state = "idle";
		}
	}

	public void draw(NeonBatch batch) {
		if (state.equals("flash_slash") && animTimer >= 6 && animTimer < 18) return;

		float cx = x + w / 2;
		float cy = y;

		// 颜色处理：受击变白，闪刀蓄力变白
		drawColor.set(this.color);
		if (hitFlashTimer > 0 || (state.equals("flash_slash") && animTimer < 6)) {
			drawColor.set(Color.WHITE);
		}

		drawStickmanFigure(batch, cx, cy, drawColor);
	}

	// [修正 1] 渲染坐标修正：NeonBatch.drawRect 原点是中心
	private void drawStickmanFigure(NeonBatch batch, float cx, float cy, Color c) {
		float lineWidth = 4f;

		// 1. Legs
		if (state.equals("jump") || state.equals("dash") || state.equals("flash_slash")) {
			// Jump: 收腿
			drawLine(batch, cx, cy, -10, 40, -25, 15, c, lineWidth);
			drawLine(batch, cx, cy, 10, 40, 25, 25, c, lineWidth);
		} else if (state.equals("run")) {
			float cycle = MathUtils.sin(animTimer * 0.5f) * 15;
			drawLine(batch, cx, cy, -10, 40, -20 + cycle, 0, c, lineWidth);
			drawLine(batch, cx, cy, 10, 40, 20 - cycle, 0, c, lineWidth);
		} else {
			// Idle
			drawLine(batch, cx, cy, -10, 40, -20, 0, c, lineWidth);
			drawLine(batch, cx, cy, 10, 40, 20, 0, c, lineWidth);
		}

		// 2. Body
		boolean isCrouch = (state.equals("flash_slash") && animTimer < 5) || (state.equals("idle") && InputContext.inst().crouch && !isAi);

		// H5 rect(-15, -80, 30, 40) (from feet 0).
		// Y range: 40 to 80 (Height 40). Center Y = 60.
		// Crouch Y range: 30 to 60 (Height 30). Center Y = 45.

		if (isCrouch) {
			// Crouch Body (Center: cx, cy+45)
			batch.drawRect(cx, cy + 45, 30, 30, 0, lineWidth, c, true);
		} else {
			// Normal Body (Center: cx, cy+60)
			batch.drawRect(cx, cy + 60, 30, 40, 0, lineWidth, c, true);
		}

		// 3. Head
		float headY = isCrouch ? 65 : 90;
		batch.drawCircle(cx, cy + headY, 12, 0, c, 16, true);

		// 4. Arms
		if (state.equals("attack")) {
			drawLine(batch, cx, cy, -10, 70, -20, 40, c, lineWidth);
			drawLine(batch, cx, cy, 10, 70, 40, 70, c, lineWidth);
		} else {
			float armS = state.equals("run") ? MathUtils.sin(animTimer * 0.5f) * 15 : 0;
			drawLine(batch, cx, cy, -10, 70, -20 + armS, 30, c, lineWidth);
			drawLine(batch, cx, cy, 10, 70, 20 - armS, 30, c, lineWidth);
		}
	}

	private void drawLine(NeonBatch batch, float cx, float cy, float lx1, float ly1, float lx2, float ly2, Color c, float width) {
		float x1 = cx + (lx1 * dir);
		float y1 = cy + ly1;
		float x2 = cx + (lx2 * dir);
		float y2 = cy + ly2;
		batch.drawLine(x1, y1, x2, y2, width, c);
	}
}
