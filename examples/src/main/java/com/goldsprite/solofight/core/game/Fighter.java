package com.goldsprite.solofight.core.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.solofight.core.FloatingTextManager; // [新增]
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.audio.SynthAudio;
import com.goldsprite.solofight.core.input.InputContext;

import java.util.List;

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
	private boolean skillTriggered = false;
	private boolean attackHasHit = false;

	private static class SlashLine {
		float x, y, len, angle, life, maxLife, delay;
	}
	private final Array<SlashLine> slashLines = new Array<>();

	public Fighter(float x, Color color, boolean isAi) {
		this.x = x; this.y = 0; this.color = color; this.isAi = isAi;
		this.dir = isAi ? -1 : 1;
		this.mp = 0; // [修复 5] 开局无能量
	}

	public void setEnemy(Fighter enemy) { this.enemy = enemy; }

	// [新增] 重置方法 (供 GameScreen 重玩时调用)
	public void reset(float startX, int startDir) {
		this.x = startX;
		this.y = 0;
		this.vx = 0;
		this.vy = 0;
		this.dir = startDir;
		this.state = "idle";
		this.hp = maxHp;
		this.mp = 0; // 重置能量
		this.inAir = false;
		this.animTimer = 0;

		// 重置所有锁和计时器
		this.hitFlashTimer = 0;
		this.ultTimer = 0;
		this.isUltActive = false;
		this.skillTriggered = false;
		this.attackHasHit = false;

		// 清理内部特效
		this.slashLines.clear();
	}

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

	// [修改] update 签名增加 platforms 参数
	public void update(float delta, List<Platform> platforms) {
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

		// [新增] 平台碰撞检测 (One-way platform)
		// 逻辑：只有当 vy <= 0 (下落) 且 脚底之前在平台上方时才碰撞
		// H5 logic simplified:
		boolean onGround = false;

		// 地面 (y=0)
		if (y <= 0) {
			y = 0;
			onGround = true;
		} else if (vy <= 0 && platforms != null) {
			// 检查平台
			for (Platform p : platforms) {
				// AABB Check
				// 玩家水平范围: x ~ x+w
				// 平台水平范围: p.x ~ p.x+p.w
				if (x + w > p.x && x < p.x + p.w) {
					// 垂直检测: 脚底 (y) 接近 平台顶部 (p.y + p.h)
					// 容差: 穿透深度不能太多，且上一帧应该在上面 (这里简化为检测当前y是否接近平台顶)
					// H5: y+h <= p.y + vy + 20? No, H5 logic checks feet.
					// 我们这里 y 是脚底。
					// 判定区间: y 在 [p.y+p.h - threshold, p.y+p.h + threshold]
					float pTop = p.y + p.h; // H5中 y向下，p.y是顶。这里y向上，p.y是底?
					// 我们定义 Platform x,y 是左下角。p.y + p.h 是顶面。

					if (y >= pTop - 15 && y <= pTop + 15) { // 命中平台
						y = pTop;
						onGround = true;
						break;
					}
				}
			}
		}

		if (onGround) {
			vy = 0;
			inAir = false;
			if(state.equals("jump")||state.equals("hit")) state="idle";
		} else {
			inAir = true;
			if(state.equals("run")) state="jump";
		}

		if (!state.equals("dash") && !state.equals("flash_slash") && vx != 0) { vx *= 0.8f; if(Math.abs(vx)<0.1f) vx=0; }

		if (x < -200) x = -200; if (x > 1200 - w) x = 1200 - w;

		// Attack Logic
		if (state.equals("attack") && animTimer >= 2 && animTimer <= 5) {
			if (!attackHasHit && enemy != null) {
				float atkX = x + w/2 + (40 * dir);
				if (Math.abs(enemy.x + enemy.w/2 - atkX) < 80 && Math.abs(enemy.y - y) < 50) {
					mp = Math.min(100, mp + 15);
					enemy.takeDamage(15, dir);
					attackHasHit = true;

					// [修复 4] 连击 UI 增加
					if (!isAi) FloatingTextManager.addCombo();
				}
			}
		}

		animTimer += 1.0f * (60 * delta);
		if (hitFlashTimer > 0) hitFlashTimer -= delta;
		updateSkills(delta);
	}

	// ... (updateUltLogic 保持不变) ...
	private void updateUltLogic(float delta) {
		ultTimer += 1.0f * (60 * delta);
		if (state.equals("ult_cast")) {
			if (ultTimer > 40) {
				state = "ult_slash"; ultTimer = 0;
				if (enemy != null) {
					float cx = enemy.x + enemy.w/2; float cy = enemy.y + enemy.h/2;
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
					if (idealX < -150 || idealX > 1150) idealX = enemy.x + (enemy.dir * 100);
					x = idealX; dir = (x < enemy.x) ? 1 : -1;
				}
			}
		} else if (state.equals("ult_end")) {
			if (ultTimer >= 30) {
				if (enemy != null) {
					enemy.state = "idle"; enemy.takeDamage(150, -this.dir);
					ParticleManager.inst().burst(enemy.x+enemy.w/2, enemy.y+enemy.h/2, Color.CYAN, 30, ParticleManager.Type.SHARD, 15f);
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
		ParticleManager.inst().burst(x + w/2, y + 40, this.color, 5, ParticleManager.Type.BOX, 10f);

		// [修复 4] 伤害飘字 (World Space)
		FloatingTextManager.addDamage(x + w/2, y + h + 20, (int)dmg, dmg > 30);
	}

	private void attack() {
		state = "attack"; animTimer = 0;
		vx = 8 * dir; // [修复 7] 攻击小位移
		attackHasHit = false;
		SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f);
	}

	private void dash(int d) { if(d==0)d=dir; this.dir=d; state="dash"; animTimer=0; vx=25*d; SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f); }

	private void flashSlash() {
		state="flash_slash"; animTimer=0; vx=0; skillTriggered=false; slashStartX=x;
		// [修复 2] 固定距离穿梭，而非追踪敌人身后
		// H5: this.flashTargetX = this.x + (350 * this.dir);
		flashTargetX = x + (350 * dir);
		SynthAudio.playTone(800, SynthAudio.WaveType.SINE, 0.1f, 0.1f, 50);
	}

	private void updateSkills(float delta) {
		// ... (Attack, Dash logic) ...

		if(state.equals("flash_slash")) {
			vy=0;
			if(animTimer>=6 && !skillTriggered) {
				skillTriggered=true;
				float startX = slashStartX+w/2; float startY = y+h/2; x=flashTargetX; float endX = x+w/2;
				EffectManager.inst().addLightning(startX, startY, endX, startY);

				// 命中判定
				float minX = Math.min(slashStartX, flashTargetX);
				float maxX = Math.max(slashStartX, flashTargetX);
				if(enemy!=null && enemy.x+enemy.w > minX && enemy.x < maxX && Math.abs(y-enemy.y)<100) {
					// [修复 2] 闪刀命中回蓝
					mp = Math.min(100, mp + 20);
					enemy.takeDamage(40, dir);
					// 只有玩家触发连击UI
					if (!isAi) com.goldsprite.solofight.core.FloatingTextManager.addCombo();
				}
				SynthAudio.playTone(600, SynthAudio.WaveType.SAWTOOTH, 0.1f, 0.2f, 2000);
			}
			if(animTimer>25) state="idle";
		}
	}

	public void drawBody(NeonBatch batch) {
		if (state.equals("flash_slash") && animTimer >= 6 && animTimer < 18) return;
		if (state.equals("ult_slash")) return;

		float cx = x + w / 2;
		float cy = y;
		drawColor.set(this.color);
		if (hitFlashTimer > 0 || (state.equals("flash_slash") && animTimer < 6)) drawColor.set(Color.WHITE);

		if (state.equals("flash_slash") && animTimer < 6) {
			float width = (6 - animTimer) * 400; float height = (6 - animTimer) * 2;
			Color c = new Color(1, 1, 1, (6 - animTimer) / 6f);
			batch.drawRect(cx, cy + h/2, width, height, 0, 0, c, true);
			return;
		}

		// [修复 6] 下蹲状态判定剥离
		// AI 的下蹲由 AI 逻辑控制 (目前 AI 只有 attack/run/idle，暂无下蹲，所以 false)
		// 玩家下蹲由 InputContext 控制
		boolean isCrouching = false;
		if (state.equals("flash_slash") && animTimer < 5) isCrouching = true; // 技能强制下蹲
		else if (!isAi && state.equals("idle") && InputContext.inst().crouch) isCrouching = true; // 玩家输入

		drawStickmanFigureStatic(batch, cx, cy, dir, state, animTimer, drawColor, isCrouching);
	}

	public void drawEffects(NeonBatch batch) {
		if (!state.equals("ult_slash")) return;
		for (SlashLine sl : slashLines) {
			if (sl.delay > 0) continue;
			float alpha = sl.life / sl.maxLife;
			Color c = new Color(1, 1, 1, alpha);
			float halfLen = sl.len / 2; float cos = MathUtils.cosDeg(sl.angle); float sin = MathUtils.sinDeg(sl.angle);
			batch.drawLine(sl.x - cos * halfLen, sl.y - sin * halfLen, sl.x + cos * halfLen, sl.y + sin * halfLen, 3f, c);
		}
	}

	// [修改] 增加 isCrouching 参数，不再读取 InputContext
	public static void drawStickmanFigureStatic(NeonBatch batch, float cx, float cy, int dir, String state, float animTimer, Color c, boolean isCrouch) {
		float lineWidth = 4f;
		LineDrawer d = (lx1, ly1, lx2, ly2) -> {
			float x1 = cx + (lx1 * dir); float y1 = cy + ly1;
			float x2 = cx + (lx2 * dir); float y2 = cy + ly2;
			batch.drawLine(x1, y1, x2, y2, lineWidth, c);
		};

		if (state.equals("jump") || state.equals("dash") || state.equals("flash_slash")) {
			d.draw(-10, 40, -25, 15); d.draw(10, 40, 25, 25);
		} else if (state.equals("run")) {
			float cycle = MathUtils.sin(animTimer * 0.5f) * 15;
			d.draw(-10, 40, -20 + cycle, 0); d.draw(10, 40, 20 - cycle, 0);
		} else {
			d.draw(-10, 40, -20, 0); d.draw(10, 40, 20, 0);
		}

		if (isCrouch) batch.drawRect(cx, cy + 45, 30, 30, 0, lineWidth, c, true);
		else batch.drawRect(cx, cy + 60, 30, 40, 0, lineWidth, c, true);

		if (state.equals("ult_cast") || state.equals("ult_end")) {
			d.draw(-10, 70, -30, 50); d.draw(10, 70, 30, 50);
		} else if (state.equals("attack")) {
			d.draw(-10, 70, -20, 40); d.draw(10, 70, 40, 70);
		} else if (isCrouch) {
			d.draw(-10, 60, 10, 50); d.draw(10, 60, 30, 50);
		} else {
			float armS = state.equals("run") ? MathUtils.sin(animTimer * 0.5f) * 15 : 0;
			d.draw(-10, 70, -20 + armS, 30); d.draw(10, 70, 20 - armS, 30);
		}

		float headY = isCrouch ? 65 : 90;
		batch.drawCircle(cx, cy + headY, 12, 0, c, 16, true);
	}

	private interface LineDrawer { void draw(float x1, float y1, float x2, float y2); }
}
