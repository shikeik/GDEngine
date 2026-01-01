package com.goldsprite.solofight.screens.editor.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.goldsprite.gameframeworks.log.Debug;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;
import com.goldsprite.solofight.core.neonbatch.NeonStage;
import com.goldsprite.solofight.tool.editor.InspectorGenerator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.VisTable;

public class InspectorTestScreen extends ExampleGScreen {

    private NeonStage stage;
    private TestObject testObj; // 我们的小白鼠

    // --- 1. 定义一个用于测试反射的数据类 (POJO) ---
    public static class TestObject {
        public String name = "Hero";
        public boolean isBoss = false;
        public int hp = 100;
        public float speed = 5.5f;
        public Color skinColor = new Color(Color.CYAN);
        public Vector2 position = new Vector2(100, 200);

        // private 字段不应显示
        private int secret = 999;

        @Override
        public String toString() {
            return "Name:" + name + ", Boss:" + isBoss + ", HP:" + hp + 
				", Spd:" + speed + ", Pos:" + position + ", Col:" + skinColor;
        }
    }

    @Override
    public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Landscape; }

    @Override
    public void create() {
        stage = new NeonStage(getUIViewport());
        getImp().addProcessor(stage);

        testObj = new TestObject();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // --- 左侧：生成的属性面板 ---
        VisWindow window = new VisWindow("Inspector (Auto Generated)");
        window.setResizable(true);
        window.setMovable(true);

        // 核心调用：生成 UI
        Table inspectorTable = InspectorGenerator.generate(testObj);

        VisScrollPane scrollPane = new VisScrollPane(inspectorTable);
        scrollPane.setFadeScrollBars(false);
		scrollPane.setCancelTouchFocus(false);
		scrollPane.setScrollingDisabled(true, false); 
        window.add(scrollPane).grow();

        root.add(window).left().pad(50);

        // --- 右侧：验证区域 ---
        VisTable verifyArea = new VisTable();
        VisLabel lblInfo = new VisLabel(testObj.toString());

        VisTextButton btnPrint = new VisTextButton("Print Object Data");
        btnPrint.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					// 打印当前对象数据，验证反射修改是否生效
					Debug.log("Current Data: " + testObj.toString());
					lblInfo.setText(testObj.toString()); // 刷新显示
				}
			});

        verifyArea.add(new VisLabel("Try changing values on the left,\nthen click Print.")).row();
        verifyArea.add(btnPrint).pad(20).row();
        verifyArea.add(lblInfo).pad(10);

        root.add(verifyArea).expand().center();

        window.setPosition(50, 50); // 初始位置
    }

    @Override
    public void render0(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if(stage!=null) stage.dispose();
    }
}
