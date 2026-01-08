package com.goldsprite.solofight.screens.tests.iconeditor.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.ui.input.SmartColorInput;
import com.goldsprite.gdengine.ui.input.SmartNumInput;
import com.goldsprite.gdengine.ui.input.SmartSelectInput;
import com.goldsprite.gdengine.ui.input.SmartTextInput;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.solofight.screens.tests.IconEditorDemo;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.ColorChangeCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.GenericPropertyChangeCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.PropertyChangeCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.BaseNode;
import com.goldsprite.solofight.screens.tests.iconeditor.model.CircleShape;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.model.GroupNode;
import com.goldsprite.solofight.screens.tests.iconeditor.model.RectShape;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

public class Inspector {
    private VisTable container;
    private final IconEditorDemo screen;
    private final SceneManager sceneManager;
    private final CommandManager commandManager;
    private final Array<Runnable> refreshTasks = new Array<>();
    
    private final Map<Class<?>, InspectorStrategy> strategies = new HashMap<>();

    public Inspector(IconEditorDemo screen, SceneManager sm, CommandManager cm) { 
        this.screen = screen;
        this.sceneManager = sm; 
        this.commandManager = cm;
        registerDefaultStrategies();
    }
    
    private void registerDefaultStrategies() {
        InspectorStrategy transformStrategy = (inspector, target) -> {
            if (target instanceof BaseNode) {
                BaseNode n = (BaseNode) target;
                inspector.addSection("Transform");
                inspector.addFloat("X", n::getX, n::setX);
                inspector.addFloat("Y", n::getY, n::setY);
                inspector.addFloat("Rot", n::getRotation, n::setRotation);
                inspector.addFloat("Scl X", n::getScaleX, n::setScaleX);
                inspector.addFloat("Scl Y", n::getScaleY, n::setScaleY);
            }
        };
        
        strategies.put(GroupNode.class, transformStrategy);
        
        strategies.put(RectShape.class, (inspector, target) -> {
            transformStrategy.inspect(inspector, target);
            RectShape r = (RectShape) target;
            inspector.addSection("Rectangle");
            inspector.addFloat("Width", () -> r.width, v -> r.width = v);
            inspector.addFloat("Height", () -> r.height, v -> r.height = v);
            inspector.addColor("Color", () -> r.color, newColor -> r.color.set(newColor));
        });
        
        strategies.put(CircleShape.class, (inspector, target) -> {
            transformStrategy.inspect(inspector, target);
            CircleShape c = (CircleShape) target;
            inspector.addSection("Circle");
            inspector.addFloat("Radius", () -> c.radius, v -> c.radius = v);
            inspector.addColor("Color", () -> c.color, newColor -> c.color.set(newColor));
        });
    }

    public void build(VisTable table, EditorTarget target) {
        this.container = table;
        table.clearChildren();
        refreshTasks.clear();

        if (target == null) {
            table.add(new VisLabel("No Selection")).pad(10);
            return;
        }

        // Name Field using SmartTextInput
        SmartTextInput nameInput = new SmartTextInput("Name", target.getName(), newName -> {
            target.setName(newName);
            // Update Tree Label
            if (screen.getHierarchyTree() != null) {
                UiNode node = screen.getHierarchyTree().findNode(target);
                if (node != null && node.getActor().getChildren().size > 0) {
                    Actor labelActor = node.getActor().getChildren().get(0);
                    if (labelActor instanceof VisLabel) {
                        ((VisLabel) labelActor).setText(target.getName());
                    }
                }
            }
        });
        // Bind Command for Undo/Redo
        nameInput.setOnCommand((oldVal, newVal) -> {
            commandManager.execute(new GenericPropertyChangeCommand<String>("Name", oldVal, newVal, 
                v -> {
                    target.setName(v);
                    // Force refresh logic is same as above, maybe cleaner to extract
                    if (screen.getHierarchyTree() != null) {
                        UiNode node = screen.getHierarchyTree().findNode(target);
                        if (node != null && node.getActor().getChildren().size > 0) {
                            ((VisLabel)node.getActor().getChildren().get(0)).setText(v);
                        }
                    }
                }, 
                () -> refreshValues()
            ));
        });
        container.add(nameInput).growX().padBottom(2).row();

        // Type Selection using SmartSelectInput
        if (target != sceneManager.getRoot()) {
            com.badlogic.gdx.utils.Array<String> items = new com.badlogic.gdx.utils.Array<>();
            for (String s : sceneManager.getShapeRegistry().keySet()) items.add(s);
            
            SmartSelectInput<String> typeInput = new SmartSelectInput<>("Type", target.getTypeName(), items, newType -> {
                if (!newType.equals(target.getTypeName())) {
                    sceneManager.changeNodeType(target, newType);
                }
            });
            
            container.add(typeInput).growX().padBottom(2).row();
        }

        InspectorStrategy strategy = strategies.get(target.getClass());
        if (strategy != null) {
            strategy.inspect(this, target);
        }
    }

    public void addSection(String title) {
        VisLabel l = new VisLabel(title);
        l.setColor(Color.CYAN);
        container.add(l).left().padTop(10).padBottom(5).row();
    }

    public void addFloat(String label, Supplier<Float> getter, Consumer<Float> setter) {
        SmartNumInput input = new SmartNumInput(label, getter.get(), 1.0f, setter);
        
        input.setOnCommand((oldVal, newVal) -> {
            commandManager.execute(new PropertyChangeCommand(label, oldVal, newVal, setter, () -> refreshValues()));
        });

        refreshTasks.add(() -> {
            // 暂时忽略，或者你可以手动重建整个 Inspector
        });
        container.add(input).growX().padBottom(2).row();
    }

    public void addColor(String label, Supplier<Color> getter, Consumer<Color> setter) {
        SmartColorInput input = new SmartColorInput(label, getter.get(), setter);
        
        // [新增] 注册 Command 回调
        input.setOnCommand((oldVal, newVal) -> {
            commandManager.execute(new ColorChangeCommand(sceneManager.getSelection(), oldVal, newVal, () -> refreshValues()));
        });

        container.add(input).growX().padBottom(2).row();
    }

    public void refreshValues() {
        // screen.markDirty(true); // Handled by CommandManager
        if (sceneManager.getSelection() != null && container != null) {
            build(container, sceneManager.getSelection());
        }
    }
}
