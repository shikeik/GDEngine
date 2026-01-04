package com.goldsprite.gdengine.screens.ecs.hub;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

/**
 * 屏幕 1: 项目管理器 (Hub)
 * 职责：展示项目列表、创建项目、记录当前选中的项目上下文
 */
public class GDEngineHubScreen extends GScreen {

    private Stage stage;
    private VisTable projectListTable;
    private NeonBatch neonBatch;

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

    @Override
    public void create() {
        stage = new Stage(getUIViewport());
        getImp().addProcessor(stage);
        neonBatch = new NeonBatch();

        initMainLayout();
        refreshList();
    }

    private void initMainLayout() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.top().pad(20);
        stage.addActor(root);

        // Top Bar
        VisTable topBar = new VisTable();
        VisLabel titleLabel = new VisLabel("GDProject Hub");
        titleLabel.setFontScale(1.5f);
        titleLabel.setColor(Color.CYAN);

        VisTextButton btnCreate = new VisTextButton("[ + New Project ]");
        btnCreate.setColor(Color.GREEN);
        btnCreate.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					stage.addActor(new CreateProjectDialog(GDEngineHubScreen.this::refreshList).fadeIn());
				}
			});

        topBar.add(titleLabel).expandX().left();
        topBar.add(btnCreate).right().height(50);
        root.add(topBar).growX().height(60).padBottom(10).row();

        // Project List Container
        projectListTable = new VisTable();
        projectListTable.top();

        VisScrollPane scrollPane = new VisScrollPane(projectListTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        VisTable container = new VisTable();
        container.setBackground("window-bg");
        container.add(scrollPane).grow().pad(5);

        root.add(container).grow();
    }

    public void refreshList() {
        projectListTable.clearChildren();
        Array<FileHandle> projects = ProjectManager.listProjects();

        if (projects.size == 0) {
            VisLabel emptyLabel = new VisLabel("No projects found.\nClick [+ New Project] to start.", Align.center);
            emptyLabel.setColor(Color.GRAY);
            projectListTable.add(emptyLabel).padTop(100);
            return;
        }

        for (FileHandle projDir : projects) {
            VisTable item = new VisTable();
            item.setBackground("button");
            item.pad(10);

            VisLabel nameLbl = new VisLabel(projDir.name());
            nameLbl.setFontScale(1.2f);
            item.add(new VisLabel("📁 ")).padRight(10);
            item.add(nameLbl).expandX().left();

            VisLabel pathLbl = new VisLabel(projDir.path());
            pathLbl.setColor(Color.GRAY);
            pathLbl.setFontScale(0.7f);
            item.add(pathLbl).right().padRight(20);

            item.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						stage.addActor(new ConfirmOpenDialog(projDir.name(), () -> {
							openProject(projDir);
						}).fadeIn());
					}
				});

            projectListTable.add(item).growX().height(80).padBottom(10).row();
        }
    }

    private void openProject(FileHandle projectDir) {
        // 1. 设置上下文
        ProjectManager.currentProject = projectDir;
        Debug.logT("Hub", "Opening project: %s", projectDir.path());

        // 2. 跳转到独立的 Editor 屏幕
        getScreenManager().setCurScreen(GDEngineEditorScreen.class, true);
    }

    @Override
    public void render0(float delta) {
        neonBatch.setProjectionMatrix(getWorldCamera().combined);
        neonBatch.begin();
        // 绘制简单的背景网格
        float w = getWorldCamera().viewportWidth;
        float h = getWorldCamera().viewportHeight;
        float cx = getWorldCamera().position.x;
        float cy = getWorldCamera().position.y;
        neonBatch.setColor(1, 1, 1, 0.05f);
        for (float x = cx - w/2; x < cx + w/2; x+=100) neonBatch.drawLine(x, cy-h/2, x, cy+h/2, 1, Color.GRAY);
        for (float y = cy - h/2; y < cy + h/2; y+=100) neonBatch.drawLine(cx-w/2, y, cx+w/2, y, 1, Color.GRAY);
        neonBatch.setColor(Color.WHITE);
        neonBatch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (neonBatch != null) neonBatch.dispose();
    }

    // =========================================================================================
    // 静态内部类：ProjectManager (逻辑核心 - 公共访问点)
    // =========================================================================================
    public static class ProjectManager {
        public static final String ROOT_DIR = "Projects";
        // 全局上下文：当前选中的项目
        public static FileHandle currentProject;

        public static Array<FileHandle> listProjects() {
            FileHandle root = Gdx.files.local(ROOT_DIR);
            if (!root.exists()) {
                root.mkdirs();
                return new Array<>();
            }
            FileHandle[] files = root.list();
            Array<FileHandle> projects = new Array<>();
            for (FileHandle f : files) {
                if (f.isDirectory()) projects.add(f);
            }
            return projects;
        }

        public static String createProject(String name) {
            if (name == null || name.trim().isEmpty()) return "Name cannot be empty.";
            if (!name.matches("[a-zA-Z0-9_]+")) return "Invalid characters.";
            FileHandle projectDir = Gdx.files.local(ROOT_DIR).child(name);
            if (projectDir.exists()) return "Project already exists.";

            try {
                projectDir.mkdirs();
                projectDir.child("Scripts").mkdirs();

                ProjectConfig config = new ProjectConfig();
                config.name = name;
                projectDir.child("project.json").writeString(new Json().prettyPrint(config), false);

                String mainCode = "package com.game;\nimport com.goldsprite.gdengine.core.scripting.IGameScriptEntry;\nimport com.goldsprite.gdengine.ecs.GameWorld;\nimport com.goldsprite.gdengine.log.Debug;\n\npublic class Main implements IGameScriptEntry {\n    @Override public void onStart(GameWorld world) {\n        Debug.log(\"Hello Script!\");\n    }\n}";
                projectDir.child("Scripts/com/game/Main.java").writeString(mainCode, false);
                return null;
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        public static class ProjectConfig {
            public String name;
            public String entryClass = "Main";
        }
    }

    // =========================================================================================
    // 弹窗逻辑
    // =========================================================================================
    public static class CreateProjectDialog extends VisDialog {
        private final VisTextField nameField;
        private final VisLabel errorLabel;
        private final Runnable onSuccess;

        public CreateProjectDialog(Runnable onSuccess) {
            super("Create Project");
            this.onSuccess = onSuccess;
            setModal(true); addCloseButton(); closeOnEscape();
            TableUtils.setSpacingDefaults(this);

            add(new VisLabel("Name:")).left();
            add(nameField = new VisTextField()).width(200).row();
            add(errorLabel = new VisLabel("")).colspan(2).row();
            errorLabel.setColor(Color.RED);

            VisTextButton createBtn = new VisTextButton("Create");
            createBtn.addListener(new ChangeListener() {
					@Override public void changed(ChangeEvent event, Actor actor) {
						String err = ProjectManager.createProject(nameField.getText());
						if (err == null) { onSuccess.run(); fadeOut(); }
						else { errorLabel.setText(err); pack(); }
					}
				});
            add(createBtn).colspan(2).right();
            pack(); centerWindow();
        }
    }

    public static class ConfirmOpenDialog extends VisDialog {
        public ConfirmOpenDialog(String name, Runnable onYes) {
            super("Confirm");
            setModal(true); addCloseButton(); closeOnEscape();
            TableUtils.setSpacingDefaults(this);
            text("Open project [" + name + "]?");
            button("Yes", true).addListener(new ChangeListener() {
					@Override public void changed(ChangeEvent event, Actor actor) { onYes.run(); }
				});
            button("No", false);
            pack(); centerWindow();
        }
    }
}
