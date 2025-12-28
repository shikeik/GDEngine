package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;

public class GestureProcessor extends InputAdapter {
	private final Viewport viewport; // 用于坐标转换
	private final List<GestureTrail> trails = new ArrayList<>();

	// 触摸状态
	private boolean isTouching = false;
	private long startTime;
	private final Vector2 startPos = new Vector2(); // Screen Coords
	private final Vector2 currPos = new Vector2();  // Screen Coords

	public GestureProcessor(Viewport uiViewport) {
		this.viewport = uiViewport;
	}

	public List<GestureTrail> getTrails() { return trails; }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// 简单处理：只认第一个手指，或者过滤掉左半屏（如果需要与摇杆共存）
		// 这里假设此 Processor 只接收有效区域的输入（通过 Multiplexer 顺序控制或区域判断）
		// 为了测试，我们规定：屏幕右侧 60% 区域有效
		if (screenX < com.badlogic.gdx.Gdx.graphics.getWidth() * 0.4f) return false;

		isTouching = true;
		startTime = System.currentTimeMillis();
		startPos.set(screenX, screenY);
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (!isTouching) return false;
		isTouching = false;

		long dt = System.currentTimeMillis() - startTime;
		float dx = screenX - startPos.x;
		float dy = screenY - startPos.y; // GDX Y-down: down is positive
		float dist = (float) Math.sqrt(dx * dx + dy * dy);

		// 1. 生成视觉轨迹 (需要转为 World/UI 坐标)
		Vector2 uiStart = screenToUI(startPos.x, startPos.y);

		if (dt < 400) {
			// 清除旧轨迹 (H5 v17.7 逻辑: 立即消除上一个白芒)
			trails.clear();

			if (dist > 20) {
				Vector2 uiEnd = screenToUI(screenX, screenY);
				trails.add(new GestureTrail(uiStart.x, uiStart.y, uiEnd.x, uiEnd.y, false));
			} else {
				trails.add(new GestureTrail(uiStart.x, uiStart.y, uiStart.x, uiStart.y, true));
			}
		}

		// 2. 逻辑判定 (8 Zone Logic)
		if (dt > 400) return true; // 超时无效

		if (dist < 20) {
			InputContext.inst().emitCommand("CMD_ATK", "GESTURE:TAP");
		} else {
			// 计算角度 (LibGDX 屏幕坐标系: Y向下)
			// 我们需要转换成笛卡尔坐标系的逻辑角度 (右=0, 上=90)
			// 向量: (dx, -dy) -> 把 Y 轴反转，变成向上为正
			float mathDy = -dy;
			float mathDx = dx;

			float deg = MathUtils.atan2(mathDy, mathDx) * MathUtils.radiansToDegrees;
			if (deg < 0) deg += 360;

			// 8扇区划分: (deg + 22.5) / 45
			// 0:Right, 1:UR, 2:Up, 3:UL, 4:Left, 5:DL, 6:Down, 7:DR
			int zone = (int) Math.floor((deg + 22.5f) / 45f) % 8;

			mapZoneToCommand(zone);
		}
		return true;
	}

	private void mapZoneToCommand(int z) {
		// 映射逻辑 (目前只处理 0, 2, 4, 6，预留 1,3,5,7)
		if (z == 0) InputContext.inst().emitCommand("CMD_DASH_R", "SWIPE:RIGHT"); // 这里可以根据需求改回 SWIPE_RIGHT，H5 v17.6 demo 里映射的是 CMD_DASH_R? 不，H5 demo 映射的是 processInput('GESTURE', 'SWIPE_RIGHT')
		// 修正：InputContext emitCommand 接收的是 (CmdID, SourceDesc)
		// 根据 InputDef，CMD_JUMP 对应 SWIPE_UP

		switch (z) {
			case 0: InputContext.inst().emitCommand("CMD_DASH_R", "GESTURE:SWIPE_RIGHT"); break; // 暂定右滑冲刺
			case 2: InputContext.inst().emitCommand("CMD_JUMP", "GESTURE:SWIPE_UP"); break;
			case 4: InputContext.inst().emitCommand("CMD_DASH_L", "GESTURE:SWIPE_LEFT"); break; // 暂定左滑冲刺
			case 6: InputContext.inst().emitCommand("CMD_ULT", "GESTURE:SWIPE_DOWN"); break;
			default:
				// 斜向手势 (暂无指令，但记录日志)
				InputContext.inst().emitCommand("NONE", "GESTURE:ZONE_" + z);
				break;
		}
	}

	private Vector2 screenToUI(float x, float y) {
		Vector2 v = new Vector2(x, y);
		viewport.unproject(v);
		return v;
	}

	public void update(float delta) {
		for (int i = trails.size() - 1; i >= 0; i--) {
			if (!trails.get(i).update(delta)) {
				trails.remove(i);
			}
		}
	}
}
