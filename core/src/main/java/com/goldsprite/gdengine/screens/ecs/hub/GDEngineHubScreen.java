package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.ecs.hub.mvp.HubPresenter;
import com.goldsprite.gdengine.screens.ecs.hub.mvp.HubViewImpl;

/**
 * Hub 主屏幕 (MVP 组装层)
 * 职责：创建 Stage，组装 View 和 Presenter，处理生命周期。
 * 不再包含任何业务逻辑或 UI 细节。
 */
public class GDEngineHubScreen extends GScreen {

	private Stage stage;

	// MVP 组件
	private HubViewImpl view;
	private HubPresenter presenter;

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	@Override
	protected void initViewport() {
		uiViewportScale = PlatformImpl.isAndroidUser() ? 1.5f : 2.0f;
		super.initViewport();
	}

	@Override
	public void create() {
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		// --- MVP 组装 ---
		// 1. 创建视图 (View)
		view = new HubViewImpl();

		// 2. 创建主持 (Presenter) 并绑定视图
		presenter = new HubPresenter(view);

		// 3. 将视图加入舞台
		stage.addActor(view);
	}

	@Override
	public void show() {
		super.show();
		Debug.showDebugUI = false; // 进入 Hub 隐藏全局调试条

		// 检查环境配置，准备就绪后启动 Presenter
		checkEnvironment();
	}

	@Override
	public void hide() {
		super.hide();
		Debug.showDebugUI = true;
	}

	private void checkEnvironment() {
		if (Gd.engineConfig == null) {
			if (GDEngineConfig.tryLoad()) {
				Gd.engineConfig = GDEngineConfig.getInstance();
				presenter.start(); // 启动业务逻辑
			} else {
				// 需要引导初始化
				new SetupDialog(() -> {
					Gd.engineConfig = GDEngineConfig.getInstance();
					presenter.start();
				}).show(stage);
			}
		} else {
			presenter.start();
		}
	}

	@Override
	public void render0(float delta) {
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		if (stage != null) stage.dispose();
	}
}
