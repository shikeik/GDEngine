package com.goldsprite.gdengine.ecs;

import com.goldsprite.gdengine.ecs.component.Component;
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
	// 更新类型
	int type() default SystemType.UPDATE;
	// 系统关心的组件列表 (用于自动筛选实体)
	Class<? extends Component>[] interestComponents() default {};
}
