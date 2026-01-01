package com.goldsprite.solofight.screens.editor;

import com.goldsprite.gameframeworks.screens.IGScreen;
import com.goldsprite.gameframeworks.screens.basics.BaseSelectionScreen;
import com.goldsprite.solofight.screens.editor.tests.InspectorTestScreen;
import java.util.Map;

public class EditorSelectionScreen extends BaseSelectionScreen {

    @Override
    public String getIntroduction() {
        return "编辑器开发实验室\n(Editor Development Lab)";
    }

    @Override
    protected void initScreenMapping(Map<String, Class<? extends IGScreen>> map) {
        // 这里将来会有: GizmoTest, HierarchyTest, FullEditor...
        map.put("属性面板生成器 (Reflector)", InspectorTestScreen.class);
    }
}
