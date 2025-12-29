package com.goldsprite.solofight.refactor.ecs.component;

/**
 * 组件基类
 */
public class Component implements IComponent {
	public final ComponentField fields = new ComponentField();

	public Component(){
		initIComponent();
	}

	@Override
	public ComponentField getCompFields() {
		return fields;
	}

	@Override
	public String toString() {
		String gobjectName = getGObject() == null ? "null" : getGObject().getName();
		return String.format("%s.%s:%d", gobjectName, getName(), getRunnableGid());
	}
}

