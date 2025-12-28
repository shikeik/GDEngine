package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;

public class VirtualJoystick extends Actor {

	private Texture baseTex;
	private Texture thumbTex;
	private Texture slotTex; // 普通灰点
	private Texture slotActiveTex; // 激活亮青点 (带 Glow)

	private final float baseRadius = 70f;
	private final float thumbRadius = 25f;
	private final float slotDist = 65f;

	private final Vector2 value = new Vector2();
	private final Vector2 touchPos = new Vector2();
	private boolean isTouched = false;
	private int currentZone = -1;

	public VirtualJoystick() {
		setSize(baseRadius * 2, baseRadius * 2);
		setTouchable(Touchable.enabled);
		generateTextures();

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
				updateZone(-1);
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

		if (dist > maxDist) {
			float scale = maxDist / dist;
			dx *= scale; dy *= scale;
		}

		touchPos.set(dx, dy);
		value.set(dx / maxDist, dy / maxDist);
		InputContext.inst().stickRaw.set(value);

		// H5 扇区算法: 0=Right, 2=Up (LibGDX Y-Up: 90 deg is Up)
		int newZone = -1;
		if (dist > 10) {
			float deg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
			if (deg < 0) deg += 360;
			newZone = (int) Math.floor((deg + 22.5f) / 45f) % 8;
		}
		updateZone(newZone);

		if (newZone != -1) mapZoneToCommand(newZone);
	}

	private void mapZoneToCommand(int z) {
		InputContext ctx = InputContext.inst();
		if (z == 0) ctx.emitCommand("CMD_MOVE_RIGHT", "STICK:RIGHT");
		if (z == 4) ctx.emitCommand("CMD_MOVE_LEFT", "STICK:LEFT");
		if (z == 6) ctx.emitCommand("CMD_CROUCH", "STICK:DOWN");
		ctx.updateVirtualState();
	}

	private void updateZone(int zone) {
		if (this.currentZone != zone) {
			this.currentZone = zone;
			InputContext.inst().stickZone = zone;
		}
	}

	private void generateTextures() {
		// 1. Base: bg(0,0,0,0.3), border(255,255,255,0.1, 2px)
		Pixmap pBase = new Pixmap((int)baseRadius*2, (int)baseRadius*2, Pixmap.Format.RGBA8888);
		pBase.setColor(0, 0, 0, 0.3f);
		pBase.fillCircle((int)baseRadius, (int)baseRadius, (int)baseRadius);
		pBase.setColor(1, 1, 1, 0.1f);
		// 画两个圆环模拟 2px 边框
		pBase.drawCircle((int)baseRadius, (int)baseRadius, (int)baseRadius - 1);
		pBase.drawCircle((int)baseRadius, (int)baseRadius, (int)baseRadius - 2);
		baseTex = new Texture(pBase); pBase.dispose();

		// 2. Thumb: bg(0, 234, 255, 0.5)
		Pixmap pThumb = new Pixmap((int)thumbRadius*2, (int)thumbRadius*2, Pixmap.Format.RGBA8888);
		pThumb.setColor(0/255f, 234/255f, 255/255f, 0.5f);
		pThumb.fillCircle((int)thumbRadius, (int)thumbRadius, (int)thumbRadius);
		thumbTex = new Texture(pThumb); pThumb.dispose();

		// 3. Slot Normal: #666 (Gray)
		Pixmap pSlot = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
		pSlot.setColor(0.4f, 0.4f, 0.4f, 1f);
		pSlot.fillCircle(4, 4, 3);
		slotTex = new Texture(pSlot); pSlot.dispose();

		// 4. Slot Active: #00eaff + Glow (Neon Blue)
		// 尺寸做大一点包含 Glow
		Pixmap pSlotA = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
		// Glow (Low Alpha)
		pSlotA.setColor(0/255f, 234/255f, 255/255f, 0.3f);
		pSlotA.fillCircle(8, 8, 7);
		// Core (High Alpha)
		pSlotA.setColor(0/255f, 234/255f, 255/255f, 1f);
		pSlotA.fillCircle(8, 8, 4);
		slotActiveTex = new Texture(pSlotA); pSlotA.dispose();
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		Color c = getColor();
		batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

		float cx = getX() + getWidth()/2;
		float cy = getY() + getHeight()/2;

		// Draw Base
		batch.draw(baseTex, getX(), getY());

		// Draw Slots
		for (int i = 0; i < 8; i++) {
			float rad = i * 45 * MathUtils.degreesToRadians;
			float sx = cx + MathUtils.cos(rad) * slotDist;
			float sy = cy + MathUtils.sin(rad) * slotDist;

			if (i == currentZone) {
				// Active: Draw Glow Texture, Scale 1.5x (Texture is 16x16)
				// H5 scale 1.5 of 6px is 9px. Our ActiveTex core is 8px, glow 14px.
				// Just draw centered.
				batch.setColor(1, 1, 1, parentAlpha);
				batch.draw(slotActiveTex, sx - 8, sy - 8);
			} else {
				// Normal
				batch.setColor(1, 1, 1, parentAlpha * 0.8f);
				batch.draw(slotTex, sx - 4, sy - 4);
			}
		}

		// Draw Thumb
		batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
		float tx = cx + touchPos.x - thumbRadius;
		float ty = cy + touchPos.y - thumbRadius;
		batch.draw(thumbTex, tx, ty);
	}
}
