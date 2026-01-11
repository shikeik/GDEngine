package com.goldsprite.solofight.screens.tests.iconeditor.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class RectShape extends BaseNode {
	public float width = 100, height = 100;
	public RectShape(String name) { super(name); }
	public RectShape() { super("Rectangle"); }
	@Override public String getTypeName() { return "Rectangle"; }
	@Override public void render(NeonBatch batch) {
		batch.drawRect(x - width/2*scaleX, y - height/2*scaleY, width*scaleX, height*scaleY, rotation, 0, color, true);
	}

	@Override public boolean hitTest(float wx, float wy) {
		float dx = wx - x;
		float dy = wy - y;
		float rad = -rotation * MathUtils.degreesToRadians;
		float c = MathUtils.cos(rad);
		float s = MathUtils.sin(rad);
		float lx = dx * c - dy * s;
		float ly = dx * s + dy * c;
		return Math.abs(lx) <= width * Math.abs(scaleX) / 2 && Math.abs(ly) <= height * Math.abs(scaleY) / 2;
	}
}
