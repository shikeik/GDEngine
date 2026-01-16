package com.goldsprite.screens.tests;

// 单向平台系统 - 完整示例
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.screens.GScreen;

import java.util.Comparator;

public class OneWayPlatformSystemWrapper extends GScreen {
	OneWayPlatformSystem c;

	@Override public void create() {
		c = new OneWayPlatformSystem();
		c.create();
	}

	@Override public void render(float delta) {
		c.render();
	}

	@Override public void resize(int width, int height) {
		c.resize(width, height);
	}

	public class OneWayPlatformSystem extends ApplicationAdapter {
		// 渲染工具
		private ShapeRenderer shapeRenderer;
		private OrthographicCamera camera;
		private Viewport viewport;

		// 游戏世界常量
		private static final float WORLD_WIDTH = 800;
		private static final float WORLD_HEIGHT = 600;
		private static final float GRAVITY = -900f;
		private static final float PLAYER_JUMP_FORCE = 500f;
		private static final float PLAYER_SPEED = 300f;

		// 玩家属性
		private Rectangle player;
		private Vector2 playerVelocity;
		private boolean isOnGround;
		private boolean isJumping;
		private Color playerColor;

		// 平台数组
		private Array<Platform> platforms;
		private Array<OneWayPlatform> oneWayPlatforms;

		// 移动平台追踪
		private OneWayPlatform currentPlatform;
		private Vector2 platformOffset;

		// 调试信息
		private boolean showDebugInfo = true;

		@Override
		public void create() {
			// 初始化相机和视口
			camera = new OrthographicCamera();
			viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
			camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

			// 初始化渲染器
			shapeRenderer = new ShapeRenderer();

			// 初始化玩家
			player = new Rectangle(100, 100, 30, 50);
			playerVelocity = new Vector2(0, 0);
			playerColor = new Color(0.2f, 0.6f, 1f, 1f);

			// 初始化平台追踪
			currentPlatform = null;
			platformOffset = new Vector2();

			// 初始化平台数组
			platforms = new Array<>();
			oneWayPlatforms = new Array<>();

			// 创建普通平台（双向碰撞）
			createStandardPlatforms();

			// 创建单向平台
			createOneWayPlatforms();
		}

		/**
		 * 创建标准平台（双向碰撞）
		 */
		private void createStandardPlatforms() {
			// 地面
			platforms.add(new Platform(0, 0, WORLD_WIDTH, 40));

			// 墙
			platforms.add(new Platform(0, 0, 40, WORLD_HEIGHT));
			platforms.add(new Platform(WORLD_WIDTH - 40, 0, 40, WORLD_HEIGHT));

			// 天花板 - 修复：确保有天花板
			platforms.add(new Platform(0, WORLD_HEIGHT - 20, WORLD_WIDTH, 20));

			// 一些平台
			platforms.add(new Platform(200, 150, 200, 20));
			platforms.add(new Platform(450, 250, 200, 20));
			platforms.add(new Platform(100, 350, 200, 20));
		}

		/**
		 * 创建单向平台
		 */
		private void createOneWayPlatforms() {
			// 单向平台 - 只能从下方跳上
			oneWayPlatforms.add(new OneWayPlatform(300, 200, 200, 15,
				new Color(0.8f, 0.4f, 0.1f, 1f)));

			oneWayPlatforms.add(new OneWayPlatform(550, 300, 150, 15,
				new Color(0.8f, 0.4f, 0.1f, 1f)));

			oneWayPlatforms.add(new OneWayPlatform(150, 400, 180, 15,
				new Color(0.8f, 0.4f, 0.1f, 1f)));

			// 移动的单向平台 - 修复移动逻辑
			OneWayPlatform movingPlatform = new OneWayPlatform(400, 100, 120, 15,
				new Color(0.9f, 0.5f, 0.2f, 1f));
			movingPlatform.setVelocity(80, 0); // 水平移动
			movingPlatform.setMoveBounds(300, 500, 100, 100);
			movingPlatform.setMoveLoop(true);
			oneWayPlatforms.add(movingPlatform);

			// 添加更多平台测试边界情况
			oneWayPlatforms.add(new OneWayPlatform(200, 500, 150, 15,
				new Color(0.7f, 0.3f, 0.8f, 1f)));
		}

		@Override
		public void render() {
			// 处理输入
			handleInput();

			// 更新游戏逻辑
			update(Gdx.graphics.getDeltaTime());

			// 清屏
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			// 更新相机
			camera.update();
			shapeRenderer.setProjectionMatrix(camera.combined);

			// 绘制游戏对象
			drawGameObjects();

			// 绘制调试信息
			if (showDebugInfo) {
				drawDebugInfo();
			}
		}

		/**
		 * 处理玩家输入
		 */
		private void handleInput() {
			// 水平移动
			playerVelocity.x = 0;
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
				playerVelocity.x = -PLAYER_SPEED;
			}
			if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
				playerVelocity.x = PLAYER_SPEED;
			}

			// 跳跃
			if ((Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W) ||
				Gdx.input.isKeyPressed(Input.Keys.SPACE)) && (isOnGround || currentPlatform != null)) {
				playerVelocity.y = PLAYER_JUMP_FORCE;
				isJumping = true;
				isOnGround = false;
				currentPlatform = null; // 跳起时离开平台
			}

			// 从单向平台下落
			if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
				// 按下时允许从单向平台下落
				for (OneWayPlatform platform : oneWayPlatforms) {
					if (platform.isPlayerOnTop(player)) {
						// 暂时禁用该平台的碰撞
						platform.allowFallThrough = true;
						currentPlatform = null; // 离开当前平台
					}
				}
			} else {
				// 重置下落标志
				for (OneWayPlatform platform : oneWayPlatforms) {
					platform.allowFallThrough = false;
				}
			}

			// 切换调试信息显示
			if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
				showDebugInfo = !showDebugInfo;
			}

			// 重置玩家位置
			if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
				player.setPosition(100, 100);
				playerVelocity.set(0, 0);
				currentPlatform = null;
			}
		}

		/**
		 * 更新游戏逻辑
		 */
		private void update(float deltaTime) {
			// 保存之前的位置用于碰撞检测
			float oldX = player.x;
			float oldY = player.y;

			// 更新移动平台
			for (OneWayPlatform platform : oneWayPlatforms) {
				platform.update(deltaTime);
			}

			// 如果玩家站在移动平台上，需要跟随平台移动
			if (currentPlatform != null && !isJumping) {
				// 计算平台移动量
				Vector2 platformMovement = currentPlatform.getMovement(deltaTime);
				player.x += platformMovement.x;
				player.y += platformMovement.y;
			}

			// 应用重力（如果不在平台上）
			if (currentPlatform == null) {
				playerVelocity.y += GRAVITY * deltaTime;
			}

			// 更新玩家位置（除了已由平台移动处理的部分）
			player.x += playerVelocity.x * deltaTime;
			if (currentPlatform == null) {
				player.y += playerVelocity.y * deltaTime;
			}

			// 重置地面状态
			isOnGround = false;
			boolean wasOnPlatform = (currentPlatform != null);

			// 1. 先处理与普通平台的碰撞
			for (Platform platform : platforms) {
				resolveCollision(platform.rect);
			}

			// 2. 处理与单向平台的碰撞
			currentPlatform = null; // 重置当前平台

			for (OneWayPlatform platform : oneWayPlatforms) {
				boolean collided = platform.resolveCollision(player, playerVelocity, oldY);

				if (collided) {
					// 如果与这个平台发生碰撞，记录为当前平台
					currentPlatform = platform;
					isOnGround = true;
					isJumping = false;
					playerVelocity.y = 0;
				}
			}

			// 3. 如果之前站在平台上但现在没有了，恢复重力
			if (wasOnPlatform && currentPlatform == null) {
				playerVelocity.y = 0; // 重置速度，让重力自然作用
			}

			// 限制玩家在世界边界内
			if (player.x < 0) {
				player.x = 0;
				playerVelocity.x = 0;
			}
			if (player.x + player.width > WORLD_WIDTH) {
				player.x = WORLD_WIDTH - player.width;
				playerVelocity.x = 0;
			}
			if (player.y < 0) {
				player.y = 0;
				playerVelocity.y = 0;
				isOnGround = true;
				currentPlatform = null;
			}
			if (player.y + player.height > WORLD_HEIGHT) {
				player.y = WORLD_HEIGHT - player.height;
				playerVelocity.y = 0;
			}

			// 更新跳跃状态
			if (isOnGround) {
				isJumping = false;
			}
		}

		/**
		 * 处理与普通平台的碰撞
		 */
		private void resolveCollision(Rectangle platform) {
			if (!player.overlaps(platform)) return;

			// 计算重叠量
			float overlapLeft = player.x + player.width - platform.x;
			float overlapRight = platform.x + platform.width - player.x;
			float overlapTop = player.y + player.height - platform.y;
			float overlapBottom = platform.y + platform.height - player.y;

			// 找到最小重叠方向
			float minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
				Math.min(overlapTop, overlapBottom));

			// 根据最小重叠方向解决碰撞
			if (minOverlap == overlapLeft) {
				// 从左碰撞
				player.x = platform.x - player.width;
				playerVelocity.x = 0;
			} else if (minOverlap == overlapRight) {
				// 从右碰撞
				player.x = platform.x + platform.width;
				playerVelocity.x = 0;
			} else if (minOverlap == overlapTop) {
				// 从上碰撞
				player.y = platform.y - player.height;
				playerVelocity.y = 0;
				isJumping = false;
			} else if (minOverlap == overlapBottom) {
				// 从下碰撞
				player.y = platform.y + platform.height;
				playerVelocity.y = 0;
				isOnGround = true;
				currentPlatform = null;
			}
		}

		/**
		 * 绘制游戏对象
		 */
		private void drawGameObjects() {
			shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

			// 绘制普通平台
			shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
			for (Platform platform : platforms) {
				shapeRenderer.rect(platform.rect.x, platform.rect.y,
					platform.rect.width, platform.rect.height);
			}

			// 绘制单向平台
			for (OneWayPlatform platform : oneWayPlatforms) {
				platform.draw(shapeRenderer);
			}

			// 绘制玩家
			shapeRenderer.setColor(playerColor);
			shapeRenderer.rect(player.x, player.y, player.width, player.height);

			// 绘制玩家脚部指示器（绿色表示在地上，红色表示在空中）
			if (isOnGround) {
				shapeRenderer.setColor(0, 1, 0, 1); // 绿色
			} else {
				shapeRenderer.setColor(1, 0, 0, 1); // 红色
			}
			shapeRenderer.rect(player.x + player.width/2 - 5, player.y - 5, 10, 5);

			// 如果站在平台上，绘制连接线
			if (currentPlatform != null) {
				shapeRenderer.setColor(1, 1, 0, 0.5f);
				float playerBottomCenterX = player.x + player.width / 2;
				float playerBottomCenterY = player.y;
				float platformTopCenterX = currentPlatform.rect.x + currentPlatform.rect.width / 2;
				float platformTopCenterY = currentPlatform.rect.y + currentPlatform.rect.height;

				shapeRenderer.rectLine(playerBottomCenterX, playerBottomCenterY,
					platformTopCenterX, platformTopCenterY, 2);
			}

			shapeRenderer.end();

			// 绘制平台碰撞区域（线框）
			shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

			// 单向平台碰撞区域
			shapeRenderer.setColor(0, 1, 0, 0.5f);
			for (OneWayPlatform platform : oneWayPlatforms) {
				shapeRenderer.rect(platform.rect.x, platform.rect.y,
					platform.rect.width, platform.rect.height);
				// 绘制单向箭头
				float centerX = platform.rect.x + platform.rect.width / 2;
				float centerY = platform.rect.y + platform.rect.height / 2;
				shapeRenderer.triangle(
					centerX - 10, centerY + 5,
					centerX + 10, centerY + 5,
					centerX, centerY + 15
				);
			}

			shapeRenderer.end();
		}

		/**
		 * 绘制调试信息
		 */
		private void drawDebugInfo() {
			shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

			// 绘制调试信息背景
			shapeRenderer.setColor(0, 0, 0, 0.7f);
			shapeRenderer.rect(10, WORLD_HEIGHT - 160, 350, 150);

			// 绘制状态信息
			shapeRenderer.setColor(1, 1, 1, 1);
			float y = WORLD_HEIGHT - 30;

			// 玩家位置信息
			drawDebugText("Player: (" + (int)player.x + ", " + (int)player.y + ")", 20, y);
			y -= 20;
			drawDebugText("Velocity: (" + (int)playerVelocity.x + ", " + (int)playerVelocity.y + ")", 20, y);
			y -= 20;
			drawDebugText("On Ground: " + isOnGround, 20, y);
			y -= 20;
			drawDebugText("Jumping: " + isJumping, 20, y);
			y -= 20;
			drawDebugText("On Platform: " + (currentPlatform != null), 20, y);
			y -= 20;
			drawDebugText("Platforms: " + oneWayPlatforms.size, 20, y);
			y -= 20;

			// 控制说明
			drawDebugText("Controls:", 20, y);
			y -= 20;
			drawDebugText("Arrow/WASD: Move", 20, y);
			y -= 20;
			drawDebugText("Space/Up: Jump", 20, y);
			y -= 20;
			drawDebugText("Down/S: Fall thru", 20, y);
			y -= 20;
			drawDebugText("F1: Toggle Debug", 20, y);
			y -= 20;
			drawDebugText("R: Reset Position", 20, y);

			shapeRenderer.end();
		}

		/**
		 * 绘制调试文字（简化版）
		 */
		private void drawDebugText(String text, float x, float y) {
			// 简单用方块表示文字，实际项目中应该使用BitmapFont
			shapeRenderer.setColor(1, 1, 1, 1);
			for (int i = 0; i < Math.min(text.length(), 30); i++) {
				char c = text.charAt(i);
				if (c != ' ') {
					shapeRenderer.rect(x + i * 8, y - 10, 6, 10);
				}
			}
		}

		@Override
		public void resize(int width, int height) {
			viewport.update(width, height);
		}

		@Override
		public void dispose() {
			shapeRenderer.dispose();
		}

		/**
		 * 普通平台类
		 */
		class Platform {
			Rectangle rect;

			Platform(float x, float y, float width, float height) {
				this.rect = new Rectangle(x, y, width, height);
			}
		}

		/**
		 * 单向平台类 - 修复版
		 */
		class OneWayPlatform {
			Rectangle rect;
			Color color;
			Vector2 velocity;
			Vector2 lastPosition;
			Rectangle moveBounds;
			boolean allowFallThrough;
			boolean moveLoop;
			float moveRangeX, moveRangeY;

			OneWayPlatform(float x, float y, float width, float height, Color color) {
				this.rect = new Rectangle(x, y, width, height);
				this.color = color;
				this.velocity = new Vector2(0, 0);
				this.lastPosition = new Vector2(x, y);
				this.moveBounds = null;
				this.allowFallThrough = false;
				this.moveLoop = false;
			}

			void setVelocity(float vx, float vy) {
				this.velocity.set(vx, vy);
			}

			void setMoveBounds(float x, float y, float width, float height) {
				this.moveBounds = new Rectangle(x, y, width, height);
				this.moveRangeX = width - rect.width;
				this.moveRangeY = height - rect.height;
			}

			void setMoveLoop(boolean loop) {
				this.moveLoop = loop;
			}

			void update(float deltaTime) {
				// 保存当前位置
				lastPosition.set(rect.x, rect.y);

				if (velocity.len2() > 0) {
					// 移动平台
					rect.x += velocity.x * deltaTime;
					rect.y += velocity.y * deltaTime;

					// 如果有移动边界，检查边界
					if (moveBounds != null) {
						if (moveLoop) {
							// 循环移动模式
							if (rect.x < moveBounds.x) {
								rect.x = moveBounds.x + moveRangeX;
							}
							if (rect.x > moveBounds.x + moveRangeX) {
								rect.x = moveBounds.x;
							}
							if (rect.y < moveBounds.y) {
								rect.y = moveBounds.y + moveRangeY;
							}
							if (rect.y > moveBounds.y + moveRangeY) {
								rect.y = moveBounds.y;
							}
						} else {
							// 反弹模式
							if (rect.x < moveBounds.x || rect.x > moveBounds.x + moveRangeX) {
								velocity.x = -velocity.x;
								rect.x = Math.max(moveBounds.x, Math.min(rect.x, moveBounds.x + moveRangeX));
							}
							if (rect.y < moveBounds.y || rect.y > moveBounds.y + moveRangeY) {
								velocity.y = -velocity.y;
								rect.y = Math.max(moveBounds.y, Math.min(rect.y, moveBounds.y + moveRangeY));
							}
						}
					}
				}
			}

			/**
			 * 获取平台移动量
			 */
			Vector2 getMovement(float deltaTime) {
				return new Vector2(velocity.x * deltaTime, velocity.y * deltaTime);
			}

			/**
			 * 解析与玩家的碰撞 - 修复版
			 */
			boolean resolveCollision(Rectangle player, Vector2 playerVelocity, float oldPlayerY) {
				// 如果玩家正在主动下落，允许穿过
				if (allowFallThrough) {
					return false;
				}

				// 检查是否发生碰撞
				if (!player.overlaps(rect)) {
					return false;
				}

				// 获取玩家的脚部位置
				float playerFeet = player.y;
				float platformTop = rect.y + rect.height;

				// 计算玩家前一帧的底部位置
				float playerPreviousBottom = oldPlayerY;

				// 关键判断：只有当玩家从上方落下时，才触发碰撞
				float tolerance = 8f; // 稍微增加容差

				// 修复逻辑：确保玩家是从上方落下的
				boolean isFallingDown = playerVelocity.y <= 0;
				boolean wasAbovePlatform = playerPreviousBottom >= platformTop;
				boolean isWithinHorizontalBounds =
					player.x + player.width > rect.x + 5 &&
						player.x < rect.x + rect.width - 5;

				if (isFallingDown && wasAbovePlatform && isWithinHorizontalBounds) {
					// 计算玩家脚部与平台顶部的距离
					float distanceToPlatformTop = platformTop - playerFeet;

					// 如果玩家在平台顶部附近
					if (distanceToPlatformTop >= -tolerance && distanceToPlatformTop <= tolerance) {
						// 将玩家放在平台顶部
						player.y = platformTop;

						// 如果平台在移动，调整玩家位置以保持相对位置
						if (velocity.len2() > 0) {
							// 计算平台移动量
							float platformDeltaX = rect.x - lastPosition.x;
							float platformDeltaY = rect.y - lastPosition.y;

							// 玩家跟随平台移动
							player.x += platformDeltaX;
							player.y += platformDeltaY;
						}

						return true;
					}
				}

				return false;
			}

			/**
			 * 检查玩家是否站在平台顶部
			 */
			boolean isPlayerOnTop(Rectangle player) {
				float tolerance = 5f;
				return Math.abs(player.y - (rect.y + rect.height)) <= tolerance &&
					player.x + player.width > rect.x + 5 &&
					player.x < rect.x + rect.width - 5;
			}

			void draw(ShapeRenderer shapeRenderer) {
				// 绘制平台主体
				shapeRenderer.setColor(color);
				shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);

				// 绘制平台顶部高亮（表示可站立区域）
				shapeRenderer.setColor(color.r * 1.2f, color.g * 1.2f, color.b * 1.2f, 1);
				shapeRenderer.rect(rect.x, rect.y + rect.height - 3, rect.width, 3);

				// 如果是移动平台，绘制移动指示器
				if (velocity.len2() > 0) {
					shapeRenderer.setColor(1, 1, 0, 0.7f);
					float centerX = rect.x + rect.width / 2;
					float centerY = rect.y + rect.height / 2;

					// 绘制移动方向箭头
					if (velocity.x > 0) {
						shapeRenderer.triangle(
							centerX + 20, centerY,
							centerX + 5, centerY - 10,
							centerX + 5, centerY + 10
						);
					} else if (velocity.x < 0) {
						shapeRenderer.triangle(
							centerX - 20, centerY,
							centerX - 5, centerY - 10,
							centerX - 5, centerY + 10
						);
					}

					if (velocity.y > 0) {
						shapeRenderer.triangle(
							centerX, centerY + 20,
							centerX - 10, centerY + 5,
							centerX + 10, centerY + 5
						);
					} else if (velocity.y < 0) {
						shapeRenderer.triangle(
							centerX, centerY - 20,
							centerX - 10, centerY - 5,
							centerX + 10, centerY - 5
						);
					}
				}
			}
		}
	}

}
