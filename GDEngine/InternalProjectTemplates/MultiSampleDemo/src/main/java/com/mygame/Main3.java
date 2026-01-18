package com.mygame;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.*;
import com.goldsprite.gdengine.core.scripting.*;
import com.goldsprite.gdengine.ecs.*;
import com.goldsprite.gdengine.ecs.component.*;
import com.goldsprite.gdengine.ecs.entity.*;
import com.goldsprite.gdengine.ecs.skeleton.animation.*;
import com.goldsprite.gdengine.log.*;
import com.goldsprite.gdengine.neonbatch.*;
import com.goldsprite.gdengine.ecs.system.*;
import com.badlogic.gdx.*;
import com.goldsprite.gdengine.*;
import com.goldsprite.gdengine.core.Gd;

/**
 *
 */
public class Main3 implements IGameScriptEntry {
	private Texture tex_role_sheet;

	private GObject role;
	private NeonAnimatorComponent roleAnim;

	private float worldWidth, worldHeight;

	//
	@Override public void onStart(GameWorld world) {
		tex_role_sheet = new Texture("sprites/roles/enma/enma01.png");


		worldWidth = GameWorld.worldCamera.viewportWidth;
		worldHeight = GameWorld.worldCamera.viewportHeight;

		String info = ": "+worldHeight
			;
		Debug.logT("Script", "RotCube onStart(). \ninfo: \n%s", info);


		GObject gridObj = new GObject("WorldGrid");
		gridObj.addComponent(new RenderComponent(){
				@Override public void render(NeonBatch batch, Camera cam) {
					drawGrid(batch, cam);
				}
				// 简化处理
				@Override public boolean contains(float x, float y) {
					return false;
				}
		});

		float size = worldHeight*0.15f;

		// Cube x y size color animSpeed
		createRotCube(-worldHeight*0.2f, worldHeight*0.3f, size, Color.RED, 0.3f);

		GObject p = createRotGdIcon(worldHeight*0.2f, worldHeight*0.3f, size, Color.WHITE, -0.3f);

		GObject c = createRotCube(0, 0, size*0.5f, Color.YELLOW, 0.3f);
		c.setParent(p);
		c.transform.position.set(worldHeight* 0.2f, 0);

		size = worldHeight * 0.4f;
		role = createRole(0, -worldHeight*0.2f, size);
	}

	// ?
	@Override public void onUpdate(float delta) {
		logic(delta);
	}

	String lastAnimName = "";
	private void logic(float delta) {
		int dir = 0;
		boolean right = Gd.input.getX() > Gd.graphics.getWidth()/2f;

		Debug.infoT("TEST", "inputX: %.1f, Gd.width: %.1f, gameCamWidth: %.1f", Gd.input.getX(), Gd.graphics.getWidth(), GameWorld.worldCamera.viewportWidth);

		boolean isTouch = Gd.input.isTouched();
		if(Gd.input.isKeyPressed(Input.Keys.A) || (isTouch&&!right)) dir -= 1;
		if(Gd.input.isKeyPressed(Input.Keys.D) || (isTouch&&right)) dir += 1;
		String animName = (Gd.input.isTouched() || dir != 0) ? "Run" : "Idle";
		float vel = worldHeight * 0.3f;
		Debug.info("right : $s, %s, %s", right, dir, isTouch);
		if(Gd.input.isTouched() || dir != 0) role.getComponent(SpriteComponent.class).flipX = dir < 0;
		role.transform.position.add(vel * dir * delta, 0);
		role.getComponent(NeonAnimatorComponent.class).play(animName);
	}

	private GObject createRotCube(final float x, final float y, final float size, final Color c, final float animSpeed) {
		final GObject obj = new GObject("RotCube");
		obj.transform.setPosition(x, y);
		obj.transform.setScale(1f);

		obj.addComponent(new RenderComponent(){
			@Override public void render(NeonBatch batch, Camera cam) {
				float angle = 360f * GameWorld.getTotalTime() * animSpeed;
				obj.transform.rotation = angle;
				float lineWidth = 6;

				batch.drawRect(
					obj.transform.worldPosition.x - size/2f,
					obj.transform.worldPosition.y - size/2f,
					size, size, angle, lineWidth, c, false);
			}

			// 简化处理
			@Override public boolean contains(float x, float y) {
				return false;
			}
		});

		return obj;
	}

	private GObject createRotGdIcon(final float x, final float y, final float size, final Color c, final float animSpeed) {
		final GObject obj = new GObject("RotGdIcon");
		obj.transform.setPosition(x, y);
		obj.transform.setScale(1f);

		SpriteComponent sprite = obj.addComponent(SpriteComponent.class);
		sprite.assetPath = "gd_icon.png";
		sprite.reloadRegion();
		sprite.sortingLayer = "Effect";
		sprite.color.set(c);
		sprite.width = size;
		sprite.height = size;

		obj.addComponent(new RenderComponent(){
				@Override public void render(NeonBatch batch, Camera cam) {
				float angle = 360f * GameWorld.getTotalTime() * animSpeed;
				obj.transform.rotation = angle;
				}

				// 简化处理
				@Override public boolean contains(float x, float y) {
					return false;
				}
		});

		return obj;
	}

	private GObject createRole(final float x, final float y, final float size) {
		Array<TextureRegion> idleFrames = splitFrames(tex_role_sheet, 0, 4);

		final GObject obj = new GObject("Role");
		obj.transform.setPosition(x, y);
		//obj.transform.setScale(2.5f);

		SpriteComponent sprite = obj.addComponent(SpriteComponent.class);
		sprite.setRegion(idleFrames.get(0));
		sprite.sortingLayer = "Effect";
		sprite.width = size;
		sprite.height = size;

		roleAnim = obj.addComponent(NeonAnimatorComponent.class);

		// 1. Idle: Row 0, 4 Frames
		NeonAnimation idle = createFrameAnim("Idle", 0.8f, idleFrames);
		roleAnim.addAnimation(idle);

		// 2. Run: Row 1, 4 Frames
		Array<TextureRegion> runFrames = splitFrames(tex_role_sheet, 1, 4);
		NeonAnimation run = createFrameAnim("Run", 0.6f, runFrames);
		roleAnim.addAnimation(run);

		roleAnim.play("Idle");

		return obj;
	}

	/**
	 *  ( SpriteUtils mode=0 )
	 * @param tex
	 * @param row
	 * @param count
	 */
	private Array<TextureRegion> splitFrames(Texture tex, int row, int count) {
		Array<TextureRegion> frames = new Array<>();
		int cellSize = 80; // 80
		for (int i = 0; i < count; i++) {
			// x, y, w, h
			frames.add(new TextureRegion(tex, i * cellSize, row * cellSize, cellSize, cellSize));
		}
		return frames;
	}

	/**
	 *
	 */
	private NeonAnimation createFrameAnim(String name, float duration, Array<TextureRegion> frames) {
		NeonAnimation anim = new NeonAnimation(name, duration, true);
		NeonTimeline timeline = new NeonTimeline("self", NeonProperty.SPRITE);

		float frameDuration = duration / frames.size;
		for (int i = 0; i < frames.size; i++) {
			//
			timeline.addKeyframe(i * frameDuration, frames.get(i));
		}
		anim.addTimeline(timeline);
		return anim;
	}

	private void drawGrid(NeonBatch batch, Camera cam) {
		// Grid
		batch.drawLine(-200, 0, 200, 0, 1, Color.GRAY);
		batch.drawLine(0, -200, 0, 200, 1, Color.GRAY);
	}
}
