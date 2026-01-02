package com.goldsprite.solofight.screens.editor.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.goldsprite.gameframeworks.screens.GScreen;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * 类Unity布局测试场景
 * <br/> 演示了如何将游戏世界渲染嵌入到 VisUI 的复杂布局中
 */
public class UnityLayoutTestScreen extends GScreen {

	private Stage stage;
	private ShapeRenderer shapeRenderer;

	// 用于演示的动态参数
	private float timeSeconds = 0f;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	protected void initViewport() {
		// 使用 ScreenViewport 保证 UI 像素点对点清晰，适合编辑器工具类界面
		uiViewport = new ScreenViewport();
		// 此处不需要 worldScale，因为 SceneViewWidget 会接管世界相机的缩放
		super.initViewport();
	}

	@Override
	public void create() {
		// 1. 初始化 VisUI (需确保只加载一次，实际项目中建议在 PlatformImpl 或 Game 类中加载)
		if (!VisUI.isLoaded()) VisUI.load();

		stage = new Stage(uiViewport);
		shapeRenderer = new ShapeRenderer();

		// 2. 将 Stage 输入处理器加入多路复用器
		if (getImp() != null) {
			getImp().addProcessor(stage);
		}

		// 3. 构建布局
		buildLayout();
	}

	private void buildLayout() {
		VisTable root = new VisTable();
		root.setFillParent(true);
		root.background("window-bg"); // 使用 VisUI 默认背景
		stage.addActor(root);

		// --- 构建各个面板 ---

		// 1. 左侧: Hierarchy (上) + Project (下)
		VisTable hierarchyPanel = createPanel("Hierarchy", Color.valueOf("222222"));
		VisTable projectPanel = createPanel("Project", Color.valueOf("1e1e1e"));
		VisSplitPane leftSplit = new VisSplitPane(hierarchyPanel, projectPanel, true);
		leftSplit.setSplitAmount(0.5f); // 默认 5:5 分割

		// 2. 右侧: Inspector
		VisTable inspectorPanel = createPanel("Inspector", Color.valueOf("222222"));

		// 3. 中间: Scene (上) + Console (下)
		// 核心: 创建 SceneViewWidget 来承载游戏渲染
		SceneViewWidget sceneWidget = new SceneViewWidget();
		VisTable sceneContainer = new VisTable();
		sceneContainer.add(sceneWidget).expand().fill(); // 填满容器

		VisTable consolePanel = createPanel("Console", Color.valueOf("1e1e1e"));
		VisSplitPane middleSplit = new VisSplitPane(sceneContainer, consolePanel, true);
		middleSplit.setSplitAmount(0.7f); // Scene 占 70%

		// --- 组合布局 ---

		// 组合 中间 和 右侧 (水平分割)
		VisSplitPane centerRightSplit = new VisSplitPane(middleSplit, inspectorPanel, false);
		centerRightSplit.setSplitAmount(0.75f); // Inspector 占 25%

		// 组合 左侧 和 (中间+右侧) (水平分割)
		VisSplitPane mainSplit = new VisSplitPane(leftSplit, centerRightSplit, false);
		mainSplit.setSplitAmount(0.2f); // 左侧占 20%

		root.add(mainSplit).expand().fill();
	}

	/**
	 * 创建一个简单的带标题和背景颜色的面板用于占位
	 */
	private VisTable createPanel(String title, Color bgColor) {
		VisTable table = new VisTable();
		// 绘制背景色
		table.setBackground(VisUI.getSkin().newDrawable("white", bgColor));
		table.add(new VisLabel(title)).top().left().pad(5);
		return table;
	}

	@Override
	public void render0(float delta) {
		timeSeconds += delta;

		// 刷新 UI
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		// Stage 的视口更新
		if (stage != null) {
			stage.getViewport().update(width, height, true);
		}
	}

	@Override
	public void dispose() {
		if (stage != null) stage.dispose();
		if (shapeRenderer != null) shapeRenderer.dispose();
		// VisUI.dispose(); // 根据项目生命周期管理决定是否在此释放
	}

	// =================================================================================
	// Inner Class: Scene View Widget
	// =================================================================================

	/**
	 * 自定义控件：用于在 UI 布局中渲染游戏场景
	 * <br/>原理：
	 * 1. 在 draw 中计算控件在屏幕上的实际位置和大小。
	 * 2. 使用 ScissorStack 限制绘制区域。
	 * 3. 暂停 Batch，开启 ShapeRenderer/NeonBatch 绘制游戏内容。
	 * 4. 恢复 Batch。
	 */
	private class SceneViewWidget extends WidgetGroup {
		private final Rectangle scissors = new Rectangle();
		private final Rectangle clipBounds = new Rectangle();
		private final Color gridColor = new Color(0.3f, 0.3f, 0.3f, 1f);

		@Override
		public void draw(Batch batch, float parentAlpha) {
			validate();

			// 1. 获取控件在 Stage 上的变换属性
			// 必须暂停当前的 UI Batch，因为我们要插入 ShapeRenderer 或其他 Batch
			batch.end();

			// 2. 计算剪裁区域 (Scissor)
			// 将控件的本地坐标 (0,0, width, height) 转换为屏幕坐标
			Vector2 screenPos = localToStageCoordinates(new Vector2(0, 0));
			// 注意：localToStage 得到的是 Stage 坐标，还需要通过 Stage 视口转为屏幕像素坐标
			// 这里的简易实现假设 Stage 也是 screen coordinates，如果 uiViewport 是 FitViewport 等需额外转换
			Vector2 screenCorner = stage.getViewport().project(new Vector2(screenPos.x, screenPos.y));
			// project 后的坐标原点可能变动，简单起见我们使用 ScissorStack 的辅助方法

			getStage().calculateScissors(this.clipBounds.set(0, 0, getWidth(), getHeight()), scissors);

			// 3. 开启剪裁，防止画出界
			if (ScissorStack.pushScissors(scissors)) {

				// -- 开始绘制 Scene 背景 --
				Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
				Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST); // 确保 clear 被剪裁限制 (部分驱动支持)
				// 简单的填充矩形作为背景
				shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
				shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
				shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
				shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 1f);
				shapeRenderer.rect(0, 0, getWidth(), getHeight());
				shapeRenderer.end();

				// -- 绘制游戏世界内容 --
				drawGameWorld();

				shapeRenderer.end();
				ScissorStack.popScissors();
			}

			// 4. 恢复 UI 渲染
			batch.begin();

			// 绘制 Label (Scene 标题)
			super.draw(batch, parentAlpha);
		}

		private void drawGameWorld() {
			// 更新世界相机以匹配当前小窗口的宽高比，保持物体不拉伸
			// 此处直接操作 GScreen 中的 worldCamera
			if (worldCamera != null) {
				worldCamera.viewportWidth = getWidth();
				worldCamera.viewportHeight = getHeight();
				// 保持相机在中心 (0,0)
				worldCamera.position.set(0, 0, 0);
				worldCamera.update();
			}

			shapeRenderer.setProjectionMatrix(worldCamera.combined);
			shapeRenderer.setTransformMatrix(worldCamera.view); // identity

			// A. 画网格 (Grid)
			shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(gridColor);
			float gap = 50;
			int lines = 20;
			for (int i = -lines; i <= lines; i++) {
				shapeRenderer.line(i * gap, -lines * gap, i * gap, lines * gap); // 竖线
				shapeRenderer.line(-lines * gap, i * gap, lines * gap, i * gap); // 横线
			}

			// B. 画动态图形 (模拟 TempTestScreen 的逻辑)
			float k = timeSeconds * 100; // 旋转/变化参数
			shapeRenderer.setColor(Color.RED);
			// 简单画一个旋转的矩形代替 drawRegularPolygon
			shapeRenderer.rect(-50, -50, 50, 50, 100, 100, 1, 1, k);

			// C. 画中心点
			shapeRenderer.setColor(Color.GREEN);
			shapeRenderer.circle(0, 0, 5);

			// 注意: 不要在 draw 内部调用 end(), 由调用者控制
			// 但因为我们切换了 ShapeType，所以上面内部会有 end/begin 切换，
			// 最后这里只负责逻辑结束，外层会处理 batch.begin
		}
	}
}
