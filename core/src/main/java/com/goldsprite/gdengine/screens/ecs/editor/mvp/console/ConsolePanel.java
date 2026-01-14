package com.goldsprite.gdengine.screens.ecs.editor.mvp.console;

import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.widget.IDEConsole;

// 这里暂时没有写 ConsolePresenter，因为 IDEConsole 内部已经监听了 Log 静态数据。为了架构统一，以后可以加，目前先跑起来。)
public class ConsolePanel extends EditorPanel {

    private IDEConsole consoleWidget;

    public ConsolePanel() {
        super("Console");

        consoleWidget = new IDEConsole();
        // 强制展开并去掉它的折叠按钮（因为现在是独立面板了）
        // 如果 IDEConsole 没有公开相应方法，暂时直接用，
        // 建议你修改 IDEConsole 让它支持 "嵌入模式"，不过这里先直接嵌入
        consoleWidget.setExpanded(true); 

        addContent(consoleWidget);
    }
}
