package com.mygame;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.*;
import com.goldsprite.gdengine.core.scripting.*;
import com.goldsprite.gdengine.ecs.*;
import com.goldsprite.gdengine.ecs.component.*;
import com.goldsprite.gdengine.ecs.entity.*;
import com.goldsprite.gdengine.ecs.skeleton.animation.*;
import com.goldsprite.gdengine.ecs.system.*;
import com.goldsprite.gdengine.neonbatch.*;

public class Main2 implements IGameScriptEntry {
	private NeonBatch neonBatch;
	private Texture tex_gd_icon, tex_role_sheet;
	
	private GObject gd_cube;

	private NeonAnimatorComponent playerAnimator;
	
	@Override
	public void onStart(GameWorld gameWorld) {
		neonBatch = new NeonBatch();
		tex_gd_icon = new Texture(GameWorld.getAsset("gd_icon.png"));
		tex_role_sheet = new Texture(gameWorld.getInternalAssets("sprites/roles/enma/enma01.png"));
		
		gd_cube = new GObject();
		SpriteComponent sprite = gd_cube.addComponent(SpriteComponent.class);
		sprite.setRegion(new TextureRegion(tex_gd_icon));
		
//		new SpriteSystem(neonBatch, gameWorld.worldCamera);
//
//        createTestEntity();
//
//        gameWorld.worldCamera.zoom = 0.8f;
//        gameWorld.worldCamera.update();
	}

	@Override
	public void onUpdate(float delta) {
		// Grid
//        neonBatch.setProjectionMatrix(GameWorld.worldCamera.combined);
//        neonBatch.begin();
//        neonBatch.drawLine(-200, 0, 200, 0, 1, Color.GRAY);
//        neonBatch.drawLine(0, -200, 0, 200, 1, Color.GRAY);
//        neonBatch.end();
	}
	
	private void createTestEntity() {
		GObject player = new GObject("Player");
		player.transform.setPosition(0, -50);
		player.transform.setScale(2.5f); // 放大显示像素细节

		player.addComponent(SpriteComponent.class);
		playerAnimator = player.addComponent(NeonAnimatorComponent.class);

		// --- 制作动画数据 ---
		// 按照您的 SpriteUtils 逻辑 (100x100, Row 0=Idle, Row 1=Run)

		// 1. Idle: Row 0, 4 Frames
		Array<TextureRegion> idleFrames = splitFrames(tex_role_sheet, 0, 4);
		NeonAnimation idle = createFrameAnim("Idle", 0.8f, idleFrames);
		playerAnimator.addAnimation(idle);

		// 2. Run: Row 1, 4 Frames
		Array<TextureRegion> runFrames = splitFrames(tex_role_sheet, 1, 4);
		NeonAnimation run = createFrameAnim("Run", 0.6f, runFrames);
		playerAnimator.addAnimation(run);

		// 默认播放
		playerAnimator.play("Run");
	}

	/**
	 * 辅助切图方法 (模拟 SpriteUtils mode=0 的逻辑)
	 * @param tex 纹理
	 * @param row 行号
	 * @param count 数量
	 */
	private Array<TextureRegion> splitFrames(Texture tex, int row, int count) {
		Array<TextureRegion> frames = new Array<>();
		int cellSize = 80; // 这里这张图最后确定是80
		for (int i = 0; i < count; i++) {
			// x, y, w, h
			frames.add(new TextureRegion(tex, i * cellSize, row * cellSize, cellSize, cellSize));
		}
		return frames;
	}

	/**
	 * 辅助构建动画数据
	 */
	private NeonAnimation createFrameAnim(String name, float duration, Array<TextureRegion> frames) {
		NeonAnimation anim = new NeonAnimation(name, duration, true);
		NeonTimeline timeline = new NeonTimeline("self", NeonProperty.SPRITE);

		float frameDuration = duration / frames.size;
		for (int i = 0; i < frames.size; i++) {
			// 在对应时间点插入图片对象
			timeline.addKeyframe(i * frameDuration, frames.get(i));
		}
		anim.addTimeline(timeline);
		return anim;
	}

}
