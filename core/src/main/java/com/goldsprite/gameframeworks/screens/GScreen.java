package com.goldsprite.gameframeworks.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;


/**
 * 创建:
 * <br/>- 设置视口, 重写该方法: initViewport(){ setViewport(...) }
 * <br/>- 添加事件处理器: getImp().addInputProcessor(...)
 */
public abstract class GScreen implements IGScreen {
	private final Vector2 viewSize = new Vector2();
	private final Vector2 viewCenter = new Vector2();
	private final Vector2 worldSize = new Vector2();
	private final Vector2 worldCenter = new Vector2();
	private final Vector2 graphicSize = new Vector2();
	private final Color clearScreenColor = Color.valueOf("#BBBBBB");
	private final Color screenBackColor = Color.valueOf("#404040");
	protected ScreenManager screenManager;
	protected InputMultiplexer imp;
	protected boolean initialized = false;
	protected boolean isWorldCameraInitialized = false;
	protected boolean visible = true;
	Vector2 tmpCoord = new Vector2();
	//绘制底色背景
	private ShapeRenderer shapeRenderer;
	private boolean drawScreenBack = true;

	// [新增] UI 视口引用 (子类可覆盖)
	protected Viewport uiViewport;
	// [新增] 世界相机：用于游戏场景渲染，与 UI 分离
	protected OrthographicCamera worldCamera;
	// [新增] 世界缩放比例 (默认 1:1，数值越小世界视野越小/像素感越强)
	protected float worldScale = 1.0f;

	public GScreen() {
	}

	public GScreen(ScreenManager screenManager) {
		this.screenManager = screenManager;
	}

	private static TextureRegion createColorTextureRegion(Color color) {
		return new TextureRegion(createColorTexture(color));
	}

	private static Texture createColorTexture(Color color) {
		Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pm.setColor(color);
		pm.fill();
		Texture tex = new Texture(pm);
		pm.dispose();
		return tex;
	}

	public void create() {
	}

	@Override
	public void initialize() {
		if (initialized) return;
		init();
		initialized = true;
	}

	//初始化一些配置
	private void init() {
		shapeRenderer = new ShapeRenderer();int k;

		// [修改] 调用可重写的初始化方法，代替直接实例化
		initViewport();
		//Debug.log("3ui视口宽高: %s", getViewSize());
		initWorldCamera(worldScale);
		//Debug.log("4ui视口宽高: %s, 相机宽高: %s", getViewSize(), getWorldSize());

		create();
	}


	/**
	 * 初始化视口
	 * 子类可重写此方法以提供自定义的 Viewport 实例
	 * 默认视口分辨率为管理器分辨率
	 */
	protected void initViewport() {
		Viewport baseViewport = getScreenManager().getViewport();
		uiViewport = new ExtendViewport(baseViewport.getWorldWidth(), baseViewport.getWorldHeight());
		uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
	}

	protected void initWorldCamera(float worldScl) {
		// 创建一个新的世界相机
		worldCamera = new OrthographicCamera();
		setWorldScale(worldScl);
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	public ScreenManager getScreenManager() {
		return screenManager;
	}

	public void setScreenManager(ScreenManager screenManager) {
		this.screenManager = screenManager;
	}

	public InputMultiplexer getImp() {
		return imp;
	}

	public void setImp(InputMultiplexer imp) {
		this.imp = imp;
	}

	@Override
	public Viewport getUIViewport() {
		// [!!! 核心修复 !!!]
		// 必须返回本地的 uiViewport 字段，而不是 getScreenManager().getViewport()
		// 这样子类注入的自定义视口才能生效
		return uiViewport;
	}

	/**
	 * 获取 UI 相机 (Viewport 绑定的相机)
	 */
	public OrthographicCamera getUICamera() {
		return (OrthographicCamera) getUIViewport().getCamera();
	}

	/**
	 * [新增] 获取世界相机
	 */
	public OrthographicCamera getWorldCamera() {
		return worldCamera;
	}

	/**
	 * [新增] 设置世界缩放比例 (影响 worldCamera 的视口数值大小)
	 */
	public void setWorldScale(float scale) {
		this.worldScale = scale;
		resizeWorldCamera(true);
	}

	public Vector2 getViewSize() { return viewSize.set(getUIViewport().getWorldWidth(), getUIViewport().getWorldHeight()); }
	public Vector2 getViewCenter() { return viewCenter.set(getUIViewport().getWorldWidth() / 2, getUIViewport().getWorldHeight() / 2); }
	public Vector2 getWorldSize() { return worldSize.set(getWorldCamera().viewportWidth, getWorldCamera().viewportHeight); }
	public Vector2 getWorldCenter() { return worldSize.set(getWorldCamera().viewportWidth / 2, getWorldCamera().viewportHeight / 2); }

	public Vector2 getGraphicSize() {
		return graphicSize.set(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	// [重命名] 原 screenToWorldCoord -> screenToUICoord
	// 明确语义：这是转换到 UI Viewport 坐标系
	public Vector2 screenToUICoord(float x, float y) {
		return screenToUICoord(tmpCoord.set(x, y));
	}

	public Vector2 screenToUICoord(Vector2 screenCoord) {
		// 将反转后的屏幕坐标转换为世界坐标 (UI Viewport)
		Vector3 worldCoordinates = new Vector3(screenCoord.x, screenCoord.y, 0);
		getUIViewport().unproject(worldCoordinates);
		// 获取转换后的世界坐标
		screenCoord.x = worldCoordinates.x;
		screenCoord.y = worldCoordinates.y;
		return screenCoord;
	}

	// [新增] 真正的 screenToWorldCoord (使用 worldCamera)
	public Vector2 screenToWorldCoord(float x, float y) {
		return screenToWorldCoord(new Vector2(x, y));
	}

	public Vector2 screenToWorldCoord(Vector2 screenCoord) {
		if (worldCamera == null) return screenCoord;
		Vector3 worldCoordinates = new Vector3(screenCoord.x, screenCoord.y, 0);
		// 使用 worldCamera 进行转换，注意需要传入当前的屏幕视口参数
		worldCamera.unproject(worldCoordinates, getUIViewport().getScreenX(), getUIViewport().getScreenY(), getUIViewport().getScreenWidth(), getUIViewport().getScreenHeight());
		screenCoord.x = worldCoordinates.x;
		screenCoord.y = worldCoordinates.y;
		return screenCoord;
	}

	public Vector2 worldToScreenCoord(Vector2 worldCoord) {
		// 将反转后的屏幕坐标转换为世界坐标
		Vector3 screenCoordinates = new Vector3(worldCoord.x, worldCoord.y, 0);
		getUIViewport().project(screenCoordinates);
		// 获取转换后的世界坐标
		worldCoord.x = screenCoordinates.x;
		worldCoord.y = screenCoordinates.y;
		return worldCoord;
	}

	public Vector2 screenToViewCoord(float x, float y, Viewport viewport) {
		return screenToViewCoord(tmpCoord.set(x, y), viewport);
	}

	public Vector2 screenToViewCoord(Vector2 screenCoord, Viewport viewport) {
		// 将反转后的屏幕坐标转换为世界坐标
		Vector3 viewCoordinates = new Vector3(screenCoord.x, screenCoord.y, 0);
		viewport.unproject(viewCoordinates);
		// 获取转换后的世界坐标
		screenCoord.set(viewCoordinates.x, viewCoordinates.y);
		return screenCoord;
	}

	public Color getClearScreenColor() {
		return clearScreenColor;
	}

	public Color getScreenBackColor() {
		return screenBackColor;
	}

	protected boolean isDrawScreenBack() {
		return drawScreenBack;
	}

	protected void setDrawScreenBack(boolean drawScreenBack) {
		this.drawScreenBack = drawScreenBack;
	}

	protected void drawScreenBack() {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setProjectionMatrix(getUICamera().combined);
		shapeRenderer.setColor(screenBackColor);
		shapeRenderer.rect(0, 0, getViewSize().x, getViewSize().y);
		shapeRenderer.end();
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(getClearScreenColor());

		if (drawScreenBack)
			drawScreenBack();

		render0(delta);
	}

	public void render0(float delta) {
	}

	protected void resizeWorldCamera(boolean centerCamera) {
		// 同步 World 相机的尺寸 (保持宽高比一致，但数值受 worldScale 影响)
		// 注意：不重置位置，不居中
		if (worldCamera != null) {
			worldCamera.viewportWidth = getUIViewport().getWorldWidth() * worldScale;
			worldCamera.viewportHeight = getUIViewport().getWorldHeight() * worldScale;
			//DebugUI.log("%s %s.重置世界相机: %s,%s", isWorldCameraInitialized?"切屏":"屏幕初始化", getClass().getSimpleName(), worldCamera.viewportWidth, worldCamera.viewportHeight);
			if(centerCamera) {
				worldCamera.position.set(
					worldCamera.viewportWidth/2f,
					worldCamera.viewportHeight/2f, 0);
				//DebugUI.log("初始化相机中心位置: %s", worldCamera.position);
			}
			worldCamera.update();
		}
	}

	@Override
	public void resize(int width, int height) {
		if (getUIViewport() != null) {
			// 1. 更新 UI 视口 (自动居中 UI 相机), 左下角为0,0
			getUIViewport().update(width, height, true);

//			//更新世界相机视口参数, 仅初始化时自动居中
//			if(!isWorldCameraInitialized) { resizeWorldCamera(true); isWorldCameraInitialized = true; }
//			else resizeWorldCamera(false);
			resizeWorldCamera(true);
		}
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void show() {
		visible = true;
		getScreenManager().enableInput(getImp());
		//切换时刷新屏幕视口
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
	public void hide() {
		visible = false;
		getScreenManager().disableInput(getImp());
	}

	@Override
	public void dispose() {
	}

}
