package com.goldsprite.gdengine.screens.ecs.editor.adapter;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

/**
 * [核心传动轴]
 * 将 ECS 的 GObject 伪装成编辑器可识别的 EditorTarget。
 * 所有的 Get 操作读取 GObject 实时数据。
 * 所有的 Set 操作直接修改 GObject 组件。
 */
public class GObjectAdapter implements EditorTarget {

    private final GObject gobj;

    // 临时变量，用于计算点击检测
    private static final Vector2 tmpVec = new Vector2();

    public GObjectAdapter(GObject gobj) {
        if (gobj == null) throw new IllegalArgumentException("GObject cannot be null");
        this.gobj = gobj;
    }

    public GObject getRealObject() {
        return gobj;
    }

    // ==========================================
    // 基础属性映射
    // ==========================================

    @Override
    public String getName() {
        return gobj.getName();
    }

    @Override
    public void setName(String name) {
        gobj.setName(name);
    }

    @Override
    public String getTypeName() {
        // 返回类名或自定义 Tag，用于 Inspector 标题
        return "Entity";
    }

    // ==========================================
    // Transform 映射 (核心)
    // ==========================================

    @Override public float getX() { return gobj.transform.position.x; }
    @Override public void setX(float v) { gobj.transform.position.x = v; }

    @Override public float getY() { return gobj.transform.position.y; }
    @Override public void setY(float v) { gobj.transform.position.y = v; }

    @Override public float getRotation() { return gobj.transform.rotation; }
    @Override public void setRotation(float v) { gobj.transform.rotation = v; }

    @Override
    public float getScaleX() {
        return gobj.transform.scale; // 目前 ECS 只支持等比缩放
    }

    @Override
    public void setScaleX(float v) {
        gobj.transform.scale = v; // 修改 ScaleX 会同步修改 ScaleY
    }

    @Override
    public float getScaleY() {
        return gobj.transform.scale;
    }

    @Override
    public void setScaleY(float v) {
        gobj.transform.scale = v;
    }

    // ==========================================
    // 亲缘关系映射 (Hierarchy)
    // ==========================================

    @Override
    public EditorTarget getParent() {
        if (gobj.getParent() == null) return null;
        // [关键] 必须通过缓存获取，保证 == 判断成立
        return GObjectWrapperCache.get(gobj.getParent());
    }

    @Override
    public void setParent(EditorTarget parent) {
        if (parent == null) {
            gobj.setParent(null);
        } else if (parent instanceof GObjectAdapter) {
            // 解包：把 Adapter 还原成 GObject 塞回去
            gobj.setParent(((GObjectAdapter) parent).gobj);
        }
    }

    @Override
    public void removeFromParent() {
        gobj.setParent(null);
    }

    @Override
    public Array<EditorTarget> getChildren() {
        // 动态构建列表，但其中的元素是从缓存取的
        Array<EditorTarget> list = new Array<>();
        for (GObject child : gobj.getChildren()) {
            list.add(GObjectWrapperCache.get(child));
        }
        return list;
    }

    @Override
    public void addChild(EditorTarget child) {
        if (child instanceof GObjectAdapter) {
            gobj.addChild(((GObjectAdapter) child).gobj);
        }
    }

    // ==========================================
    // 交互与渲染
    // ==========================================

    @Override
    public boolean hitTest(float wx, float wy) {
        // 1. 获取世界坐标
        float tx = gobj.transform.worldPosition.x;
        float ty = gobj.transform.worldPosition.y;

        // 2. 尝试获取 Sprite 大小作为点击区域
        float width = 50; // 默认大小
        float height = 50;

        SpriteComponent sprite = gobj.getComponent(SpriteComponent.class);
        if (sprite != null && sprite.region != null) {
            width = sprite.width * Math.abs(gobj.transform.scale);
            height = sprite.height * Math.abs(gobj.transform.scale);
        }

        // 3. 简单的矩形判定 (暂不考虑旋转带来的 OBB 问题，够用了)
        float halfW = width / 2;
        float halfH = height / 2;

        return wx >= tx - halfW && wx <= tx + halfW &&
               wy >= ty - halfH && wy <= ty + halfH;
    }

    @Override
    public void render(NeonBatch batch) {
        // GObject 已经由 ECS 的 System (SpriteSystem) 渲染了。
        // 这里可以画一些 编辑器专用 的辅助线 (Gizmo Outline)
        // 例如：选中时画一个白框

        // 仅作演示：画一个极淡的十字准星表示锚点位置
        float x = gobj.transform.worldPosition.x;
        float y = gobj.transform.worldPosition.y;
        batch.drawLine(x - 10, y, x + 10, y, 1, Color.GRAY);
        batch.drawLine(x, y - 10, x, y + 10, 1, Color.GRAY);
    }

    // 用于 equals/hashCode，保证 List 查找正确
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof GObjectAdapter) {
            return this.gobj == ((GObjectAdapter) obj).gobj;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return gobj.hashCode();
    }
}
