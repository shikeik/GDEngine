package com.goldsprite.gdengine.core.scripting;

import com.goldsprite.gdengine.ecs.GameWorld;

/**
 * 游戏脚本入口 (上帝接口)
 * 职责：作为游戏的"造物主"，在引擎启动时初始化世界。
 */
public interface IGameScriptEntry {

	/**
	 * 游戏开始
	 * 在这里创建实体、添加组件、注册自定义系统等。
	 * @param world 引擎的核心 ECS 世界实例
	 */
	void onStart(GameWorld world);

	/**
	 * 游戏每帧更新 (可选)
	 * 通常建议使用 System 来驱动逻辑，但这里提供一个全局挂钩方便简单逻辑。
	 */
	default void onUpdate(float delta) {}
}
