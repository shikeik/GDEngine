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
import com.goldsprite.solofight.core.input.*;

public class InputTestScreen extends ExampleGScreen {

	private Stage stage;
	private VirtualJoystick joystick;
	private GestureProcessor gestureProcessor;
	private CommandHistoryUI historyUI;
	private ToastUI toastUI;

	private SpriteBatch batch;
	private NeonBatch neonBatch;

	@Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);
		stage = new Stage(getUIViewport());

		// A. 虚拟摇杆 (左下角)
		joystick = new VirtualJoystick();
		joystick.setPosition(50, 50);
		stage.addActor(joystick);

		// B. 指令历史 (左侧偏上)
		historyUI = new CommandHistoryUI();
		historyUI.setPosition(10, getUIViewport().getWorldHeight() * 0.4f);
		stage.addActor(historyUI);

		// C. Toast (屏幕底部)
		toastUI = new ToastUI();
		toastUI.setPosition(getUIViewport().getWorldWidth() / 2 - 50, 100);
		stage.addActor(toastUI);

		// D. 手势处理器 (World Space)
		gestureProcessor = new GestureProcessor(getUIViewport());

		// E. 输入管线
		getImp().addProcessor(stage);
		getImp().addProcessor(gestureProcessor);
		getImp().addProcessor(new KeyboardProcessor());

		// F. 指令监听
		InputContext.inst().commandListener = (cmdId, src) -> {
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
		};
	}

	@Override
	public void render0(float delta) {
		// 1. 逻辑更新
		gestureProcessor.update(delta);

		// 2. 绘制手势 (UI Space)
		batch.setProjectionMatrix(getUIViewport().getCamera().combined);
		neonBatch.begin();
		float midX = getUIViewport().getWorldWidth() * 0.5f;
		neonBatch.drawLine(midX, 0, midX, getUIViewport().getWorldHeight(), 2, new Color(1,1,1,0.1f));
		for (GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
		neonBatch.end();

		// 3. UI 绘制
		stage.act(delta);
		stage.draw();

		// 4. Debug Info (使用全局 DebugUI)
		InputContext ctx = InputContext.inst();
		DebugUI.info("MoveX: %.1f | Crouch: %b", ctx.moveX, ctx.crouch);
		DebugUI.info("Stick: (%.2f, %.2f) Zone: %d", ctx.stickRaw.x, ctx.stickRaw.y, ctx.stickZone);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (toastUI != null) toastUI.setPosition(getUIViewport().getWorldWidth() / 2 - toastUI.getPrefWidth() / 2, 120);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (stage != null) stage.dispose();
		if (batch != null) batch.dispose();
		InputContext.inst().commandListener = null;
	}
}
