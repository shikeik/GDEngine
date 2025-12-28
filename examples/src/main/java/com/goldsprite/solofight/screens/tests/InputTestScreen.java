package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.VisUIHelper;
import com.goldsprite.solofight.core.input.GestureProcessor;
import com.goldsprite.solofight.core.input.GestureTrail;
import com.goldsprite.solofight.core.input.InputContext;
import com.goldsprite.solofight.core.input.VirtualJoystick;
import com.kotcrab.vis.ui.VisUI;

import java.util.ArrayList;
import java.util.List;

public class InputTestScreen extends ExampleGScreen {

	private Stage stage;
	private VirtualJoystick joystick;
	private GestureProcessor gestureProcessor;
	private SpriteBatch batch;
	private NeonBatch neonBatch;

	// Debug UI
	private Label lblInfo;
	private Label lblLog;
	private List<String> cmdLogs = new ArrayList<>();

	@Override
	public String getIntroduction() {
		return "输入系统综合测试\n左屏: 8区摇杆 | 右屏: 8区手势\n观察左上角状态与中间日志";
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);

		// 1. 设置 Stage 和 摇杆
		stage = new Stage(getViewport());
		joystick = new VirtualJoystick();
		// 放在左下角
		joystick.setPosition(50, 50);
		stage.addActor(joystick);

		// 2. 设置手势处理器
		gestureProcessor = new GestureProcessor(getViewport());

		// 3. 设置输入管线 (UI First, then Gesture)
		getImp().addProcessor(stage);
		getImp().addProcessor(gestureProcessor); // 处理未被 UI 拦截的触摸

		// 4. Debug UI
		Table table = new Table();
		table.setFillParent(true);
		table.top().left().pad(20);

		lblInfo = new Label("INFO", VisUI.getSkin());
		lblInfo.setColor(Color.CYAN);
		table.add(lblInfo).left().row();

		lblLog = new Label("LOGS", VisUI.getSkin());
		lblLog.setColor(Color.YELLOW);
		table.add(lblLog).left().padTop(20).row();

		stage.addActor(table);

		// 5. 绑定指令监听
		InputContext.inst().commandListener = (cmdId, src) -> {
			String log = String.format("[%s] %s", src, cmdId);
			cmdLogs.add(0, log);
			if (cmdLogs.size() > 10) cmdLogs.remove(cmdLogs.size() - 1);

			// 简单的 Console 输出
			System.out.println("Command Triggered: " + log);
		};
	}

	@Override
	public void render0(float delta) {
		// 更新逻辑
		gestureProcessor.update(delta);

		// 绘制手势轨迹 (World Space / UI Space based on viewport)
		// 我们的 GestureProcessor 使用 uiViewport 生成坐标，所以用 uiCamera 绘制
		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		for (GestureTrail trail : gestureProcessor.getTrails()) {
			trail.draw(neonBatch);
		}
		neonBatch.end();

		// 更新调试文本
		updateDebugText();

		// 绘制 UI (摇杆 + 文字)
		stage.act(delta);
		stage.draw();

		// 绘制手势区边界提示
		drawZoneHint();
	}

	private void updateDebugText() {
		InputContext ctx = InputContext.inst();
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Virtual MoveX: %.1f\n", ctx.moveX));
		sb.append(String.format("Virtual Crouch: %b\n", ctx.crouch));
		sb.append(String.format("Stick Raw: (%.2f, %.2f)\n", ctx.stickRaw.x, ctx.stickRaw.y));
		sb.append(String.format("Stick Zone: %d\n", ctx.stickZone));

		lblInfo.setText(sb.toString());

		StringBuilder logSb = new StringBuilder();
		for (String s : cmdLogs) {
			logSb.append(s).append("\n");
		}
		lblLog.setText(logSb.toString());
	}

	private void drawZoneHint() {
		// 画一条虚线分隔左右屏 (40% 位置)
		float splitX = getViewport().getWorldWidth() * 0.4f;

		// 简单的 ShapeRenderer 或者 NeonBatch 线
		// 这里复用 NeonBatch 画条细线
		batch.setProjectionMatrix(getViewport().getCamera().combined);
		neonBatch.begin();
		neonBatch.drawLine(splitX, 0, splitX, getViewport().getWorldHeight(), 2, new Color(1,1,1,0.2f));
		neonBatch.end();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (stage != null) stage.dispose();
		if (batch != null) batch.dispose();
	}
}
