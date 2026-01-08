package com.goldsprite.solofight.screens.tests.iconeditor.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class CircleShape extends BaseNode {
    public float radius = 50;
    public CircleShape(String name) { super(name); }
    public CircleShape() { super("Circle"); }
    @Override public String getTypeName() { return "Circle"; }
    @Override public void render(NeonBatch batch) {
        batch.drawCircle(x, y, radius * Math.max(Math.abs(scaleX), Math.abs(scaleY)), 0, color, 32, true);
    }

    @Override public boolean hitTest(float wx, float wy) {
        float dx = wx - x;
        float dy = wy - y;
        float r = radius * Math.max(Math.abs(scaleX), Math.abs(scaleY));
        return dx * dx + dy * dy <= r * r;
    }
}
