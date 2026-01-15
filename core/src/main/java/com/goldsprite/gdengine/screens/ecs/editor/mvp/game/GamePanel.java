package com.goldsprite.gdengine.screens.ecs.editor.mvp.game;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameGraphics;
import com.goldsprite.gdengine.screens.ecs.editor.EditorGameInput;
import com.goldsprite.gdengine.screens.ecs.editor.ViewTarget;
import com.goldsprite.gdengine.screens.ecs.editor.ViewWidget;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorEvents;

public class GamePanel extends EditorPanel {

	private GamePresenter presenter;
	private ViewWidget gameWidget;
	private ViewTarget renderTarget;

	public GamePanel() {
		super("Game");

		renderTarget = new ViewTarget(1280, 720);
		gameWidget = new ViewWidget(renderTarget);
		gameWidget.setDisplayMode(ViewWidget.DisplayMode.FIT); // Default

		// 补回游戏核心代理
		Gd.init(Gd.Mode.EDITOR, new EditorGameInput(gameWidget), new EditorGameGraphics(renderTarget), Gd.compiler);

		Stack stack = new Stack();
		stack.add(gameWidget);

		// Mode Selector (Overlay)
		VisTable overlay = new VisTable();
		overlay.top().right();

		VisSelectBox<String> modeBox = new VisSelectBox<>();
		modeBox.setItems("FIT", "STRETCH", "EXTEND");
		modeBox.addListener(new ChangeListener() {
			@Override public void changed(ChangeEvent event, Actor actor) {
				if(presenter != null) presenter.setViewportMode(modeBox.getSelected());
			}
		});

		VisTextButton btnMax = new VisTextButton("[ ]");
		btnMax.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				EditorEvents.inst().emitToggleMaximizeGame();
			}
		});

		overlay.add(modeBox).pad(5);
		overlay.add(btnMax).pad(5).padRight(15);
		stack.add(overlay);

		addContent(stack);
	}

	public void setPresenter(GamePresenter presenter) {
		this.presenter = presenter;
	}

	public ViewTarget getRenderTarget() { return renderTarget; }

	public void setWidgetDisplayMode(ViewWidget.DisplayMode mode) {
		gameWidget.setDisplayMode(mode);
	}

	public void dispose() {
		if(renderTarget != null) renderTarget.dispose();
	}
}
