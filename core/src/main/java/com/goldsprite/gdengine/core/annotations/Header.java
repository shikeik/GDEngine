package com.goldsprite.gdengine.core.annotations;
import java.lang.annotation.*;
/** 在字段上方显示标题 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
public @interface Header {
	String value();
}