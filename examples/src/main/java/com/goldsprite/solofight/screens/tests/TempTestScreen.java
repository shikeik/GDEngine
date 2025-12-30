package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;

public class TempTestScreen extends ExampleGScreen {

    private ShapeRenderer shape;

    @Override
    public String getIntroduction() {
        return "UI 动画临时测试场\n请测试 DebugConsole 的抽屉动画\n用完即删";
    }

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Portrait; // 竖屏测试布局更明显
    }

    @Override
    public void create() {
        shape = new ShapeRenderer();

        // 确保 DebugUI 已初始化
        DebugUI.getInstance().initUI();

        // 打印一些测试日志
        for(int i=0; i<5; i++) {
            DebugUI.log("Test Log Entry " + i);
        }
        DebugUI.info("Test Info Monitor: Active");
    }

    @Override
    public void render0(float delta) {
        // 绘制一个简单的背景网格，方便观察 UI 是否穿透
        shape.setProjectionMatrix(getUICamera().combined);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.DARK_GRAY);
        float w = getUIViewport().getWorldWidth();
        float h = getUIViewport().getWorldHeight();
        for (int i = 0; i < w; i += 100) shape.line(i, 0, i, h);
        for (int i = 0; i < h; i += 100) shape.line(0, i, w, i);

        // 画个圈指示中心
        shape.setColor(Color.YELLOW);
        shape.circle(w/2, h/2, 50);
        shape.end();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (shape != null) shape.dispose();
    }
}
