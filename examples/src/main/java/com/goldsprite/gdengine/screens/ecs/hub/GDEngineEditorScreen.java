package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * 引擎编辑器主界面 (Placeholder)
 * 职责：集成 CodeEditor, ProjectTree, GameView
 */
public class GDEngineEditorScreen extends GScreen {

    private Stage stage;

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

    @Override
    public void create() {
        stage = new Stage(getUIViewport());
        getImp().addProcessor(stage);

        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg"); // 深色背景
        stage.addActor(root);

        // 获取当前打开的项目信息
        String projName = "Unknown";
        if (GDEngineHubScreen.ProjectManager.currentProject != null) {
            projName = GDEngineHubScreen.ProjectManager.currentProject.name();
        }

        VisLabel title = new VisLabel("Editor: " + projName);
        title.setFontScale(2.0f);
        title.setColor(Color.CYAN);
        root.add(title).row();

        VisLabel hint = new VisLabel("Editor UI is under construction...", Align.center);
        root.add(hint).pad(20).row();

        VisTextButton backBtn = new VisTextButton("<< Back to Hub");
        backBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					// 返回 Hub (通常不需要销毁 Editor，但这里简单起见可以 pop)
					getScreenManager().popLastScreen();
				}
			});
        root.add(backBtn).padTop(50);

        Debug.logT("Editor", "Opened project context: %s", projName);
    }

    @Override
    public void render0(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
    }
}
