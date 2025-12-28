package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.game.Fighter;
import com.goldsprite.solofight.core.input.CommandHistoryUI;
import com.goldsprite.solofight.core.input.GestureProcessor;
import com.goldsprite.solofight.core.input.InputContext;
import com.goldsprite.solofight.core.input.InputDef;
import com.goldsprite.solofight.core.input.ToastUI;
import com.goldsprite.solofight.core.input.VirtualJoystick;

public class CombatScreen extends ExampleGScreen {

	private SpriteBatch batch;
	private NeonBatch neonBatch;
	private Stage uiStage;

	// UI Components
	private VirtualJoystick joystick;
	private GestureProcessor gestureProcessor;
	private CommandHistoryUI historyUI;
	private ToastUI toastUI;

	// Game Entities
	private Fighter p1;
	private Fighter p2;

	@Override
	public String getIntroduction() { return ""; }

	@Override
	protected void initViewportAndCamera() {
		float scl = 1.4f;
		worldCamera = new OrthographicCamera();
		uiViewport = new ExtendViewport(960 * scl, 540 * scl);
	}

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

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);

		// 1. 初始化实体
		p1 = new Fighter(200, Color.CYAN, false);
		p2 = new Fighter(800, Color.valueOf("ff0055"), true);
		p1.setEnemy(p2);
		p2.setEnemy(p1);

		// 2. 初始化 UI 和 输入
		initUI();

		// 3. 绑定指令
		InputContext.inst().commandListener = (cmdId, src) -> {
			// UI 反馈
			String icon = "?";
			String type = "raw";
			for (InputDef.Command cmd : InputDef.COMMANDS) {
				if (cmd.id.equals(cmdId)) {
					icon = cmd.icon;
					type = "move";
					break;
				}
			}
			historyUI.addHistory(cmdId, src, type, icon);
			if (type.equals("move")) toastUI.show(cmdId.replace("CMD_", ""));

			// 游戏逻辑反馈
			p1.handleCommand(cmdId);
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

		gestureProcessor = new GestureProcessor(getViewport());

		getImp().addProcessor(uiStage);
		getImp().addProcessor(gestureProcessor);
	}

	@Override
	public void render0(float delta) {
		// --- 1. Update ---
		gestureProcessor.update(delta);
		p1.update(delta);
		p2.update(delta);

		// Camera Follow (简单逻辑：居中)
		float midX = (p1.x + p2.x) / 2 + 20; // +20 width offset
		// 简单的相机平滑跟随
		float camX = getWorldCamera().position.x;
		float targetX = midX;
		getWorldCamera().position.x += (targetX - camX) * 5 * delta;

		// 限制相机边界 (假设地图宽 1000)
		float viewHalfW = getWorldCamera().viewportWidth / 2 * getWorldCamera().zoom;
		if (getWorldCamera().position.x < viewHalfW) getWorldCamera().position.x = viewHalfW;
		if (getWorldCamera().position.x > 1000 - viewHalfW) getWorldCamera().position.x = 1000 - viewHalfW;
		getWorldCamera().update();

		// --- 2. Draw World ---
		batch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		// Draw Ground
		neonBatch.drawLine(-500, 0, 1500, 0, 2, Color.GRAY);

		// Draw Fighters
		p1.draw(neonBatch);
		p2.draw(neonBatch);

		neonBatch.end();

		// --- 3. Draw UI ---
		// 绘制手势轨迹
		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		// 分隔线
		float splitX = getViewport().getWorldWidth() * 0.5f;
		neonBatch.drawLine(splitX, 0, splitX, getViewport().getWorldHeight(), 1, new Color(1,1,1,0.1f));
		// 轨迹
		for (com.goldsprite.solofight.core.input.GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
		neonBatch.end();

		uiStage.act(delta);
		uiStage.draw();

		// Debug Info
		DebugUI.info("P1 State: %s | Ground: %b", p1.state, !p1.inAir);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (toastUI != null) toastUI.setPosition(getViewport().getWorldWidth() / 2 - toastUI.getPrefWidth() / 2, 120);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (uiStage != null) uiStage.dispose();
		if (batch != null) batch.dispose();
		InputContext.inst().commandListener = null;
	}
}
