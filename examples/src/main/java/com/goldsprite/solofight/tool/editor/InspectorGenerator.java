package com.goldsprite.solofight.tool.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.solofight.core.ui.input.SmartColorInput;
import com.goldsprite.solofight.core.ui.input.SmartNumInput; // 引用现有的 SmartNumInput
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 检查器生成器 (v2.0: 工业级布局)
 * 吸取了 BioInspectorBuilder 的精华：
 * 1. Label(70px) + Content(Grow) 黄金布局
 * 2. 智能步长与防误触
 * 3. 分割线与紧凑间距
 */
public class InspectorGenerator {

    public static Table generate(Object target) {
        VisTable table = new VisTable();
        table.top().left(); // 顶部对齐
        table.defaults().padBottom(2).fillX().expandX(); // 统一行间距

        if (target == null) return table;

        Class<?> clazz = target.getClass();

        // 标题栏
        VisLabel title = new VisLabel(clazz.getSimpleName());
        title.setColor(Color.CYAN);
        title.setAlignment(Align.center);
        table.add(title).pad(5).row();
        table.add(new Separator()).padBottom(5).row();

        Field[] fields = clazz.getFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            try {
                String name = field.getName();
                Object value = field.get(target);
                Class<?> type = field.getType();

				// --- 1. Float / Int ---
				if (type == float.class || type == Float.class || type == int.class || type == Integer.class) {
					float val = (type == int.class || type == Integer.class) ? (float)((int)value) : (float)value;

					// 智能步长
					float step = 0.1f;
					String lowerName = name.toLowerCase();
					if (lowerName.contains("scale") || lowerName.contains("alpha")) step = 0.01f;
					else if (lowerName.contains("rot") || lowerName.contains("angle")) step = 0.5f;
					else if (type == int.class || type == Integer.class) step = 1.0f;

					VisTable row = new VisTable();
					row.add(new VisLabel(name)).width(70).left(); // 我们自己控制 Label

					// 【复用】直接使用无 Label 版本的 SmartNumInput
					SmartNumInput input = new SmartNumInput(val, step, newValue -> {
						// 兼容 Int 类型写回
						if (type == int.class || type == Integer.class) {
							setField(field, target, newValue.intValue());
						} else {
							setField(field, target, newValue);
						}
					});

					row.add(input).growX();
					table.add(row).row();
				}

                // --- 2. Boolean ---
                else if (type == boolean.class || type == Boolean.class) {
                    VisTable row = new VisTable();
                    row.add(new VisLabel(name)).width(70).left();

                    VisCheckBox cb = new VisCheckBox("");
                    cb.setChecked((boolean) value);
                    cb.addListener(new ChangeListener() {
							@Override public void changed(ChangeEvent event, Actor actor) {
								setField(field, target, cb.isChecked());
							}
						});
                    row.add(cb).left().expandX();
                    table.add(row).row();
                }

                // --- 3. String ---
                else if (type == String.class) {
                    VisTable row = new VisTable();
                    row.add(new VisLabel(name)).width(70).left();

                    VisTextField tf = new VisTextField((String) value);
                    tf.setAlignment(Align.center);
                    tf.addListener(new ChangeListener() {
							@Override public void changed(ChangeEvent event, Actor actor) {
								setField(field, target, tf.getText());
							}
						});
                    row.add(tf).growX();
                    table.add(row).row();
                }

                // --- 4. Color ---
                else if (type == Color.class) {
                    // SmartColorInput 本身已经有了 Label，我们可以直接用，或者改造它
                    // 这里直接用，但把它包裹在行里以保持对齐
                    // SmartColorInput(label, val, callback)
                    table.add(new SmartColorInput(name, (Color) value, c -> setField(field, target, c))).row();
                }

				// --- 5. Vector2 ---
				else if (type == Vector2.class) {
					Vector2 v = (Vector2) value;
					table.add(new VisLabel(name)).left().padTop(5).row(); 

					VisTable vRow = new VisTable();
					// X
					vRow.add(new VisLabel("X")).padRight(5);
					vRow.add(new SmartNumInput(v.x, 1f, val -> v.x = val)).growX().padRight(5);
					// Y
					vRow.add(new VisLabel("Y")).padRight(5);
					vRow.add(new SmartNumInput(v.y, 1f, val -> v.y = val)).growX();

					table.add(vRow).growX().padBottom(5).row();
				}

            } catch (IllegalAccessException e) { e.printStackTrace(); }
        }

        return table;
    }

    private static void setField(Field field, Object target, Object value) {
        try { field.set(target, value); } catch (Exception e) { e.printStackTrace(); }
    }
}
