package com.goldsprite.dockablewindow.core;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class NestingSplitPanel extends Table {
	private final Table first;
	private final Table second;

	public NestingSplitPanel(Table first, Table second, boolean isVerti, Skin skin) {
		super(skin);
		this.first = first;
		this.second = second;

//		setClip(true);

		if (isVerti) {
			add(first).grow().row();
			add(second).grow();
		} else {
			add(first).grow();
			add(second).grow();
		}
	}
}
