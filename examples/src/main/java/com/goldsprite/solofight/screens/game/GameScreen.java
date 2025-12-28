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
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.TextDB;
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

		// [新增] 清理粒子
		ParticleManager.inst().clear();

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

		gestureProcessor.update(delta);

		boolean ultActive = p1.isUltActive || p2.isUltActive;
		float timeScale = ultActive ? 0.05f : 1.0f;

		// [新增] 更新粒子 (受时缓影响)
		ParticleManager.inst().update(delta * (ultActive ? 0.05f : 1.0f));

		if (p1.isUltActive) { p1.update(delta); p2.update(delta * timeScale); }
		else if (p2.isUltActive) { p2.update(delta); p1.update(delta * timeScale); }
		else { p1.update(delta); p2.update(delta); }

		if ((p1.state.equals("ult_slash") && p1.ultTimer % 4 == 0) || (p1.state.equals("ult_end") && p1.ultTimer == 30)) {
			shake = p1.state.equals("ult_end") ? 20 : 5;
		}
		if (shake > 0) shake *= 0.9f;

		barP1.setValue(p1.hp); barP2.setValue(p2.hp);

		float targetX = (p1.x + p2.x) / 2 + 20;
		float targetZoom = 1.0f;
		if (p1.isUltActive && p1.state.equals("ult_slash")) { targetX = p2.x + p2.w/2; targetZoom = 0.7f; }

		getWorldCamera().position.x += (targetX - getWorldCamera().position.x) * 5 * delta;
		getWorldCamera().zoom += (targetZoom - getWorldCamera().zoom) * 5 * delta;

		float sx = (MathUtils.random()-0.5f) * shake;
		float sy = (MathUtils.random()-0.5f) * shake;
		getWorldCamera().position.add(sx, sy, 0);

		float vw = getWorldCamera().viewportWidth/2 * getWorldCamera().zoom;
		if(getWorldCamera().position.x < -200 + vw) getWorldCamera().position.x = -200 + vw;
		if(getWorldCamera().position.x > 1200 - vw) getWorldCamera().position.x = 1200 - vw;
		getWorldCamera().position.y = getWorldCamera().viewportHeight/2 * getWorldCamera().zoom + sy;
		getWorldCamera().update();

		// --- Draw World ---
		batch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		parallaxBG.draw(neonBatch, getWorldCamera());

		// [关键修复 4] 遮罩覆盖
		if (ultActive) {
			OrthographicCamera cam = getWorldCamera();
			// 直接画一个极大值，无视 Zoom 计算误差
			float massiveSize = 10000f;
			neonBatch.drawRect(
				cam.position.x, cam.position.y, // 中心
				massiveSize, massiveSize,       // 尺寸
				0, 0,
				new Color(0,0,0,0.8f),
				true
			);
		}

		neonBatch.drawLine(-500, 0, 1500, 0, 2, Color.GRAY);

		p1.drawBody(neonBatch);
		p2.drawBody(neonBatch);

		// [新增] 绘制粒子 (在角色之上，特效之下? 或者最上面?)
		// H5: 粒子在角色之上。
		ParticleManager.inst().draw(neonBatch);

		p1.drawEffects(neonBatch);
		p2.drawEffects(neonBatch);

		neonBatch.end();
		getWorldCamera().position.sub(sx, sy, 0);

		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		float splitX = getViewport().getWorldWidth() * 0.5f;
		neonBatch.drawLine(splitX, 0, splitX, getViewport().getWorldHeight(), 1, new Color(1,1,1,0.1f));
		for (com.goldsprite.solofight.core.input.GestureTrail t : gestureProcessor.getTrails()) t.draw(neonBatch);
		neonBatch.end();

		uiStage.act(delta);
		uiStage.draw();

		DebugUI.info("P1 MP: %.0f | Ult: %b", p1.mp, p1.isUltActive);
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
