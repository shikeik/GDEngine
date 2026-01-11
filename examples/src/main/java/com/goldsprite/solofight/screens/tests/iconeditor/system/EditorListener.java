package com.goldsprite.solofight.screens.tests.iconeditor.system;

import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

public interface EditorListener {
	default void onStructureChanged() {}
	default void onSelectionChanged(EditorTarget selection) {}
}
