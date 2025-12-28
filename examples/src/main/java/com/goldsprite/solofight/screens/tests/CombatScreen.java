package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.game.Fighter;
import com.goldsprite.solofight.core.input.*;
import com.goldsprite.solofight.core.ui.H5SkewBar;

public class CombatScreen extends ExampleGScreen {

	private SpriteBatch batch;
	private NeonBatch neonBatch;
	private Stage uiStage;

	private VirtualJoystick joystick;
	private GestureProcessor gestureProcessor;
	private CommandHistoryUI historyUI;
	private ToastUI toastUI;
	private H5SkewBar barP1, barP2;

	private Fighter p1;
	private Fighter p2;

	// [新增] 屏幕震动变量
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
		super.initViewportAndCamera();
		float scl = 1.4f;
		if (uiViewport instanceof ExtendViewport) {
			((ExtendViewport) uiViewport).setMinWorldWidth(960 * scl);
			((ExtendViewport) uiViewport).setMinWorldHeight(540 * scl);
		} else {
			uiViewport = new ExtendViewport(960 * scl, 540 * scl, getCamera());
		}
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);

		p1 = new Fighter(200, Color.CYAN, false);
		p2 = new Fighter(800, Color.valueOf("ff0055"), true);
		p1.setEnemy(p2); p2.setEnemy(p1);

		initUI();

		InputContext.inst().commandListener = (cmdId, src) -> ComboEngine.inst().push(cmdId, src);

		ComboEngine.inst().onCommandExecuted = (finalCmd, src) -> {
			String icon = "?";
			String type = "raw";
			for (InputDef.Command cmd : InputDef.COMMANDS) {
				if (cmd.id.equals(finalCmd)) { icon = cmd.icon; type = "move"; break; }
			}
			if (finalCmd.equals("FLASH SLASH")) { icon = "⚡"; type = "move"; }
			historyUI.addHistory(finalCmd, src, type, icon);
			if (type.equals("move")) toastUI.show(finalCmd.replace("CMD_", ""));
			p1.handleCommand(finalCmd);
		};
	}

	private void initUI() {
		uiStage = new Stage(getViewport());
		joystick = new VirtualJoystick(); joystick.setPosition(50, 50); uiStage.addActor(joystick);
		historyUI = new CommandHistoryUI(); historyUI.setPosition(10, getViewport().getWorldHeight() * 0.4f); uiStage.addActor(historyUI);
		toastUI = new ToastUI(); toastUI.setPosition(getViewport().getWorldWidth() / 2 - 50, 100); uiStage.addActor(toastUI);

		H5SkewBar.BarStyle style1 = new H5SkewBar.BarStyle();
		style1.gradientStart = Color.valueOf("00eaff"); style1.gradientEnd = Color.valueOf("0088aa"); style1.skewDeg = -20f;
		barP1 = new H5SkewBar(0, 500, style1); barP1.setSize(350, 25); barP1.setPosition(20, getViewport().getWorldHeight() - 50); uiStage.addActor(barP1);

		H5SkewBar.BarStyle style2 = new H5SkewBar.BarStyle();
		style2.gradientStart = Color.valueOf("ff0055"); style2.gradientEnd = Color.valueOf("aa0033"); style2.skewDeg = 20f;
		barP2 = new H5SkewBar(0, 500, style2); barP2.setSize(350, 25); barP2.setFillFromRight(true);
		barP2.setPosition(getViewport().getWorldWidth() - 350 - 20, getViewport().getWorldHeight() - 50); uiStage.addActor(barP2);

		gestureProcessor = new GestureProcessor(getViewport());

		getImp().addProcessor(uiStage);
		getImp().addProcessor(gestureProcessor);
		getImp().addProcessor(new KeyboardProcessor());
	}

	@Override
	public void render0(float delta) {
		gestureProcessor.update(delta);

		// [新增] 全局时缓逻辑
		// 如果有人在放 H5 大招，世界变慢
		boolean ultActive = p1.isUltActive || p2.isUltActive;
		// H5: cast=0.05, slash=0.05? Actually H5 sets timeScale globally.
		// Cast: 0.05. Slash: 0.1? Let's use 0.1 for visual clarity in Java.
		// 终结技最后一帧恢复 1.0 (由 Fighter 内部控制 isUltActive = false)
		float timeScale = ultActive ? 0.05f : 1.0f;

		// 玩家 1 如果在放招，他自己不吃时缓；敌人吃时缓
		// 注意：Fighter.update 内部使用 60*delta。
		// 我们传入的 delta 应该是被 timeScale 缩放过的。

		if (p1.isUltActive) {
			p1.update(delta); // 自己正常更新 (或者按逻辑慢动作? H5里Ult也是按帧走的)
			// H5: ult update runs every frame, logic depends on internal timer increment.
			// In Fighter.updateUltLogic: ultTimer += 1.0f * (60 * delta).
			// If we pass real delta, ult logic runs realtime.
			p2.update(delta * timeScale); // 敌人变慢
		} else if (p2.isUltActive) {
			p2.update(delta);
			p1.update(delta * timeScale);
		} else {
			p1.update(delta);
			p2.update(delta);
		}

		// [新增] 大招震动检测 (Fighter 状态检测)
		if ((p1.state.equals("ult_slash") && p1.ultTimer % 4 == 0) || (p1.state.equals("ult_end") && p1.ultTimer == 30)) {
			shake = p1.state.equals("ult_end") ? 20 : 5;
		}
		// Apply Shake decay
		if (shake > 0) shake *= 0.9f;

		barP1.setValue(p1.hp); barP1.setPercent(p1.hp/p1.maxHp); // setValue update
		barP2.setValue(p2.hp); barP2.setPercent(p2.hp/p2.maxHp);

		// Camera logic
		float camX = getWorldCamera().position.x;
		float camY = getWorldCamera().position.y;
		float targetX = (p1.x + p2.x) / 2 + 20;

		// [新增] 大招特写
		float targetZoom = 1.0f;
		if (p1.isUltActive) {
			// 聚焦受害者
			if (p1.state.equals("ult_slash")) {
				targetX = p2.x + p2.w/2;
				targetZoom = 0.7f;
			}
		}

		getWorldCamera().position.x += (targetX - camX) * 5 * delta;
		getWorldCamera().zoom += (targetZoom - getWorldCamera().zoom) * 5 * delta;

		// Shake Apply
		float shakeX = (MathUtils.random()-0.5f) * shake;
		float shakeY = (MathUtils.random()-0.5f) * shake;
		getWorldCamera().position.add(shakeX, shakeY, 0);

		// Limit
		float viewHalfW = getWorldCamera().viewportWidth / 2 * getWorldCamera().zoom;
		if (getWorldCamera().position.x < viewHalfW) getWorldCamera().position.x = viewHalfW;
		if (getWorldCamera().position.x > 1000 - viewHalfW) getWorldCamera().position.x = 1000 - viewHalfW;
		// Reset Y (shake might displace it permanently if not careful, but here we add/sub per frame? No, we need base pos)
		// Simplification: hard set Y
		getWorldCamera().position.y = getWorldCamera().viewportHeight/2 * getWorldCamera().zoom + shakeY;

		getWorldCamera().update();

		// Draw World
		batch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		// [新增] 暗场遮罩 (Dark Overlay)
		if (ultActive) {
			// 画一个巨大的黑色半透明矩形覆盖全图
			neonBatch.drawRect(-1000, -1000, 3000, 3000, 0, 0, new Color(0,0,0,0.8f), true);
		}

		neonBatch.drawLine(-500, 0, 1500, 0, 2, Color.GRAY);
		p1.draw(neonBatch);
		p2.draw(neonBatch);
		neonBatch.end();

		// Restore Camera from Shake (Optional, next frame overwrites X/Y anyway)
		getWorldCamera().position.sub(shakeX, shakeY, 0);

		// Draw UI Trails
		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		float splitX = getViewport().getWorldWidth() * 0.5f;
		neonBatch.drawLine(splitX, 0, splitX, getViewport().getWorldHeight(), 1, new Color(1,1,1,0.1f));
		for (com.goldsprite.solofight.core.input.GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
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
	}

	@Override
	public void dispose() {
		super.dispose();
		if (uiStage != null) uiStage.dispose();
		if (batch != null) batch.dispose();
		InputContext.inst().commandListener = null;
		ComboEngine.inst().onCommandExecuted = null;
	}
}
