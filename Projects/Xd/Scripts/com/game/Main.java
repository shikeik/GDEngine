package com.game;
import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;

public class Main implements IGameScriptEntry {
    @Override public void onStart(GameWorld world) {
        Debug.log("Hello Script!");
    }
}