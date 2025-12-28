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

	private NeonBatch neon; // 高性能绘图器

	private final float baseRadius = 70f;
	private final float thumbRadius = 25f;
	private final float slotDist = 65f;

	private final Vector2 value = new Vector2();
	private final Vector2 touchPos = new Vector2(); // 相对中心的触摸偏移
	private boolean isTouched = false;
	private int currentZone = -1;

	// 颜色常量
	private final Color COL_BG = new Color(0, 0, 0, 0.3f);
	private final Color COL_BORDER = new Color(1, 1, 1, 0.1f);
	private final Color COL_SLOT_INACTIVE = new Color(0.4f, 0.4f, 0.4f, 0.5f);
	private final Color COL_SLOT_ACTIVE = new Color(0, 234/255f, 255/255f, 1f); // #00eaff
	private final Color COL_LINE_INACTIVE = new Color(1, 1, 1, 0.05f);
	private final Color COL_LINE_ACTIVE = new Color(0, 234/255f, 255/255f, 0.6f);
	private final Color COL_THUMB = new Color(0/255f, 234/255f, 255/255f, 0.5f);

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
				updateZone(-1); // 重置区域

				InputContext.inst().stickRaw.setZero();
				InputContext.inst().stickZone = -1;
				InputContext.inst().updateVirtualState();
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

		// [修复] 只有当区域发生变化时，才更新并触发指令
		if (newZone != currentZone) {
			updateZone(newZone);
			if (newZone != -1) {
				mapZoneToCommand(newZone);
			}
		}
	}

	private void mapZoneToCommand(int z) {
		InputContext ctx = InputContext.inst();
		if (z == 0) ctx.emitCommand("CMD_MOVE_RIGHT", "STICK:RIGHT");
		if (z == 4) ctx.emitCommand("CMD_MOVE_LEFT", "STICK:LEFT");
		if (z == 6) ctx.emitCommand("CMD_CROUCH", "STICK:DOWN");
		ctx.updateVirtualState();
	}

	private void updateZone(int zone) {
		this.currentZone = zone;
		InputContext.inst().stickZone = zone;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		// 懒加载 NeonBatch (复用 Stage 的 SpriteBatch)
		if (neon == null || neon.getBatch() != batch) {
			if (batch instanceof SpriteBatch) neon = new NeonBatch((SpriteBatch) batch);
			else return; // Should not happen in standard setup
		}

		float cx = getX() + getWidth()/2;
		float cy = getY() + getHeight()/2;

		// 1. 绘制底座 (Base)
		// 填充半透明黑
		neon.drawCircle(cx, cy, baseRadius, 0, applyAlpha(COL_BG, parentAlpha), 32, true);
		// 描边微弱白
		neon.drawCircle(cx, cy, baseRadius, 2f, applyAlpha(COL_BORDER, parentAlpha), 32, false);

		// 2. 绘制 8 扇区槽 (Slots & Lines)
		for (int i = 0; i < 8; i++) {
			float rad = i * 45 * MathUtils.degreesToRadians;
			float cos = MathUtils.cos(rad);
			float sin = MathUtils.sin(rad);

			float sx = cx + cos * slotDist;
			float sy = cy + sin * slotDist;

			boolean isActive = (i == currentZone);

			// A. 连线 (Center to Slot)
			Color lineColor = isActive ? COL_LINE_ACTIVE : COL_LINE_INACTIVE;
			float lineWidth = isActive ? 3f : 1f;
			// 线不从圆心画，稍微留点空隙
			float startOff = 10f;
			neon.drawLine(cx + cos*startOff, cy + sin*startOff, sx, sy, lineWidth, applyAlpha(lineColor, parentAlpha));

			// B. 宝石 (Octagon)
			Color slotColor = isActive ? COL_SLOT_ACTIVE : COL_SLOT_INACTIVE;
			float size = isActive ? 9f : 5f; // Active时放大

			// 正八边形 (8 sides)
			// 旋转 22.5 度让平边对齐
			if (isActive) {
				// Glow effect (画个大的半透明)
				neon.drawRegularPolygon(sx, sy, size * 1.5f, 8, 22.5f, 0, applyAlpha(slotColor, parentAlpha * 0.4f), true);
			}
			// Core
			neon.drawRegularPolygon(sx, sy, size, 8, 22.5f, 0, applyAlpha(slotColor, parentAlpha), true);
		}

		// 3. 绘制摇杆头 (Thumb)
		float tx = cx + touchPos.x;
		float ty = cy + touchPos.y;
		neon.drawCircle(tx, ty, thumbRadius, 0, applyAlpha(COL_THUMB, parentAlpha), 24, true);
	}

	private Color tmpC = new Color();
	private Color applyAlpha(Color c, float alpha) {
		return tmpC.set(c).mul(1, 1, 1, alpha);
	}
}
