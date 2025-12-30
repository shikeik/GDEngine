package com.goldsprite.solofight.refactor.ecs;

import com.goldsprite.solofight.refactor.ecs.component.Component;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统配置注解：声明系统关注的组件类型和更新模式
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameSystemInfo {
	// 系统关心的组件列表 (用于自动筛选实体)
	Class<? extends Component>[] interestComponents() default {};

	// 更新类型
	SystemType type() default SystemType.UPDATE;

	enum SystemType {
		UPDATE,
		FIXED_UPDATE,
		BOTH;

		public boolean has(SystemType systemType) {
			if (this == BOTH) return true;
			if (this == systemType) return true;
			return false;
		}
	}
}
