package com.goldsprite.gdengine.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记该组件在编辑器模式 (EDIT) 下也会执行 update 和生命周期方法。
 * 未标记的组件仅在运行模式 (PLAY) 下执行。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExecuteInEditMode {
}
