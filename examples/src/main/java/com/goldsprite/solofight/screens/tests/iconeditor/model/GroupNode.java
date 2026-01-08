package com.goldsprite.solofight.screens.tests.iconeditor.model;

import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class GroupNode extends BaseNode {
    public GroupNode(String name) { super(name); }
    public GroupNode() { super("Group"); }
    @Override public String getTypeName() { return "Group"; }
    @Override public void render(NeonBatch batch) {}
}
