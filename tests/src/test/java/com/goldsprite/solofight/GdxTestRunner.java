package com.goldsprite.solofight;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import static org.mockito.Mockito.mock;

public class GdxTestRunner extends BlockJUnit4ClassRunner implements ApplicationListener {

	private static boolean initialized = false;

	public GdxTestRunner(Class<?> klass) throws InitializationError {
		super(klass);
		synchronized (GdxTestRunner.class) {
			if (!initialized) {
				// 1. 配置无头模式
				HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();

				// 2. 启动 HeadlessApp (它会初始化 Gdx.app, Gdx.files 等)
				new HeadlessApplication(this, config);

				// 3. 【关键】模拟 OpenGL 上下文
				// 因为没有显卡，任何 Gdx.gl 的调用都会空指针。
				// 我们用 Mockito 创建一个“假”的 GL20 对象，骗过所有绘图代码。
				Gdx.gl = mock(GL20.class);
				Gdx.gl20 = Gdx.gl;

				initialized = true;
			}
		}
	}

	@Override
	public void create() {}
	@Override
	public void resize(int width, int height) {}
	@Override
	public void render() {} // 这里是空跑，你可以手动触发 update
	@Override
	public void pause() {}
	@Override
	public void resume() {}
	@Override
	public void dispose() {}

	@Override
	public void run(RunNotifier notifier) {
		super.run(notifier);
	}
}
