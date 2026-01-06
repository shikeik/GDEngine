package com.flappy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.audio.SynthAudio;
import com.goldsprite.gdengine.audio.SynthAudio.WaveType; // 引入引擎音频

import java.util.ArrayList;
import java.util.List;

public class Game {

	enum State { READY, PLAYING, DEAD }
	State currentState = State.READY;

	GObject player;
	float velocityY = 0;
	final float GRAVITY = -1200f;
	final float JUMP_FORCE = 450f; // 手感微调
	final float PLAYER_SIZE = 30f;

	List<Pipe> pipes = new ArrayList<>();
	List<Particle> particles = new ArrayList<>(); // 简单的粒子系统

	float pipeTimer = 0;
	float timeSinceDeath = 0;
	float backgroundOffset = 0; // 背景滚动

	final float PIPE_SPEED = 200f;
	final float PIPE_SPAWN_RATE = 1.8f;
	final float GAP_SIZE = 140f;
	final float PIPE_WIDTH = 50f;

	NeonBatch batch;
	int score = 0;
	int bestScore = 0; // 本地最高分(暂不持久化)

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

	class Particle {
		float x, y, vx, vy, life;
		Color color;
		public Particle(float x, float y, Color c) {
			this.x = x; this.y = y; this.color = new Color(c);
			this.life = 1.0f;
			float speed = MathUtils.random(50, 150);
			float angle = MathUtils.random(0, 360) * MathUtils.degreesToRadians;
			this.vx = MathUtils.cos(angle) * speed;
			this.vy = MathUtils.sin(angle) * speed;
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
			// 输入
			if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
				handleInput();
			}

			// 逻辑
			if (currentState == State.PLAYING) {
				updatePhysics(delta);
				updatePipes(delta);
				updateBackground(delta);
				checkCollisions();
			} else if (currentState == State.DEAD) {
				updatePhysics(delta); // 尸体也要下落
				updateParticles(delta);
				timeSinceDeath += delta;
			} else {
				// Ready 状态: 玩家上下浮动
				float hover = MathUtils.sin(GameWorld.getTotalTime() * 5f) * 10f;
				transform.position.y = hover;
				updateBackground(delta * 0.1f); // 慢速背景
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
			// 音效: 快速升调的正弦波
			SynthAudio.playTone(200, WaveType.SINE, 0.1f, 0.1f, 400);

			// 跳跃尾迹粒子
			for(int i=0; i<3; i++) {
				particles.add(new Particle(transform.position.x, transform.position.y - PLAYER_SIZE/2, Color.WHITE));
			}
		}

		private void updatePhysics(float delta) {
			velocityY += GRAVITY * delta;
			float y = transform.position.y + velocityY * delta;
			float floor = -270 + PLAYER_SIZE/2;

			if (y < floor) {
				y = floor;
				if (currentState == State.PLAYING) die();
				else if (currentState == State.DEAD) { velocityY = 0; } // 尸体落地不动
			}
			// 撞顶不死，只是被挡住
			if (y > 270) { y = 270; velocityY = 0; }

			transform.position.y = y;

			// 旋转: 上升抬头(20度), 下落低头(-90度)
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
					// 得分音效: 高音 Ping
					SynthAudio.playTone(800, WaveType.SINE, 0.1f, 0.1f, 1200);
				}

				if (p.x < -600) pipes.remove(i);
			}
		}

		private void updateBackground(float delta) {
			backgroundOffset -= PIPE_SPEED * 0.5f * delta;
			if(backgroundOffset < -100) backgroundOffset += 100;
		}

		private void updateParticles(float delta) {
			for(int i = particles.size()-1; i>=0; i--) {
				Particle p = particles.get(i);
				p.x += p.vx * delta;
				p.y += p.vy * delta;
				p.life -= delta * 1.5f;
				if(p.life <= 0) particles.remove(i);
			}
		}

		private void spawnPipe() {
			float spawnX = 600;
			float gapY = MathUtils.random(-120, 120);
			pipes.add(new Pipe(spawnX, gapY));
		}

		private void checkCollisions() {
			// 收缩一下判定框，增加容错 (手感优化)
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

			// 死亡音效: 噪音 + 锯齿波下坠
			SynthAudio.playTone(0, WaveType.NOISE, 0.1f, 0.2f);
			SynthAudio.playTone(400, WaveType.SAWTOOTH, 0.3f, 0.2f, 50);

			// 爆炸粒子
			for(int i=0; i<20; i++) particles.add(new Particle(transform.position.x, transform.position.y, Color.ORANGE));

			// 屏幕震动? 暂时没有相机震动API，以后加上
		}

		private void resetGame() {
			transform.position.set(-100, 0); // 稍微往左一点
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

			// 1. 背景 (简单的线条视差)
			drawBackground(batch);

			// 2. 管道
			for (Pipe p : pipes) {
				float topY = p.gapY + GAP_SIZE/2;
				float bottomY = p.gapY - GAP_SIZE/2 - 500;

				// 管道颜色: 霓虹绿
				Color pipeColor = Color.valueOf("00ff99");
				batch.drawRect(p.x - PIPE_WIDTH/2, topY, PIPE_WIDTH, 500, 0, 2f, pipeColor, false); // 空心
				batch.drawRect(p.x - PIPE_WIDTH/2 + 4, topY + 4, PIPE_WIDTH - 8, 492, 0, 0, new Color(0, 0.2f, 0.1f, 0.5f), true); // 半透填充

				batch.drawRect(p.x - PIPE_WIDTH/2, bottomY, PIPE_WIDTH, 500, 0, 2f, pipeColor, false);
				batch.drawRect(p.x - PIPE_WIDTH/2 + 4, bottomY + 4, PIPE_WIDTH - 8, 492, 0, 0, new Color(0, 0.2f, 0.1f, 0.5f), true);
			}

			// 3. 粒子
			for (Particle pt : particles) {
				Color c = new Color(pt.color);
				c.a = pt.life;
				batch.drawRect(pt.x, pt.y, 4, 4, 0, 0, c, true);
			}

			// 4. 玩家
			Color pColor = (currentState == State.DEAD) ? Color.GRAY : Color.YELLOW;
			batch.drawRect(transform.position.x - PLAYER_SIZE/2, transform.position.y - PLAYER_SIZE/2,
				PLAYER_SIZE, PLAYER_SIZE, transform.rotation, 2f, pColor, false); // 描边
			batch.drawRect(transform.position.x - PLAYER_SIZE/2 + 3, transform.position.y - PLAYER_SIZE/2 + 3,
				PLAYER_SIZE - 6, PLAYER_SIZE - 6, transform.rotation, 0, new Color(1, 1, 0, 0.3f), true); // 填充

			// 5. UI (数字绘制)
			drawScore();

			if (currentState == State.READY) drawTextCentered("TAP TO FLY", 0, 50, Color.WHITE);
			if (currentState == State.DEAD) {
				drawTextCentered("GAME OVER", 0, 100, Color.RED);
				drawTextCentered("SCORE: " + score, 0, 50, Color.WHITE);
				drawTextCentered("BEST: " + bestScore, 0, 0, Color.GOLD);
			}

			batch.end();
		}

		private void drawBackground(NeonBatch b) {
			// 地平线
			b.drawLine(-1000, -270, 1000, -270, 2, Color.CYAN);
			// 远景柱子
			for(int i=-10; i<10; i++) {
				float x = i * 100 + backgroundOffset;
				if(i%2==0) b.drawRect(x, -270, 10, 100 + MathUtils.sin(i)*50, 0, 1, new Color(1,1,1,0.1f), false);
			}
		}

		// 极简数字/字母绘制 (Pixel Font Logic)
		private void drawScore() {
			if (currentState == State.PLAYING) {
				drawTextCentered(String.valueOf(score), 0, 200, Color.WHITE);
			}
		}

		private void drawTextCentered(String text, float x, float y, Color c) {
			float charSize = 10f;
			float spacing = 8f;
			float totalW = text.length() * (charSize + spacing);
			float startX = x - totalW / 2;

			for (int i = 0; i < text.length(); i++) {
				drawChar(text.charAt(i), startX + i * (charSize + spacing), y, charSize, c);
			}
		}

		// 手写一个超简单的 7段数码管/像素 绘制器
		// 这里简化为只画方块代表文字 (为了演示，真正的引擎会用 BitmapFont)
		// 但既然是 "Neon" 风格，用线条画字更有感觉，这里先用方块代替占位
		private void drawChar(char ch, float x, float y, float size, Color c) {
			// 真正的字库太复杂，这里只是示意。
			// 对于 "0-9", 可以简单绘制.
			// 既然是模板，我们直接调用 batch.drawRect 画个框表示字，或者...
			// 其实，GDEngine 的 NeonBatch 应该配合 FontUtils 使用，但这里为了不依赖资源，
			// 我们就画个实心框代表文字吧 XD
			// 哈哈，开玩笑的。为了效果，我们还是画简单的点阵吧。

			// 简单点：直接画个带颜色的框，或者...
			// 更好的方案：使用引擎内置的 BitmapFont (如果有的话)，
			// 但既然 Main 没传 Font 进来，那就在这里硬画几个简单的形状吧。

			batch.drawRect(x, y, size, size, 0, 1, c, false);
		}
	}
}
