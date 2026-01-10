package com.goldsprite.solofight.screens.editor.adapter;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

public class GObjectAdapter implements EditorTarget {
    private final GObject gobject;

    public GObjectAdapter(GObject gobject) {
        this.gobject = gobject;
    }

    public GObject getGObject() {
        return gobject;
    }

    @Override
    public String getName() {
        return gobject.getName();
    }

    @Override
    public void setName(String name) {
        gobject.setName(name);
    }

    @Override
    public String getTypeName() {
        return "GObject";
    }

    @Override
    public float getX() {
        return gobject.transform.x;
    }

    @Override
    public void setX(float v) {
        gobject.transform.x = v;
    }

    @Override
    public float getY() {
        return gobject.transform.y;
    }

    @Override
    public void setY(float v) {
        gobject.transform.y = v;
    }

    @Override
    public float getRotation() {
        return gobject.transform.rotation;
    }

    @Override
    public void setRotation(float v) {
        gobject.transform.rotation = v;
    }

    @Override
    public float getScaleX() {
        return gobject.transform.scaleX;
    }

    @Override
    public void setScaleX(float v) {
        gobject.transform.scaleX = v;
    }

    @Override
    public float getScaleY() {
        return gobject.transform.scaleY;
    }

    @Override
    public void setScaleY(float v) {
        gobject.transform.scaleY = v;
    }

    @Override
    public EditorTarget getParent() {
        if (gobject.getParent() == null) return null;
        return new GObjectAdapter(gobject.getParent());
    }

    @Override
    public void setParent(EditorTarget parent) {
        if (parent instanceof GObjectAdapter) {
            gobject.setParent(((GObjectAdapter) parent).gobject);
        } else if (parent == null) {
            gobject.setParent(null);
        }
    }

    @Override
    public void removeFromParent() {
        gobject.setParent(null);
    }

    @Override
    public Array<EditorTarget> getChildren() {
        Array<EditorTarget> list = new Array<>();
        for (GObject child : gobject.getChildren()) {
            list.add(new GObjectAdapter(child));
        }
        return list;
    }

    @Override
    public void addChild(EditorTarget child) {
        if (child instanceof GObjectAdapter) {
            ((GObjectAdapter) child).gobject.setParent(gobject);
        }
    }

    @Override
    public boolean hitTest(float x, float y) {
        // Simple bounding box check (20 units radius)
        float dist = com.badlogic.gdx.math.Vector2.dst(gobject.transform.x, gobject.transform.y, x, y);
        return dist < 20; 
    }

    @Override
    public void render(NeonBatch batch) {
        // GObject rendering is handled by ECS systems
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GObjectAdapter that = (GObjectAdapter) obj;
        return gobject.equals(that.gobject);
    }

    @Override
    public int hashCode() {
        return gobject.hashCode();
    }
}
