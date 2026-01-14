package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.core.config.GDEngineConfig;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class SetupDialog extends BaseDialog {

	private final VisTextField pathField;
	private final Runnable onSuccess;

	public SetupDialog(Runnable onSuccess) {
		super("Welcome to GDEngine");
		this.onSuccess = onSuccess;

		getContentTable().add(new VisLabel("Initialize Engine Workspace")).padBottom(20).row();
		getContentTable().add(new VisLabel("Engine Root Directory:")).left().row();

		pathField = new VisTextField(GDEngineConfig.getRecommendedRoot());
		getContentTable().add(pathField).width(400).padBottom(20).row();

		VisTextButton btnConfirm = new VisTextButton("Initialize 和 Enter");
		btnConfirm.setColor(Color.CYAN);
		btnConfirm.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String path = pathField.getText().trim();
				if (path.isEmpty()) return;

				try {
					// 1. 初始化配置
					GDEngineConfig.initialize(path);

					// 2. 绑定到 Gd
					Gd.engineConfig = GDEngineConfig.getInstance();

					// 3. 回调跳转
					fadeOut();
					if (onSuccess != null) onSuccess.run();
				} catch (Exception e) {
					// 简单容错
					pathField.setText("Error creating dir!");
				}
			}
		});

		getContentTable().add(btnConfirm).growX().height(50);
		pack();
		centerWindow();
	}
}
