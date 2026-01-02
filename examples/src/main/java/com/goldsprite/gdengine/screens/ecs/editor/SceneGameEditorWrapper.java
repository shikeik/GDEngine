package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.IntBuffer;

public class SceneGameEditorWrapper extends GScreen {
	private SceneGameEditor d;
	
	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		d = new SceneGameEditor();
		d.create();
	}

	@Override
	public void render(float delta) {
		d.render();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		d.resize(width, height);
	}
}

	

	class SceneGameEditor extends ApplicationAdapter {

		// ==================== 主渲染组件 ====================
		private SpriteBatch batch;
		private ShapeRenderer shapes;

		// ==================== 窗口系统 ====================
		private Array<VisWindow> windows = new Array<>();
		private GameWindow gameWindow;
		private SceneWindow sceneWindow;

		// ==================== 输入管理 ====================
		private EditorInputManager inputManager;

		@Override
		public void create() {
			batch = new SpriteBatch();
			shapes = new ShapeRenderer();

			// 创建游戏窗口（ExtendViewport）
			gameWindow = new GameWindow("Game View", 50, 50, 400, 400);

			// 创建场景窗口（ScreenViewport）
			sceneWindow = new SceneWindow("Scene View", 500, 50, 400, 400);

			// 添加到窗口列表
			windows.add(gameWindow);
			windows.add(sceneWindow);

			// 初始化输入管理器
			inputManager = new EditorInputManager(windows);
			Gdx.input.setInputProcessor(inputManager);

			// 创建测试对象
			sceneWindow.addTestObjects();
		}

		@Override
		public void render() {
			Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			// 渲染所有窗口
			for (VisWindow window : windows) {
				window.render();
			}
		}

		@Override
		public void resize(int width, int height) {
			// 可以在这里处理屏幕大小变化
		}

		@Override
		public void dispose() {
			batch.dispose();
			shapes.dispose();
			for (VisWindow window : windows) {
				window.dispose();
			}
		}

		// ==================== 窗口基类 ====================
		public abstract class VisWindow {
			// 窗口属性
			protected String title;
			protected float x, y;
			protected float width, height;
			protected float minWidth = 200, minHeight = 200;

			// 视口和相机
			protected Viewport viewport;
			protected OrthographicCamera camera;

			// 渲染状态
			protected boolean needsRender = true;

			// 窗口装饰
			protected static final int TITLE_BAR_HEIGHT = 30;
			protected static final int BORDER_SIZE = 8;
			protected static final Color WINDOW_BG = new Color(0.25f, 0.25f, 0.25f, 0.9f);
			protected static final Color TITLE_BAR_COLOR = new Color(0.3f, 0.5f, 0.7f, 0.9f);

			// 窗口操作状态
			protected boolean isDragging = false;
			protected boolean isResizing = false;
			protected int dragMode = -1; // 0=移动, 1=左上, 2=上, 3=右上, 4=左, 5=右, 6=左下, 7=下, 8=右下
			protected float dragStartX, dragStartY;
			protected float windowStartX, windowStartY, windowStartW, windowStartH;

			// GL状态保存
			private IntBuffer oldScissor;
			private boolean wasScissorEnabled;

			public VisWindow(String title, float x, float y, float width, float height) {
				this.title = title;
				this.x = x;
				this.y = y;
				this.width = width;
				this.height = height;

				// 初始化IntBuffer
				oldScissor = BufferUtils.newIntBuffer(4);
			}

			// 初始化视口（子类实现）
			protected abstract void initViewport();

			// 渲染内容（子类实现）
			protected abstract void renderContent();

			// 处理输入（子类实现）
			public abstract boolean handleInput(float screenX, float screenY, int action, int button);

			// 完整渲染流程
			public void render() {
				// 开始渲染 - 设置视口和剪裁
				beginRender();

				// 渲染窗口内容
				renderContent();

				// 结束渲染 - 恢复状态
				endRender();

				// 渲染窗口装饰（在屏幕坐标）
				renderWindowDecoration();
			}

			protected void beginRender() {
				// 保存当前OpenGL状态
				pushGLState();

				// 设置视口
				Gdx.gl.glViewport((int)x, (int)y, (int)width, (int)height);

				// 设置剪裁（防止绘制到窗口外）
				Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
				Gdx.gl.glScissor((int)x, (int)y, (int)width, (int)height);

				// 清除视口区域
				Gdx.gl.glClearColor(WINDOW_BG.r, WINDOW_BG.g, WINDOW_BG.b, WINDOW_BG.a);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

				// 应用视口和相机
				viewport.apply();
			}

			protected void endRender() {
				// 恢复OpenGL状态
				popGLState();
			}

			protected void pushGLState() {
				// 保存剪裁状态
				wasScissorEnabled = Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST);

				// 使用IntBuffer获取剪裁区域
				Gdx.gl.glGetIntegerv(GL20.GL_SCISSOR_BOX, oldScissor);
			}

			protected void popGLState() {
				// 恢复剪裁区域
				if (wasScissorEnabled) {
					Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
				} else {
					Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
				}

				// 从IntBuffer读取值
				oldScissor.rewind();
				int oldX = oldScissor.get();
				int oldY = oldScissor.get();
				int oldWidth = oldScissor.get();
				int oldHeight = oldScissor.get();
				Gdx.gl.glScissor(oldX, oldY, oldWidth, oldHeight);
			}

			protected void renderWindowDecoration() {
				// 使用ShapeRenderer绘制窗口边框和标题栏
				shapes.begin(ShapeRenderer.ShapeType.Filled);

				// 窗口背景（带圆角）
				shapes.setColor(WINDOW_BG);
				shapes.rect(x, y, width, height);

				// 标题栏
				shapes.setColor(TITLE_BAR_COLOR);
				shapes.rect(x, y + height - TITLE_BAR_HEIGHT, width, TITLE_BAR_HEIGHT);

				// 边框（可拖动区域指示）
				if (isResizing || isDragging) {
					shapes.setColor(1, 1, 1, 0.3f);
					shapes.rect(x, y, width, BORDER_SIZE); // 下
					shapes.rect(x, y + height - BORDER_SIZE, width, BORDER_SIZE); // 上
					shapes.rect(x, y, BORDER_SIZE, height); // 左
					shapes.rect(x + width - BORDER_SIZE, y, BORDER_SIZE, height); // 右
				}

				shapes.end();
			}

			// 检查是否在窗口内
			public boolean contains(float screenX, float screenY) {
				return screenX >= x && screenX <= x + width &&
					screenY >= y && screenY <= y + height;
			}

			// 检查鼠标在哪个区域
			public int checkArea(float screenX, float screenY) {
				float localX = screenX - x;
				float localY = screenY - y;

				// 检查标题栏
				if (localY > height - TITLE_BAR_HEIGHT) {
					return 0; // 移动
				}

				// 检查边框
				boolean left = localX < BORDER_SIZE;
				boolean right = localX > width - BORDER_SIZE;
				boolean top = localY > height - BORDER_SIZE;
				boolean bottom = localY < BORDER_SIZE;

				if (left && top) return 1;
				if (top && !left && !right) return 2;
				if (right && top) return 3;
				if (left && !top && !bottom) return 4;
				if (right && !top && !bottom) return 5;
				if (left && bottom) return 6;
				if (bottom && !left && !right) return 7;
				if (right && bottom) return 8;

				return -1; // 内容区域
			}

			// 开始窗口操作
			public boolean startWindowOperation(float screenX, float screenY, int area) {
				dragMode = area;
				dragStartX = screenX;
				dragStartY = screenY;
				windowStartX = x;
				windowStartY = y;
				windowStartW = width;
				windowStartH = height;

				if (area == 0) {
					isDragging = true;
				} else if (area >= 1 && area <= 8) {
					isResizing = true;
				}

				return area != -1;
			}

			// 更新窗口操作
			public void updateWindowOperation(float screenX, float screenY) {
				if (!isDragging && !isResizing) return;

				float deltaX = screenX - dragStartX;
				float deltaY = screenY - dragStartY;

				switch (dragMode) {
					case 0: // 移动
						x = windowStartX + deltaX;
						y = windowStartY + deltaY;
						break;

					case 1: // 左上
						x = windowStartX + deltaX;
						y = windowStartY + deltaY;
						width = windowStartW - deltaX;
						height = windowStartH + deltaY;
						break;

					case 2: // 上
						y = windowStartY + deltaY;
						height = windowStartH + deltaY;
						break;

					case 3: // 右上
						y = windowStartY + deltaY;
						width = windowStartW + deltaX;
						height = windowStartH + deltaY;
						break;

					case 4: // 左
						x = windowStartX + deltaX;
						width = windowStartW - deltaX;
						break;

					case 5: // 右
						width = windowStartW + deltaX;
						break;

					case 6: // 左下
						x = windowStartX + deltaX;
						width = windowStartW - deltaX;
						height = windowStartH - deltaY;
						break;

					case 7: // 下
						height = windowStartH - deltaY;
						break;

					case 8: // 右下
						width = windowStartW + deltaX;
						height = windowStartH - deltaY;
						break;
				}

				// 限制最小尺寸
				width = Math.max(minWidth, width);
				height = Math.max(minHeight, height);

				// 更新视口
				viewport.update((int)width, (int)height);
				needsRender = true;
			}

			// 结束窗口操作
			public void endWindowOperation() {
				isDragging = false;
				isResizing = false;
				dragMode = -1;
			}

			// 屏幕坐标转窗口坐标
			public Vector2 screenToWindow(float screenX, float screenY) {
				return new Vector2(screenX - x, screenY - y);
			}

			// 屏幕坐标转世界坐标
			public Vector2 screenToWorld(float screenX, float screenY) {
				Vector3 worldPos = viewport.unproject(new Vector3(screenX - x, screenY - y, 0));
				return new Vector2(worldPos.x, worldPos.y);
			}

			public void dispose() {
				// 子类可以覆盖
			}
		}

		// ==================== 游戏窗口 ====================
		public class GameWindow extends VisWindow {
			// 游戏世界参数
			private static final float WORLD_WIDTH = 800;
			private static final float WORLD_HEIGHT = 600;

			// 游戏对象
			private Vector2 playerPos = new Vector2(WORLD_WIDTH / 2, WORLD_HEIGHT / 2);
			private float playerSpeed = 300;
			private float playerSize = 20;

			// 摇杆
			private Joystick joystick;
			private Vector2 joystickInput = new Vector2();

			// 网格
			private float gridSize = 50;

			public GameWindow(String title, float x, float y, float width, float height) {
				super(title, x, y, width, height);
				initViewport();

				// 初始化摇杆（放在窗口左下角）
				joystick = new Joystick(80, 80, 60);
			}

			@Override
			protected void initViewport() {
				camera = new OrthographicCamera();
				viewport = new ExtendViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
				viewport.update((int)width, (int)height);
				camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
				camera.update();
			}

			@Override
			protected void renderContent() {
				// 更新游戏逻辑
				updateGame(Gdx.graphics.getDeltaTime());

				// 开始批量渲染
				batch.setProjectionMatrix(camera.combined);
				batch.begin();

				// 绘制网格
				drawGrid();

				// 绘制玩家
				batch.setColor(Color.RED);
				batch.draw(createTexture(Color.RED), 
						   playerPos.x - playerSize / 2, 
						   playerPos.y - playerSize / 2, 
						   playerSize, playerSize);

				batch.end();

				// 使用ShapeRenderer绘制摇杆
				shapes.setProjectionMatrix(camera.combined);
				joystick.render(shapes, 80, 80);
			}

			private void updateGame(float deltaTime) {
				// 获取摇杆输入
				joystickInput = joystick.getInput();

				// 更新玩家位置
				playerPos.x += joystickInput.x * playerSpeed * deltaTime;
				playerPos.y += joystickInput.y * playerSpeed * deltaTime;

				// 限制玩家在世界范围内
				playerPos.x = MathUtils.clamp(playerPos.x, playerSize, WORLD_WIDTH - playerSize);
				playerPos.y = MathUtils.clamp(playerPos.y, playerSize, WORLD_HEIGHT - playerSize);

				// 相机平滑跟随玩家
				float targetX = playerPos.x;
				float targetY = playerPos.y;

				float currentX = camera.position.x;
				float currentY = camera.position.y;

				// 平滑插值（Lerp）
				camera.position.x = currentX + (targetX - currentX) * 0.1f;
				camera.position.y = currentY + (targetY - currentY) * 0.1f;

				// 限制相机边界
				float halfViewportWidth = camera.viewportWidth * camera.zoom / 2;
				float halfViewportHeight = camera.viewportHeight * camera.zoom / 2;

				camera.position.x = MathUtils.clamp(camera.position.x, 
													halfViewportWidth, 
													WORLD_WIDTH - halfViewportWidth);
				camera.position.y = MathUtils.clamp(camera.position.y, 
													halfViewportHeight, 
													WORLD_HEIGHT - halfViewportHeight);

				camera.update();
			}

			private void drawGrid() {
				shapes.setProjectionMatrix(camera.combined);
				shapes.begin(ShapeRenderer.ShapeType.Line);
				shapes.setColor(0.3f, 0.3f, 0.3f, 0.5f);

				// 水平线
				for (float y = 0; y <= WORLD_HEIGHT; y += gridSize) {
					shapes.line(0, y, WORLD_WIDTH, y);
				}

				// 垂直线
				for (float x = 0; x <= WORLD_WIDTH; x += gridSize) {
					shapes.line(x, 0, x, WORLD_HEIGHT);
				}

				shapes.end();
			}

			@Override
			public boolean handleInput(float screenX, float screenY, int action, int button) {
				// 转换为窗口坐标
				Vector2 windowPos = screenToWindow(screenX, screenY);
				float wx = windowPos.x;
				float wy = windowPos.y;

				// 转换为世界坐标
				Vector2 worldPos = screenToWorld(screenX, screenY);

				// 处理摇杆输入
				if (joystick.handleInput(wx, wy, action)) {
					needsRender = true;
					return true;
				}

				return false;
			}

			// 创建简单纹理（白色方块）
			private Texture createTexture(Color color) {
				Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
				pixmap.setColor(color);
				pixmap.fill();
				Texture texture = new Texture(pixmap);
				pixmap.dispose();
				return texture;
			}

			@Override
			public void dispose() {
				// 清理资源
			}
		}

		// ==================== 场景窗口 ====================
		public class SceneWindow extends VisWindow {
			// 场景对象
			private Array<GameObject> objects = new Array<>();
			private GameObject selectedObject = null;

			// 相机控制
			private boolean isDraggingCamera = false;
			private float cameraDragStartX, cameraDragStartY;
			private float cameraStartX, cameraStartY;

			// 网格
			private float gridSize = 50;

			public SceneWindow(String title, float x, float y, float width, float height) {
				super(title, x, y, width, height);
				initViewport();
			}

			@Override
			protected void initViewport() {
				camera = new OrthographicCamera();
				viewport = new ScreenViewport(camera);
				viewport.update((int)width, (int)height);
				camera.position.set(width / 2, height / 2, 0);
				camera.update();
			}

			@Override
			protected void renderContent() {
				// 开始批量渲染
				batch.setProjectionMatrix(camera.combined);
				batch.begin();

				// 绘制网格
				drawGrid();

				// 绘制所有对象
				for (GameObject obj : objects) {
					obj.render(batch);
				}

				// 绘制选中对象的高亮
				if (selectedObject != null) {
					selectedObject.renderSelection(shapes);
				}

				batch.end();
			}

			private void drawGrid() {
				shapes.setProjectionMatrix(camera.combined);
				shapes.begin(ShapeRenderer.ShapeType.Line);

				// 主网格（较深）
				shapes.setColor(0.2f, 0.2f, 0.2f, 0.5f);
				for (float x = 0; x <= width; x += gridSize) {
					shapes.line(x, 0, x, height);
				}
				for (float y = 0; y <= height; y += gridSize) {
					shapes.line(0, y, width, y);
				}

				// 次网格（较浅）
				shapes.setColor(0.15f, 0.15f, 0.15f, 0.3f);
				for (float x = 0; x <= width; x += gridSize / 2) {
					shapes.line(x, 0, x, height);
				}
				for (float y = 0; y <= height; y += gridSize / 2) {
					shapes.line(0, y, width, y);
				}

				shapes.end();
			}

			@Override
			public boolean handleInput(float screenX, float screenY, int action, int button) {
				// 转换为窗口坐标
				Vector2 windowPos = screenToWindow(screenX, screenY);
				float wx = windowPos.x;
				float wy = windowPos.y;

				// 转换为世界坐标（对于ScreenViewport，世界坐标=窗口坐标）
				Vector2 worldPos = new Vector2(wx, wy);

				if (action == 0) { // TOUCH_DOWN
					// 检查是否点击了对象
					GameObject clicked = getObjectAt(worldPos.x, worldPos.y);
					if (clicked != null) {
						selectedObject = clicked;
						selectedObject.startDrag(worldPos.x, worldPos.y);
						needsRender = true;
						return true;
					} else {
						// 开始拖动相机（右键或中键）
						if (button == 1 || button == 2) {
							isDraggingCamera = true;
							cameraDragStartX = wx;
							cameraDragStartY = wy;
							cameraStartX = camera.position.x;
							cameraStartY = camera.position.y;
							return true;
						}
					}
				} else if (action == 1) { // TOUCH_DRAGGED
					if (selectedObject != null && selectedObject.isDragging()) {
						// 拖动选中的对象
						selectedObject.updateDrag(worldPos.x, worldPos.y);
						needsRender = true;
						return true;
					} else if (isDraggingCamera) {
						// 拖动相机
						float deltaX = wx - cameraDragStartX;
						float deltaY = wy - cameraDragStartY;

						camera.position.x = cameraStartX - deltaX;
						camera.position.y = cameraStartY + deltaY; // Y轴反转
						camera.update();
						needsRender = true;
						return true;
					}
				} else if (action == 2) { // TOUCH_UP
					if (selectedObject != null) {
						selectedObject.endDrag();
					}
					isDraggingCamera = false;
				}

				return false;
			}

			private GameObject getObjectAt(float x, float y) {
				// 从后往前检查（最后绘制的在最上面）
				for (int i = objects.size - 1; i >= 0; i--) {
					GameObject obj = objects.get(i);
					if (obj.contains(x, y)) {
						return obj;
					}
				}
				return null;
			}

			public void addTestObjects() {
				// 添加一些测试对象
				for (int i = 0; i < 5; i++) {
					float x = 100 + i * 120;
					float y = 100 + i * 80;
					Color color = new Color(
						MathUtils.random(0.5f, 1f),
						MathUtils.random(0.5f, 1f),
						MathUtils.random(0.5f, 1f),
						1
					);
					objects.add(new GameObject("Obj" + i, x, y, 60, 60, color));
				}
			}

			class GameObject {
				String name;
				float x, y, width, height;
				Color color;
				boolean isDragging = false;
				float dragOffsetX, dragOffsetY;
				Texture texture;

				public GameObject(String name, float x, float y, float width, float height, Color color) {
					this.name = name;
					this.x = x;
					this.y = y;
					this.width = width;
					this.height = height;
					this.color = color;
					this.texture = createTexture(color);
				}

				public void render(SpriteBatch batch) {
					batch.setColor(color);
					batch.draw(texture, x - width/2, y - height/2, width, height);
				}

				public void renderSelection(ShapeRenderer shapes) {
					shapes.setProjectionMatrix(camera.combined);
					shapes.begin(ShapeRenderer.ShapeType.Line);
					shapes.setColor(1, 1, 0, 1);
					shapes.rect(x - width/2 - 2, y - height/2 - 2, width + 4, height + 4);
					shapes.end();
				}

				public boolean contains(float px, float py) {
					return px >= x - width/2 && px <= x + width/2 &&
						py >= y - height/2 && py <= y + height/2;
				}

				public void startDrag(float dragX, float dragY) {
					isDragging = true;
					dragOffsetX = dragX - x;
					dragOffsetY = dragY - y;
				}

				public void updateDrag(float dragX, float dragY) {
					if (isDragging) {
						x = dragX - dragOffsetX;
						y = dragY - dragOffsetY;
					}
				}

				public void endDrag() {
					isDragging = false;
				}

				public boolean isDragging() {
					return isDragging;
				}

				private Texture createTexture(Color color) {
					Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
					pixmap.setColor(color);
					pixmap.fill();
					Texture texture = new Texture(pixmap);
					pixmap.dispose();
					return texture;
				}
			}
		}

		// ==================== 虚拟摇杆 ====================
		public class Joystick {
			private float centerX, centerY;
			private float radius;
			private float knobX, knobY;
			private float knobRadius;
			private boolean isActive = false;

			private Color bgColor = new Color(0.3f, 0.3f, 0.3f, 0.7f);
			private Color knobColor = new Color(0.2f, 0.6f, 1f, 0.9f);

			public Joystick(float centerX, float centerY, float radius) {
				this.centerX = centerX;
				this.centerY = centerY;
				this.radius = radius;
				this.knobRadius = radius * 0.4f;
				reset();
			}

			public void reset() {
				knobX = centerX;
				knobY = centerY;
				isActive = false;
			}

			public Vector2 getInput() {
				if (!isActive) return new Vector2(0, 0);

				float dx = (knobX - centerX) / radius;
				float dy = (knobY - centerY) / radius;

				// 限制在单位圆内
				float length = (float)Math.sqrt(dx * dx + dy * dy);
				if (length > 1.0f) {
					dx /= length;
					dy /= length;
				}

				return new Vector2(dx, dy);
			}

			public boolean handleInput(float x, float y, int action) {
				if (action == 0) { // TOUCH_DOWN
					// 检查是否在摇杆范围内
					float distance = Vector2.dst(x, y, centerX, centerY);
					if (distance <= radius * 1.5f) {
						isActive = true;
						updateKnob(x, y);
						return true;
					}
				} else if (action == 1) { // TOUCH_DRAGGED
					if (isActive) {
						updateKnob(x, y);
						return true;
					}
				} else if (action == 2) { // TOUCH_UP
					if (isActive) {
						reset();
						return true;
					}
				}
				return false;
			}

			private void updateKnob(float x, float y) {
				float dx = x - centerX;
				float dy = y - centerY;
				float distance = (float)Math.sqrt(dx * dx + dy * dy);

				if (distance > radius) {
					dx = dx / distance * radius;
					dy = dy / distance * radius;
					knobX = centerX + dx;
					knobY = centerY + dy;
				} else {
					knobX = x;
					knobY = y;
				}
			}

			public void render(ShapeRenderer shapes, float offsetX, float offsetY) {
				shapes.begin(ShapeRenderer.ShapeType.Filled);

				// 摇杆背景
				shapes.setColor(bgColor);
				shapes.circle(offsetX + centerX, offsetY + centerY, radius);

				// 摇杆
				shapes.setColor(knobColor);
				shapes.circle(offsetX + knobX, offsetY + knobY, knobRadius);

				shapes.end();

				// 绘制方向指示线
				if (isActive) {
					shapes.begin(ShapeRenderer.ShapeType.Line);
					shapes.setColor(1, 1, 1, 0.5f);
					shapes.line(offsetX + centerX, offsetY + centerY, 
								offsetX + knobX, offsetY + knobY);
					shapes.end();
				}
			}
		}

		// ==================== 输入管理器 ====================
		public class EditorInputManager extends InputAdapter {
			private Array<VisWindow> windows;
			private VisWindow focusedWindow = null;
			private int activePointer = -1;

			// 输入动作常量
			private static final int ACTION_DOWN = 0;
			private static final int ACTION_DRAGGED = 1;
			private static final int ACTION_UP = 2;

			public EditorInputManager(Array<VisWindow> windows) {
				this.windows = windows;
			}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				// 转换Y坐标（LibGDX原点在左上，OpenGL在左下）
				screenY = Gdx.graphics.getHeight() - screenY;

				// 从后往前检查窗口（最后添加的窗口在最上面）
				for (int i = windows.size - 1; i >= 0; i--) {
					VisWindow window = windows.get(i);

					if (window.contains(screenX, screenY)) {
						focusedWindow = window;
						activePointer = pointer;

						// 检查是否在窗口操作区域（边框、标题栏）
						int area = window.checkArea(screenX, screenY);
						if (area != -1) {
							// 开始窗口操作
							window.startWindowOperation(screenX, screenY, area);
							return true;
						} else {
							// 处理窗口内容输入
							return window.handleInput(screenX, screenY, ACTION_DOWN, button);
						}
					}
				}

				return false;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) {
				if (pointer != activePointer || focusedWindow == null) return false;

				screenY = Gdx.graphics.getHeight() - screenY;

				// 更新窗口操作（拖动或调整大小）
				if (focusedWindow.isDragging || focusedWindow.isResizing) {
					focusedWindow.updateWindowOperation(screenX, screenY);
					return true;
				}

				// 处理窗口内容拖动
				return focusedWindow.handleInput(screenX, screenY, ACTION_DRAGGED, 0);
			}

			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {
				if (pointer == activePointer && focusedWindow != null) {
					screenY = Gdx.graphics.getHeight() - screenY;

					// 结束窗口操作
					focusedWindow.endWindowOperation();

					// 处理窗口内容抬起
					focusedWindow.handleInput(screenX, screenY, ACTION_UP, button);

					focusedWindow = null;
					activePointer = -1;
					return true;
				}
				return false;
			}
		}
	}
