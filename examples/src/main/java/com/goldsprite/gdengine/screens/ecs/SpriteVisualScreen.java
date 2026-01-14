package com.goldsprite.gdengine.screens.ecs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.component.NeonAnimatorComponent;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonAnimation;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.neonbatch.NeonStage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.goldsprite.gdengine.ecs.system.WorldRenderSystem;

public class SpriteVisualScreen extends ExampleGScreen {

	private GameWorld world;
	private NeonBatch neonBatch;
	private NeonStage uiStage;

	private Texture texture;
	private NeonAnimatorComponent playerAnimator;

	@Override
	public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

	@Override
	public String getIntroduction() {
		return "帧动画集成测试 (v1.6.0)\n素材: Enma01 (100x100)\nRow0:Idle, Row1:Run";
	}

	@Override
	public void show() {
		super.show();
		Debug.logT("VisualCheck", "Checking visual center for " + this.getClass().getSimpleName() + " Camera Pos: " + getWorldCamera().position);
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

		try {
			texture = new Texture(Gdx.files.internal("sprites/roles/enma/enma01.png"));
		} catch (Exception e) {
			Debug.log("Error loading texture: " + e.getMessage());
			return;
		}

		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		world = new GameWorld();
		world.setReferences(getUIViewport(), worldCamera);

		new WorldRenderSystem(neonBatch, getWorldCamera());

		createTestEntity();
		initUI();

		//autoCenterWorldCamera = false; // Default is false now
		getWorldCamera().zoom = 0.8f;
		getWorldCamera().update();
	}

	private void createTestEntity() {
		GObject player = new GObject("Player");
		player.transform.setPosition(0, -50);
		player.transform.setScale(2.5f); // 放大显示像素细节

		SpriteComponent spriteComp = player.addComponent(SpriteComponent.class);
		playerAnimator = player.addComponent(NeonAnimatorComponent.class);

		// --- 制作动画数据 ---
		// 按照您的 SpriteUtils 逻辑 (100x100, Row 0=Idle, Row 1=Run)

		// 1. Idle: Row 0, 4 Frames
		Array<TextureRegion> idleFrames = splitFrames(texture, 0, 4);
		NeonAnimation idle = createFrameAnim("Idle", 0.8f, idleFrames);
		playerAnimator.addAnimation(idle);

		// 2. Run: Row 1, 4 Frames
		Array<TextureRegion> runFrames = splitFrames(texture, 1, 4);
		NeonAnimation run = createFrameAnim("Run", 0.6f, runFrames);
		playerAnimator.addAnimation(run);

		// 默认播放
		playerAnimator.play("Idle");
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

	private void initUI() {
		uiStage = new NeonStage(getUIViewport());
		getImp().addProcessor(uiStage);
		Table root = new Table();
		root.setFillParent(true); root.left().top().pad(20);
		uiStage.addActor(root);

		root.add(new VisLabel("Sprite Controls")).padBottom(10).row();

		VisTextButton btnIdle = new VisTextButton("Play Idle");
		btnIdle.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					playerAnimator.play("Idle");
				}
			});
		root.add(btnIdle).width(150).padBottom(5).row();

		VisTextButton btnRun = new VisTextButton("Play Run");
		btnRun.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					playerAnimator.play("Run");
				}
			});
		root.add(btnRun).width(150).row();
	}

	@Override
	public void render0(float delta) {
		world.update(delta);

		// Grid
		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		neonBatch.drawLine(-200, 0, 200, 0, 1, Color.GRAY);
		neonBatch.drawLine(0, -200, 0, 200, 1, Color.GRAY);
		neonBatch.end();

		world.render(neonBatch, worldCamera);

		uiStage.act(delta);
		uiStage.draw();
	}

	@Override
	public void dispose() {
		if(world!=null) world.dispose();
		if(texture!=null) texture.dispose();
		if(neonBatch!=null) neonBatch.dispose();
		if(uiStage!=null) uiStage.dispose();
	}
}
