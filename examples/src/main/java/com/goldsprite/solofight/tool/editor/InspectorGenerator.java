package com.goldsprite.solofight.tool.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gameframeworks.ecs.component.Component;
import com.goldsprite.gameframeworks.ecs.component.TransformComponent;
import com.goldsprite.gameframeworks.ecs.entity.GObject;
import com.goldsprite.solofight.core.ui.input.SmartColorInput;
import com.goldsprite.solofight.core.ui.input.SmartNumInput;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * 检查器生成器 (v2.0: 实体组件堆栈版)
 */
public class InspectorGenerator {

    /**
     * [主入口] 生成 GObject 的完整属性面板
     */
    public static Table generateGObjectInspector(GObject entity) {
        VisTable mainTable = new VisTable();
        mainTable.top().left();
        mainTable.defaults().growX().padBottom(5);

        if (entity == null) return mainTable;

        // 1. 实体头部 (Active, Name, Tag...)
        mainTable.add(buildEntityHeader(entity)).row();
        mainTable.add(new Separator()).padBottom(10).row();

        // 2. 组件堆栈
        // 遍历所有组件 (LinkedHashMap 保证 Transform 在第一个)
        for (List<Component> list : entity.getComponentsMap().values()) {
            for (Component comp : list) {
                mainTable.add(buildComponentCard(comp, mainTable)).row();
            }
        }

        return mainTable;
    }

    // --- 实体头构建 ---
    private static Table buildEntityHeader(GObject entity) {
        VisTable table = new VisTable();
        table.defaults().pad(2);

        // Row 1: [Check] [NameField]
        VisCheckBox activeCheck = new VisCheckBox("", entity.isActive());
        activeCheck.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					entity.setActive(activeCheck.isChecked());
				}
			});

        VisTextField nameField = new VisTextField(entity.getName());
        nameField.setMessageText("Entity Name");
        nameField.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					entity.setName(nameField.getText());
				}
			});

        table.add(activeCheck);
        table.add(nameField).growX().row();

        // Row 2: Tag & Layer
        VisTable row2 = new VisTable();

        row2.add(new VisLabel("Tag:")).padRight(5);
        VisTextField tagField = new VisTextField(entity.getTag());
        tagField.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					entity.setTag(tagField.getText());
				}
			});
        row2.add(tagField).growX().padRight(10);

        row2.add(new VisLabel("Layer:")).padRight(5);
        // int 类型 SmartNumInput
        SmartNumInput layerInput = new SmartNumInput(entity.getLayer(), 1f, val -> entity.setLayer(val.intValue()));
        row2.add(layerInput).width(60);

        table.add(row2).growX().row();

        return table;
    }

    // --- 组件卡片构建 ---
    private static Table buildComponentCard(Component comp, VisTable parentContainer) {
        VisTable card = new VisTable();
        card.setBackground("button"); // 给个背景区分
        card.pad(5);

        // 1. 标题栏
        VisTable header = new VisTable();

        // 组件开关 (Toggle)
        VisCheckBox enableCheck = new VisCheckBox(comp.getClass().getSimpleName());
        enableCheck.setChecked(comp.isEnable());
        enableCheck.getLabel().setColor(Color.ORANGE); // 高亮类名
        enableCheck.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					comp.setEnable(enableCheck.isChecked());
				}
			});
        header.add(enableCheck).left().expandX();

        // 移除按钮 [X] (Transform 除外)
        if (!(comp instanceof TransformComponent)) {
            VisTextButton removeBtn = new VisTextButton("X");
            removeBtn.setColor(Color.RED);
            removeBtn.addListener(new ClickListener() {
					@Override public void clicked(InputEvent event, float x, float y) {
						// 逻辑移除
						comp.getGObject().removeComponent(comp); // 解绑
						comp.destroy(); // 标记销毁
						// UI 移除
						card.remove(); 
						parentContainer.pack(); // 刷新布局
					}
				});
            header.add(removeBtn).size(20);
        }

        card.add(header).growX().padBottom(5).row();
        card.add(new Separator()).growX().padBottom(5).row();

        // 2. 属性内容 (反射生成)
        Table body = buildReflectedFields(comp);
        card.add(body).growX();

        return card;
    }

    // --- 属性反射逻辑 (原 generate 方法改造) ---
    private static Table buildReflectedFields(Object target) {
        VisTable table = new VisTable();
        table.defaults().padBottom(2).fillX().expandX();

        Class<?> clazz = target.getClass();
        Field[] fields = clazz.getFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;

            try {
                String name = field.getName();
                Object value = field.get(target);
                Class<?> type = field.getType();

                // 1. Float / Int
                if (type == float.class || type == Float.class || type == int.class || type == Integer.class) {
                    float val = (type == int.class || type == Integer.class) ? (float)((int)value) : (float)value;
                    float step = 0.1f;
                    String lowerName = name.toLowerCase();
                    if (lowerName.contains("scale") || lowerName.contains("alpha")) step = 0.01f;
                    else if (lowerName.contains("rot") || lowerName.contains("angle")) step = 0.5f;
                    else if (lowerName.equals("x") || lowerName.equals("y") || lowerName.equals("width") || lowerName.equals("height")) step = 1.0f;
                    else if (type == int.class || type == Integer.class) step = 1.0f;

                    VisTable row = new VisTable();
                    row.add(new VisLabel(name)).width(70).left();
                    row.add(new SmartNumInput(val, step, newValue -> {
						if (type == int.class || type == Integer.class) setField(field, target, newValue.intValue());
						else setField(field, target, newValue);
                    })).growX();
                    table.add(row).row();
                }
                // 2. Boolean
                else if (type == boolean.class || type == Boolean.class) {
                    VisTable row = new VisTable();
                    row.add(new VisLabel(name)).width(70).left();
                    VisCheckBox cb = new VisCheckBox("");
                    cb.setChecked((boolean) value);
                    cb.addListener(new ChangeListener() {
							@Override public void changed(ChangeEvent event, Actor actor) { setField(field, target, cb.isChecked()); }
						});
                    row.add(cb).left().expandX();
                    table.add(row).row();
                }
                // 3. String
                else if (type == String.class) {
                    VisTable row = new VisTable();
                    row.add(new VisLabel(name)).width(70).left();
                    VisTextField tf = new VisTextField((String) value);
                    tf.setAlignment(Align.center);
                    tf.addListener(new ChangeListener() {
							@Override public void changed(ChangeEvent event, Actor actor) { setField(field, target, tf.getText()); }
						});
                    row.add(tf).growX();
                    table.add(row).row();
                }
                // 4. Color
                else if (type == Color.class) {
                    VisTable row = new VisTable();
                    row.add(new VisLabel(name)).width(70).left();
                    row.add(new SmartColorInput(name, (Color) value, c -> setField(field, target, c))).growX();
                    table.add(row).row();
                }
                // 5. Vector2
                else if (type == Vector2.class) {
                    Vector2 v = (Vector2) value;
                    table.add(new VisLabel(name)).left().padTop(5).row(); 
                    VisTable vRow = new VisTable();
                    vRow.add(new VisLabel("X")).padRight(5);
                    vRow.add(new SmartNumInput(v.x, 1f, val -> v.x = val)).growX().padRight(5);
                    vRow.add(new VisLabel("Y")).padRight(5);
                    vRow.add(new SmartNumInput(v.y, 1f, val -> v.y = val)).growX();
                    table.add(vRow).growX().padBottom(5).row();
                }
                // 6. [新增] Enum
                else if (type.isEnum()) {
                    VisTable row = new VisTable();
                    row.add(new VisLabel(name)).width(70).left();

                    VisSelectBox<Object> sb = new VisSelectBox<>();
                    sb.setItems(type.getEnumConstants());
                    sb.setSelected(value);
                    sb.addListener(new ChangeListener() {
							@Override public void changed(ChangeEvent event, Actor actor) {
								setField(field, target, sb.getSelected());
							}
						});

                    row.add(sb).growX();
                    table.add(row).row();
                }

            } catch (IllegalAccessException e) { e.printStackTrace(); }
        }
        return table;
    }

    private static void setField(Field field, Object target, Object value) {
        try { field.set(target, value); } catch (Exception e) { e.printStackTrace(); }
    }
}
