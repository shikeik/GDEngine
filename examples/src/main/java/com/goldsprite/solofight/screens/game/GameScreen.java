package com.goldsprite.solofight.screens.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.FloatingTextManager;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.TextDB;
import com.goldsprite.solofight.core.game.EffectManager;
import com.goldsprite.solofight.core.game.Fighter;
import com.goldsprite.solofight.core.game.ParallaxBackground;
import com.goldsprite.solofight.core.game.ParticleManager;
import com.goldsprite.solofight.core.input.ComboEngine;
import com.goldsprite.solofight.core.input.CommandHistoryUI;
import com.goldsprite.solofight.core.input.GestureProcessor;
import com.goldsprite.solofight.core.input.InputContext;
import com.goldsprite.solofight.core.input.InputDef;
import com.goldsprite.solofight.core.input.KeyboardProcessor;
import com.goldsprite.solofight.core.input.ToastUI;
import com.goldsprite.solofight.core.input.VirtualJoystick;
import com.goldsprite.solofight.core.ui.GameOverUI;
import com.goldsprite.solofight.core.ui.H5SkewBar;
import com.goldsprite.solofight.core.ui.HelpWindow;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class GameScreen extends ExampleGScreen {

	// Rendering
	private SpriteBatch batch;
	private NeonBatch neonBatch;
	private ParallaxBackground parallaxBG;

	// UI
	private Stage uiStage;
	private VirtualJoystick joystick;
	private GestureProcessor gestureProcessor;
	private CommandHistoryUI historyUI;
	private ToastUI toastUI;
	private H5SkewBar barP1, barP2;
	private HelpWindow helpWindow;
	private VisTextButton btnLang, btnHelp;

	// Entities
	private Fighter p1, p2;
	private float shake = 0;

	// [新增] 结算相关
	private GameOverUI gameOverUI;
	private boolean gameEnded = false; // 防止重复触发
	private float globalTimeScale = 1.0f; // 全局时间缩放 (用于结算慢动作)

	@Override
	public String getIntroduction() { return ""; }
	@Override
	protected void drawIntros() {}

	@Override
	public void show() { super.show(); getScreenManager().setOrientation(ScreenManager.Orientation.LANDSCAPE); }
	@Override
	public void hide() { super.hide(); getScreenManager().setOrientation(ScreenManager.Orientation.PORTRAIT); }

	@Override
	protected void initViewportAndCamera() {
		worldCamera = new OrthographicCamera();
		uiViewport = new ExtendViewport(1344, 756, new OrthographicCamera());
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);
		parallaxBG = new ParallaxBackground();

		// Init Game World
		p1 = new Fighter(200, Color.CYAN, false);
		p2 = new Fighter(800, Color.valueOf("ff0055"), true);
		p1.setEnemy(p2); p2.setEnemy(p1);

		// [修复 4] 初始化飘字
		FloatingTextManager.init();
		FloatingTextManager.resetCombo();

		// [新增] 清理粒子
		ParticleManager.inst().clear();

		// [新增] 清理特效
		EffectManager.inst().clear();

		initUI();
		initInputLogic();
	}

	private void initUI() {
		uiStage = new Stage(getViewport());

		// 1. In-Game Controls
		joystick = new VirtualJoystick();
		joystick.setPosition(50, 50);
		uiStage.addActor(joystick);

		historyUI = new CommandHistoryUI();
		historyUI.setPosition(10, getViewport().getWorldHeight() * 0.4f);
		uiStage.addActor(historyUI);

		toastUI = new ToastUI();
		toastUI.setPosition(getViewport().getWorldWidth()/2 - 50, 150);
		uiStage.addActor(toastUI);

		// 2. Bars
		initBars();

		// 3. Top Buttons (Help & Lang)
		Table topTable = new Table();
		topTable.setFillParent(true);
		topTable.top().pad(10);

		btnHelp = new VisTextButton("?");
		btnHelp.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				helpWindow.setVisible(true);
				helpWindow.toFront();
				helpWindow.refreshLang();
			}
		});

		btnLang = new VisTextButton("CN");
		btnLang.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				TextDB.toggle();
				btnLang.setText(TextDB.getLangName());
				helpWindow.refreshLang();
			}
		});

		topTable.add(btnHelp).size(40, 40).padRight(10);
		topTable.add(btnLang).size(50, 40);
		uiStage.addActor(topTable);

		// 4. Help Modal
		helpWindow = new HelpWindow();
		helpWindow.setVisible(false);
		uiStage.addActor(helpWindow);

		// 5. Game Over UI (最后添加，确保在最上层)
		gameOverUI = new GameOverUI();
		uiStage.addActor(gameOverUI);
	}

	private void initBars() {
		H5SkewBar.BarStyle s1 = new H5SkewBar.BarStyle();
		s1.gradientStart = Color.valueOf("00eaff"); s1.gradientEnd = Color.valueOf("0088aa"); s1.skewDeg = -20f;
		barP1 = new H5SkewBar(0, 500, s1);
		barP1.setSize(350, 25);
		barP1.setPosition(20, getViewport().getWorldHeight() - 50);
		uiStage.addActor(barP1);

		H5SkewBar.BarStyle s2 = new H5SkewBar.BarStyle();
		s2.gradientStart = Color.valueOf("ff0055"); s2.gradientEnd = Color.valueOf("aa0033"); s2.skewDeg = 20f;
		barP2 = new H5SkewBar(0, 500, s2);
		barP2.setSize(350, 25);
		barP2.setFillFromRight(true);
		barP2.setPosition(getViewport().getWorldWidth() - 370, getViewport().getWorldHeight() - 50);
		uiStage.addActor(barP2);
	}

	private void initInputLogic() {
		gestureProcessor = new GestureProcessor(getViewport());

		// [关键修复 1] 调整处理器顺序
		// 键盘处理器优先级最高 (防止 Stage 吞键? 其实通常 Stage 不吞 WASD)
		// 但为了保险，我们把 Keyboard 放在 Stage 后面 (LibGDX Multiplexer 0 is processed FIRST).
		// 实际上: Stage 如果 focused TextField 会吞，否则 ignore.
		// 我们的问题可能是之前的 Screen 没有重置 Processor.

		// 重新构建 Multiplexer:
		// 1. Keyboard (最优先，处理 WASD 快捷键)
		getImp().addProcessor(new KeyboardProcessor());
		// 2. UI Stage (处理按钮点击)
		getImp().addProcessor(uiStage);
		// 3. Gesture (处理 World Touch，如果 UI 没处理)
		getImp().addProcessor(gestureProcessor);

		InputContext.inst().commandListener = (cmdId, src) -> ComboEngine.inst().push(cmdId, src);

		ComboEngine.inst().onCommandExecuted = (finalCmd, src) -> {
			String icon = "?";
			String type = "raw";
			for (InputDef.Command cmd : InputDef.COMMANDS) {
				if (cmd.id.equals(finalCmd)) { icon = cmd.icon; type = "move"; break; }
			}
			if (finalCmd.equals("FLASH SLASH")) { icon = "⚡"; type = "move"; }

			String showName = TextDB.get(finalCmd);
			if (showName.equals(finalCmd)) showName = TextDB.get(finalCmd.replace("CMD_", ""));

			historyUI.addHistory(showName, src, type, icon);
			if (type.equals("move")) toastUI.show(showName);

			p1.handleCommand(finalCmd);
		};
	}

	@Override
	public void render0(float delta) {
		if (helpWindow.isVisible()) {
			uiStage.act(delta);
			uiStage.draw();
			return;
		}

		// 重置 Batch 颜色，防止 UI 透明度污染
		batch.setColor(Color.WHITE);

		float dt = delta * globalTimeScale;

		// --- Logic Updates ---
		// [修复 4] 更新飘字
		FloatingTextManager.getInstance().update(dt);

		gestureProcessor.update(dt);

		boolean ultActive = p1.isUltActive || p2.isUltActive;
		float ultScale = ultActive ? 0.05f : 1.0f;
		float finalDt = dt * ultScale;

		if (p1.isUltActive) { p1.update(dt); p2.update(finalDt); }
		else if (p2.isUltActive) { p2.update(dt); p1.update(finalDt); }
		else { p1.update(finalDt); p2.update(finalDt); }

		// [关键修复] 更新特效管理器
		// 使用 dt (受全局时缓影响，但不受大招 0.05x 极慢速影响，避免闪电定格太久)
		// 如果你希望闪电在大招期间也变慢，可以改传 finalDt
		EffectManager.inst().update(finalDt);

		ParticleManager.inst().update(finalDt); // 粒子通常随时间流逝变慢，用 finalDt 较好
		FloatingTextManager.getInstance().update(finalDt); // 你修改过的逻辑

		checkGameResult();

		if ((p1.state.equals("ult_slash") && p1.ultTimer % 4 == 0) || (p1.state.equals("ult_end") && p1.ultTimer == 30)) {
			shake = p1.state.equals("ult_end") ? 20 : 5;
		}
		if (shake > 0) shake *= 0.9f;

		barP1.setValue(p1.hp); barP1.setPercent(p1.hp/p1.maxHp);
		barP2.setValue(p2.hp); barP2.setPercent(p2.hp/p2.maxHp);

		float camX = getWorldCamera().position.x;
		float targetX = (p1.x + p2.x) / 2 + 20;
		float targetZoom = 1.0f;

		if (p1.isUltActive && p1.state.equals("ult_slash")) {
			targetX = p2.x + p2.w/2;
			targetZoom = 0.7f;
		}

		getWorldCamera().position.x += (targetX - camX) * 5 * delta;
		getWorldCamera().zoom += (targetZoom - getWorldCamera().zoom) * 5 * delta;

		float shakeX = (MathUtils.random()-0.5f) * shake;
		float shakeY = (MathUtils.random()-0.5f) * shake;
		getWorldCamera().position.add(shakeX, shakeY, 0);

		float viewHalfW = getWorldCamera().viewportWidth / 2 * getWorldCamera().zoom;
		if (getWorldCamera().position.x < -200 + viewHalfW) getWorldCamera().position.x = -200 + viewHalfW;
		if (getWorldCamera().position.x > 1200 - viewHalfW) getWorldCamera().position.x = 1200 - viewHalfW;
		getWorldCamera().position.y = getWorldCamera().viewportHeight/2 * getWorldCamera().zoom + shakeY;

		getWorldCamera().update();

		// --- Draw World ---
		batch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		parallaxBG.draw(neonBatch, getWorldCamera());

		if (ultActive) {
			OrthographicCamera cam = getWorldCamera();
			neonBatch.drawRect(cam.position.x, cam.position.y, 10000, 10000, 0, 0, new Color(0,0,0,0.8f), true);
		}

		neonBatch.drawLine(-500, 0, 1500, 0, 2, Color.GRAY);
		p1.drawBody(neonBatch);
		p2.drawBody(neonBatch);

		// 绘制特效 (残影/电光)
		EffectManager.inst().draw(neonBatch);

		ParticleManager.inst().draw(neonBatch);
		p1.drawEffects(neonBatch);
		p2.drawEffects(neonBatch);

		// 绘制世界空间飘字
		FloatingTextManager.getInstance().renderWorld(batch);

		neonBatch.end();

		getWorldCamera().position.sub(shakeX, shakeY, 0);

		// --- Draw UI ---

		// 重置 Batch 颜色，防止 UI 透明度污染
		batch.setColor(Color.WHITE);
		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		float splitX = getViewport().getWorldWidth() * 0.5f;
		neonBatch.drawLine(splitX, 0, splitX, getViewport().getWorldHeight(), 1, new Color(1,1,1,0.1f));
		for (com.goldsprite.solofight.core.input.GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
		neonBatch.end();

		// 绘制 UI 空间连击
		batch.begin();
		FloatingTextManager.getInstance().renderUI(batch, getViewport().getWorldWidth(), getViewport().getWorldHeight());
		batch.end();

		uiStage.act(delta);
		uiStage.draw();

		DebugUI.info("P1 MP: %.0f | Ult: %b", p1.mp, p1.isUltActive);
	}

	// [新增] 胜负检测与结算触发
	private void checkGameResult() {
		if (gameEnded) return;

		boolean p1Dead = p1.hp <= 0;
		boolean p2Dead = p2.hp <= 0;

		if (p1Dead || p2Dead) {
			gameEnded = true;

			// 1. 瞬间慢动作 (H5: timeScale = 0.1)
			globalTimeScale = 0.1f;

			// 2. 延迟 2.5秒 (真实时间) 后显示 UI
			Timer.schedule(new Timer.Task() {
				@Override
				public void run() {
					// 游戏完全暂停 (或者保持极慢? H5里 running=false 停止 update)
					// 这里我们设置 timeScale = 0，彻底冻结画面
					globalTimeScale = 0f;

					// 显示结算面板
					boolean isWin = p2Dead; // 敌人死 = 胜利
					gameOverUI.show(isWin, () -> restartGame());
				}
			}, 2.5f);
		}
	}

	// [新增] 重开游戏逻辑
	private void restartGame() {
		gameEnded = false;
		globalTimeScale = 1.0f; // 恢复时间

		// 重置 P1
		p1.hp = p1.maxHp;
		p1.mp = 100; // 测试方便给满蓝
		p1.state = "idle";
		p1.x = 200; p1.y = 0; p1.vx = 0; p1.vy = 0;
		p1.isUltActive = false;

		// 重置 P2
		p2.hp = p2.maxHp;
		p2.mp = 0;
		p2.state = "idle";
		p2.x = 800; p2.y = 0; p2.vx = 0; p2.vy = 0;
		p2.isUltActive = false;

		// 清理场面
		ParticleManager.inst().clear();

		// 重置相机
		getWorldCamera().position.set(0, 0, 0); // 具体位置会在 render 中被 Camera Follow 修正
		getWorldCamera().zoom = 1.0f;
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (barP2 != null) barP2.setPosition(getViewport().getWorldWidth() - 370, getViewport().getWorldHeight() - 50);
		if (barP1 != null) barP1.setPosition(20, getViewport().getWorldHeight() - 50);
		if (helpWindow != null) helpWindow.centerWindow();
	}

	@Override
	public void dispose() {
		super.dispose();
		if(uiStage!=null)uiStage.dispose(); if(batch!=null)batch.dispose();
		InputContext.inst().commandListener = null; ComboEngine.inst().onCommandExecuted = null;
	}
}
