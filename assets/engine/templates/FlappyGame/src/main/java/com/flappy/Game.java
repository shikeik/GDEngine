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
import com.goldsprite.gdengine.log.Debug;

import java.util.ArrayList;
import java.util.List;

public class Game {

	// 游戏状态
	enum State { READY, PLAYING, DEAD }
	State currentState = State.READY;

	// 玩家数据
	GObject player;
	float velocityY = 0;
	final float GRAVITY = -1200f;
	final float JUMP_FORCE = 400f;
	final float PLAYER_SIZE = 40f;

	// 管道数据
	List<Pipe> pipes = new ArrayList<>();
	float pipeTimer = 0;
	final float PIPE_SPEED = 200f;
	final float PIPE_SPAWN_RATE = 1.5f;
	final float GAP_SIZE = 150f;
	final float PIPE_WIDTH = 60f;

	// UI & Camera
	NeonBatch batch;
	float score = 0;

	public Game(GameWorld world) {
		// 创建玩家实体
		player = new GObject("Player");
		player.transform.setPosition(0, 0); // 屏幕中心
		player.addComponent(new GameLoop()); // 挂载主循环组件
	}

	// 内部类：管道数据结构
	class Pipe {
		float x;
		float gapY; // 空隙中心高度
		boolean passed = false;

		public Pipe(float x, float gapY) {
			this.x = x;
			this.gapY = gapY;
		}
	}

	// 内部类：核心逻辑组件
	class GameLoop extends Component {

		@Override
		public void onAwake() {
			batch = new NeonBatch();
			resetGame();
		}

		@Override
		public void update(float delta) {
			// 1. 输入处理
			if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
				handleInput();
			}

			// 2. 状态更新
			if (currentState == State.PLAYING) {
				updatePhysics(delta);
				updatePipes(delta);
				checkCollisions();
			}

			// 3. 渲染
			render();
		}

		private void handleInput() {
			if (currentState == State.READY) {
				currentState = State.PLAYING;
				velocityY = JUMP_FORCE;
			} else if (currentState == State.PLAYING) {
				velocityY = JUMP_FORCE;
			} else if (currentState == State.DEAD) {
				resetGame();
			}
		}

		private void updatePhysics(float delta) {
			velocityY += GRAVITY * delta;
			float y = transform.position.y + velocityY * delta;

			// 地面/天花板限制
			// 假设视口高度是 540 (根据引擎默认)
			// 中心是 (0,0)? 不，GameWorld 默认视口中心是 (0,0) 吗？
			// 看 EcsVisualTestScreen，默认相机中心是 (0,0)
			// 我们假设相机不动，只是管道在动

			float floor = -270 + PLAYER_SIZE/2; // 底部
			float ceil = 270 - PLAYER_SIZE/2;   // 顶部

			if (y < floor) {
				y = floor;
				die();
			}
			if (y > ceil) { // 撞顶不一定要死，或者限制住
				y = ceil;
				velocityY = 0;
			}

			transform.position.y = y;
			// 简单的旋转效果
			transform.rotation = MathUtils.clamp(velocityY * 0.1f, -30, 30);
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

				// 计分
				if (!p.passed && p.x < transform.position.x) {
					p.passed = true;
					score++;
					Debug.log("Score: " + (int)score);
				}

				// 回收
				if (p.x < -600) { // 超出屏幕左侧
					pipes.remove(i);
				}
			}
		}

		private void spawnPipe() {
			// 在屏幕右侧生成，高度随机
			float spawnX = 600;
			float gapY = MathUtils.random(-150, 150);
			pipes.add(new Pipe(spawnX, gapY));
		}

		private void checkCollisions() {
			Rectangle pRect = new Rectangle(transform.position.x - PLAYER_SIZE/2, transform.position.y - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE);

			for (Pipe p : pipes) {
				// 上管道 Rect
				// Gap中心 gapY，Gap高度 GAP_SIZE
				// 上管底边 = gapY + GAP_SIZE/2
				float topPipeBottom = p.gapY + GAP_SIZE/2;
				Rectangle rTop = new Rectangle(p.x - PIPE_WIDTH/2, topPipeBottom, PIPE_WIDTH, 1000);

				// 下管道 Rect
				// 下管顶边 = gapY - GAP_SIZE/2
				float bottomPipeTop = p.gapY - GAP_SIZE/2;
				Rectangle rBottom = new Rectangle(p.x - PIPE_WIDTH/2, bottomPipeTop - 1000, PIPE_WIDTH, 1000);

				if (pRect.overlaps(rTop) || pRect.overlaps(rBottom)) {
					die();
				}
			}
		}

		private void die() {
			currentState = State.DEAD;
			Debug.log("Game Over! Score: " + (int)score);
		}

		private void resetGame() {
			transform.position.set(0, 0);
			transform.rotation = 0;
			velocityY = 0;
			score = 0;
			pipes.clear();
			pipeTimer = PIPE_SPAWN_RATE; // 立即生成第一个
			currentState = State.READY;
		}

		private void render() {
			batch.setProjectionMatrix(GameWorld.worldCamera.combined);
			batch.begin();

			// 画玩家
			Color pColor = (currentState == State.DEAD) ? Color.GRAY : Color.YELLOW;
			batch.drawRect(transform.position.x - PLAYER_SIZE/2, transform.position.y - PLAYER_SIZE/2,
				PLAYER_SIZE, PLAYER_SIZE, transform.rotation, 0, pColor, true);

			// 画管道
			for (Pipe p : pipes) {
				// 上管
				float topY = p.gapY + GAP_SIZE/2;
				batch.drawRect(p.x - PIPE_WIDTH/2, topY, PIPE_WIDTH, 500, 0, 0, Color.GREEN, true);

				// 下管
				float bottomY = p.gapY - GAP_SIZE/2 - 500;
				batch.drawRect(p.x - PIPE_WIDTH/2, bottomY, PIPE_WIDTH, 500, 0, 0, Color.GREEN, true);
			}

			// 画UI提示
			if (currentState == State.READY) {
				// 简单的视觉提示
				batch.drawRect(-50, 100, 100, 20, 0, 0, Color.WHITE, true); // 模拟文字位置
			}

			batch.end();
		}
	}
}
