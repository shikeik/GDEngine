package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.solofight.modules.Fighter;
import com.goldsprite.solofight.input.ComboEngine;
import com.goldsprite.solofight.ui.widget.CommandHistoryUI;
import com.goldsprite.solofight.input.GestureProcessor;
import com.goldsprite.solofight.input.InputContext;
import com.goldsprite.solofight.input.InputDef;
import com.goldsprite.solofight.input.KeyboardProcessor;
import com.goldsprite.solofight.ui.widget.ToastUI;
import com.goldsprite.solofight.ui.widget.VirtualJoystick;
import com.goldsprite.solofight.ui.widget.GestureTrail;
import com.goldsprite.gdengine.ui.widget.SkewBar;
import com.goldsprite.gdengine.neonbatch.NeonStage;

public class CombatScreen extends ExampleGScreen {

	private NeonBatch neonBatch;
	private NeonStage uiStage;

	private VirtualJoystick joystick;
	private GestureProcessor gestureProcessor;
	private CommandHistoryUI historyUI;
	private ToastUI toastUI;
	private SkewBar barP1, barP2;

	private Fighter p1;
	private Fighter p2;
	private float shake = 0;

	@Override
	public ScreenManager.Orientation getOrientation() {
	return ScreenManager.Orientation.Landscape;
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

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
		uiStage = new NeonStage(getUIViewport());
		joystick = new VirtualJoystick(); joystick.setPosition(50, 50); uiStage.addActor(joystick);
		historyUI = new CommandHistoryUI(); historyUI.setPosition(10, getUIViewport().getWorldHeight() * 0.4f); uiStage.addActor(historyUI);
		toastUI = new ToastUI(); toastUI.setPosition(getUIViewport().getWorldWidth() / 2 - 50, 100); uiStage.addActor(toastUI);

		SkewBar.BarStyle style1 = new SkewBar.BarStyle();
		style1.gradientStart = Color.valueOf("00eaff"); style1.gradientEnd = Color.valueOf("0088aa"); style1.skewDeg = -20f;
		barP1 = new SkewBar(0, 500, style1); barP1.setSize(350, 25); barP1.setPosition(20, getUIViewport().getWorldHeight() - 50); uiStage.addActor(barP1);

		SkewBar.BarStyle style2 = new SkewBar.BarStyle();
		style2.gradientStart = Color.valueOf("ff0055"); style2.gradientEnd = Color.valueOf("aa0033"); style2.skewDeg = 20f;
		barP2 = new SkewBar(0, 500, style2); barP2.setSize(350, 25); barP2.setFillFromRight(true);
		barP2.setPosition(getUIViewport().getWorldWidth() - 350 - 20, getUIViewport().getWorldHeight() - 50); uiStage.addActor(barP2);

		gestureProcessor = new GestureProcessor(getUIViewport());

		// [修正 1] 添加键盘处理器
		getImp().addProcessor(uiStage);
		getImp().addProcessor(gestureProcessor);
		getImp().addProcessor(new KeyboardProcessor());
	}

	@Override
	public void render0(float dt) {
		gestureProcessor.update(dt);

		boolean ultActive = p1.isUltActive || p2.isUltActive;
		float ultScale = ultActive ? 0.05f : 1.0f;
		float finalDt = dt * ultScale;

		// [修复] 传入 null 作为 platforms 参数
		if (p1.isUltActive) {
			p1.update(dt, null);
			p2.update(finalDt, null);
		} else if (p2.isUltActive) {
			p2.update(dt, null);
			p1.update(finalDt, null);
		} else {
			p1.update(finalDt, null);
			p2.update(finalDt, null);
		}

		if ((p1.state.equals("ult_slash") && p1.ultTimer % 4 == 0) || (p1.state.equals("ult_end") && p1.ultTimer == 30)) {
			shake = p1.state.equals("ult_end") ? 20 : 5;
		}
		if (shake > 0) shake *= 0.9f;

		barP1.setValue(p1.hp); barP1.setPercent(p1.hp/p1.maxHp);
		barP2.setValue(p2.hp); barP2.setPercent(p2.hp/p2.maxHp);

		// Camera logic
		float midX = (p1.x + p2.x) / 2 + 20;
		float camX = getWorldCamera().position.x;
		float targetX = midX;
		float targetZoom = 1.0f;

		if (p1.isUltActive && p1.state.equals("ult_slash")) {
			targetX = p2.x + p2.w/2;
			targetZoom = 0.7f;
		}

		// Camera Follow
		getWorldCamera().position.x += (targetX - camX) * 5 * dt;
		getWorldCamera().zoom += (targetZoom - getWorldCamera().zoom) * 5 * dt;

		// Shake Apply
		float shakeX = (MathUtils.random()-0.5f) * shake;
		float shakeY = (MathUtils.random()-0.5f) * shake;
		getWorldCamera().position.add(shakeX, shakeY, 0);

		// [修正 2] Camera Limit (正确计算 View Half Width)
		float viewHalfW = getWorldCamera().viewportWidth / 2 * getWorldCamera().zoom;
		// 扩展地图范围到 -500 ~ 1500 (2000宽)，防止相机在 1.4x 缩放下卡死
		float mapMinX = -500;
		float mapMaxX = 1500;

		if (getWorldCamera().position.x < mapMinX + viewHalfW) getWorldCamera().position.x = mapMinX + viewHalfW;
		if (getWorldCamera().position.x > mapMaxX - viewHalfW) getWorldCamera().position.x = mapMaxX - viewHalfW;
		// 锁定 Y 轴
		getWorldCamera().position.y = getWorldCamera().viewportHeight / 2 * getWorldCamera().zoom + shakeY;

		getWorldCamera().update();

		// Draw World
		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		// [修正] 暗场遮罩
		// 原因：NeonBatch.drawRect 的 x,y 是矩形中心点。
		// 之前错误地减去了 overlayW/H，导致中心点偏移到了左下角。
		// 现在直接使用相机位置作为中心，并加上一定的容错尺寸 (overlayW * 2) 确保覆盖。
		if (ultActive) {
			OrthographicCamera cam = getWorldCamera();
			float overlayW = cam.viewportWidth * cam.zoom;
			float overlayH = cam.viewportHeight * cam.zoom;

			// 修正：直接画在相机中心 (cam.position.x, cam.position.y)
			neonBatch.drawRect(
				cam.position.x - overlayW,      // 中心 X
				cam.position.y - overlayH,      // 中心 Y
				overlayW * 2,        // 宽 (2倍屏幕宽，防止震动漏边)
				overlayH * 2,        // 高
				0,
				0,
				new Color(0, 0, 0, 0.8f),
				true
			);
		}

		neonBatch.drawLine(-500, 0, 1500, 0, 2, Color.GRAY);

		// [修正 3] 渲染层级：先画身体
		p1.drawBody(neonBatch);
		p2.drawBody(neonBatch);

		// [修正 3] 渲染层级：后画特效 (剑气)，确保剑气覆盖在身体之上
		p1.drawEffects(neonBatch);
		p2.drawEffects(neonBatch);

		neonBatch.end();

		// Restore Camera from Shake
		getWorldCamera().position.sub(shakeX, shakeY, 0);

		// Draw UI Trails
		neonBatch.setProjectionMatrix(getUIViewport().getCamera().combined);
		neonBatch.begin();
		float splitX = getUIViewport().getWorldWidth() * 0.5f;
		neonBatch.drawLine(splitX, 0, splitX, getUIViewport().getWorldHeight(), 1, new Color(1,1,1,0.1f));
		for (GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
		neonBatch.end();

		uiStage.act(dt);
		uiStage.draw();

		Debug.info("P1 MP: %.0f | Ult: %b", p1.mp, p1.isUltActive);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (barP2 != null) barP2.setPosition(getUIViewport().getWorldWidth() - 370, getUIViewport().getWorldHeight() - 50);
		if (barP1 != null) barP1.setPosition(20, getUIViewport().getWorldHeight() - 50);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (uiStage != null) uiStage.dispose();
		if (neonBatch != null) neonBatch.dispose();
		InputContext.inst().commandListener = null;
		ComboEngine.inst().onCommandExecuted = null;
	}
}
