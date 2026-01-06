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

/**
 * 混合演示
 */
public class Main3 implements IGameScriptEntry {
	private NeonBatch neonBatch;
	private Texture tex_gd_icon, tex_role_sheet;

	private GObject role;
	private NeonAnimatorComponent roleAnim;

	private float worldWidth, worldHeight;
	
	//开始时执行
	@Override public void onStart(GameWorld world) {
		neonBatch = new NeonBatch();
		tex_gd_icon = new Texture(GameWorld.getAsset("gd_icon.png"));
		tex_role_sheet = new Texture("sprites/roles/enma/enma01.png");
		
		
		worldWidth = GameWorld.worldCamera.viewportWidth;
		worldHeight = GameWorld.worldCamera.viewportHeight;
		
		String info = "世界高度: "+worldHeight
		+"\n"+GameWorld.inst().getSystem(SpriteSystem.class)
		;
		Debug.logT("Script", "RotCube脚本项目 onStart(). \ninfo: \n%s", info);

		
		float size = worldHeight*0.15f;
		// 创建一个旋转Cube x y size color animSpeed
		createRotCube(-worldHeight*0.2f, worldHeight*0.3f, size, Color.RED, 0.3f);

		GObject p = createRotGdIcon(worldHeight*0.2f, worldHeight*0.3f, size, Color.WHITE, -0.3f);
		GObject c = createRotCube(0, 0, size*0.5f, Color.YELLOW, 0.3f);
		c.setParent(p);
		c.transform.position.set(worldHeight* 0.2f, 0);
		
		size = worldHeight * 0.4f;
		role = createRole(0, -worldHeight*0.2f, size);
	}

	// 每帧更新
	@Override public void onUpdate(float delta) {
		logic(delta);
		drawGrid();
	}
	
	String lastAnimName = "";
	private void logic(float delta) {
		boolean isTouch = Gdx.input.isTouched();
		String animName = isTouch ? "Run" : "Idle";
		if(isTouch){
			int fingerCount = PlatformImpl.getTouchCount();
			boolean doubleFinger = fingerCount == 2;
			int dir = doubleFinger ? 1 : -1;
			float vel = worldHeight * 0.3f;
			
			role.transform.position.add(vel * dir * delta, 0);
			role.getComponent(SpriteComponent.class).flipX = dir < 0;
		}
		role.getComponent(NeonAnimatorComponent.class).play(animName);
	}

	private GObject createRotCube(final float x, final float y, final float size, final Color c, final float animSpeed) {
		final GObject obj = new GObject("RotCube");
		obj.transform.setPosition(x, y);
		obj.transform.setScale(1f);

		obj.addComponent(new Component(){
				private NeonBatch neonBatch;

				@Override public void onAwake() {
					neonBatch = new NeonBatch();
				}
				@Override public void update(float delta) {
					float angle = 360f * GameWorld.getTotalTime() * animSpeed;
					obj.transform.rotation = angle;
					neonBatch.setProjectionMatrix(GameWorld.worldCamera.combined);
					neonBatch.begin();
					float lineWidth = 6;
					neonBatch.drawRect(
						obj.transform.worldPosition.x - size/2f, 
						obj.transform.worldPosition.y - size/2f, 
						size, size, angle, lineWidth, c, false);
					neonBatch.end();
				}
			});

		return obj;
	}
	
	private GObject createRotGdIcon(final float x, final float y, final float size, final Color c, final float animSpeed) {
		final GObject obj = new GObject("RotGdIcon");
		obj.transform.setPosition(x, y);
		obj.transform.setScale(1f);
		
		SpriteComponent sprite = obj.addComponent(SpriteComponent.class);
		sprite.setRegion(new TextureRegion(tex_gd_icon));
		sprite.color.set(c);
		sprite.width = size;
		sprite.height = size;
		
		obj.addComponent(new Component(){
				@Override public void update(float delta) {
					float angle = 360f * GameWorld.getTotalTime() * animSpeed;
					obj.transform.rotation = angle;
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
	
	private void drawGrid() {
        // Grid
        neonBatch.setProjectionMatrix(GameWorld.worldCamera.combined);
        neonBatch.begin();
        neonBatch.drawLine(-200, 0, 200, 0, 1, Color.GRAY);
        neonBatch.drawLine(0, -200, 0, 200, 1, Color.GRAY);
        neonBatch.end();
	}
}
