package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisDialog;

public class BaseDialog extends VisDialog {
	public BaseDialog(String title) {
		super(title);
		setModal(true);
		addCloseButton();
		closeOnEscape();
		TableUtils.setSpacingDefaults(this);
	}

	// 方便子类显示
	public VisDialog show(Stage stage) {
		pack();
		centerWindow();
		stage.addActor(this.fadeIn());
		return this;
	}
}
