package com.goldsprite.solofight.screens.tests.iconeditor.system;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.ui.UiNode;
import com.kotcrab.vis.ui.widget.VisTree;

public interface EditorUIProvider {
	VisTree<UiNode, EditorTarget> getHierarchyTree();
	SceneManager getSceneManager();
	CommandManager getCommandManager();
	Stage getUiStage();
	DragAndDrop getDragAndDrop();
}
