package com.goldsprite.solofight.screens.editor.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gameframeworks.ecs.ComponentManager;
import com.goldsprite.gameframeworks.ecs.GameWorld;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;
import com.goldsprite.solofight.core.neonbatch.NeonStage;
import com.goldsprite.solofight.ecs.tests.ShapeRendererComponent;
import com.goldsprite.solofight.tool.editor.InspectorGenerator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import java.util.List;

public class InspectorTestScreen extends ExampleGScreen {

    private NeonStage uiStage;
    private GameWorld world;
    private NeonBatch neonBatch;

    private GObject testEntity;

    @Override
    public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

    @Override
    public void create() {
		autoCenterWorldCamera = false;
		worldCamera.position.set(200, 0, 0);
		worldCamera.update();
		
        neonBatch = new NeonBatch();

        // 1. 初始化 ECS 世界
        try { if (GameWorld.inst() != null) GameWorld.inst().dispose(); } catch(Exception ignored){}
        world = new GameWorld();
        world.setReferences(getUIViewport(), worldCamera);

        // 2. 创建测试实体 (用于被编辑)
        createTestEntity();

        // 3. 构建编辑器 UI
        initEditorUI();
    }

    private void createTestEntity() {
        testEntity = new GObject("Hero");
        testEntity.transform.setPosition(200, 0); // 放在视野内

        // 挂载两个组件，测试堆栈显示
        testEntity.addComponent(ShapeRendererComponent.class)
			.set(ShapeRendererComponent.ShapeType.TRIANGLE, Color.CYAN, 80f);

        // 挂载一个带自转逻辑的组件，测试 Enable 开关
        testEntity.getComponent(ShapeRendererComponent.class).rotateSpeed = 45f;
    }

    private void initEditorUI() {
        uiStage = new NeonStage(getUIViewport());
        getImp().addProcessor(uiStage);

        Table root = new Table();
        root.setFillParent(true);
        uiStage.addActor(root);

        // --- 核心布局：SplitPane ---

        // 左侧：场景视图 (这里放个 Label 占位，实际上我们会直接画在背景上)
        VisTable sceneViewPlaceholder = new VisTable();
        sceneViewPlaceholder.add(new VisLabel("Scene View Area")).top().left().pad(10);

        // 右侧：属性面板容器
        VisTable inspectorContainer = new VisTable();
        inspectorContainer.setBackground("window-bg"); // 给个深色背景

        // 生成 Inspector 内容
        Table inspectorContent = InspectorGenerator.generateGObjectInspector(testEntity);

        VisScrollPane scrollPane = new VisScrollPane(inspectorContent);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // 【关键】禁用X轴滚动，强制填满

        inspectorContainer.add(new VisLabel("Inspector")).pad(5).row();
        inspectorContainer.add(scrollPane).grow();

        // 组装 SplitPane (分界线在 70% 处)
        VisSplitPane splitPane = new VisSplitPane(sceneViewPlaceholder, inspectorContainer, false);
        splitPane.setSplitAmount(0.7f); 

        root.add(splitPane).grow();
    }

    @Override
    public void render0(float delta) {
        // 1. 驱动 ECS
        world.update(delta);

        // 2. 渲染场景 (真实的 GameWorld 渲染)
        // 这里手动调用 ShapeRendererComponent 的 draw，模拟 RenderSystem
        List<GObject> renderables = ComponentManager.getEntitiesWithComponents(ShapeRendererComponent.class);

        neonBatch.setProjectionMatrix(getWorldCamera().combined);
        neonBatch.begin();
        // 画个网格当参考
        neonBatch.setColor(Color.DARK_GRAY);
        neonBatch.drawLine(-1000, 0, 1000, 0, 2, Color.DARK_GRAY);
        neonBatch.drawLine(0, -1000, 0, 1000, 2, Color.DARK_GRAY);
        neonBatch.setColor(Color.WHITE);

        for (GObject obj : renderables) {
            ShapeRendererComponent shape = obj.getComponent(ShapeRendererComponent.class);
            if (shape != null && shape.isEnable()) {
                shape.draw(neonBatch);
            }
        }
        neonBatch.end();

        // 3. 渲染 UI (编辑器覆盖层)
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void dispose() {
        if(world!=null) world.dispose();
        if(neonBatch!=null) neonBatch.dispose();
        if(uiStage!=null) uiStage.dispose();
    }
}
