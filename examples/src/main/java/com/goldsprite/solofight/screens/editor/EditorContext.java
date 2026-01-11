package com.goldsprite.solofight.screens.editor;

import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.input.Event;
import com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;
import com.goldsprite.solofight.screens.editor.adapter.GObjectAdapter;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

public class EditorContext {
	public final GameWorld gameWorld;
	public final CommandManager commandManager;
	
	// IconEditor systems integration
	public final SceneManager sceneManager;
	public final GizmoSystem gizmoSystem;
	
	private GObject selection;
	public final Event<GObject> onSelectionChanged = new Event<>();

	public EditorContext() {
		this.gameWorld = GameWorld.inst();
		this.commandManager = new CommandManager();
		
		// Initialize SceneManager wrapper
		this.sceneManager = new SceneManager(commandManager) {
			@Override
			public EditorTarget getSelection() {
				GObject sel = EditorContext.this.selection;
				return sel != null ? new GObjectAdapter(sel) : null;
			}
			
			@Override
			public void selectNode(EditorTarget node) {
				if (node instanceof GObjectAdapter) {
					EditorContext.this.setSelection(((GObjectAdapter)node).getGObject());
				} else if (node == null) {
					EditorContext.this.setSelection(null);
				}
			}
		};
		
		this.gizmoSystem = new GizmoSystem(sceneManager, commandManager);
	}
	
	public GObject getSelection() {
		return selection;
	}
	
	public void setSelection(GObject selection) {
		if (this.selection != selection) {
			this.selection = selection;
			onSelectionChanged.invoke(selection);
		}
	}
}
