package com.goldsprite.solofight.core.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.audio.SynthAudio;
import com.goldsprite.solofight.core.input.InputContext;

public class Fighter {
	// 基础属性
	public float x, y;
	public float vx, vy;
	public float w = 40, h = 90;
	public Color color;
	public int dir = 1; // 1: Right, -1: Left
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
	private Fighter enemy; // 简单的目标引用

	public Fighter(float x, Color color, boolean isAi) {
		this.x = x;
		this.y = 0; // 地面高度
		this.color = color;
		this.isAi = isAi;
		this.dir = isAi ? -1 : 1;
	}

	public void setEnemy(Fighter enemy) {
		this.enemy = enemy;
	}

	/**
	 * 处理离散指令 (来自 ComboEngine / InputContext)
	 */
	public void handleCommand(String cmd) {
		if (state.equals("dash") || state.equals("flash_slash") || state.equals("hit")) return;

		switch (cmd) {
			case "CMD_JUMP":
				if (!inAir) {
					vy = 18; // H5 vy=-20(向上). GDX vy=18(向上)
					state = "jump";
					SynthAudio.playTone(200, SynthAudio.WaveType.SINE, 0.2f, 0.1f, 400); // Jump SFX
				}
				break;
			case "CMD_ATK":
				attack();
				break;
			case "CMD_DASH_L":
				dash(-1);
				break;
			case "CMD_DASH_R":
				dash(1);
				break;
			case "CMD_DASH_AUTO":
				dash(dir); // 向前冲
				break;
			case "FLASH SLASH": // 组合技
				flashSlash();
				break;
			case "CMD_ULT":
				// 暂未实现大招逻辑，先留空
				break;
		}
	}

	public void update(float delta) {
		InputContext input = InputContext.inst();

		// --- 1. AI / Input Logic ---
		if (!isAi) {
			// 玩家输入 (持续性状态: 移动/下蹲)
			if (state.equals("idle") || state.equals("run") || state.equals("jump")) {
				float moveX = input.moveX;
				this.vx = moveX * 6; // H5 Speed: 6

				if (moveX != 0) {
					this.dir = (int) Math.signum(moveX);
					if (!inAir) this.state = "run";
				} else {
					if (!inAir) this.state = "idle";
				}
			}
		} else {
			// 简单的 AI (永远面向玩家)
			if (enemy != null) {
				this.dir = (enemy.x > this.x) ? 1 : -1;
			}
		}

		// --- 2. Physics ---
		// 重力 (H5: vy += 0.8)
		vy -= 0.8f * (60 * delta); // delta 修正

		// 积分
		x += vx * (60 * delta);
		y += vy * (60 * delta);

		// 地面碰撞 (Ground Y = 0)
		if (y < 0) {
			y = 0;
			vy = 0;
			inAir = false;
			if (state.equals("jump")) state = "idle";
		} else {
			inAir = true;
		}

		// 摩擦力
		if (!state.equals("dash") && !state.equals("flash_slash") && vx != 0) {
			// H5: vx *= 0.8
			vx *= 0.8f;
			if (Math.abs(vx) < 0.1f) vx = 0;
		}

		// 边界限制 (Map Width 1000)
		if (x < 0) x = 0;
		if (x > 1000 - w) x = 1000 - w;

		// --- 3. Animation Timer ---
		animTimer += 1.0f * (60 * delta);

		// --- 4. Skill Logic Updates ---
		updateSkills(delta);
	}

	private void attack() {
		state = "attack";
		animTimer = 0;
		vx = 0; // 攻击定身
		SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f); // Swing SFX

		// 简单的攻击恢复
		// 注意：这里没有 Timer 延迟回调，简化处理，依靠 update 里的 animTimer 恢复
	}

	private void dash(int d) {
		if (d == 0) d = dir;
		this.dir = d;
		this.state = "dash";
		this.animTimer = 0;
		this.vx = 25 * d; // H5 Dash Speed
		SynthAudio.playTone(0, SynthAudio.WaveType.NOISE, 0.1f, 0.1f); // Dash SFX
	}

	private void flashSlash() {
		if (mp < 0) return; // 假设无消耗测试
		this.state = "flash_slash";
		this.animTimer = 0;
		this.vx = 0;
		this.slashStartX = this.x;
		// 目标位置：敌人身后 100 像素，或者前方固定距离
		if (enemy != null) {
			this.flashTargetX = enemy.x + (enemy.x > this.x ? 100 : -100);
		} else {
			this.flashTargetX = this.x + (300 * dir);
		}
		SynthAudio.playTone(800, SynthAudio.WaveType.SINE, 0.1f, 0.1f, 50); // Charge SFX
	}

	private void updateSkills(float delta) {
		// Attack Recovery
		if (state.equals("attack") && animTimer > 15) { // 15帧动作
			state = "idle";
		}

		// Dash Logic
		if (state.equals("dash")) {
			vy = 0; // 冲刺滞空
			if (animTimer > 10) { // 10帧冲刺
				state = "idle";
				vx = 0;
			}
		}

		// Flash Slash Logic
		if (state.equals("flash_slash")) {
			vy = 0;
			// Phase 2: Teleport at frame 6
			if (animTimer >= 6 && animTimer < 7) {
				this.x = flashTargetX;
				// TODO: 添加 LightningTrail 特效 (在 Screen 中处理)
				SynthAudio.playTone(600, SynthAudio.WaveType.SAWTOOTH, 0.1f, 0.2f, 2000); // Teleport SFX
			}
			// Phase 3: End
			if (animTimer > 25) {
				state = "idle";
			}
		}
	}

	/**
	 * 核心渲染：程序化火柴人
	 */
	public void draw(NeonBatch batch) {
		// 如果处于闪烁隐身帧
		if (state.equals("flash_slash") && animTimer >= 6 && animTimer < 18) return;

		// 计算绘制基准点 (Bottom Center)
		float cx = x + w / 2;
		float cy = y;

		Color drawColor = this.color;
		// 闪刀蓄力变白
		if (state.equals("flash_slash") && animTimer < 6) drawColor = Color.WHITE;

		// 绘制身体 (Procedural Stickman)
		drawStickmanFigure(batch, cx, cy, drawColor);
	}

	// 1:1 复刻 H5 drawStickmanFigure
	private void drawStickmanFigure(NeonBatch batch, float cx, float cy, Color c) {
		float lineWidth = 4f;

		// Helper to transform local (H5 style) coords to World
		// H5: Y-down (Feet=0, Head=-90).
		// GDX: Y-up (Feet=0, Head=90).
		// So we negate H5's Y.

		// 1. Legs
		if (state.equals("jump") || state.equals("dash") || state.equals("flash_slash")) {
			// Jump Pose: 收腿
			drawLine(batch, cx, cy, -10, 40, -25, 15, c, lineWidth); // Left Leg
			drawLine(batch, cx, cy, 10, 40, 25, 25, c, lineWidth);   // Right Leg
		} else if (state.equals("run")) {
			// Run Pose: Sin wave
			float cycle = MathUtils.sin(animTimer * 0.5f) * 15;
			drawLine(batch, cx, cy, -10, 40, -20 + cycle, 0, c, lineWidth);
			drawLine(batch, cx, cy, 10, 40, 20 - cycle, 0, c, lineWidth);
		} else {
			// Idle Pose: Stand
			drawLine(batch, cx, cy, -10, 40, -20, 0, c, lineWidth);
			drawLine(batch, cx, cy, 10, 40, 20, 0, c, lineWidth);
		}

		// 2. Body
		// H5: rect(-15, -80, 30, 40) -> hip at -40, top at -80.
		// GDX: y from 40 to 80.
		boolean isCrouch = (state.equals("flash_slash") && animTimer < 5) || (state.equals("idle") && InputContext.inst().crouch && !isAi);

		float bodyBottom = 40;
		float bodyHeight = 40;

		if (isCrouch) {
			// Low Body
			batch.drawRect(cx - 15, cy + 60 - 15, 30, 30, 0, lineWidth, c, true); // Fill Box
		} else {
			// Normal Body
			batch.drawRect(cx - 15, cy + bodyBottom, 30, bodyHeight, 0, lineWidth, c, true);
		}

		// 3. Head
		float headY = isCrouch ? 65 : 90;
		batch.drawCircle(cx, cy + headY, 12, 0, c, 16, true);

		// 4. Arms
		if (state.equals("attack")) {
			// Attack Pose: Hands forward
			drawLine(batch, cx, cy, -10, 70, -20, 40, c, lineWidth); // Back arm
			drawLine(batch, cx, cy, 10, 70, 40, 70, c, lineWidth);   // Front arm (stabbing)
		} else if (isCrouch) {
			// Charge Pose
			drawLine(batch, cx, cy, -10, 60, 10, 50, c, lineWidth);
			drawLine(batch, cx, cy, 10, 60, 30, 50, c, lineWidth);
		} else {
			// Idle/Run
			float armS = state.equals("run") ? MathUtils.sin(animTimer * 0.5f) * 15 : 0;
			drawLine(batch, cx, cy, -10, 70, -20 + armS, 30, c, lineWidth);
			drawLine(batch, cx, cy, 10, 70, 20 - armS, 30, c, lineWidth);
		}
	}

	// 辅助：绘制相对坐标线段
	// lx1, ly1: 相对于中心的局部坐标 (H5逻辑，Y轴向上为正)
	private void drawLine(NeonBatch batch, float cx, float cy, float lx1, float ly1, float lx2, float ly2, Color c, float width) {
		float x1 = cx + (lx1 * dir);
		float y1 = cy + ly1;
		float x2 = cx + (lx2 * dir);
		float y2 = cy + ly2;
		batch.drawLine(x1, y1, x2, y2, width, c);
	}
}
