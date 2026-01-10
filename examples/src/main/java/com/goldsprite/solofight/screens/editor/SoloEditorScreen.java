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

import com.badlogic.gdx.utils.Timer;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.Gdx;

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
        
        runSelfCheck();
    }

    private void runSelfCheck() {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                Debug.logT("TEST", "Starting Editor Self-Check...");
                
                // Load dummy texture
                Texture tex = new Texture(Gdx.files.internal("libgdx.png"));
                TextureRegion region = new TextureRegion(tex);
                Debug.logT("TEST", "Loaded Texture: libgdx.png");
                
                // 1. Create a test entity (Hero)
                GObject hero = new GObject("Hero");
                hero.transform.position.set(0, 0);
                SpriteComponent sprite = hero.addComponent(SpriteComponent.class);
                sprite.region = region;
                sprite.width = 100;
                sprite.height = 100;
                Debug.logT("TEST", "Created Entity: Hero at (0,0)");
                
                // 2. Create another entity (Enemy)
                GObject enemy = new GObject("Enemy");
                enemy.transform.position.set(200, 100);
                SpriteComponent sprite2 = enemy.addComponent(SpriteComponent.class);
                sprite2.region = region;
                sprite2.color.set(1, 0, 0, 1); // Red
                sprite2.width = 80;
                sprite2.height = 80;
                Debug.logT("TEST", "Created Entity: Enemy at (200,100) [Red]");
                
                // 3. Select Hero (Triggers Inspector and Gizmos)
                context.setSelection(hero);
                Debug.logT("TEST", "Selected Entity: " + context.getSelection().getName());
                
                // 4. Verify Panels
                if (hierarchyPanel != null) Debug.logT("TEST", "HierarchyPanel initialized");
                if (inspectorPanel != null) Debug.logT("TEST", "InspectorPanel initialized");
                if (sceneViewPanel != null) Debug.logT("TEST", "SceneViewPanel initialized");
                if (gameViewPanel != null) Debug.logT("TEST", "GameViewPanel initialized");
                
                // 5. Position Game Camera to see entities
                if (context.gameWorld.worldCamera != null) {
                    context.gameWorld.worldCamera.position.set(100, 50, 0);
                    context.gameWorld.worldCamera.update();
                    Debug.logT("TEST", "Adjusted World Camera Position");
                }
                
                Debug.logT("TEST", "Self-Check Completed.");
            }
        }, 1.0f); // Delay 1s to ensure UI is ready
    }

    private void initPanels() {
        hierarchyPanel = new HierarchyPanel(skin, context);
        fileTreePanel = new FileTreePanel(skin, context);
        inspectorPanel = new InspectorPanel(skin, context);
        historyPanel = new HistoryPanel(skin, context);
        sceneViewPanel = new SceneViewPanel(skin, context);
        gameViewPanel = new GameViewPanel(skin, context);
        consolePanel = new ConsolePanel(skin, context);

        // Layout Configuration
        float w = getUIViewport().getWorldWidth();
        float h = getUIViewport().getWorldHeight();
        
        float leftWidth = 300;
        float rightWidth = 300;
        float bottomHeight = 200;
        float toolbarHeight = 40;
        
        float centerW = w - leftWidth - rightWidth;
        float centerH = h - bottomHeight - toolbarHeight;
        
        // Left Dock
        addWindow(fileTreePanel, 0, 0, leftWidth, h / 2);
        addWindow(hierarchyPanel, 0, h / 2, leftWidth, h / 2);
        
        // Right Dock
        addWindow(historyPanel, w - rightWidth, 0, rightWidth, 200);
        addWindow(inspectorPanel, w - rightWidth, 200, rightWidth, h - 200);
        
        // Bottom Dock
        addWindow(consolePanel, leftWidth, 0, centerW, bottomHeight);
        
        // Center Split (Scene & Game)
        float splitW = centerW / 2;
        addWindow(sceneViewPanel, leftWidth, bottomHeight, splitW, centerH);
        addWindow(gameViewPanel, leftWidth + splitW, bottomHeight, splitW, centerH);
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
