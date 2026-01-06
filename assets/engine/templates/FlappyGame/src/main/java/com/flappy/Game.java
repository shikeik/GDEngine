package com.flappy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.audio.SynthAudio;
import com.goldsprite.gdengine.audio.SynthAudio.WaveType;

import java.util.ArrayList;
import java.util.List;

public class Game {

	enum State { READY, PLAYING, DEAD }
	State currentState = State.READY;

	GObject player;
	float velocityY = 0;
	final float GRAVITY = -1200f;
	final float JUMP_FORCE = 450f;
	final float PLAYER_SIZE = 30f;

	List<Pipe> pipes = new ArrayList<>();
	List<Particle> particles = new ArrayList<>();

	float pipeTimer = 0;
	float timeSinceDeath = 0;

	// 背景参数
	final float BG_SPEED = 100f; // 背景移动速度
	final float BG_SPACING = 150f; // 柱子间距

	final float PIPE_SPEED = 200f;
	final float PIPE_SPAWN_RATE = 1.8f;
	final float GAP_SIZE = 140f;
	final float PIPE_WIDTH = 50f;

	NeonBatch batch;
	int score = 0;
	int bestScore = 0;

	public Game(GameWorld world) {
		player = new GObject("Player");
		player.transform.setPosition(0, 0);
		player.addComponent(new GameLoop());
	}

	class Pipe {
		float x;
		float gapY;
		boolean passed = false;
		public Pipe(float x, float gapY) { this.x = x; this.gapY = gapY; }
	}

	// 优化后的粒子
	class Particle {
		float x, y, vx, vy, life, maxLife;
		float size;
		Color color;
		public Particle(float x, float y, Color c) {
			this.x = x; this.y = y; this.color = new Color(c);
			this.maxLife = MathUtils.random(0.3f, 0.6f);
			this.life = maxLife;
			this.size = MathUtils.random(3f, 6f);

			// 核心修改：向左喷射 (模拟玩家向前飞，烟雾向后)
			// 基础向左速度 + 随机扩散
			this.vx = -PIPE_SPEED * 0.8f + MathUtils.random(-50, 50);
			this.vy = MathUtils.random(-50, 50);
		}
	}

	class GameLoop extends Component {

		@Override
		public void onAwake() {
			batch = new NeonBatch();
			resetGame();
		}

		@Override
		public void update(float delta) {
			if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
				handleInput();
			}

			if (currentState == State.PLAYING) {
				updatePhysics(delta);
				updatePipes(delta);
				checkCollisions();
				updateParticles(delta); // 游戏中更新粒子
			} else if (currentState == State.DEAD) {
				updatePhysics(delta);
				timeSinceDeath += delta;
				updateParticles(delta); // 死亡时粒子继续飘
			} else {
				float hover = MathUtils.sin(GameWorld.getTotalTime() * 5f) * 10f;
				transform.position.y = hover;
			}

			render();
		}

		private void handleInput() {
			if (currentState == State.READY) {
				currentState = State.PLAYING;
				jump();
			} else if (currentState == State.PLAYING) {
				jump();
			} else if (currentState == State.DEAD && timeSinceDeath > 0.5f) {
				resetGame();
			}
		}

		private void jump() {
			velocityY = JUMP_FORCE;
			SynthAudio.playTone(200, WaveType.SINE, 0.1f, 0.1f, 400);

			// 生成喷气粒子 (位置在玩家底部稍微偏后)
			for(int i=0; i<5; i++) {
				particles.add(new Particle(
					transform.position.x - PLAYER_SIZE/2,
					transform.position.y - PLAYER_SIZE/2,
					Color.WHITE
				));
			}
		}

		private void updatePhysics(float delta) {
			velocityY += GRAVITY * delta;
			float y = transform.position.y + velocityY * delta;
			float floor = -270 + PLAYER_SIZE/2;

			if (y < floor) {
				y = floor;
				if (currentState == State.PLAYING) die();
				else if (currentState == State.DEAD) { velocityY = 0; }
			}
			if (y > 270) { y = 270; velocityY = 0; }

			transform.position.y = y;
			float targetRot = MathUtils.clamp(velocityY * 0.2f, -90, 25);
			transform.rotation = MathUtils.lerp(transform.rotation, targetRot, 10 * delta);
		}

		private void updatePipes(float delta) {
			pipeTimer += delta;
			if (pipeTimer >= PIPE_SPAWN_RATE) {
				pipeTimer = 0;
				spawnPipe();
			}

			for (int i = pipes.size() - 1; i >= 0; i--) {
				Pipe p = pipes.get(i);
				p.x -= PIPE_SPEED * delta;

				if (!p.passed && p.x < transform.position.x) {
					p.passed = true;
					score++;
					SynthAudio.playTone(800, WaveType.SINE, 0.1f, 0.1f, 1200);
				}

				if (p.x < -600) pipes.remove(i);
			}
		}

		private void updateParticles(float delta) {
			for(int i = particles.size()-1; i>=0; i--) {
				Particle p = particles.get(i);
				p.x += p.vx * delta;
				p.y += p.vy * delta;
				p.life -= delta;
				if(p.life <= 0) particles.remove(i);
			}
		}

		private void spawnPipe() {
			float spawnX = 600;
			float gapY = MathUtils.random(-120, 120);
			pipes.add(new Pipe(spawnX, gapY));
		}

		private void checkCollisions() {
			float size = PLAYER_SIZE * 0.8f;
			Rectangle pRect = new Rectangle(transform.position.x - size/2, transform.position.y - size/2, size, size);

			for (Pipe p : pipes) {
				float topPipeBottom = p.gapY + GAP_SIZE/2;
				float bottomPipeTop = p.gapY - GAP_SIZE/2;

				Rectangle rTop = new Rectangle(p.x - PIPE_WIDTH/2, topPipeBottom, PIPE_WIDTH, 1000);
				Rectangle rBottom = new Rectangle(p.x - PIPE_WIDTH/2, bottomPipeTop - 1000, PIPE_WIDTH, 1000);

				if (pRect.overlaps(rTop) || pRect.overlaps(rBottom)) {
					die();
				}
			}
		}

		private void die() {
			currentState = State.DEAD;
			timeSinceDeath = 0;
			if (score > bestScore) bestScore = score;

			SynthAudio.playTone(0, WaveType.NOISE, 0.1f, 0.2f);
			SynthAudio.playTone(400, WaveType.SAWTOOTH, 0.3f, 0.2f, 50);

			// 死亡大爆炸
			for(int i=0; i<30; i++) particles.add(new Particle(transform.position.x, transform.position.y, Color.ORANGE));
		}

		private void resetGame() {
			transform.position.set(-100, 0);
			transform.rotation = 0;
			velocityY = 0;
			score = 0;
			pipes.clear();
			particles.clear();
			pipeTimer = PIPE_SPAWN_RATE;
			currentState = State.READY;
		}

		private void render() {
			batch.setProjectionMatrix(GameWorld.worldCamera.combined);
			batch.begin();

			// 1. 背景 (平滑视差优化)
			drawBackground(batch);

			// 2. 管道
			for (Pipe p : pipes) {
				float topY = p.gapY + GAP_SIZE/2;
				float bottomY = p.gapY - GAP_SIZE/2 - 500;
				Color pipeColor = Color.valueOf("00ff99");

				batch.drawRect(p.x - PIPE_WIDTH/2, topY, PIPE_WIDTH, 500, 0, 2f, pipeColor, false);
				// 内部加点半透填充增加体积感
				batch.drawRect(p.x - PIPE_WIDTH/2 + 2, topY + 2, PIPE_WIDTH - 4, 496, 0, 0, new Color(0, 1, 0.5f, 0.1f), true);

				batch.drawRect(p.x - PIPE_WIDTH/2, bottomY, PIPE_WIDTH, 500, 0, 2f, pipeColor, false);
				batch.drawRect(p.x - PIPE_WIDTH/2 + 2, bottomY + 2, PIPE_WIDTH - 4, 496, 0, 0, new Color(0, 1, 0.5f, 0.1f), true);
			}

			// 3. 粒子
			for (Particle pt : particles) {
				Color c = new Color(pt.color);
				c.a = pt.life; // 透明度渐变
				float s = pt.size * pt.life; // 大小渐变
				batch.drawRect(pt.x - s/2, pt.y - s/2, s, s, 0, 0, c, true);
			}

			// 4. 玩家
			if (currentState != State.DEAD) { // 死了就不画方块了，只看粒子
				Color pColor = Color.YELLOW;
				batch.drawRect(transform.position.x - PLAYER_SIZE/2, transform.position.y - PLAYER_SIZE/2,
					PLAYER_SIZE, PLAYER_SIZE, transform.rotation, 2f, pColor, false);
				batch.drawRect(transform.position.x - PLAYER_SIZE/2 + 4, transform.position.y - PLAYER_SIZE/2 + 4,
					PLAYER_SIZE - 8, PLAYER_SIZE - 8, transform.rotation, 0, new Color(1, 1, 0, 0.5f), true);
			}

			// 5. UI (使用 NeonDigitDrawer)
			if (currentState == State.PLAYING) {
				// 顶部大分
				NeonDigitDrawer.drawNumber(batch, score, 0, 200, 40, 4, Color.WHITE);
			} else if (currentState == State.DEAD) {
				// Game Over 面板
				float panelY = 50;
				NeonDigitDrawer.drawNumber(batch, score, 0, panelY + 20, 40, 4, Color.WHITE);
				NeonDigitDrawer.drawNumber(batch, bestScore, 0, panelY - 40, 20, 2, Color.GOLD);
			}

			batch.end();
		}

		private void drawBackground(NeonBatch b) {
			// 地平线
			b.drawLine(-1000, -270, 1000, -270, 2, Color.CYAN);

			// 优化后的平滑滚动背景
			// 计算当前总位移 (基于时间)
			float totalScroll = GameWorld.getTotalTime() * BG_SPEED;

			// 只需要画屏幕范围内的柱子 (-500 到 500)
			// BG_SPACING = 150
			// 计算第一个柱子的偏移
			float startX = -(totalScroll % BG_SPACING);

			// 从屏幕左侧外开始画，覆盖整个视口
			for(float x = startX - 500; x < 500; x += BG_SPACING) {
				// 根据绝对位置计算高度 (伪随机但固定)
				float absIndex = (x + totalScroll) / BG_SPACING;
				float h = 100 + MathUtils.sin(absIndex) * 50;

				b.drawRect(x, -270, 10, h, 0, 1, new Color(1,1,1,0.1f), false);
			}
		}
	}
}
