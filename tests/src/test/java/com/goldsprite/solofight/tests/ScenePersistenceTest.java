package com.goldsprite.solofight.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.goldsprite.gdengine.core.utils.SceneLoader;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.solofight.CLogAssert;
import com.goldsprite.solofight.GdxTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(GdxTestRunner.class)
public class ScenePersistenceTest {

	private FileHandle testFile;

	@Before
	public void setUp() {
		GdxTestRunner.mockAssetsRoot = "assets";
		// 1. 初始化环境
		try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
		new GameWorld();

		// 临时测试文件
		testFile = Gdx.files.local("build/test_scene.json");
	}

	@After
	public void tearDown() {
		if (testFile.exists()) testFile.delete();
		if (GameWorld.inst() != null) GameWorld.inst().dispose();
	}

	@Test
	public void testScenePipeline() {
		System.out.println(">>> 验证: 场景保存、加载与生命周期 (DDOL 和 Filter)");

		// ==========================================
		// 1. 搭建场景 (Setup)
		// ==========================================

		// A. 创建一个“不死鸟” (Global Manager)
		GObject globalMgr = new GObject("GlobalManager");
		globalMgr.setDontDestroyOnLoad(true); // 赐予免死金牌
		globalMgr.transform.setPosition(999, 999);

		// B. 创建一个普通物体 (Player)
		GObject player = new GObject("Player");
		player.transform.setPosition(100, 50);

		// C. 添加数据组件 (Sprite)
		SpriteComponent sprite = player.addComponent(SpriteComponent.class);
		sprite.assetPath = "gd_icon.png";
		sprite.width = 50;

		// D. 添加逻辑组件 (匿名内部类 - 应该被过滤)
		player.addComponent(new Component() {
			@Override public void update(float delta) { System.out.println("Logic running..."); }
			@Override public String getName() { return "AnonymousLogic"; }
		});

		// E. 创建子物体 (Weapon) - 验证层级
		GObject weapon = new GObject("Weapon");
		weapon.setParent(player);
		weapon.transform.setPosition(10, 0);

		// 验证当前状态
		worldUpdate(); // Flush add queue
		CLogAssert.assertEquals("初始根物体数量", 2, GameWorld.inst().getRootEntities().size()); // Player + GlobalMgr

		// ==========================================
		// 2. 保存场景 (Save)
		// ==========================================
		System.out.println("  -> 保存场景...");
		SceneLoader.saveCurrentScene(testFile);
		CLogAssert.assertTrue("文件应生成", testFile.exists());

		// 打印一下 JSON 内容看看有没有匿名类 (可选)
		// System.out.println(testFile.readString());

		// ==========================================
		// 3. 执行加载 (Load 和 Clear)
		// ==========================================
		System.out.println("  -> 加载场景 (含 Clear)...");

		// SceneLoader.load 内部会先调用 world.clear()
		// 预期：Player 和 Weapon 被销毁，GlobalMgr 被保留，然后从 JSON 加载出新的 Player 和 Weapon
		SceneLoader.load(testFile);

		worldUpdate(); // Flush add/remove queue

		// ==========================================
		// 4. 验证结果 (Verification)
		// ==========================================

		List<GObject> roots = GameWorld.inst().getRootEntities();
		CLogAssert.assertEquals("加载后根物体数量应仍为 2", 2, roots.size());

		// 4.1 验证 DDOL (不死鸟是否还在？)
		GObject newGlobal = findObject(roots, "GlobalManager");
		CLogAssert.assertTrue("GlobalManager 应该存活", newGlobal != null);
		// 这是一个强验证：它必须是同一个实例，不能是重新加载出来的
		CLogAssert.assertTrue("GlobalManager 应该是同一个实例", newGlobal == globalMgr);
		CLogAssert.assertEquals("位置保持不变", 999f, newGlobal.transform.position.x);

		// 4.2 验证普通物体加载 (Player)
		GObject newPlayer = findObject(roots, "Player");
		CLogAssert.assertTrue("Player 应该被重新加载", newPlayer != null);
		CLogAssert.assertTrue("Player 应该是新实例", newPlayer != player);
		CLogAssert.assertEquals("Player 坐标还原", 100f, newPlayer.transform.position.x);

		// 4.3 验证组件数据 (Sprite)
		SpriteComponent newSprite = newPlayer.getComponent(SpriteComponent.class);
		CLogAssert.assertTrue("Sprite组件存在", newSprite != null);
		CLogAssert.assertEquals("AssetPath 还原", "gd_icon.png", newSprite.assetPath);
		CLogAssert.assertEquals("Width 还原", 50f, newSprite.width);

		// 4.4 验证匿名类过滤 (Filter)
		// Player 原本有 3 个组件：Transform(自带) + Sprite + Anonymous
		// 现在应该只有 2 个：Transform + Sprite
		int compCount = newPlayer.getComponentsMap().values().stream().mapToInt(List::size).sum();
		CLogAssert.assertEquals("匿名组件应该被过滤掉 (只剩Trans+Sprite)", 2, compCount);

		// 4.5 验证层级 (Hierarchy)
		CLogAssert.assertEquals("Player 应该有 1 个子物体", 1, newPlayer.getChildren().size());
		GObject newWeapon = newPlayer.getChildren().get(0);
		CLogAssert.assertEquals("子物体名字还原", "Weapon", newWeapon.getName());
		CLogAssert.assertEquals("子物体局部坐标还原", 10f, newWeapon.transform.position.x);
	}

	// 辅助方法
	private void worldUpdate() {
		GameWorld.inst().update(0f);
	}

	private GObject findObject(List<GObject> list, String name) {
		for (GObject obj : list) {
			if (obj.getName().equals(name)) return obj;
		}
		return null;
	}
}
