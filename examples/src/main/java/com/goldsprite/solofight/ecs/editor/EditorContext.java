package com.goldsprite.solofight.ecs.editor;

import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.entity.GObject;

/**
 * 编辑器的大脑，持有世界和当前选中的物体
 */
public class EditorContext {
    private final GameWorld gameWorld;
    private GObject selection; // 当前选中的物体

    public EditorContext(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    public GameWorld getWorld() { return gameWorld; }

    public void setSelection(GObject obj) {
        this.selection = obj;
        // 这里未来可以触发 UI 刷新事件
    }

    public GObject getSelection() { return selection; }
}
