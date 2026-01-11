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
import com.goldsprite.gdengine.screens.ScreenManager;
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
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Landscape;
	}

	public EditorContext getContext() {
		return context;
	}

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
		
		// MRS Tools
		toolbar.add().width(20);
		
		TextButton btnSelect = new TextButton("Select (Q)", skin, "toggle");
		TextButton btnMove = new TextButton("Move (W)", skin, "toggle");
		TextButton btnRotate = new TextButton("Rotate (E)", skin, "toggle");
		TextButton btnScale = new TextButton("Scale (R)", skin, "toggle");
		
		// Group toggle buttons
		com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup<TextButton> group = new com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup<>(btnSelect, btnMove, btnRotate, btnScale);
		group.setMaxCheckCount(1);
		group.setMinCheckCount(1);
		group.setChecked("Select (Q)");
		
		btnSelect.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { 
			context.gizmoSystem.mode = com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem.Mode.SELECT; 
		}});
		btnMove.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { 
			context.gizmoSystem.mode = com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem.Mode.MOVE; 
		}});
		btnRotate.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { 
			context.gizmoSystem.mode = com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem.Mode.ROTATE; 
		}});
		btnScale.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { 
			context.gizmoSystem.mode = com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem.Mode.SCALE; 
		}});
		
		toolbar.add(btnSelect).pad(5);
		toolbar.add(btnMove).pad(5);
		toolbar.add(btnRotate).pad(5);
		toolbar.add(btnScale).pad(5);

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
		// Start the Automation Pipeline
		EditorAutomation.start(this);
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
