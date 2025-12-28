package com.goldsprite.solofight.core.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.audio.SynthAudio;
import com.goldsprite.solofight.core.input.InputContext;

public class Fighter {
	// ... (原有属性保持不变) ...
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

	// [新增] 大招相关
	public float ultTimer = 0;
	public boolean isUltActive = false; // 标记是否正在释放大招 (影响全局 TimeScale)

	// 乱斩线条粒子 (内部类)
	private static class SlashLine {
		float x, y, len, angle, life, maxLife;
		float delay;
	}
	private final Array<SlashLine> slashLines = new Array<>();

	public Fighter(float x, Color color, boolean isAi) {
		this.x = x; this.y = 0; this.color = color; this.isAi = isAi;
		this.dir = isAi ? -1 : 1;
		// 测试用：开局满蓝
		this.mp = 100;
	}

	public void setEnemy(Fighter enemy) { this.enemy = enemy; }

	public void handleCommand(String cmd) {
		// [新增] 大招状态下禁止操作
		if (state.startsWith("ult")) return;
		if (state.equals("dead") || state.equals("hit") || state.equals("flash_slash")) return;

		switch (cmd) {
			// ... (其他指令不变) ...
			case "CMD_JUMP": if(!inAir) { vy=22; state="jump"; inAir=true; SynthAudio.playTone(200, SynthAudio.WaveType.SINE, 0.2f, 0.1f, 400); } break;
			case "CMD_ATK": attack(); break;
			case "CMD_DASH_L": dash(-1); break;
			case "CMD_DASH_R": dash(1); break;
			case "CMD_DASH_AUTO": dash(dir); break;
			case "FLASH SLASH": flashSlash(); break;
			// [新增] 大招触发
			case "CMD_ULT": performUlt(); break;
		}
	}

	// [新增] 大招启动逻辑
	private void performUlt() {
		if (mp < 100) return;
		mp = 0;
		state = "ult_cast";
		ultTimer = 0;
		isUltActive = true;

		// SFX: 蓄力音效
		SynthAudio.playTone(50, SynthAudio.WaveType.SQUARE, 1.0f, 0.2f);
		SynthAudio.playTone(1000, SynthAudio.WaveType.SAWTOOTH, 1.0f, 0.1f, 100);

		// 冻结敌人
		if (enemy != null) {
			enemy.state = "ult_frozen";
			enemy.vx = 0;
			enemy.vy = 0;
		}
	}

	public void update(float delta) {
		InputContext input = InputContext.inst();

		// [新增] 状态机拦截：大招期间不执行常规物理/输入
		if (state.startsWith("ult")) {
			updateUltLogic(delta);
			return; // 跳过常规物理
		}

		// 被大招锁定时，完全静止
		if (state.equals("ult_frozen")) {
			return;
		}

		// ... (原有的 Control Logic, Physics, Hit Logic, Timers 代码保持不变) ...
		// 为了节省篇幅，这里省略原有代码，请确保 update 方法保留了原来的物理逻辑
		// ------------------------------------------------------------------
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
		if (x < 0) x = 0; if (x > 1000 - w) x = 1000 - w;

		// Hit Logic (Attack Collision)
		if (state.equals("attack") && animTimer >= 2 && animTimer <= 5) {
			float atkX = x + w/2 + (40 * dir);
			if (enemy != null && Math.abs(enemy.x + enemy.w/2 - atkX) < 80 && Math.abs(enemy.y - y) < 50) {
				// 增加回蓝
				mp = Math.min(100, mp + 15);
				enemy.takeDamage(15, dir);
			}
		}
		// ------------------------------------------------------------------

		animTimer += 1.0f * (60 * delta);
		if (hitFlashTimer > 0) hitFlashTimer -= delta;
		updateSkills(delta);
	}

	// [新增] 大招核心状态机
	private void updateUltLogic(float delta) {
		ultTimer += 1.0f * (60 * delta); // 这里的 delta 应该是未缩放的实时间

		// 阶段 1: Cast (蓄力 + 生成线条)
		if (state.equals("ult_cast")) {
			// 在第 40 帧进入斩击
			if (ultTimer > 40) {
				state = "ult_slash";
				ultTimer = 0;
				// 生成乱斩线条 (围绕敌人)
				if (enemy != null) {
					float cx = enemy.x + enemy.w/2;
					float cy = enemy.y + enemy.h/2;
					for(int i=0; i<15; i++) {
						SlashLine sl = new SlashLine();
						sl.x = cx + MathUtils.random(-100, 100);
						sl.y = cy + MathUtils.random(-100, 100);
						sl.len = MathUtils.random(100, 300);
						sl.angle = MathUtils.random(0, 360);
						sl.delay = MathUtils.random(0, 30);
						sl.maxLife = 10;
						sl.life = sl.maxLife;
						slashLines.add(sl);
					}
				}
			}
		}
		// 阶段 2: Slash (乱斩动画)
		else if (state.equals("ult_slash")) {
			// 每4帧播放一次斩击音效
			if ((int)ultTimer % 4 == 0) {
				SynthAudio.playTone(2000, SynthAudio.WaveType.TRIANGLE, 0.05f, 0.1f, 1000); // Slash SFX
				// TODO: 可以在 CombatScreen 里添加 Screen Shake
			}

			// 更新线条
			for(int i = slashLines.size - 1; i >= 0; i--) {
				SlashLine sl = slashLines.get(i);
				if (sl.delay > 0) {
					sl.delay -= 1.0f * (60 * delta);
				} else {
					sl.life -= 1.0f * (60 * delta);
					if (sl.life <= 0) slashLines.removeIndex(i);
				}
			}

			if (ultTimer > 60) {
				state = "ult_end";
				ultTimer = 0;
				// 瞬移到敌人身后/身前
				if (enemy != null) {
					this.y = enemy.y;
					this.vy = 0;
					this.inAir = false;
					// 尝试出现在敌人身后
					float idealX = enemy.x - (enemy.dir * 100);
					// 边界检查
					if (idealX < 50 || idealX > 1000 - 90) idealX = enemy.x + (enemy.dir * 100);
					this.x = idealX;
					// 面向敌人
					this.dir = (this.x < enemy.x) ? 1 : -1;
				}
			}
		}
		// 阶段 3: End (终结一击)
		else if (state.equals("ult_end")) {
			if (ultTimer >= 30) {
				// 结算伤害
				if (enemy != null) {
					enemy.state = "idle"; // 解除冻结
					enemy.takeDamage(150, -this.dir); // 巨大的伤害
				}

				// 终结音效
				SynthAudio.playTone(50, SynthAudio.WaveType.SAWTOOTH, 1.5f, 0.5f, 10); // Boom
				SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 1.0f, 0.8f);

				isUltActive = false; // 恢复时间流动
				state = "idle";
				slashLines.clear();
			}
		}
	}

	// ... (takeDamage, attack, dash, flashSlash, updateSkills 方法保持不变) ...
	public void takeDamage(float dmg, int hitDir) {
		if (state.equals("dead") || state.equals("flash_slash") || state.equals("ult_frozen")) return;
		hp -= dmg; if(hp<=0) hp=0;
		state = "hit"; vx = 8*hitDir; vy = 5; animTimer = 0; hitFlashTimer = 0.2f;
		SynthAudio.playTone(100, SynthAudio.WaveType.SAWTOOTH, 0.2f, 0.2f, 50);
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

	public void draw(NeonBatch batch) {
		if (state.equals("flash_slash") && animTimer >= 6 && animTimer < 18) return;
		// 大招斩击阶段本体消失 (H5逻辑)
		if (state.equals("ult_slash")) {
			drawUltSlashes(batch); // 只画线条
			return;
		}

		float cx = x + w / 2;
		float cy = y;
		drawColor.set(this.color);
		if (hitFlashTimer > 0 || (state.equals("flash_slash") && animTimer < 6)) drawColor.set(Color.WHITE);

		drawStickmanFigure(batch, cx, cy, drawColor);
	}

	// [新增] 绘制乱斩线条
	private void drawUltSlashes(NeonBatch batch) {
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

			// 绘制发光白线
			batch.drawLine(x1, y1, x2, y2, 3f, c);
		}
	}

	// ... (drawStickmanFigure 及 drawLine 辅助方法保持不变) ...
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

		// [新增] 大招 Pose (H5: ult_cast / ult_end)
		if (state.equals("ult_cast") || state.equals("ult_end")) {
			// Hands up/out logic from H5
			// H5: moveTo(-10, -70); lineTo(-30, -50); moveTo(10, -70); lineTo(30, -50);
			// GDX Y: 70 -> 50
			drawLine(batch, cx, cy, -10, 70, -30, 50, c, lineWidth);
			drawLine(batch, cx, cy, 10, 70, 30, 50, c, lineWidth);
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
