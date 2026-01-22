package com.goldsprite.gdengine.core.annotations;
import java.lang.annotation.*;
/** 鼠标悬停提示 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
public @interface Tooltip {
	String value();
}