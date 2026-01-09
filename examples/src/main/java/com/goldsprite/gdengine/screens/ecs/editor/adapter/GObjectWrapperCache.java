package com.goldsprite.gdengine.screens.ecs.editor.adapter;

import com.goldsprite.gdengine.ecs.entity.GObject;
import java.util.WeakHashMap;

/**
 * 享元模式缓存
 * 确保同一个 GObject 在编辑器中永远对应同一个 Adapter 实例。
 * 这对于 SceneManager 中的 == 比较和 HashMap 查找至关重要。
 */
public class GObjectWrapperCache {
    // 使用 WeakHashMap，当 GObject 被销毁时，Adapter 也会自动释放
    private static final WeakHashMap<GObject, GObjectAdapter> cache = new WeakHashMap<>();

    public static GObjectAdapter get(GObject obj) {
        if (obj == null) return null;

        GObjectAdapter adapter = cache.get(obj);
        if (adapter == null) {
            adapter = new GObjectAdapter(obj);
            cache.put(obj, adapter);
        }
        return adapter;
    }

    /**
     * 场景切换或重置时清理
     */
    public static void clear() {
        cache.clear();
    }
}
