package com.goldsprite.gdengine.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;

import java.util.*;
import java.util.function.Consumer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * 使用：
 * * 创建:
 * * * 实例化: ScreenManager.getInstance().addScreen(new YourScreen()).setLaunchScreen(YourScreen.class);
 * * * 渲染: ScreenManager.getInstance().render();
 * * * 重大小: ScreenManager.getInstance().resize(width, height);
 * * * 回收: ScreenManager.getInstance().dispose();
 * * 切换屏幕:
 * * * 首先需要已添加目标屏幕，然后在屏幕内部使用getScreenManager().setCurScreen(TargetScreen.class)
 * * 返回上次屏幕:
 * * * 在屏幕历史堆栈有屏幕时getScreenManager().popLastScreen()来返回上个屏幕
 */
public class ScreenManager implements Disposable {

	// 1. 定义屏幕方向枚举
	public enum Orientation {
		Portrait, // 竖屏
		Landscape // 横屏
		}

	// 2. 定义回调接口 (底层不依赖 Android/Lwjgl)
	public static Consumer<Orientation> orientationChanger;
	public static List<Runnable> exitGame = new ArrayList<>();//声明退出游戏事件回调，需要在各平台自身实现
	private static ScreenManager instance;
	private final Map<Class<?>, GScreen> screens = new HashMap<>();
	private InputMultiplexer imp;
	//屏幕历史堆栈
	private final Stack<GScreen> screenHistory = new Stack<>();
	private GScreen curScreen;
	private GScreen launchScreen;
	private boolean popping;//用于标记是否为弹出历史屏幕状态
	private Viewport viewport;//统一视口

	public ScreenManager() {
		this(new InputMultiplexer());
	}

	public ScreenManager(InputMultiplexer imp) {
		this(new ScreenViewport(), imp);
	}
	public ScreenManager(Viewport viewport) {
		this(viewport, new InputMultiplexer());
	}

	public ScreenManager(Viewport viewport, InputMultiplexer imp) {
		instance = this;
		this.viewport = viewport;
		//关键 这里需要手动update才能触发worldWidth/Height初始化赋值，之后GScreen才能拿到视口数据
		viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		initInputHandler(imp);
	}


	/**
	 * 单例屏幕管理器
	 */
	public static synchronized ScreenManager getInstance() {
		return instance;
	}

	// 3. 增加切换方法
	public void setOrientation(Orientation orientation) {
		if (orientationChanger != null) {
			orientationChanger.accept(orientation);
			//curScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
		// 注意：实际视口 resize 会在 resize() 回调中被触发，这里不需要手动改 Viewport
	}

	private void initInputHandler(InputMultiplexer imp) {
		this.imp = imp;

		//设置到gdx输入管线
		if (Gdx.input.getInputProcessor() == null) Gdx.input.setInputProcessor(imp);
		//创建默认处理器
		InputAdapter defaultHandler = new InputAdapter() {
			boolean isFullscreen;

			public boolean keyDown(int keyCode) {
				Application.ApplicationType userType = Gdx.app.getType();

				//从堆栈弹出并返回上个屏幕
				if (keyCode == Input.Keys.BACK || keyCode == Input.Keys.ESCAPE) {
					if (!popLastScreen()) {
						//如果已在最顶层则退出游戏
						if (exitGame != null && !exitGame.isEmpty()) exitGame.forEach(r -> r.run());
					}
					return true;
				}
				else if(keyCode == Input.Keys.F11) {
					isFullscreen = !isFullscreen;
					PlatformImpl.fullScreenEvent.accept(isFullscreen);
//					if(Application.ApplicationType.Desktop.equals(userType))
//						ScreenManager.orientationChanger.accept(isFullscreen ? ScreenManager.Orientation.LANDSCAPE : ScreenManager.Orientation.PORTRAIT);
//					Debug.logT("FullscreenManager", "切换到%s模式", isFullscreen ? "全屏" : "窗口");
				}
				return false;
			}
		};
		imp.addProcessor(defaultHandler);
	}

	public InputMultiplexer getImp() {
		return imp;
	}

	public Viewport getViewport() {
		return viewport;
	}

	public ScreenManager setViewport(Viewport viewport) {
		this.viewport = viewport;
		return this;
	}

	/**
	 * 渲染(已初始化的)当前屏幕
	 */
	public void render() {
		if (!curScreen.isInitialized()) return;

		float delta = Gdx.graphics.getDeltaTime();
		curScreen.getUIViewport().apply();
		curScreen.render(delta);
	}

	/**
	 * 释放资源
	 */
	@Override
	public void dispose() {
		for (GScreen screen : screens.values()) {
			if (screen.isInitialized())
				screen.dispose();
		}
	}

	public void resize(int width, int height) {
		if(curScreen != null) curScreen.resize(width, height);
	}

	/**
	 * 添加并配置游戏屏幕
	 */
	public ScreenManager addScreen(GScreen screen) {
		Class<?> key = screen.getClass();
		if (!screens.containsKey(key)) {
			screens.put(key, screen);
			screen.setScreenManager(this);
			screen.setImp(new InputMultiplexer());
		}
		return this;
	}

	private void removeScreen(Class<? extends GScreen> key) {
		screens.remove(key);
	}

	/**
	 * 通过键获取游戏屏幕
	 */
	public GScreen getScreen(Class<?> key) {
		if (!screens.containsKey(key)) throw new RuntimeException("未找到此屏幕." + key.getSimpleName());
		return screens.get(key);
	}

	public boolean existsScreen(Class<?> key) {
		return screens.containsKey(key);
	}

	/**
	 * 获取当前屏幕
	 */
	public GScreen getCurScreen() {
		return curScreen;
	}

	public void setCurScreen(Class<? extends GScreen> key) {
		setCurScreen(key, false);
	}

	public void setCurScreen(GScreen screen) {
		// [修复] 自动依赖注入
		// 如果是临时 new 出来的屏幕，还没有绑定 Manager 或 Input，这里自动补全
		if (screen.getScreenManager() == null) {
			screen.setScreenManager(this);
		}
		if (screen.getImp() == null) {
			screen.setImp(new InputMultiplexer());
		}
		//如果屏幕未准备则初始化屏幕
		screen.initialize();
		//隐藏上个屏幕并切换到目标屏幕
		if (this.curScreen != null) {
			this.curScreen.hide();
			//如果为非popping状态，将旧屏幕推入历史堆栈以记录
			if (!popping) screenHistory.push(curScreen);
		}
		this.curScreen = screen;
		this.curScreen.show();
	}

	public void turnNewScreen(Class<? extends GScreen> key) {
		if(existsScreen(key)) {
			getScreen(key).dispose();
			removeScreen(key);
		}
		setCurScreen(key, true);
	}

	public void setCurScreen(Class<? extends GScreen> key, boolean autoCreate) {
		//自动加入管理屏幕中
		if (autoCreate && !existsScreen(key)) {
			try {
				addScreen(key.getConstructor().newInstance());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		setCurScreen(getScreen(key));
	}

	//回到上个屏幕
	public boolean popLastScreen() {
		if (screenHistory.isEmpty()) return false;
		GScreen lastScreen = screenHistory.pop();
		popping = true;
		setCurScreen(lastScreen);
		popping = false;
		return true;
	}

	public GScreen getLaunchScreen() {
		return launchScreen;
	}

	public void setLaunchScreen(Class<? extends GScreen> key) {
		setLaunchScreen(getScreen(key));
	}

	public void setLaunchScreen(GScreen screen) {
		launchScreen = screen;
		if (!screen.isInitialized())
			setCurScreen(launchScreen);
	}

	public void enableInput(InputMultiplexer screenImp) {
		imp.addProcessor(screenImp);
	}

	public void disableInput(InputMultiplexer screenImp) {
		imp.removeProcessor(screenImp);
	}

}
