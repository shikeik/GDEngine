package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.ui.SmartColorInput;
import com.goldsprite.solofight.core.ui.SmartNumInput;
import com.kotcrab.vis.ui.widget.VisLabel;

public class DebugSmartInputScreen extends ExampleGScreen {

    private Stage stage;

    @Override
    public String getIntroduction() {
        return "UI 控件隔离测试\n验证样式污染问题";
    }

    @Override
    public void create() {
        stage = new Stage(getViewport());
        getImp().addProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(20);
        stage.addActor(root);

        // 1. 先添加一个正常的 SmartNumInput
        // 理论上此时它的 <> 按钮应该是正常的 VisUI 灰色样式
        root.add(new VisLabel("1. NumInput (Before ColorInput):")).left();
        root.add(new SmartNumInput("Test A", 10, 1, v -> {})).row();

        // 2. 添加 SmartColorInput
        // 它的构造函数里有一行代码修改了全局样式
        root.add(new VisLabel("2. ColorInput (The Polluter):")).left();
        root.add(new SmartColorInput("Color", Color.RED, c -> {})).row();

        // 3. 再添加一个 SmartNumInput
        // 如果存在污染，这个按钮背景会变成纯白（SmartColorInput 设置的那个 drawable）
        // 且上面的 Test A 也会瞬间变白，因为它们共享同一个 Style 对象引用
        root.add(new VisLabel("3. NumInput (After ColorInput):")).left();
        root.add(new SmartNumInput("Test B", 20, 1, v -> {})).row();
    }

    @Override
    public void render0(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if(stage != null) stage.dispose();
    }
}
