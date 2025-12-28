package com.goldsprite.solofight.core.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.audio.SynthAudio;
import com.goldsprite.solofight.core.input.InputContext;

public class Fighter {
	public float x, y, vx, vy;
	public float w = 40, h = 90;
	public Color color;
	public int dir = 1;
	public boolean isAi;
	public String state = "idle";
	public float animTimer = 0;
	public boolean inAir = false;
	public float hp = 500, maxHp = 500;
	public float mp = 0;
	public float slashStartX, flashTargetX;
	private Fighter enemy;
	private final Color drawColor = new Color();
	private float hitFlashTimer = 0;

	public float ultTimer = 0;
	public boolean isUltActive = false;

	private static class SlashLine {
		float x, y, len, angle, life, maxLife, delay;
	}
	private final Array<SlashLine> slashLines = new Array<>();

	public Fighter(float x, Color color, boolean isAi) {
		this.x = x; this.y = 0; this.color = color; this.isAi = isAi;
		this.dir = isAi ? -1 : 1;
		this.mp = 100;
	}

	public void setEnemy(Fighter enemy) { this.enemy = enemy; }

	public void handleCommand(String cmd) {
		if (state.startsWith("ult") || state.equals("dead") || state.equals("hit") || state.equals("flash_slash")) return;

		switch (cmd) {
			case "CMD_JUMP": if(!inAir) { vy=22; state="jump"; inAir=true; SynthAudio.playTone(200, SynthAudio.WaveType.SINE, 0.2f, 0.1f, 400); } break;
			case "CMD_ATK": attack(); break;
			case "CMD_DASH_L": dash(-1); break;
			case "CMD_DASH_R": dash(1); break;
			case "CMD_DASH_AUTO": dash(dir); break;
			case "FLASH SLASH": flashSlash(); break;
			case "CMD_ULT": performUlt(); break;
		}
	}

	private void performUlt() {
		if (mp < 100) return;
		mp = 0; state = "ult_cast"; ultTimer = 0; isUltActive = true;
		SynthAudio.playTone(50, SynthAudio.WaveType.SQUARE, 1.0f, 0.2f);
		SynthAudio.playTone(1000, SynthAudio.WaveType.SAWTOOTH, 1.0f, 0.1f, 100);
		if (enemy != null) { enemy.state = "ult_frozen"; enemy.vx = 0; enemy.vy = 0; }
	}

	public void update(float delta) {
		InputContext input = InputContext.inst();

		if (state.startsWith("ult")) { updateUltLogic(delta); return; }
		if (state.equals("ult_frozen")) return;

		if (!isAi) {
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
		}

		vy -= 0.9f * (60 * delta);
		x += vx * (60 * delta);
		y += vy * (60 * delta);

		if (y < 0) { y = 0; vy = 0; inAir = false; if(state.equals("jump")||state.equals("hit")) state="idle"; }
		else { inAir = true; if(state.equals("run")) state="jump"; }

		if (!state.equals("dash") && !state.equals("flash_slash") && vx != 0) { vx *= 0.8f; if(Math.abs(vx)<0.1f) vx=0; }

		// 扩展地图边界 (为了配合 1.4x 缩放视口，避免太窄)
		if (x < -200) x = -200;
		if (x > 1200 - w) x = 1200 - w;

		if (state.equals("attack") && animTimer >= 2 && animTimer <= 5) {
			float atkX = x + w/2 + (40 * dir);
			if (enemy != null && Math.abs(enemy.x + enemy.w/2 - atkX) < 80 && Math.abs(enemy.y - y) < 50) {
				mp = Math.min(100, mp + 15);
				enemy.takeDamage(15, dir);
			}
		}

		animTimer += 1.0f * (60 * delta);
		if (hitFlashTimer > 0) hitFlashTimer -= delta;
		updateSkills(delta);
	}

	private void updateUltLogic(float delta) {
		ultTimer += 1.0f * (60 * delta);
		if (state.equals("ult_cast")) {
			if (ultTimer > 40) {
				state = "ult_slash"; ultTimer = 0;
				if (enemy != null) {
					float cx = enemy.x + enemy.w/2;
					float cy = enemy.y + enemy.h/2;
					for(int i=0; i<15; i++) {
						SlashLine sl = new SlashLine();
						sl.x = cx + MathUtils.random(-100, 100); sl.y = cy + MathUtils.random(-100, 100);
						sl.len = MathUtils.random(100, 300); sl.angle = MathUtils.random(0, 360);
						sl.delay = MathUtils.random(0, 30); sl.maxLife = 10; sl.life = sl.maxLife;
						slashLines.add(sl);
					}
				}
			}
		} else if (state.equals("ult_slash")) {
			if ((int)ultTimer % 4 == 0) SynthAudio.playTone(2000, SynthAudio.WaveType.TRIANGLE, 0.05f, 0.1f, 1000);
			for(int i = slashLines.size - 1; i >= 0; i--) {
				SlashLine sl = slashLines.get(i);
				if (sl.delay > 0) sl.delay -= 1.0f * (60 * delta);
				else { sl.life -= 1.0f * (60 * delta); if (sl.life <= 0) slashLines.removeIndex(i); }
			}
			if (ultTimer > 60) {
				state = "ult_end"; ultTimer = 0;
				if (enemy != null) {
					y = enemy.y; vy = 0; inAir = false;
					float idealX = enemy.x - (enemy.dir * 100);
					// 修正边界判断
					if (idealX < -150 || idealX > 1150) idealX = enemy.x + (enemy.dir * 100);
					x = idealX; dir = (x < enemy.x) ? 1 : -1;
				}
			}
		} else if (state.equals("ult_end")) {
			if (ultTimer >= 30) {
				if (enemy != null) {
					enemy.state = "idle";
					enemy.takeDamage(150, -this.dir);

					// [新增] 大招终结粒子 (Shards)
					ParticleManager.inst().burst(enemy.x + enemy.w/2, enemy.y + enemy.h/2, Color.CYAN, 30, ParticleManager.Type.SHARD, 15f);
				}
				SynthAudio.playTone(50, SynthAudio.WaveType.SAWTOOTH, 1.5f, 0.5f, 10);
				SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 1.0f, 0.8f);
				isUltActive = false; state = "idle"; slashLines.clear();
			}
		}
	}

	public void takeDamage(float dmg, int hitDir) {
		if (state.equals("dead") || state.equals("flash_slash") || state.equals("ult_frozen")) return;
		hp -= dmg; if(hp<=0) hp=0;
		state = "hit"; vx = 8*hitDir; vy = 5; animTimer = 0; hitFlashTimer = 0.2f;
		SynthAudio.playTone(100, SynthAudio.WaveType.SAWTOOTH, 0.2f, 0.2f, 50);

		// [新增] 受击粒子 (Box)
		ParticleManager.inst().burst(x + w/2, y + 40, this.color, 5, ParticleManager.Type.BOX, 10f);
	}

	private void attack() { state = "attack"; animTimer = 0; vx = 0; SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f); }
	private void dash(int d) { if(d==0)d=dir; this.dir=d; state="dash"; animTimer=0; vx=25*d; SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f); }
	private void flashSlash() { state="flash_slash"; animTimer=0; vx=0; slashStartX=x;
		flashTargetX = (enemy!=null) ? enemy.x + (enemy.x>x?100:-100) : x+(300*dir);
		SynthAudio.playTone(800, SynthAudio.WaveType.SINE, 0.1f, 0.1f, 50);
	}
	private void updateSkills(float delta) {
		if(state.equals("attack") && animTimer>15) state="idle";
		if(state.equals("dash")) { vy=0; if(animTimer>10){state="idle"; vx=0;} }
		if(state.equals("flash_slash")) { vy=0; if(animTimer>=6 && animTimer<7){ x=flashTargetX;
			if(enemy!=null && Math.abs(x-enemy.x)<150) enemy.takeDamage(40, dir);
			SynthAudio.playTone(600, SynthAudio.WaveType.SAWTOOTH, 0.1f, 0.2f, 2000);
		} if(animTimer>25) state="idle"; }
	}

	// [修正 2] 拆分绘制逻辑 - 仅绘制身体
	public void drawBody(NeonBatch batch) {
		// 隐身或乱斩期间不画身体
		if (state.equals("flash_slash") && animTimer >= 6 && animTimer < 18) return;
		if (state.equals("ult_slash")) return; // 本体现身由 slashLines 代替

		float cx = x + w / 2;
		float cy = y;
		drawColor.set(this.color);
		if (hitFlashTimer > 0 || (state.equals("flash_slash") && animTimer < 6)) drawColor.set(Color.WHITE);

		drawStickmanFigure(batch, cx, cy, drawColor);
	}

	// [修正 2] 拆分绘制逻辑 - 仅绘制特效 (剑气)
	public void drawEffects(NeonBatch batch) {
		if (!state.equals("ult_slash")) return;

		for (SlashLine sl : slashLines) {
			if (sl.delay > 0) continue;
			float alpha = sl.life / sl.maxLife;
			Color c = new Color(1, 1, 1, alpha);

			float halfLen = sl.len / 2;
			float cos = MathUtils.cosDeg(sl.angle);
			float sin = MathUtils.sinDeg(sl.angle);

			float x1 = sl.x - cos * halfLen;
			float y1 = sl.y - sin * halfLen;
			float x2 = sl.x + cos * halfLen;
			float y2 = sl.y + sin * halfLen;

			batch.drawLine(x1, y1, x2, y2, 3f, c);
		}
	}

	private void drawStickmanFigure(NeonBatch batch, float cx, float cy, Color c) {
		float lineWidth = 4f;
		if (state.equals("jump") || state.equals("dash") || state.equals("flash_slash")) {
			drawLine(batch, cx, cy, -10, 40, -25, 15, c, lineWidth); drawLine(batch, cx, cy, 10, 40, 25, 25, c, lineWidth);
		} else if (state.equals("run")) {
			float cycle = MathUtils.sin(animTimer * 0.5f) * 15;
			drawLine(batch, cx, cy, -10, 40, -20 + cycle, 0, c, lineWidth); drawLine(batch, cx, cy, 10, 40, 20 - cycle, 0, c, lineWidth);
		} else {
			drawLine(batch, cx, cy, -10, 40, -20, 0, c, lineWidth); drawLine(batch, cx, cy, 10, 40, 20, 0, c, lineWidth);
		}
		boolean isCrouch = (state.equals("flash_slash") && animTimer < 5) || (state.equals("idle") && InputContext.inst().crouch && !isAi);
		if (isCrouch) batch.drawRect(cx, cy + 45, 30, 30, 0, lineWidth, c, true);
		else batch.drawRect(cx, cy + 60, 30, 40, 0, lineWidth, c, true);

		if (state.equals("ult_cast") || state.equals("ult_end")) {
			drawLine(batch, cx, cy, -10, 70, -30, 50, c, lineWidth); drawLine(batch, cx, cy, 10, 70, 30, 50, c, lineWidth);
		} else if (state.equals("attack")) {
			drawLine(batch, cx, cy, -10, 70, -20, 40, c, lineWidth); drawLine(batch, cx, cy, 10, 70, 40, 70, c, lineWidth);
		} else {
			float armS = state.equals("run") ? MathUtils.sin(animTimer * 0.5f) * 15 : 0;
			drawLine(batch, cx, cy, -10, 70, -20 + armS, 30, c, lineWidth); drawLine(batch, cx, cy, 10, 70, 20 - armS, 30, c, lineWidth);
		}
		float headY = isCrouch ? 65 : 90;
		batch.drawCircle(cx, cy + headY, 12, 0, c, 16, true);
	}

	private void drawLine(NeonBatch batch, float cx, float cy, float lx1, float ly1, float lx2, float ly2, Color c, float width) {
		float x1 = cx + (lx1 * dir); float y1 = cy + ly1; float x2 = cx + (lx2 * dir); float y2 = cy + ly2;
		batch.drawLine(x1, y1, x2, y2, width, c);
	}
}
