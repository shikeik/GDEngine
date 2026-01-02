// 文件: ./examples/src/main/java/com/goldsprite/solofight/screens/ecs/tests/skeleton/SkeletonVisualScreen.java
package com.goldsprite.solofight.screens.ecs.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.log.Debug;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.gameframeworks.neonbatch.NeonBatch;
import com.goldsprite.gameframeworks.neonbatch.NeonStage;
import com.goldsprite.solofight.ecs.skeleton.*;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class SkeletonVisualScreen extends ExampleGScreen {

	private GameWorld world;
	private NeonBatch neonBatch;
	private NeonStage uiStage;

	// 演员
	private GObject player;
	private GObject dummy;
	private NeonAnimatorComponent playerAnimator;

	// 导演系统
	private enum State { WAIT, RUN, LAUNCH, AIR_FIGHT, SMASH, LAND, POSE, END }
	private State state = State.WAIT;
	private float stateTimer = 0f;
	private boolean isPlayingCutscene = false;

	@Override
	public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

	@Override
	public String getIntroduction() {
		return "导演模式测试\n点击 [ACTION!] 开始演出\n展示：骨骼动画/位移/相机/混合 综合应用";
	}

	@Override
	public void create() {
		neonBatch = new NeonBatch();

		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		world = new GameWorld();
		world.setReferences(getUIViewport(), worldCamera);

		new SkeletonSystem();
		new SkeletonRenderSystem(neonBatch, getWorldCamera());

		// 初始化演员
		createActors();
		initUI();

		// 初始镜头
		getWorldCamera().zoom = 1.2f;
		getWorldCamera().position.set(400, 200, 0); // 放在中间偏上
	}

	private void createActors() {
		// --- 主角 ---
		player = new GObject("Player");
		player.transform.setPosition(0, 0); // 地面
		player.transform.setScale(1.5f);
		SkeletonComponent skelComp = player.addComponent(SkeletonComponent.class);
		playerAnimator = player.addComponent(NeonAnimatorComponent.class);
		TestSkeletonFactory.buildStickman(skelComp.getSkeleton());
		TestAnimationFactory.setupAnimations(playerAnimator);
		playerAnimator.play("Idle");

		// --- 木桩 (Target) ---
		dummy = new GObject("Dummy");
		dummy.transform.setPosition(800, 0); // 放在右边 800 处
		SkeletonComponent dSkel = dummy.addComponent(SkeletonComponent.class);
		// 给木桩一个简单的骨架 (Box Skin)
		NeonBone box = dSkel.getSkeleton().createBone("Body", "root", 80,
			new NeonGeometrySkin(NeonGeometrySkin.Shape.BOX, 40, true));
		box.rotation = 90;
		dSkel.getSkeleton().getSlot("Body").color.set(Color.WHITE);
	}

	private void initUI() {
		uiStage = new NeonStage(getUIViewport());
		getImp().addProcessor(uiStage);
		Table root = new Table();
		root.setFillParent(true); root.top().pad(20);
		uiStage.addActor(root);

		VisTextButton btnAction = new VisTextButton(">>> ACTION! <<<");
		btnAction.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				startCutscene();
			}
		});
		root.add(btnAction).width(200).height(50);
	}

	private void startCutscene() {
		resetScene();
		isPlayingCutscene = true;
		state = State.WAIT;
		stateTimer = 0f;
	}

	private void resetScene() {
		player.transform.setPosition(0, 0);
		dummy.transform.setPosition(800, 0);
		dummy.transform.rotation = 0;
		playerAnimator.play("Idle");
		GameWorld.timeScale = 1.0f;
	}

	@Override
	public void render0(float delta) {
		// 导演逻辑 Update
		if (isPlayingCutscene) {
			updateDirector(delta);
		}

		// ECS Update
		world.update(delta);

		// 绘制
		neonBatch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();
		neonBatch.drawLine(-1000, 0, 2000, 0, 2, Color.GRAY); // 地面
		neonBatch.end();

		world.getSystem(SkeletonRenderSystem.class).update(delta);

		uiStage.act(delta);
		uiStage.draw();

		Debug.info("Director State: %s (%.1fs)", state, stateTimer);
	}

	/**
	 * 核心导演逻辑
	 */
	private void updateDirector(float delta) {
		stateTimer += delta;

		switch (state) {
			case WAIT: // 待机 1秒
				if (stateTimer > 1.0f) {
					changeState(State.RUN);
					playerAnimator.crossFade("Run_Drag", 0.3f);
				}
				break;

			case RUN: // 奔跑 2秒 (逼近敌人)
				// 物理位移
				player.transform.position.x += 400 * delta; // 速度 400

				// 接近判断
				if (player.transform.position.x >= 700) {
					changeState(State.LAUNCH);
					playerAnimator.crossFade("Atk_Launcher", 0.1f); // 极快切入
					// 慢动作特写
					GameWorld.timeScale = 0.5f;
				}
				break;

			case LAUNCH: // 挑飞 (0.4s)
				// 模拟挑飞物理
				if (stateTimer > 0.2f) { // 动作挥出瞬间
					dummy.transform.position.y += 600 * delta; // 木桩起飞
					dummy.transform.position.x += 100 * delta;
					dummy.transform.rotation -= 360 * delta;   // 旋转
				}

				if (stateTimer > 0.4f) {
					changeState(State.AIR_FIGHT);
					playerAnimator.crossFade("Atk_Air", 0.1f);
					// 恢复速度
					GameWorld.timeScale = 1.0f;
				}
				break;

			case AIR_FIGHT: // 空中连斩 (1.5s)
				// 玩家起跳跟上
				player.transform.position.y = MathUtils.lerp(player.transform.position.y, 250, 5 * delta);
				player.transform.position.x += 50 * delta;

				// 木桩悬空受击
				dummy.transform.position.y = MathUtils.lerp(dummy.transform.position.y, 300, 3 * delta);
				dummy.transform.position.x = player.transform.position.x + 80;
				dummy.transform.rotation += 720 * delta; // 疯狂旋转

				// 屏幕震动模拟
				getWorldCamera().position.add((MathUtils.random()-0.5f)*5, (MathUtils.random()-0.5f)*5, 0);

				if (stateTimer > 1.5f) {
					changeState(State.SMASH);
					playerAnimator.crossFade("Atk_Smash", 0.1f);
					GameWorld.timeScale = 0.2f; // 再次慢动作
				}
				break;

			case SMASH: // 下劈 (0.5s)
				if (stateTimer > 0.3f) { // 劈下瞬间
					// 极速下坠
					player.transform.position.y -= 800 * delta;
					dummy.transform.position.y -= 1000 * delta;
					dummy.transform.position.x += 200 * delta;
				}

				if (player.transform.position.y <= 0) {
					player.transform.position.y = 0;
					dummy.transform.position.y = 0;
					dummy.transform.rotation = 90; // 躺平
					changeState(State.LAND);
					playerAnimator.crossFade("Idle", 0.1f); // 落地瞬间切回Idle或Land姿势
					GameWorld.timeScale = 1.0f;

					// 落地大震动
					getWorldCamera().position.add((MathUtils.random()-0.5f)*20, (MathUtils.random()-0.5f)*20, 0);
				}
				break;

			case LAND: // 落地缓冲 (0.5s)
				if (stateTimer > 0.5f) {
					changeState(State.POSE);
					playerAnimator.crossFade("Pose_Back", 0.5f); // 帅气收刀
				}
				break;

			case POSE: // 摆Pose
				if (stateTimer > 2.0f) {
					changeState(State.END);
				}
				break;
		}

		// 相机跟随 (始终看着玩家和木桩的中间)
		float midX = (player.transform.position.x + dummy.transform.position.x) / 2f;
		float midY = (player.transform.position.y + dummy.transform.position.y) / 2f + 100;

		float camX = getWorldCamera().position.x;
		float camY = getWorldCamera().position.y;

		getWorldCamera().position.x += (midX - camX) * 5 * delta;
		getWorldCamera().position.y += (midY - camY) * 5 * delta;
		getWorldCamera().update();
	}

	private void changeState(State newState) {
		state = newState;
		stateTimer = 0f;
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (getWorldCamera() != null) {
			// resize 后重置一下位置，避免跳变，但 updateDirector 会接管它
			// 这里我们不需要强制置零，让 Director 控制即可
		}
	}

	@Override
	public void dispose() {
		if(world!=null) world.dispose();
		if(neonBatch!=null) neonBatch.dispose();
		if(uiStage!=null) uiStage.dispose();
	}
}
