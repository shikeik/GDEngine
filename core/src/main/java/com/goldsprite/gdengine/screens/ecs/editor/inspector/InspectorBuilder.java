package com.goldsprite.gdengine.screens.ecs.editor.inspector;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.annotations.*;
import com.goldsprite.gdengine.ui.input.SmartInput;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class InspectorBuilder {

	public static void build(VisTable container, Object target) {
		if (target == null) return;

//		container.debugAll();

		Class<?> currentClass = target.getClass();

		// 1. 收集继承链 (从 Child -> Parent)
		Array<Class<?>> hierarchy = new Array<>();
		while (currentClass != null && currentClass != Object.class && currentClass != com.goldsprite.gdengine.ecs.component.Component.class) {
			hierarchy.add(currentClass);
			currentClass = currentClass.getSuperclass();
		}
		// 反转为 Parent -> Child 顺序
		hierarchy.reverse();

		// 2. 依次绘制
		for (int i = 0; i < hierarchy.size; i++) {
			Class<?> clazz = hierarchy.get(i);

			// 如果不是第一个类，添加分割线和
			if (i > 0) {
				container.addSeparator();
			}

			// 使用 getDeclaredFields 获取当前类的所有字段 (包含 private)
			Field[] fields = clazz.getDeclaredFields();

			for (Field field : fields) {
				// ... (原有字段绘制逻辑不变)
				// 1. 黑名单过滤
				if (field.isAnnotationPresent(Hide.class)) continue;

				// 2. 权限检查
				int mod = field.getModifiers();
				boolean isPublic = Modifier.isPublic(mod);
				boolean show = isPublic || field.isAnnotationPresent(Show.class);

				if (!show) continue; // 既不是 public 也没加 @Show

				// 允许访问 private
				field.setAccessible(true);

				// 3. 装饰器：Header
				if (field.isAnnotationPresent(Header.class)) {
					String title = field.getAnnotation(Header.class).value();
					VisLabel headerLbl = new VisLabel(title);
					headerLbl.setColor(Color.CYAN);
					container.add(headerLbl).colspan(2).left().padTop(10).padBottom(2).row();
				}

				// 4. [核心修复] 智能只读判定
				boolean isFinal = Modifier.isFinal(mod);
				boolean isStatic = Modifier.isStatic(mod);

				// 默认只读逻辑：如果是 static 或者 有 @ReadOnly 注解
				boolean isReadOnly = isStatic || field.isAnnotationPresent(ReadOnly.class);

				// 针对 Final 的特殊处理：
				// 如果字段是 final，且类型是"不可变值类型"(int, string等)，则必须只读，因为无法赋值。
				// 如果字段是 final，但类型是"对象"(Vector2)，我们是修改其内部属性而非引用，所以允许编辑。
				if (!isReadOnly && isFinal) {
					if (isImmutableType(field.getType())) {
						isReadOnly = true;
					}
				}

				// 5. 查找绘制器并绘制
				IPropertyDrawer drawer = DrawerRegistry.getDrawer(field.getType());
				Actor widget = drawer.draw(target, field, isReadOnly);

				if (widget != null) {
	//				widget.debug();
					// 应用 SmartInput 的额外视觉效果
					if (widget instanceof SmartInput<?> input) {
						if (isStatic) input.markAsStatic();
						if (isReadOnly) input.markAsReadOnly();

						if (field.isAnnotationPresent(Tooltip.class)) {
							input.setTooltip(field.getAnnotation(Tooltip.class).value());
						}
					}

					// [优化] 给每一行属性添加微弱的背景色，提升可读性
					// 注意：需要在 VisTable 中嵌套一个 Table 并设置背景
					// 或者简单点，我们假设外部调用者已经设置好了 Body 的背景
					// 这里我们直接 add，如果需要斑马纹，可以在外部容器层做

					// 暂时方案：只加 Padding，依靠外部容器背景
					container.add(widget).growX().padBottom(4).row();
				}
			}
		}
	}

	/**
	 * 判断类型是否为不可变值类型 (一旦 Final 就无法通过 UI 修改)
	 */
	private static boolean isImmutableType(Class<?> type) {
		return type.isPrimitive() ||
			Number.class.isAssignableFrom(type) ||
			Boolean.class.isAssignableFrom(type) ||
			Character.class.isAssignableFrom(type) ||
			type == String.class ||
			type.isEnum();
	}
}
