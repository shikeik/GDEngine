package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

	// [新增] 血条
	private H5SkewBar barP1, barP2;

	private Fighter p1;
	private Fighter p2;

	@Override
	public String getIntroduction() { return ""; }

	@Override
	public void show() {
		super.show();
		getScreenManager().setOrientation(ScreenManager.Orientation.LANDSCAPE);
	}

	@Override
	public void hide() {
		super.hide();
		getScreenManager().setOrientation(ScreenManager.Orientation.PORTRAIT);
	}

	// [修正 6] 视口初始化
	@Override
	protected void initViewportAndCamera() {
		super.initViewportAndCamera();
		// 960x540 * 1.4f = 1344x756
		float scl = 1.4f;
		if (uiViewport instanceof ExtendViewport) {
			((ExtendViewport) uiViewport).setMinWorldWidth(960 * scl);
			((ExtendViewport) uiViewport).setMinWorldHeight(540 * scl);
		} else {
			// 如果父类默认不是 ExtendViewport (BaseSelectionScreen通常是Extend)，这里强制设置
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

		// [修正 3] 绑定到 ComboEngine
		InputContext.inst().commandListener = (cmdId, src) -> {
			// 发送到连招引擎
			ComboEngine.inst().push(cmdId, src);
		};

		// 监听连招引擎的最终输出
		ComboEngine.inst().onCommandExecuted = (finalCmd, src) -> {
			// UI 反馈
			String icon = "?";
			String type = "raw";
			for (InputDef.Command cmd : InputDef.COMMANDS) {
				if (cmd.id.equals(finalCmd)) {
					icon = cmd.icon;
					type = "move";
					break;
				}
			}
			// Flash Slash 特殊图标
			if (finalCmd.equals("FLASH SLASH")) { icon = "⚡"; type = "move"; }

			historyUI.addHistory(finalCmd, src, type, icon);
			if (type.equals("move")) toastUI.show(finalCmd.replace("CMD_", ""));

			// 游戏逻辑
			p1.handleCommand(finalCmd);
		};
	}

	private void initUI() {
		uiStage = new Stage(getViewport());

		joystick = new VirtualJoystick();
		joystick.setPosition(50, 50);
		uiStage.addActor(joystick);

		historyUI = new CommandHistoryUI();
		historyUI.setPosition(10, getViewport().getWorldHeight() * 0.4f);
		uiStage.addActor(historyUI);

		toastUI = new ToastUI();
		toastUI.setPosition(getViewport().getWorldWidth() / 2 - 50, 100);
		uiStage.addActor(toastUI);

		// [修正 5] 添加血条
		initBars();

		gestureProcessor = new GestureProcessor(getViewport());

		getImp().addProcessor(uiStage);
		getImp().addProcessor(gestureProcessor);
		getImp().addProcessor(new KeyboardProcessor());
	}

	private void initBars() {
		// P1 Bar (Cyan)
		H5SkewBar.BarStyle style1 = new H5SkewBar.BarStyle();
		style1.gradientStart = Color.valueOf("00eaff");
		style1.gradientEnd = Color.valueOf("0088aa");
		style1.skewDeg = -20f;
		barP1 = new H5SkewBar(0, 500, style1);
		barP1.setSize(350, 25);
		barP1.setPosition(20, getViewport().getWorldHeight() - 50);
		uiStage.addActor(barP1);

		// P2 Bar (Red)
		H5SkewBar.BarStyle style2 = new H5SkewBar.BarStyle();
		style2.gradientStart = Color.valueOf("ff0055");
		style2.gradientEnd = Color.valueOf("aa0033");
		style2.skewDeg = 20f;
		barP2 = new H5SkewBar(0, 500, style2);
		barP2.setSize(350, 25);
		barP2.setFillFromRight(true);
		barP2.setPosition(getViewport().getWorldWidth() - 350 - 20, getViewport().getWorldHeight() - 50);
		uiStage.addActor(barP2);
	}

	@Override
	public void render0(float delta) {
		gestureProcessor.update(delta);
		p1.update(delta);
		p2.update(delta);

		// 更新血条
		barP1.setValue(p1.hp);
		barP2.setValue(p2.hp);

		// Camera logic
		float midX = (p1.x + p2.x) / 2 + 20;
		float camX = getWorldCamera().position.x;
		getWorldCamera().position.x += (midX - camX) * 5 * delta;
		float viewHalfW = getWorldCamera().viewportWidth / 2 * getWorldCamera().zoom;
		if (getWorldCamera().position.x < viewHalfW) getWorldCamera().position.x = viewHalfW;
		if (getWorldCamera().position.x > 1000 - viewHalfW) getWorldCamera().position.x = 1000 - viewHalfW;
		getWorldCamera().update();

		// Draw World
		batch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		neonBatch.drawLine(-500, 0, 1500, 0, 2, Color.GRAY);
		p1.draw(neonBatch);
		p2.draw(neonBatch);
		neonBatch.end();

		// Draw UI Trails
		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		for (com.goldsprite.solofight.core.input.GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
		neonBatch.end();

		uiStage.act(delta);
		uiStage.draw();

		DebugUI.info("P1 State: %s", p1.state);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		// 简单的自适应布局调整
		if (barP2 != null) barP2.setPosition(getViewport().getWorldWidth() - 370, getViewport().getWorldHeight() - 50);
		if (barP1 != null) barP1.setPosition(20, getViewport().getWorldHeight() - 50);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (uiStage != null) uiStage.dispose();
		if (batch != null) batch.dispose();
		InputContext.inst().commandListener = null;
		ComboEngine.inst().onCommandExecuted = null; // 清理引用
	}
}
