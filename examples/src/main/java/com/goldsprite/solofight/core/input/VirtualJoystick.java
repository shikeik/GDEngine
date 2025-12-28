package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.goldsprite.solofight.core.NeonBatch;

public class VirtualJoystick extends Actor {

	private NeonBatch neon;

	private final float baseRadius = 70f;
	private final float thumbRadius = 25f;
	private final float slotDist = 65f;

	private final Vector2 value = new Vector2();
	private final Vector2 touchPos = new Vector2();
	private boolean isTouched = false;
	private int currentZone = -1;

	// 预定义颜色常量 (避免每帧 new)
	private static final Color C_BASE_BG = new Color(0, 0, 0, 0.3f);
	private static final Color C_BASE_BORDER = new Color(1, 1, 1, 0.1f);
	private static final Color C_THUMB = new Color(0, 0.917f, 1f, 0.5f); // #00eaff
	private static final Color C_SLOT_INACTIVE = new Color(0.4f, 0.4f, 0.4f, 0.5f);
	private static final Color C_SLOT_ACTIVE_CORE = new Color(0, 0.917f, 1f, 1f);
	private static final Color C_SLOT_ACTIVE_GLOW = new Color(0, 0.917f, 1f, 0.3f);
	private static final Color C_LINE_ACTIVE = new Color(0, 0.917f, 1f, 0.6f);
	private static final Color C_LINE_INACTIVE = new Color(1, 1, 1, 0.05f);

	private final Color tmpC = new Color(); // 绘图临时变量

	public VirtualJoystick() {
		setSize(baseRadius * 2, baseRadius * 2);
		setTouchable(Touchable.enabled);

		addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				isTouched = true;
				updateJoystick(x, y);
				return true;
			}
			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				updateJoystick(x, y);
			}
			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				isTouched = false;
				value.setZero();
				touchPos.setZero();
				if (currentZone != -1) {
					currentZone = -1;
					InputContext.inst().stickZone = -1;
					InputContext.inst().stickRaw.setZero();
					InputContext.inst().updateVirtualState();
				}
			}
		});
	}

	private void updateJoystick(float x, float y) {
		float centerX = getWidth() / 2;
		float centerY = getHeight() / 2;
		float dx = x - centerX;
		float dy = y - centerY;
		float dist = (float) Math.sqrt(dx * dx + dy * dy);
		float maxDist = 40f;

		// 限制拖动范围
		if (dist > maxDist) {
			float scale = maxDist / dist;
			dx *= scale; dy *= scale;
		}

		touchPos.set(dx, dy);
		value.set(dx / maxDist, dy / maxDist);
		InputContext.inst().stickRaw.set(value);

		// 计算扇区
		int newZone = -1;
		if (dist > 10) {
			float deg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
			if (deg < 0) deg += 360;
			newZone = (int) Math.floor((deg + 22.5f) / 45f) % 8;
		}

		// 只有当区域发生变化时，才更新并触发指令
		if (newZone != currentZone) {
			currentZone = newZone;
			InputContext.inst().stickZone = newZone;
			if (newZone != -1) mapZoneToCommand(newZone);
		}
	}

	private void mapZoneToCommand(int z) {
		InputContext ctx = InputContext.inst();
		if (z == 0) ctx.emitCommand("CMD_MOVE_RIGHT", "STICK:RIGHT");
		if (z == 4) ctx.emitCommand("CMD_MOVE_LEFT", "STICK:LEFT");
		if (z == 6) ctx.emitCommand("CMD_CROUCH", "STICK:DOWN");
		ctx.updateVirtualState();
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		if (neon == null || neon.getBatch() != batch) {
			if (batch instanceof SpriteBatch) neon = new NeonBatch((SpriteBatch) batch);
			else return;
		}

		float cx = getX() + getWidth()/2;
		float cy = getY() + getHeight()/2;

		// 1. Base (Circle with border)
		neon.drawCircle(cx, cy, baseRadius, 0, alpha(C_BASE_BG, parentAlpha), 32, true);
		neon.drawCircle(cx, cy, baseRadius, 2f, alpha(C_BASE_BORDER, parentAlpha), 32, false);

		// 2. Slots & Lines
		for (int i = 0; i < 8; i++) {
			float rad = i * 45 * MathUtils.degreesToRadians;
			float cos = MathUtils.cos(rad);
			float sin = MathUtils.sin(rad);
			float sx = cx + cos * slotDist;
			float sy = cy + sin * slotDist;

			boolean active = (i == currentZone);

			// Connection Line
			Color lc = active ? C_LINE_ACTIVE : C_LINE_INACTIVE;
			float lw = active ? 3f : 1f;
			neon.drawLine(cx + cos*10, cy + sin*10, sx, sy, lw, alpha(lc, parentAlpha));

			// Octagon Slot (正八边形)
			if (active) {
				// Glow
				neon.drawRegularPolygon(sx, sy, 9f * 1.5f, 8, 22.5f, 0, alpha(C_SLOT_ACTIVE_GLOW, parentAlpha), true);
				// Core
				neon.drawRegularPolygon(sx, sy, 9f, 8, 22.5f, 0, alpha(C_SLOT_ACTIVE_CORE, parentAlpha), true);
			} else {
				neon.drawRegularPolygon(sx, sy, 5f, 8, 22.5f, 0, alpha(C_SLOT_INACTIVE, parentAlpha), true);
			}
		}

		// 3. Thumb
		neon.drawCircle(cx + touchPos.x, cy + touchPos.y, thumbRadius, 0, alpha(C_THUMB, parentAlpha), 24, true);
	}

	private Color alpha(Color c, float a) {
		return tmpC.set(c).mul(1, 1, 1, a);
	}
}
