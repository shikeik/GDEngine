package com.goldsprite.solofight.screens.tests.iconeditor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.solofight.screens.tests.IconEditorDemo;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

public class UiNode extends Tree.Node<UiNode, EditorTarget, VisTable> {
    
    public enum DropState { NONE, INSERT_ABOVE, INSERT_BELOW, REPARENT }
    public DropState dropState = DropState.NONE;
    
    public static class UiNodeTable extends VisTable {
        private UiNode node;
        private final IconEditorDemo screen;
        
        public UiNodeTable(IconEditorDemo screen) {
            this.screen = screen;
        }
        
        public void setNode(UiNode node) { this.node = node; }
        
        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (screen.getHierarchyTree() != null) {
                float targetWidth = screen.getHierarchyTree().getWidth() - getX();
                if (targetWidth > 0 && getWidth() != targetWidth) {
                    setWidth(targetWidth);
                    invalidate(); 
                    validate();
                }
            }
            super.draw(batch, parentAlpha);
            
            if (node != null && node.dropState != DropState.NONE) {
                com.badlogic.gdx.scenes.scene2d.utils.Drawable white = com.kotcrab.vis.ui.VisUI.getSkin().getDrawable("white");
                if (white != null) {
                    float w = getWidth();
                    float h = getHeight();
                    float x = getX();
                    float y = getY();
                    
                    Color oldColor = batch.getColor();
                    batch.setColor(Color.CYAN);
                    
                    if (node.dropState == DropState.INSERT_ABOVE) {
                        white.draw(batch, x, y + h - 2, w, 2);
                    } else if (node.dropState == DropState.INSERT_BELOW) {
                        white.draw(batch, x, y, w, 2);
                    } else if (node.dropState == DropState.REPARENT) {
                        white.draw(batch, x, y, w, 2); // bottom
                        white.draw(batch, x, y + h - 2, w, 2); // top
                        white.draw(batch, x, y, 2, h); // left
                        white.draw(batch, x + w - 2, y, 2, h); // right
                    }
                    
                    batch.setColor(oldColor);
                }
            }
        }
    }
    
    public UiNode(EditorTarget target, IconEditorDemo screen) {
        super(new UiNodeTable(screen));
        ((UiNodeTable)getActor()).setNode(this);
        setValue(target);
        
        VisTable table = getActor();
        
        // 1. 名字 Label (左侧，填满剩余空间)
        VisLabel nameLbl = new VisLabel(target.getName());
        // 左键点击 -> 选中
        nameLbl.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                screen.getSceneManager().selectNode(target);
            }
        });
        // 右键/长按 -> 菜单
        ActorGestureListener menuListener = new ActorGestureListener() {
            @Override
            public boolean longPress(Actor actor, float x, float y) {
                showMenu(); return true;
            }
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {
                if (button == Input.Buttons.RIGHT) showMenu();
            }
            
            private void showMenu() {
                PopupMenu menu = new PopupMenu();
                
                // 1. Add Shape (SubMenu)
                MenuItem addItem = new MenuItem("Add Shape");
                PopupMenu subMenu = new PopupMenu();
                for (String type : screen.getSceneManager().getShapeRegistry().keySet()) {
                    subMenu.addItem(new MenuItem(type, new ChangeListener() { 
                        @Override public void changed(ChangeEvent event, Actor actor) { 
                            screen.getSceneManager().addNode(target, type); 
                        } 
                    }));
                }
                addItem.setSubMenu(subMenu);
                menu.addItem(addItem);

                // 2. Delete (根节点不能删除)
                if (target != screen.getSceneManager().getRoot()) {
                    MenuItem delItem = new MenuItem("Delete");
                    delItem.getLabel().setColor(Color.RED);
                    delItem.addListener(new ChangeListener() {
                        @Override public void changed(ChangeEvent event, Actor actor) {
                            screen.getSceneManager().deleteNode(target);
                        }
                    });
                    menu.addItem(delItem);
                }
                
                // [修复] 坐标系转换 Screen -> Stage
                Vector2 stagePos = screen.getUiStage().screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
                menu.showMenu(screen.getUiStage(), stagePos.x, stagePos.y);
            }
        };
        nameLbl.addListener(menuListener);
        
        // [修复] 使用 expandX().fillX() 撑开宽度，实现左对齐文字，右对齐把手
        table.add(nameLbl).expandX().fillX().left().padLeft(5);
        
        // 2. 拖拽把手 (右侧)
        if (target != screen.getSceneManager().getRoot()) {
            VisLabel handle = new VisLabel("::"); 
            handle.setColor(Color.GRAY);
            
            // [修复] 增加交互反馈
            handle.addListener(new ClickListener() {
                @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    handle.setColor(Color.CYAN);
                }
                @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    handle.setColor(Color.GRAY);
                }
            });
            
            // [修复] 靠右对齐
            table.add(handle).right().padRight(10).width(20);
            
            // 注册拖拽源
            screen.getDragAndDrop().addSource(new Source(handle) {
                @Override
                public Payload dragStart(InputEvent event, float x, float y, int pointer) {
                    Payload payload = new Payload();
                    payload.setObject(target);
                    
                    VisLabel dragActor = new VisLabel(target.getName());
                    dragActor.setColor(Color.YELLOW);
                    payload.setDragActor(dragActor);
                    
                    return payload;
                }
            });
        } else {
            // 根节点占位，保持对齐
            table.add().width(20).padRight(10);
        }
        
        // 注册拖拽目标
        screen.getDragAndDrop().addTarget(new Target(table) {
            @Override
            public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
                EditorTarget dragging = (EditorTarget) payload.getObject();
                if (dragging == target) return false;
                if (isDescendant(dragging, target)) return false;
                
                float h = getActor().getHeight();
                
                if (y > h * 0.75f && target.getParent() != null) {
                    dropState = DropState.INSERT_ABOVE;
                } else if (y < h * 0.25f && target.getParent() != null) {
                    dropState = DropState.INSERT_BELOW;
                } else {
                    dropState = DropState.REPARENT;
                }
                
                return true;
            }

            @Override
            public void drop(Source source, Payload payload, float x, float y, int pointer) {
                dropState = DropState.NONE; // Reset state
                
                EditorTarget dragging = (EditorTarget) payload.getObject();
                if (dragging == target) return;

                float h = getActor().getHeight();
                SceneManager sm = screen.getSceneManager();
                
                // Insert Before (Top 25%)
                if (y > h * 0.75f && target.getParent() != null) {
                    EditorTarget parent = target.getParent();
                    int targetIndex = parent.getChildren().indexOf(target, true);
                    sm.moveNode(dragging, parent, targetIndex);
                } 
                // Insert After (Bottom 25%)
                else if (y < h * 0.25f && target.getParent() != null) {
                    EditorTarget parent = target.getParent();
                    int targetIndex = parent.getChildren().indexOf(target, true);
                    sm.moveNode(dragging, parent, targetIndex + 1);
                }
                // Reparent
                else {
                    sm.moveNode(dragging, target, -1);
                }
            }
            
            @Override
            public void reset(Source source, Payload payload) {
                dropState = DropState.NONE;
            }
        });
    }
    
    private boolean isDescendant(EditorTarget root, EditorTarget check) {
        if (root == check) return true;
        for (EditorTarget child : root.getChildren()) {
            if (isDescendant(child, check)) return true;
        }
        return false;
    }
}
