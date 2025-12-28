package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;

/**
 * 手势处理器
 * 职责：
 * 1. 监听屏幕触摸
 * 2. 生成 GestureTrail 视觉对象
 * 3. 执行 Tap/Swipe 判定及 8 扇区映射
 */
public class GestureProcessor extends InputAdapter {
	private final Viewport viewport; // 用于将屏幕坐标转为 UI 世界坐标
	private final List<GestureTrail> trails = new ArrayList<>();

	// 触摸状态
	private boolean isTouching = false;
	private long startTime;
	private final Vector2 startPos = new Vector2(); // Screen Coords

	public GestureProcessor(Viewport uiViewport) {
		this.viewport = uiViewport;
	}

	public List<GestureTrail> getTrails() { return trails; }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// 简单区域判定：只处理屏幕右侧 50%
		// 注意：InputMultiplexer 顺序很重要，Stage 应优先处理按钮点击
		if (screenX < Gdx.graphics.getWidth() * 0.5f) return false;

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

		// 1. 生成视觉轨迹 (Visuals)
		// 转换坐标系: Screen -> UI World
		Vector2 uiStart = screenToUI(startPos.x, startPos.y);

		if (dt < 400) { // 有效时间窗
			// 清除旧轨迹 (复刻 H5: 立即消除上一个白芒)
			trails.clear();

			if (dist > 20) {
				Vector2 uiEnd = screenToUI(screenX, screenY);
				// 创建 Swipe 轨迹 (会自动计算 Snapped Angle)
				trails.add(new GestureTrail(uiStart.x, uiStart.y, uiEnd.x, uiEnd.y, false));
			} else {
				// 创建 Tap 轨迹
				trails.add(new GestureTrail(uiStart.x, uiStart.y, uiStart.x, uiStart.y, true));
			}
		}

		// 2. 逻辑判定 (Logic)
		if (dt > 400) return true; // 超时仅消耗事件，不触发指令

		if (dist < 20) {
			// 点击 (Tap) -> Attack
			InputContext.inst().emitCommand("CMD_ATK", "GESTURE:TAP");
		} else {
			// 划动 (Swipe) -> 8 Sector Analysis
			// 转换向量逻辑: dx, -dy (将Y轴反转为笛卡尔向上为正)
			float mathDx = dx;
			float mathDy = -dy;

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
		// H5 v17.6 逻辑映射
		switch (z) {
			case 0: // Right
				InputContext.inst().emitCommand("CMD_DASH_R", "GESTURE:SWIPE_RIGHT");
				break;
			case 2: // Up
				InputContext.inst().emitCommand("CMD_JUMP", "GESTURE:SWIPE_UP");
				break;
			case 4: // Left
				InputContext.inst().emitCommand("CMD_DASH_L", "GESTURE:SWIPE_LEFT");
				break;
			case 6: // Down
				InputContext.inst().emitCommand("CMD_ULT", "GESTURE:SWIPE_DOWN");
				break;
			default:
				// 斜向手势 (暂无具体指令，但记录日志以供调试)
				InputContext.inst().emitCommand("NONE", "GESTURE:ZONE_" + z);
				break;
		}
	}

	private Vector2 screenToUI(float x, float y) {
		// 必须使用一个新的 Vector 对象，否则 unproject 会修改传入的对象
		Vector2 v = new Vector2(x, y);
		viewport.unproject(v);
		return v;
	}

	public void update(float delta) {
		for (int i = trails.size() - 1; i >= 0; i--) {
			// 如果轨迹生命周期结束，移除
			if (!trails.get(i).update(delta)) {
				trails.remove(i);
			}
		}
	}
}
