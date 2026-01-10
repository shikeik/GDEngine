package com.goldsprite.solofight.screens.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.goldsprite.dockablewindow.core.DockableWindow;
import com.goldsprite.dockablewindow.core.DockableWindowManager;
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.VisUI;
import com.goldsprite.solofight.screens.editor.panels.*;

public class SoloEditorScreen extends GScreen {
    private Stage uiStage;
    private DockableWindowManager windowManager;
    private Skin skin;
    private EditorContext context;

    // Panels
    private HierarchyPanel hierarchyPanel;
    private FileTreePanel fileTreePanel;
    private InspectorPanel inspectorPanel;
    private HistoryPanel historyPanel;
    private SceneViewPanel sceneViewPanel;
    private GameViewPanel gameViewPanel;
    private ConsolePanel consolePanel;

    @Override
    public void create() {
        uiStage = new Stage(getUIViewport());
        skin = VisUI.getSkin();
        context = new EditorContext();

        windowManager = new DockableWindowManager(uiStage);

        // Toolbar
        initToolbar();

        // Panels
        initPanels();
        
        getImp().addProcessor(uiStage);
    }

    private void initToolbar() {
        Table toolbar = new Table(skin);
        toolbar.setBackground("button"); // Simple background
        toolbar.top().left();
        
        toolbar.add(new TextButton("Save", skin)).pad(5);
        toolbar.add(new TextButton("Load", skin)).pad(5);
        toolbar.add(new TextButton("Play", skin)).pad(5);
        
        // Toolbar is fixed at top, not a dockable window
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.add(toolbar).growX().height(40).row();
        root.add().grow(); // Placeholder for rest
        
        uiStage.addActor(root);
        // Note: DockableWindows are added directly to stage, they float above root
    }

    private void initPanels() {
        hierarchyPanel = new HierarchyPanel(skin, context);
        fileTreePanel = new FileTreePanel(skin, context);
        inspectorPanel = new InspectorPanel(skin, context);
        historyPanel = new HistoryPanel(skin, context);
        sceneViewPanel = new SceneViewPanel(skin, context);
        gameViewPanel = new GameViewPanel(skin, context);
        consolePanel = new ConsolePanel(skin, context);

        // Initial positions (Simulating a layout)
        float w = getUIViewport().getWorldWidth();
        float h = getUIViewport().getWorldHeight();
        
        // Left
        addWindow(hierarchyPanel, 0, h - 400, 300, 400);
        addWindow(fileTreePanel, 0, 0, 300, h - 400);
        
        // Right
        addWindow(inspectorPanel, w - 300, h - 600, 300, 600);
        addWindow(historyPanel, w - 300, 0, 300, 200); // Adjusted height
        
        // Center
        addWindow(sceneViewPanel, 300, 200, w - 600, h - 200 - 40); // Minus toolbar
        addWindow(gameViewPanel, 350, 250, w - 700, h - 300); // Slightly offset
        
        // Bottom
        addWindow(consolePanel, 300, 0, w - 600, 200);
    }

    private void addWindow(DockableWindow window, float x, float y, float w, float h) {
        window.setPosition(x, y);
        window.setSize(w, h);
        window.registerEdgeDraggingListener(uiStage);
        uiStage.addActor(window);
        windowManager.addWindow(window);
    }

    @Override
    public void render0(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        uiStage.act(delta);
        uiStage.draw();
        
        windowManager.update(delta);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        uiStage.dispose();
    }
}
