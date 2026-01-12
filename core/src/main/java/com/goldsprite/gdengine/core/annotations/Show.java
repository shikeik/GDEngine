package com.goldsprite.gdengine.core.annotations;
import java.lang.annotation.*;
/** 强制显示非 Public 字段 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
public @interface Show {}