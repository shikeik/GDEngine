package com.goldsprite.solofight.screens.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
import com.goldsprite.solofight.core.game.Platform; // [新增]
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

import java.util.ArrayList;
import java.util.List;

public class GameScreen extends ExampleGScreen {

	private SpriteBatch batch;
	private NeonBatch neonBatch;
	private ParallaxBackground parallaxBG;

	private Stage uiStage;
	private VirtualJoystick joystick;
	private GestureProcessor gestureProcessor;
	private CommandHistoryUI historyUI;
	private ToastUI toastUI;
	private H5SkewBar barP1, barP2;
	private HelpWindow helpWindow;
	private GameOverUI gameOverUI;

	private Fighter p1, p2;
	private float shake = 0;

	// [新增] 平台列表
	private List<Platform> platforms = new ArrayList<>();

	// 结算逻辑变量
	private boolean gameEnded = false;
	private float globalTimeScale = 1.0f;

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

		// [新增] 初始化平台 (H5 数据)
		// { x: 200, y: 350, w: 200, h: 20 }
		// { x: 600, y: 200, w: 200, h: 20 }
		// { x: 450, y: 300, w: 100, h: 20 }
		platforms.add(new Platform(200, 350, 200, 20));
		platforms.add(new Platform(600, 200, 200, 20));
		platforms.add(new Platform(450, 300, 100, 20));

		p1 = new Fighter(200, Color.CYAN, false);
		p2 = new Fighter(800, Color.valueOf("ff0055"), true);
		p1.setEnemy(p2); p2.setEnemy(p1);

		FloatingTextManager.init();
		FloatingTextManager.getInstance().resetCombo();
		EffectManager.inst().clear();
		ParticleManager.inst().clear();

		initUI();
		initInputLogic();
	}

	private void initUI() {
		uiStage = new Stage(getViewport());

		// --- Root Table 布局管理 (修复 UI 错位) ---
		Table root = new Table();
		root.setFillParent(true);
		uiStage.addActor(root);

		// 1. Top Layer (Buttons & Bars)
		Table topTable = new Table();

		// Left: Help & Lang Buttons
		VisTextButton btnHelp = new VisTextButton("?");
		btnHelp.addListener(new ClickListener() {
			public void clicked(InputEvent e, float x, float y) {
				helpWindow.setVisible(true); helpWindow.toFront(); helpWindow.refreshLang();
			}
		});
		VisTextButton btnLang = new VisTextButton("CN");
		btnLang.addListener(new ClickListener() {
			public void clicked(InputEvent e, float x, float y) {
				TextDB.toggle(); btnLang.setText(TextDB.getLangName()); helpWindow.refreshLang();
			}
		});
		topTable.add(btnHelp).size(40).padRight(10);
		topTable.add(btnLang).size(50, 40).padRight(20);

		// Middle: Spacer
		topTable.add().expandX();

		root.add(topTable).growX().top().pad(10).row();

		// 2. Center Layer (History & Toast)
		Table centerTable = new Table();

		// Left: History UI (Align Left, Top)
		historyUI = new CommandHistoryUI();
		centerTable.add(historyUI).left().top().padLeft(10).padTop(10);

		// Center: Toast (Expand X to center it)
		toastUI = new ToastUI();
		centerTable.add(toastUI).expandX().center().top().padTop(50);

		// Right: Empty
		centerTable.add().expandX();

		root.add(centerTable).grow().row();

		// 3. Absolute/Overlay Actors (Joystick, Bars, Modals)
		// 摇杆如果不放 Table 里更容易控制位置
		joystick = new VirtualJoystick();
		joystick.setPosition(50, 50);
		uiStage.addActor(joystick);

		// 血条 (手动定位，因为要倾斜特效，放 Table 里容易被裁切)
		H5SkewBar.BarStyle s1 = new H5SkewBar.BarStyle();
		s1.gradientStart = Color.valueOf("00eaff"); s1.gradientEnd = Color.valueOf("0088aa"); s1.skewDeg = -20f;
		barP1 = new H5SkewBar(0, 500, s1);
		barP1.setSize(350, 25);
		uiStage.addActor(barP1); // 在 resize 中定位

		H5SkewBar.BarStyle s2 = new H5SkewBar.BarStyle();
		s2.gradientStart = Color.valueOf("ff0055"); s2.gradientEnd = Color.valueOf("aa0033"); s2.skewDeg = 20f;
		barP2 = new H5SkewBar(0, 500, s2);
		barP2.setSize(350, 25);
		barP2.setFillFromRight(true);
		uiStage.addActor(barP2); // 在 resize 中定位

		helpWindow = new HelpWindow();
		helpWindow.setVisible(false);
		uiStage.addActor(helpWindow);

		gameOverUI = new GameOverUI();
		uiStage.addActor(gameOverUI);
	}

	private void initInputLogic() {
		gestureProcessor = new GestureProcessor(getViewport());
		getImp().addProcessor(new KeyboardProcessor()); // Key First
		getImp().addProcessor(uiStage); // UI Second
		getImp().addProcessor(gestureProcessor); // Gesture Last

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
			uiStage.act(delta); uiStage.draw(); return;
		}

		// [修复透明度 1] 帧首重置
		batch.setColor(Color.WHITE);

		float dt = delta * globalTimeScale;
		gestureProcessor.update(dt);

		boolean ultActive = p1.isUltActive || p2.isUltActive;
		float ultScale = ultActive ? 0.05f : 1.0f;
		float finalDt = dt * ultScale;

		// [修改] 传递 platforms
		if (p1.isUltActive) { p1.update(dt, platforms); p2.update(finalDt, platforms); }
		else if (p2.isUltActive) { p2.update(dt, platforms); p1.update(finalDt, platforms); }
		else { p1.update(finalDt, platforms); p2.update(finalDt, platforms); }

		EffectManager.inst().update(dt);
		ParticleManager.inst().update(finalDt);
		FloatingTextManager.getInstance().update(dt);
		checkGameResult();

		if ((p1.state.equals("ult_slash") && p1.ultTimer % 4 == 0) || (p1.state.equals("ult_end") && p1.ultTimer == 30)) {
			shake = p1.state.equals("ult_end") ? 20 : 5;
		}
		if (shake > 0) shake *= 0.9f;

		barP1.setValue(p1.hp); barP1.setPercent(p1.hp/p1.maxHp);
		barP2.setValue(p2.hp); barP2.setPercent(p2.hp/p2.maxHp);

		// Camera logic... (保持不变)
		float camX = getWorldCamera().position.x;
		float targetX = (p1.x + p2.x) / 2 + 20;
		float targetZoom = 1.0f;
		if (p1.isUltActive && p1.state.equals("ult_slash")) { targetX = p2.x + p2.w/2; targetZoom = 0.7f; }

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

		// [新增] 绘制平台
		for (Platform p : platforms) {
			// 样式：实心填充 + 描边 (Neon Style)
			neonBatch.drawRect(p.x + p.w/2, p.y + p.h/2, p.w, p.h, 0, 0, Color.valueOf("333333"), true);
			neonBatch.drawRect(p.x + p.w/2, p.y + p.h/2, p.w, p.h, 0, 2f, Color.valueOf("00eaff"), false);
		}

		if (ultActive) {
			OrthographicCamera cam = getWorldCamera();
			neonBatch.drawRect(cam.position.x, cam.position.y, 10000, 10000, 0, 0, new Color(0,0,0,0.8f), true);
		}

		neonBatch.drawLine(-500, 0, 1500, 0, 2, Color.GRAY);
		p1.drawBody(neonBatch); p2.drawBody(neonBatch);
		EffectManager.inst().draw(neonBatch);
		ParticleManager.inst().draw(neonBatch);
		p1.drawEffects(neonBatch); p2.drawEffects(neonBatch);
		FloatingTextManager.getInstance().renderWorld(batch);

		neonBatch.end();
		getWorldCamera().position.sub(shakeX, shakeY, 0);

		// --- Draw UI ---
		// [修复透明度 2] 强制重置 Batch 颜色，防止 FloatingTextManager 或 NeonBatch 污染 UI
		batch.setColor(Color.WHITE);

		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		float splitX = getViewport().getWorldWidth() * 0.5f;
		neonBatch.drawLine(splitX, 0, splitX, getViewport().getWorldHeight(), 1, new Color(1,1,1,0.1f));
		for (com.goldsprite.solofight.core.input.GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
		neonBatch.end();

		batch.begin();
		FloatingTextManager.getInstance().renderUI(batch, getViewport().getWorldWidth(), getViewport().getWorldHeight());
		batch.end();

		uiStage.act(delta);
		uiStage.draw();

		DebugUI.info("P1 MP: %.0f | Ult: %b", p1.mp, p1.isUltActive);
	}

	// ... (checkGameResult, restartGame 等保持不变) ...
	private void checkGameResult() {
		if (gameEnded) return;
		boolean p1Dead = p1.hp <= 0;
		boolean p2Dead = p2.hp <= 0;
		if (p1Dead || p2Dead) {
			gameEnded = true;
			globalTimeScale = 0.1f;
			com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
				@Override
				public void run() {
					globalTimeScale = 0f;
					boolean isWin = p2Dead;
					gameOverUI.show(isWin, () -> restartGame());
				}
			}, 2.5f);
		}
	}

	private void restartGame() {
		gameEnded = false; globalTimeScale = 1.0f;
		p1.hp = p1.maxHp; p1.mp = 100; p1.state = "idle"; p1.x = 200; p1.y = 0; p1.vx = 0; p1.vy = 0; p1.isUltActive = false;
		p2.hp = p2.maxHp; p2.mp = 0; p2.state = "idle"; p2.x = 800; p2.y = 0; p2.vx = 0; p2.vy = 0; p2.isUltActive = false;
		ParticleManager.inst().clear(); EffectManager.inst().clear();
		getWorldCamera().position.set(0, 0, 0); getWorldCamera().zoom = 1.0f;
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		// 手动定位绝对布局的组件
		if (barP2 != null) barP2.setPosition(getViewport().getWorldWidth() - 370, getViewport().getWorldHeight() - 50);
		if (barP1 != null) barP1.setPosition(20, getViewport().getWorldHeight() - 50);
		if (helpWindow != null) helpWindow.centerWindow();
		// Toast 不需要手动定位了，Table 会管理
	}

	// ... (dispose) ...
	@Override
	public void dispose() {
		super.dispose();
		if(uiStage!=null)uiStage.dispose(); if(batch!=null)batch.dispose();
		InputContext.inst().commandListener = null; ComboEngine.inst().onCommandExecuted = null;
	}
}
