package com.goldsprite;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.audio.SynthAudio;
import com.goldsprite.gdengine.core.scripting.IScriptCompiler;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.log.DebugConsole;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.screens.ExampleSelectScreen;
import com.badlogic.gdx.utils.Timer;
import com.goldsprite.solofight.screens.tests.TransformTestScreen;
import com.goldsprite.gdengine.screens.ecs.EcsVisualTestScreen;
import com.goldsprite.solofight.screens.main.GameScreen;
import com.goldsprite.solofight.screens.tests.CombatScreen;
import com.goldsprite.solofight.screens.tests.TextTestScreen;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.screens.ecs.SpriteVisualScreen;
import com.goldsprite.gdengine.screens.ecs.JsonLiveEditScreen;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameScreen;
import com.goldsprite.solofight.screens.editor.SoloEditorScreen;
import com.kotcrab.vis.ui.VisUI;

public class GdxLauncher extends Game {
	private IScriptCompiler scriptCompiler; // 去掉 final，允许后期注入
	public SpriteBatch batch;
	public Debug debug;
	private Application.ApplicationType userType;

	// [新增] 标记是否已初始化完成
	private boolean isInitialized = false;

	public GdxLauncher() {
		this(null);
	}

	public GdxLauncher(IScriptCompiler scriptCompiler) {
		this.scriptCompiler = scriptCompiler;
	}

	@Override
	public void create() {
		userType = Gdx.app.getType();

		new ScreenManager();

		// 如果是 Android 端且没有编译器，说明权限还在申请中
		// 我们先暂停初始化，防止读取资源报错
		if (userType == Application.ApplicationType.Android && scriptCompiler == null) {
			return;
		}

		initGameContent();
	}

	// [新增] 真正的初始化逻辑提取出来
	private void initGameContent() {
		if (isInitialized) return;

		batch = new SpriteBatch();
		VisUIHelper.loadWithChineseFont();

		debug = Debug.getInstance();
		debug.initUI();

		SynthAudio.init();

		// 【修改点】注入原生实现和编译器
		Gd.init(Gd.Mode.RELEASE, Gdx.input, Gdx.graphics, scriptCompiler);
		Debug.logT("Engine", "[GREEN]Gd initialized. Compiler available: %b[WHITE]", (scriptCompiler != null));
		
		new GameWorld();

		ScreenManager.getInstance()
			.addScreen(new SoloEditorScreen())
			.setLaunchScreen(SoloEditorScreen.class);
		//startVisualCheckSequence();

		isInitialized = true;
	}

    private void startVisualCheckSequence() {
        Debug.logT("VisualCheck", "Starting visual check sequence...");
        
        ScreenManager sm = ScreenManager.getInstance();
        
        // Add all screens first
        sm.addScreen(new TransformTestScreen());
        sm.addScreen(new EcsVisualTestScreen());
        sm.addScreen(new GameScreen());
        sm.addScreen(new CombatScreen());
        sm.addScreen(new TextTestScreen());
        sm.addScreen(new SpriteVisualScreen());
        sm.addScreen(new JsonLiveEditScreen());
        // ExampleSelectScreen is already added in initGameContent if I keep it there, or I add it here.
        // I kept addScreen(new ExampleSelectScreen()) above, so it's fine.

        // Schedule switches
        sm.setCurScreen(TransformTestScreen.class);
        Timer.schedule(new Timer.Task() { @Override public void run() { sm.setCurScreen(EcsVisualTestScreen.class); } }, 3f);
        Timer.schedule(new Timer.Task() { @Override public void run() { sm.setCurScreen(GameScreen.class); } }, 6f);
        Timer.schedule(new Timer.Task() { @Override public void run() { sm.setCurScreen(CombatScreen.class); } }, 9f);
        Timer.schedule(new Timer.Task() { @Override public void run() { sm.setCurScreen(TextTestScreen.class); } }, 12f);
        Timer.schedule(new Timer.Task() { @Override public void run() { sm.setCurScreen(SpriteVisualScreen.class); } }, 15f);
        Timer.schedule(new Timer.Task() { @Override public void run() { sm.setCurScreen(JsonLiveEditScreen.class); } }, 18f);
        Timer.schedule(new Timer.Task() { @Override public void run() { 
            Debug.logT("VisualCheck", "Sequence completed.");
            sm.setCurScreen(ExampleSelectScreen.class); 
        } }, 21f);
    }

	// [新增] Android 端拿到权限后调用此方法注入编译器并启动
	public void onAndroidReady(IScriptCompiler compiler) {
		this.scriptCompiler = compiler;
		// 在 GL 线程执行初始化
		Gdx.app.postRunnable(this::initGameContent);
	}

	@Override
	public void render() {
		// [核心] 如果还没初始化，只清屏，不跑逻辑
		if (!isInitialized) {
			ScreenUtils.clear(0, 0, 0, 1);
			return;
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
//			Debug.showDebugUI = !Debug.showDebugUI;
			if(Debug.shortcuts) DebugConsole.autoSwitchState();
		}

		ScreenManager.getInstance().render();
		if (debug != null) debug.render();
	}

	@Override
	public void resize(int width, int height) {
		ScreenManager.getInstance().resize(width, height);
		if (debug != null) debug.resize(width, height);
	}

	@Override
	public void dispose() {
		ScreenManager.getInstance().dispose();
		if (debug != null) debug.dispose();
		batch.dispose();
		VisUI.dispose();
		SynthAudio.dispose();
	}
}
