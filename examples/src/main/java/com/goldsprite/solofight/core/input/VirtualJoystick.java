package com.goldsprite.solofight.core.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;

/**
 * 赛博朋克风格虚拟摇杆
 * 特性：程序化纹理、8扇区判定、Neon Glow 风格
 */
public class VirtualJoystick extends Actor {

	private Texture baseTex;
	private Texture knobTex;
	private Texture slotTex; // 扇区指示点

	private final float baseRadius = 70f;
	private final float knobRadius = 25f;
	private final float slotDist = 65f; // 指示点距离中心的距离

	private final Vector2 value = new Vector2(); // -1 ~ 1
	private final Vector2 touchPos = new Vector2(); // 相对中心的触摸偏移
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
		// 转换相对于中心的坐标
		float centerX = getWidth() / 2;
		float centerY = getHeight() / 2;
		float dx = x - centerX;
		float dy = y - centerY;

		float dist = (float) Math.sqrt(dx * dx + dy * dy);
		float maxDist = 40f; // 限制移动范围

		// 归一化计算
		if (dist > maxDist) {
			float scale = maxDist / dist;
			dx *= scale;
			dy *= scale;
		}

		touchPos.set(dx, dy);
		value.set(dx / maxDist, dy / maxDist);

		// 更新全局上下文
		InputContext.inst().stickRaw.set(value);

		// --- 扇区判定算法 (复刻 H5) ---
		// H5: let deg = Math.atan2(-dy, dx) * (180/Math.PI);
		// LibGDX Y轴向上，H5 Y轴向下。
		// H5 stick.y = -dy/max (H5 dy是向下增).
		// 在Actor中，y向上增。若想保持逻辑一致：
		// 摇杆向上推，dy > 0。H5向上推 dy < 0。
		// H5 atan2(-dy, dx)。LibGDX 应该是 atan2(dy, dx)。

		int newZone = -1;
		// 死区判断
		if (dist > 10) {
			float deg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
			if (deg < 0) deg += 360;
			// 0度是右，90度是上，180是左，270是下
			// H5: 0=Right, 2=Up, 4=Left, 6=Down
			// Math: 0=Right, 90=Up. (deg + 22.5) / 45
			newZone = (int) Math.floor((deg + 22.5f) / 45f) % 8;
		}

		updateZone(newZone);

		// 触发指令
		if (newZone != -1) {
			mapZoneToCommand(newZone);
		}
	}

	private void mapZoneToCommand(int z) {
		// H5: if(z===0) RIGHT; if(z===4) LEFT; if(z===6) DOWN;
		// 注意：LibGDX atan2(dy, dx) -> Up is 90 deg (Zone 2), Down is 270 deg (Zone 6)
		// 这一步与 H5 完全一致
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
			// TODO: 这里可以加震动
		}
	}

	/**
	 * 程序化生成纹理 (Pixmaps)
	 */
	private void generateTextures() {
		// 1. Base (底座): 半透明黑底 + 白边框
		Pixmap pBase = new Pixmap((int)baseRadius*2, (int)baseRadius*2, Pixmap.Format.RGBA8888);
		pBase.setColor(0, 0, 0, 0.3f);
		pBase.fillCircle((int)baseRadius, (int)baseRadius, (int)baseRadius);
		pBase.setColor(1, 1, 1, 0.1f);
		pBase.drawCircle((int)baseRadius, (int)baseRadius, (int)baseRadius - 2);
		baseTex = new Texture(pBase);
		pBase.dispose();

		// 2. Knob (摇杆头): 半透明青色
		Pixmap pKnob = new Pixmap((int)knobRadius*2, (int)knobRadius*2, Pixmap.Format.RGBA8888);
		pKnob.setColor(0, 234/255f, 255/255f, 0.5f); // #00eaff
		pKnob.fillCircle((int)knobRadius, (int)knobRadius, (int)knobRadius);
		knobTex = new Texture(pKnob);
		pKnob.dispose();

		// 3. Slot (扇区指示点): 小灰点
		Pixmap pSlot = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
		pSlot.setColor(0.4f, 0.4f, 0.4f, 1f);
		pSlot.fillCircle(6, 6, 3);
		slotTex = new Texture(pSlot); // 居中绘制需要 offset
		pSlot.dispose();
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		Color c = getColor();
		batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

		float centerX = getX() + getWidth()/2;
		float centerY = getY() + getHeight()/2;

		// 1. Draw Base
		batch.draw(baseTex, getX(), getY());

		// 2. Draw 8 Slots (指示点)
		for (int i = 0; i < 8; i++) {
			float angleRad = i * 45 * MathUtils.degreesToRadians;
			float sx = centerX + MathUtils.cos(angleRad) * slotDist;
			float sy = centerY + MathUtils.sin(angleRad) * slotDist;

			// 高亮当前扇区
			if (i == currentZone) {
				batch.setColor(0, 234/255f, 255/255f, 1f); // Neon Blue
				// 放大一点
				batch.draw(slotTex, sx - 6, sy - 6, 6, 6, 12, 12, 2f, 2f, 0, 0, 0, 12, 12, false, false);
			} else {
				batch.setColor(0.4f, 0.4f, 0.4f, 0.5f);
				batch.draw(slotTex, sx - 6, sy - 6);
			}
		}

		// 3. Draw Knob
		batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
		float knobX = centerX + touchPos.x - knobRadius;
		float knobY = centerY + touchPos.y - knobRadius;
		batch.draw(knobTex, knobX, knobY);
	}
}
