package com.goldsprite.gdengine.screens.ecs.editor.mvp;

import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EditorEvents {
    private static EditorEvents instance;
    public static EditorEvents inst() {
        if (instance == null) instance = new EditorEvents();
        return instance;
    }

    // --- 事件定义 ---
    // [修改] 支持任意类型的选中 (GObject, FileHandle, etc.)
    private final List<Consumer<Object>> onSelectionChanged = new ArrayList<>();
    private final List<Consumer<Void>> onStructureChanged = new ArrayList<>();
    private final List<Consumer<Void>> onPropertyChanged = new ArrayList<>();
    private final List<Consumer<Void>> onSceneLoaded = new ArrayList<>();
	private final List<Consumer<FileHandle>> onOpenFile = new ArrayList<>(); // 打开文件事件 (Project -> Editor/Code)
	private final List<Runnable> onToggleMaximizeCode = new ArrayList<>(); // [新增] 切换代码编辑器最大化

    // --- 订阅接口 ---
    public void subscribeSelection(Consumer<Object> listener) { onSelectionChanged.add(listener); }
    public void subscribeStructure(Consumer<Void> listener) { onStructureChanged.add(listener); }
    public void subscribeProperty(Consumer<Void> listener) { onPropertyChanged.add(listener); }
    public void subscribeSceneLoaded(Consumer<Void> listener) { onSceneLoaded.add(listener); }
	public void subscribeOpenFile(Consumer<FileHandle> listener) { onOpenFile.add(listener); }
	public void subscribeToggleMaximizeCode(Runnable listener) { onToggleMaximizeCode.add(listener); }

    // --- 发布接口 ---
    public void emitSelectionChanged(Object selection) { for (var l : onSelectionChanged) l.accept(selection); }

    public void emitStructureChanged() { for (var l : onStructureChanged) l.accept(null); }
    public void emitPropertyChanged() { for (var l : onPropertyChanged) l.accept(null); }
    public void emitSceneLoaded() { for (var l : onSceneLoaded) l.accept(null); }
	public void emitOpenFile(FileHandle file) { for (var l : onOpenFile) l.accept(file); }
	public void emitToggleMaximizeCode() { for (var l : onToggleMaximizeCode) l.run(); }

    public void clear() {
        onSelectionChanged.clear();
        onStructureChanged.clear();
        onPropertyChanged.clear();
        onSceneLoaded.clear();
		onToggleMaximizeCode.clear();
    }
}
