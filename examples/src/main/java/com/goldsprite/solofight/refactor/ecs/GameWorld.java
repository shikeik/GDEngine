package com.goldsprite.solofight.refactor.ecs;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.refactor.ecs.component.IComponent;
import com.goldsprite.solofight.refactor.ecs.entity.GObject;
import com.goldsprite.solofight.refactor.ecs.enums.ManageMode;
import com.goldsprite.solofight.refactor.ecs.system.BaseSystem;
import com.goldsprite.solofight.refactor.ecs.system.SceneSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 游戏世界容器 (原 GameSystem 重构)
 * 负责驱动 Update Loop 和 FixedUpdate Loop
 */
public class GameWorld {
	private static GameWorld instance;

	// --- 核心状态 ---
	private boolean paused = false;
	private boolean awaked = false;

	// --- 时间控制 ---
	private float fixedUpdateAccumulator = 0f;
	public static final float FIXED_DELTA_TIME = 1f / 60f; // 60Hz 物理步长
	private static float deltaTime;
	private static float totalDeltaTime;

	// --- 系统管理 ---
	private final List<BaseSystem> systems = new CopyOnWriteArrayList<>();
	private final Map<Class<? extends BaseSystem>, BaseSystem> systemMap = new HashMap<>();
	private final List<BaseSystem> updateSystems = new ArrayList<>();
	private final List<BaseSystem> fixedUpdateSystems = new ArrayList<>();

	// --- 实体管理 ---
	private final List<GObject> allEntities = new CopyOnWriteArrayList<>();

	// --- 核心内置系统引用 ---
	public SceneSystem sceneSystem;
	// public PhysicsSystem physicsSystem; // 稍后添加
	// public RenderSystem renderSystem;   // 稍后添加

	// --- 视口引用 ---
	public static Viewport worldViewport, uiViewport;
	public static OrthographicCamera worldCamera, uiCamera;

	public GameWorld() {
		instance = this;
		initializeCoreSystems();
	}

	public static GameWorld inst() {
		return instance;
	}

	private void initializeCoreSystems() {
		DebugUI.log("GameWorld: 初始化核心系统...");

		// 1. 初始化场景系统 (负责生命周期)
		sceneSystem = new SceneSystem();

		// 2. TODO: 初始化物理、渲染系统
		// physicsSystem = new PhysicsSystem();

		DebugUI.log("GameWorld: 初始化完成, 系统数: " + systems.size());
	}

	public void setViewport(Viewport worldViewport, Viewport uiViewport) {
		GameWorld.worldViewport = worldViewport;
		GameWorld.worldCamera = (OrthographicCamera) worldViewport.getCamera();
		GameWorld.uiViewport = uiViewport;
		GameWorld.uiCamera = (OrthographicCamera) uiViewport.getCamera();
	}

	// --- 游戏主循环 ---
	public void update(float delta) {
		deltaTime = delta;
		totalDeltaTime += delta;

		if (!awaked) {
			awaked = true;
			for(BaseSystem system : systems) system.awake();
			sceneSystem.awakeScene();
			DebugUI.log("GameWorld: 世界已唤醒");
			return;
		}

		if (paused) return;

		// 1. Fixed Update (物理/逻辑)
		fixedUpdateAccumulator += delta;
		while (fixedUpdateAccumulator >= FIXED_DELTA_TIME) {
			for (BaseSystem sys : fixedUpdateSystems) {
				if (sys.isEnabled()) sys.fixedUpdate(FIXED_DELTA_TIME);
			}
			fixedUpdateAccumulator -= FIXED_DELTA_TIME;
		}

		// 2. Variable Update (渲染/输入)
		for (BaseSystem sys : updateSystems) {
			if (sys.isEnabled()) sys.update(delta);
		}

		// 3. Cleanup
		sceneSystem.executeDestroyTask();
	}

	// --- 实体管理 API ---
	public static void manageGObject(GObject gobject, ManageMode mode) {
		if (instance != null) {
			if (mode == ManageMode.ADD) {
				instance.registerEntity(gobject);
			} else {
				instance.unregisterEntity(gobject);
			}
		}
	}

	private void registerEntity(GObject entity) {
		if (!allEntities.contains(entity)) {
			allEntities.add(entity);
		}
	}

	private void unregisterEntity(GObject entity) {
		allEntities.remove(entity);
	}

	public List<GObject> getAllEntities() {
		return allEntities; // CopyOnWriteArrayList 支持直接遍历
	}

	public void addDestroyGObject(GObject gobject) {
		sceneSystem.addDestroyGObject(gobject);
	}

	public void addDestroyComponent(IComponent component) {
		sceneSystem.addDestroyComponent(component);
	}
	
	public static float getTotalDeltaTime() {
		return totalDeltaTime;
	}

	// --- 系统注册 API ---
	public void registerSystem(BaseSystem system) {
		if (!systemMap.containsKey(system.getClass())) {
			systems.add(system);
			systemMap.put(system.getClass(), system);

			// 归类
			GameSystemInfo info = system.getSystemInfo();
			if (info != null) {
				if (info.type().has(GameSystemInfo.SystemType.UPDATE)) updateSystems.add(system);
				if (info.type().has(GameSystemInfo.SystemType.FIXED_UPDATE)) fixedUpdateSystems.add(system);
			} else {
				// 默认归为 Update
				updateSystems.add(system);
			}

			DebugUI.log("System Registered: " + system.getSystemName());
		}
	}

	public void dispose() {
		allEntities.clear();
		systems.clear();
		systemMap.clear();
		ComponentManager.clearCache();
		instance = null;
	}
}
